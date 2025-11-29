package com.animestudio.utils

import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging system for the Anime Studio app
 * Features:
 * - Color-coded console logs
 * - File-based persistent logging
 * - Tag-based filtering
 * - Performance metrics logging
 */
object Logger {
    private const val TAG = "AnimeStudio"
    private var fileLogger: FileLogger? = null
    private var isDebugEnabled = false  // Will be set by init() or configuration

    /**
     * Initialize logger with debug mode preference
     * Call this early in app lifecycle
     */
    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    fun init(logDir: File?) {
        if (logDir != null && logDir.exists()) {
            fileLogger = FileLogger(logDir)
        }
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG:$tag"

        // Console logging
        val logMessage = buildString {
            append(message)
            if (throwable != null) {
                append("\n${Log.getStackTraceString(throwable)}")
            }
        }

        when (level) {
            Level.VERBOSE -> if (isDebugEnabled) Log.v(fullTag, logMessage)
            Level.DEBUG -> if (isDebugEnabled) Log.d(fullTag, logMessage)
            Level.INFO -> Log.i(fullTag, logMessage)
            Level.WARN -> Log.w(fullTag, logMessage)
            Level.ERROR -> Log.e(fullTag, logMessage, throwable)
        }

        // File logging
        fileLogger?.log(level, fullTag, logMessage)
    }

    /**
     * Log performance metrics (time taken for an operation)
     */
    fun logPerformance(tag: String, operation: String, durationMs: Long) {
        val message = "$operation completed in ${durationMs}ms"
        i(tag, message)
    }

    /**
     * Log device information for debugging
     */
    fun logDeviceInfo() {
        val info = buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("RAM: ${getAvailableMemory()}MB\n")
            append("Cores: ${Runtime.getRuntime().availableProcessors()}")
        }
        i("Device", info)
    }

    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }

    /**
     * Get logs as string (for crash reporting)
     */
    fun getLogs(): String? {
        return fileLogger?.getLogContent()
    }

    /**
     * Clear old logs
     */
    fun clearOldLogs(maxAgeHours: Int = 24) {
        fileLogger?.clearOldLogs(maxAgeHours)
    }

    private class FileLogger(private val logDir: File) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        private val currentLogFile: File
            get() = File(logDir, "log_${System.currentTimeMillis() / 1000 / 3600}.txt")

        fun log(level: Level, tag: String, message: String) {
            try {
                val logFile = currentLogFile
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }

                val timestamp = dateFormat.format(Date())
                val logEntry = "[$timestamp] ${level.name} $tag: $message\n"

                logFile.appendText(logEntry)
            } catch (e: Exception) {
                Log.e("FileLogger", "Failed to write log", e)
            }
        }

        fun getLogContent(): String? {
            return try {
                currentLogFile.readText()
            } catch (e: Exception) {
                null
            }
        }

        fun clearOldLogs(maxAgeHours: Int) {
            try {
                val now = System.currentTimeMillis()
                val maxAge = maxAgeHours * 3600 * 1000

                logDir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAge) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("FileLogger", "Failed to clear old logs", e)
            }
        }
    }
}

