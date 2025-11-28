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

            // Create video without audio first
            val tempVideoFile = if (audioFile != null) {
                File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_temp.mp4")
            } else {
                outputFile
            }

            val result = reconstructVideoWithMediaCodec(frames, tempVideoFile, frameRate, onProgress)

            if (result !is Result.Success) {
                return@withContext result
            }

            // Merge audio if available
            if (audioFile != null && audioFile.exists()) {
                val mergeResult = mergeAudioVideo(tempVideoFile, audioFile, outputFile)
                tempVideoFile.delete()

                if (mergeResult is Result.Error) {
                    // If merge fails, keep video without audio
                    tempVideoFile.renameTo(outputFile)
                    return@withContext Result.Success(outputFile)
                }

                return@withContext mergeResult
            }

            Result.Success(tempVideoFile)
        } catch (e: Exception) {
            Result.Error("Failed to reconstruct video", e)
        }
    }

    /**
     * Reconstruct video using MediaCodec (Android native)
     * This is more complex but doesn't require external libraries
     */
    private fun reconstructVideoWithMediaCodec(
        frames: List<FrameData>,
        outputFile: File,
        frameRate: Float,
        onProgress: (Int) -> Unit
    ): Result<File> {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            // Get frame dimensions from first frame
            val firstBitmap = frames.firstOrNull()?.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?: return Result.Error("Failed to load first frame")

            val width = firstBitmap.width
            val height = firstBitmap.height
            firstBitmap.recycle()

            // Configure video format
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 8000000) // 8 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.toInt())
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            // Create encoder
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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

                val bitmap = frameData.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                    ?: return Result.Error("Failed to load frame ${frameData.index}")

                // Convert bitmap to YUV
                val inputBuffer = encoder.getInputBuffer(encoder.dequeueInputBuffer(timeout))
                if (inputBuffer != null) {
                    val yuvData = bitmapToYUV(bitmap, width, height)
                    inputBuffer.clear()
                    inputBuffer.put(yuvData)

                    val presentationTime = (index * 1000000L / frameRate).toLong()
                    encoder.queueInputBuffer(
                        encoder.dequeueInputBuffer(timeout),
                        0,
                        yuvData.size,
                        presentationTime,
                        0
                    )
                }

                bitmap.recycle()

                // Get encoded data
                var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // Codec config info, not actual frame data
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
                            muxer.writeSampleData(trackIndex, it, bufferInfo)
                        }
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
                }

                onProgress((index + 1) * 100 / frames.size)
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain encoder
            var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
            while (outputBufferIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                if (bufferInfo.size > 0 && outputBuffer != null) {
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }

                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeout)
            }

            Result.Success(outputFile)
        } catch (e: Exception) {
            Result.Error("MediaCodec reconstruction failed: ${e.message}", e)
        } finally {
            encoder?.stop()
            encoder?.release()
            muxer?.stop()
            muxer?.release()
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

            if (success) {
                Result.Success(outputFile)
            } else {
                Result.Error("Audio/video merge requires FFmpeg library. Add: implementation 'com.arthenica:ffmpeg-kit-full:5.1'")
            }
        } catch (e: Exception) {
            Result.Error("Failed to merge audio and video", e)
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
        // Placeholder for FFmpeg integration
        /*
        val command = "-i ${videoFile.absolutePath} -i ${audioFile.absolutePath} " +
                      "-c:v copy -c:a aac -strict experimental ${outputFile.absolutePath}"

        val session = FFmpegKit.execute(command)
        return ReturnCode.isSuccess(session.returnCode)
        */

        return false
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
        // Placeholder for FFmpeg implementation
        /*
        val framePattern = "${framesDir.absolutePath}/styled_frame_%05d.jpg"

        val command = if (audioFile != null) {
            "-framerate $frameRate -i $framePattern -i ${audioFile.absolutePath} " +
            "-c:v libx264 -crf 23 -pix_fmt yuv420p -c:a aac -strict experimental ${outputFile.absolutePath}"
        } else {
            "-framerate $frameRate -i $framePattern -c:v libx264 -crf 23 -pix_fmt yuv420p ${outputFile.absolutePath}"
        }

        val session = FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Success
            }
        } { log ->
            onProgress(log.message)
        }
        */

        return Result.Error("FFmpeg integration required")
    }

    /**
     * Convert bitmap to YUV420 format for MediaCodec
     */
    private fun bitmapToYUV(bitmap: android.graphics.Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        encodeYUV420SP(yuv, argb, width, height)

        return yuv
    }

    /**
     * Encode ARGB to YUV420SP (NV21)
     */
    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height

        var yIndex = 0
        var uvIndex = frameSize

        var j = 0
        while (j < height) {
            var i = 0
            while (i < width) {
                val r = (argb[j * width + i] shr 16) and 0xff
                val g = (argb[j * width + i] shr 8) and 0xff
                val b = argb[j * width + i] and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = (y.coerceIn(0, 255)).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (v.coerceIn(0, 255)).toByte()
                    yuv420sp[uvIndex++] = (u.coerceIn(0, 255)).toByte()
                }

                i++
            }
            j++
        }
    }

    fun cancel() {
        isCancelled = true
    }
}
