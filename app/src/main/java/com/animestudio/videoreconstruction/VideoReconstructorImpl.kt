package com.animestudio.videoreconstruction

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.animestudio.domain.FrameData
import com.animestudio.domain.Result
import com.animestudio.domain.VideoReconstructor
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Implementation of VideoReconstructor using MediaCodec and MediaMuxer
 *
 * This module handles:
 * - Video reconstruction from frames using MediaCodec
 * - Audio merging using FFmpeg
 * - Progress tracking during reconstruction
 *
 * For better quality and audio merging, FFmpeg is recommended:
 * - implementation 'com.arthenica:ffmpeg-kit-full:5.1'
 */
class VideoReconstructorImpl(
    private val context: Context
) : VideoReconstructor {

    @Volatile
    private var isCancelled = false

    override suspend fun reconstructVideo(
        frames: List<FrameData>,
        audioFile: File?,
        outputFile: File,
        frameRate: Float,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        isCancelled = false

        try {
            if (frames.isEmpty()) {
                return@withContext Result.Error("No frames to reconstruct")
            }

            // Get directory containing frames
            val firstFrameFile = frames.first().file
            val framesDir = firstFrameFile?.parentFile
                ?: return@withContext Result.Error("Frame files not found")

            // 1. Try FFmpeg for reconstruction (Preferred)
            onProgress(10) // Started
            
            val ffmpegResult = reconstructVideoWithFFmpeg(
                framesDir = framesDir,
                audioFile = audioFile,
                outputFile = outputFile,
                frameRate = frameRate.toInt(),
                onProgress = { _ -> }
            )

            if (ffmpegResult is Result.Success) {
                onProgress(100) // Completed
                return@withContext ffmpegResult
            }

            // 2. Fallback to MediaCodec if FFmpeg fails
            println("FFmpeg failed, falling back to native MediaCodec...")
            val mediaCodecOutputFile = if (audioFile != null) {
                // If we need to merge audio later, output to a temp file first
                File(outputFile.parent, "temp_video_only.mp4")
            } else {
                outputFile
            }

            val mediaCodecResult = reconstructVideoWithMediaCodec(
                frames = frames,
                outputFile = mediaCodecOutputFile,
                frameRate = frameRate,
                onProgress = { progress ->
                    // Map 0-100 to 10-90 range
                    onProgress(10 + (progress * 0.8).toInt())
                }
            )

            if (mediaCodecResult is Result.Error) {
                return@withContext mediaCodecResult
            }

            // 3. Merge Audio if needed
            if (audioFile != null && mediaCodecResult is Result.Success) {
                onProgress(90)
                println("Merging audio with MediaCodec video...")
                
                // Try to merge audio using FFmpeg
                val mergeResult = mergeAudioVideo(mediaCodecOutputFile, audioFile, outputFile)
                
                if (mergeResult is Result.Success) {
                    // Merge successful, delete temp video
                    mediaCodecOutputFile.delete()
                    onProgress(100)
                    return@withContext mergeResult
                } else {
                    println("Audio merge failed. Returning video without audio.")
                    // If merge fails, rename temp video to output file
                    if (mediaCodecOutputFile.exists()) {
                        mediaCodecOutputFile.renameTo(outputFile)
                    }
                    return@withContext Result.Success(outputFile)
                }
            } else if (mediaCodecResult is Result.Success) {
                onProgress(100)
                return@withContext mediaCodecResult
            }
            
            return@withContext Result.Error("Reconstruction failed with both FFmpeg and MediaCodec")

        } catch (e: Throwable) {
            e.printStackTrace()
            Result.Error("Failed to reconstruct video: ${e.message}", e as? Exception ?: Exception(e))
        }
    }

    /**
     * Reconstruct video using MediaCodec (Android native)
     * This is more complex but doesn't require external libraries
     */
    /**
     * Reconstruct video using MediaCodec with InputSurface (OpenGL)
     * This is the most robust way to encode video from bitmaps on Android
     */
    private fun reconstructVideoWithMediaCodec(
        frames: List<FrameData>,
        outputFile: File,
        frameRate: Float,
        onProgress: (Int) -> Unit
    ): Result<File> {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: CodeInputSurface? = null

        try {
            // Get frame dimensions from first frame
            val firstBitmap = frames.firstOrNull()?.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?: return Result.Error("Failed to load first frame")

            // Ensure dimensions are even (required by most codecs)
            val width = if (firstBitmap.width % 2 == 0) firstBitmap.width else firstBitmap.width - 1
            val height = if (firstBitmap.height % 2 == 0) firstBitmap.height else firstBitmap.height - 1
            firstBitmap.recycle()

            // Configure video format
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 6000000) // 6 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.toInt())
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            // Create encoder
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Create input surface
            inputSurface = CodeInputSurface(encoder.createInputSurface())
            inputSurface.makeCurrent()
            
            encoder.start()

            // Create muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            val timeout = 10000L

            // Process each frame
            frames.forEachIndexed { index, frameData ->
                if (isCancelled) {
                    return Result.Error("Video reconstruction cancelled")
                }

                // Drain encoder output
                var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0) {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            trackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        outputBuffer?.let {
                            it.position(bufferInfo.offset)
                            it.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, it, bufferInfo)
                        }
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
                }

                // Draw frame to input surface
                val bitmap = frameData.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                if (bitmap != null) {
                    // Draw bitmap to surface
                    inputSurface.drawBitmap(bitmap)
                    
                    // Set presentation time (in nanoseconds)
                    val presentationTimeNs = (index * 1000000000L / frameRate).toLong()
                    inputSurface.setPresentationTime(presentationTimeNs)
                    
                    // Submit frame
                    inputSurface.swapBuffers()
                    
                    bitmap.recycle()
                }

                onProgress((index + 1) * 100 / frames.size)
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain remaining output
            var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
            while (outputBufferIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                if (bufferInfo.size > 0 && outputBuffer != null) {
                    if (!muxerStarted) {
                         // Should have started by now, but just in case
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false)
                
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
                
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
            }

            return Result.Success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.Error("MediaCodec reconstruction failed: ${e.message}", e)
        } finally {
            try {
                inputSurface?.release()
                encoder?.stop()
                encoder?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    override suspend fun mergeAudioVideo(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Use FFmpeg to merge audio and video
            val success = mergeAudioVideoWithFFmpeg(videoFile, audioFile, outputFile)

            return@withContext if (success) {
                Result.Success(outputFile)
            } else {
                Result.Error("Audio/video merge failed with FFmpeg.")
            }
        } catch (e: Exception) {
            return@withContext Result.Error("Failed to merge audio and video", e)
        }
    }

    /**
     * Merge audio and video using FFmpeg
     * Requires: implementation 'com.arthenica:ffmpeg-kit-full:5.1'
     */
    private fun mergeAudioVideoWithFFmpeg(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Boolean {
        val command = "-y -i \"${videoFile.absolutePath}\" -i \"${audioFile.absolutePath}\" " +
                      "-c:v copy -c:a aac -strict experimental \"${outputFile.absolutePath}\""

        val session = FFmpegKit.execute(command)
        return ReturnCode.isSuccess(session.getReturnCode())
    }

    /**
     * Alternative: Reconstruct video using FFmpeg (recommended for production)
     */
    fun reconstructVideoWithFFmpeg(
        framesDir: File,
        audioFile: File?,
        outputFile: File,
        frameRate: Int = 30,
        onProgress: (String) -> Unit
    ): Result<File> {
        val framePattern = "${framesDir.absolutePath}/styled_frame_%05d.jpg" // Adjust pattern as needed

        val command = if (audioFile != null) {
            "-y -framerate $frameRate -i \"$framePattern\" -i \"${audioFile.absolutePath}\" " +
            "-c:v libx264 -crf 23 -pix_fmt yuv420p -c:a aac -strict experimental \"${outputFile.absolutePath}\""
        } else {
            "-y -framerate $frameRate -i \"$framePattern\" -c:v libx264 -crf 23 -pix_fmt yuv420p \"${outputFile.absolutePath}\""
        }

        // Execute synchronously to return Result<File> immediately,
        // and use the async version for progress updates if needed elsewhere.
        // For this function signature, a synchronous call is more appropriate for the return value.
        val session = FFmpegKit.execute(command)

        if (ReturnCode.isSuccess(session.getReturnCode())) {
            return Result.Success(outputFile)
        } else {
            val logs = session.allLogsAsString
            return Result.Error("FFmpeg reconstruction failed. Logs: $logs")
        }
    }

    fun cancel() {
        isCancelled = true
        FFmpegKit.cancel()
    }
    /**
     * Helper class to manage EGL context and Surface for MediaCodec input
     * Simplified version of CodecInputSurface from Bigflake/Grafika
     */
    private class CodeInputSurface(surface: android.view.Surface) {
        private var eglDisplay: android.opengl.EGLDisplay? = android.opengl.EGL14.EGL_NO_DISPLAY
        private var eglContext: android.opengl.EGLContext? = android.opengl.EGL14.EGL_NO_CONTEXT
        private var eglSurface: android.opengl.EGLSurface? = android.opengl.EGL14.EGL_NO_SURFACE
        private val surface = surface
        
        // Shader components
        private val vertexShaderCode =
            "attribute vec4 position;" +
            "attribute vec2 texCoords;" +
            "varying vec2 outTexCoords;" +
            "void main() {" +
            "  outTexCoords = texCoords;" +
            "  gl_Position = position;" +
            "}"

        private val fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D texture;" +
            "varying vec2 outTexCoords;" +
            "void main() {" +
            "  gl_FragColor = texture2D(texture, outTexCoords);" +
            "}"
            
        private var programId = 0
        private var textureId = 0
        
        // Buffers
        private val vertexBuffer: java.nio.FloatBuffer
        private val texCoordBuffer: java.nio.FloatBuffer
        
        init {
            eglSetup()
            makeCurrent() // CRITICAL: Context must be current before setting up graphics
            setupGraphics()
            
            // Full screen quad
            val vertices = floatArrayOf(
                -1.0f, -1.0f,  // Bottom left
                 1.0f, -1.0f,  // Bottom right
                -1.0f,  1.0f,  // Top left
                 1.0f,  1.0f   // Top right
            )
            
            // Texture coordinates (flipped vertically because Bitmap is top-down, OpenGL is bottom-up)
            val texCoords = floatArrayOf(
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
            )
            
            vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            vertexBuffer.put(vertices).position(0)
            
            texCoordBuffer = java.nio.ByteBuffer.allocateDirect(texCoords.size * 4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            texCoordBuffer.put(texCoords).position(0)
        }

        private fun eglSetup() {
            eglDisplay = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == android.opengl.EGL14.EGL_NO_DISPLAY) throw RuntimeException("unable to get EGL14 display")
            
            val version = IntArray(2)
            if (!android.opengl.EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("unable to initialize EGL14")
            }

            val attribList = intArrayOf(
                android.opengl.EGL14.EGL_RED_SIZE, 8,
                android.opengl.EGL14.EGL_GREEN_SIZE, 8,
                android.opengl.EGL14.EGL_BLUE_SIZE, 8,
                android.opengl.EGL14.EGL_ALPHA_SIZE, 8,
                android.opengl.EGL14.EGL_RENDERABLE_TYPE, android.opengl.EGL14.EGL_OPENGL_ES2_BIT,
                0x3142, 1, // EGL_RECORDABLE_ANDROID
                android.opengl.EGL14.EGL_NONE
            )
            
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            android.opengl.EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
            
            val attrib_list = intArrayOf(
                android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                android.opengl.EGL14.EGL_NONE
            )
            
            eglContext = android.opengl.EGL14.eglCreateContext(eglDisplay, configs[0], android.opengl.EGL14.EGL_NO_CONTEXT, attrib_list, 0)
            checkGlError("eglCreateContext")
            
            val surfaceAttribs = intArrayOf(android.opengl.EGL14.EGL_NONE)
            eglSurface = android.opengl.EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
            checkGlError("eglCreateWindowSurface")
        }

        private fun setupGraphics() {
            val vertexShader = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            
            programId = android.opengl.GLES20.glCreateProgram()
            android.opengl.GLES20.glAttachShader(programId, vertexShader)
            android.opengl.GLES20.glAttachShader(programId, fragmentShader)
            android.opengl.GLES20.glLinkProgram(programId)
            
            val linkStatus = IntArray(1)
            android.opengl.GLES20.glGetProgramiv(programId, android.opengl.GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != android.opengl.GLES20.GL_TRUE) {
                android.util.Log.e("CodeInputSurface", "Could not link program: ")
                android.util.Log.e("CodeInputSurface", android.opengl.GLES20.glGetProgramInfoLog(programId))
                android.opengl.GLES20.glDeleteProgram(programId)
                programId = 0
            }
            
            // Generate texture
            val textures = IntArray(1)
            android.opengl.GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            
            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, textureId)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_S, android.opengl.GLES20.GL_CLAMP_TO_EDGE)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_T, android.opengl.GLES20.GL_CLAMP_TO_EDGE)
            
            checkGlError("setupGraphics")
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(shader, shaderCode)
            android.opengl.GLES20.glCompileShader(shader)
            
            val compiled = IntArray(1)
            android.opengl.GLES20.glGetShaderiv(shader, android.opengl.GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                android.util.Log.e("CodeInputSurface", "Could not compile shader $type:")
                android.util.Log.e("CodeInputSurface", android.opengl.GLES20.glGetShaderInfoLog(shader))
                android.opengl.GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }

        fun makeCurrent() {
            if (!android.opengl.EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("eglMakeCurrent failed")
            }
        }

        fun swapBuffers() {
            android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        fun setPresentationTime(nsecs: Long) {
            android.opengl.EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        }

        fun drawBitmap(bitmap: android.graphics.Bitmap) {
            checkGlError("drawBitmap start")
            
            android.opengl.GLES20.glUseProgram(programId)
            android.opengl.GLES20.glViewport(0, 0, bitmap.width, bitmap.height)
            
            // Upload bitmap to texture
            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, textureId)
            android.opengl.GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            
            val positionHandle = android.opengl.GLES20.glGetAttribLocation(programId, "position")
            android.opengl.GLES20.glEnableVertexAttribArray(positionHandle)
            android.opengl.GLES20.glVertexAttribPointer(positionHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 0, vertexBuffer)
            
            val texCoordHandle = android.opengl.GLES20.glGetAttribLocation(programId, "texCoords")
            android.opengl.GLES20.glEnableVertexAttribArray(texCoordHandle)
            android.opengl.GLES20.glVertexAttribPointer(texCoordHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 0, texCoordBuffer)
            
            android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
            
            android.opengl.GLES20.glDisableVertexAttribArray(positionHandle)
            android.opengl.GLES20.glDisableVertexAttribArray(texCoordHandle)
            
            checkGlError("drawBitmap end")
        }
        
        private fun checkGlError(op: String) {
            val error = android.opengl.GLES20.glGetError()
            if (error != android.opengl.GLES20.GL_NO_ERROR) {
                android.util.Log.e("CodeInputSurface", "$op: glError $error")
                throw RuntimeException("$op: glError $error")
            }
        }

        fun release() {
            if (eglDisplay !== android.opengl.EGL14.EGL_NO_DISPLAY) {
                android.opengl.EGL14.eglMakeCurrent(eglDisplay, android.opengl.EGL14.EGL_NO_SURFACE, android.opengl.EGL14.EGL_NO_SURFACE, android.opengl.EGL14.EGL_NO_CONTEXT)
                android.opengl.EGL14.eglDestroySurface(eglDisplay, eglSurface)
                android.opengl.EGL14.eglDestroyContext(eglDisplay, eglContext)
                android.opengl.EGL14.eglReleaseThread()
                android.opengl.EGL14.eglTerminate(eglDisplay)
            }
            surface.release()
            eglDisplay = android.opengl.EGL14.EGL_NO_DISPLAY
            eglContext = android.opengl.EGL14.EGL_NO_CONTEXT
            eglSurface = android.opengl.EGL14.EGL_NO_SURFACE
        }
    }
}
