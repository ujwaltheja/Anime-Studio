package com.animestudio.utils

import android.content.Context
import com.animestudio.config.AppConfig
import com.animestudio.domain.Result
import java.io.IOException

/**
 * Advanced error handling and recovery system
 * Provides:
 * - User-friendly error messages
 * - Automatic recovery suggestions
 * - Error categorization
 * - Crash reporting support
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"

    enum class ErrorCategory {
        MEMORY_ERROR,
        MODEL_ERROR,
        VIDEO_FORMAT_ERROR,
        GPU_ERROR,
        FILE_ERROR,
        PROCESSING_ERROR,
        UNKNOWN_ERROR
    }

    data class ErrorInfo(
        val category: ErrorCategory,
        val userMessage: String,
        val technicalMessage: String,
        val suggestions: List<String>,
        val canRetry: Boolean = true,
        val severity: ErrorSeverity = ErrorSeverity.ERROR
    )

    enum class ErrorSeverity {
        WARNING, ERROR, CRITICAL
    }

    /**
     * Handle and categorize exceptions
     */
    fun handleException(
        context: Context,
        exception: Throwable,
        operationName: String = "Unknown operation"
    ): ErrorInfo {
        Logger.e(TAG, "Exception in $operationName", exception)

        return when (exception) {
            is OutOfMemoryError -> handleOutOfMemoryError(exception)
            is IOException -> handleIOError(exception)
            is IllegalArgumentException -> handleIllegalArgumentError(exception)
            is SecurityException -> handleSecurityError(exception)
            else -> handleGenericError(exception)
        }
    }

    private fun handleOutOfMemoryError(exception: OutOfMemoryError): ErrorInfo {
        return ErrorInfo(
            category = ErrorCategory.MEMORY_ERROR,
            userMessage = AppConfig.ErrorMessages.INSUFFICIENT_MEMORY,
            technicalMessage = "OutOfMemoryError: ${exception.message}",
            suggestions = listOf(
                "Close other apps running in background",
                "Reduce video resolution or length",
                "Clear app cache",
                "Restart your device"
            ),
            canRetry = true,
            severity = ErrorSeverity.CRITICAL
        )
    }

    private fun handleIOError(exception: IOException): ErrorInfo {
        return when {
            exception.message?.contains("ENOENT") == true -> {
                ErrorInfo(
                    category = ErrorCategory.FILE_ERROR,
                    userMessage = "Could not find the video file.\nPlease select a valid video.",
                    technicalMessage = "File not found: ${exception.message}",
                    suggestions = listOf(
                        "Ensure the video file still exists",
                        "Try selecting a different video",
                        "Check file permissions"
                    ),
                    canRetry = true
                )
            }
            exception.message?.contains("Permission") == true -> {
                ErrorInfo(
                    category = ErrorCategory.FILE_ERROR,
                    userMessage = "Permission denied.\nPlease grant file access permissions.",
                    technicalMessage = "Permission error: ${exception.message}",
                    suggestions = listOf(
                        "Grant storage permission in app settings",
                        "Ensure video is in accessible location"
                    ),
                    canRetry = false
                )
            }
            else -> {
                ErrorInfo(
                    category = ErrorCategory.FILE_ERROR,
                    userMessage = "Could not read the video file.\nTry another file.",
                    technicalMessage = "IO Error: ${exception.message}",
                    suggestions = listOf(
                        "Try selecting a different video",
                        "Check if file is corrupted",
                        "Try a smaller file size"
                    ),
                    canRetry = true
                )
            }
        }
    }

    private fun handleIllegalArgumentError(exception: IllegalArgumentException): ErrorInfo {
        return when {
            exception.message?.contains("model", ignoreCase = true) == true -> {
                ErrorInfo(
                    category = ErrorCategory.MODEL_ERROR,
                    userMessage = AppConfig.ErrorMessages.MODEL_NOT_FOUND,
                    technicalMessage = "Invalid model: ${exception.message}",
                    suggestions = listOf(
                        "Ensure model files are in assets/models/",
                        "Run download_models.ps1 script",
                        "Verify model file integrity"
                    ),
                    canRetry = true
                )
            }
            else -> {
                ErrorInfo(
                    category = ErrorCategory.PROCESSING_ERROR,
                    userMessage = "Invalid configuration for processing.\nPlease try again.",
                    technicalMessage = "Illegal Argument: ${exception.message}",
                    suggestions = listOf(
                        "Check video format is supported",
                        "Try with default settings"
                    ),
                    canRetry = true
                )
            }
        }
    }

    private fun handleSecurityError(exception: SecurityException): ErrorInfo {
        return ErrorInfo(
            category = ErrorCategory.FILE_ERROR,
            userMessage = "Access denied.\nPlease grant necessary permissions.",
            technicalMessage = "Security Exception: ${exception.message}",
            suggestions = listOf(
                "Grant storage permission",
                "Grant camera permission if needed",
                "Check app permissions in Settings"
            ),
            canRetry = false
        )
    }

    private fun handleGenericError(exception: Throwable): ErrorInfo {
        return ErrorInfo(
            category = ErrorCategory.UNKNOWN_ERROR,
            userMessage = AppConfig.ErrorMessages.UNKNOWN_ERROR,
            technicalMessage = "${exception::class.simpleName}: ${exception.message}",
            suggestions = listOf(
                "Close other apps",
                "Clear cache",
                "Restart the app",
                "Check device storage space"
            ),
            canRetry = true,
            severity = ErrorSeverity.ERROR
        )
    }

    /**
     * Get recovery actions based on error
     */
    fun getRecoveryActions(errorInfo: ErrorInfo): List<RecoveryAction> {
        return buildList {
            when (errorInfo.category) {
                ErrorCategory.MEMORY_ERROR -> {
                    add(RecoveryAction.CLEAR_CACHE)
                    add(RecoveryAction.REDUCE_QUALITY)
                    add(RecoveryAction.CLOSE_APPS)
                }
                ErrorCategory.MODEL_ERROR -> {
                    add(RecoveryAction.DOWNLOAD_MODELS)
                    add(RecoveryAction.SELECT_DIFFERENT_MODEL)
                }
                ErrorCategory.GPU_ERROR -> {
                    add(RecoveryAction.DISABLE_GPU)
                    add(RecoveryAction.RETRY)
                }
                ErrorCategory.FILE_ERROR -> {
                    add(RecoveryAction.SELECT_DIFFERENT_VIDEO)
                    add(RecoveryAction.CHECK_PERMISSIONS)
                }
                else -> {
                    if (errorInfo.canRetry) {
                        add(RecoveryAction.RETRY)
                    }
                    add(RecoveryAction.CLEAR_CACHE)
                }
            }
        }
    }

    enum class RecoveryAction {
        RETRY,
        CLEAR_CACHE,
        REDUCE_QUALITY,
        CLOSE_APPS,
        DOWNLOAD_MODELS,
        SELECT_DIFFERENT_MODEL,
        SELECT_DIFFERENT_VIDEO,
        DISABLE_GPU,
        CHECK_PERMISSIONS
    }

    /**
     * Log error for crash reporting
     */
    fun logErrorForReporting(errorInfo: ErrorInfo) {
        Logger.e(TAG, buildString {
            append("Error Category: ${errorInfo.category}\n")
            append("User Message: ${errorInfo.userMessage}\n")
            append("Technical: ${errorInfo.technicalMessage}\n")
            append("Suggestions: ${errorInfo.suggestions.joinToString(", ")}")
        })
    }

    /**
     * Create detailed error report
     */
    fun createErrorReport(errorInfo: ErrorInfo, context: Context): String {
        return buildString {
            append("=== Anime Studio Error Report ===\n")
            append("Time: ${System.currentTimeMillis()}\n")
            append("Category: ${errorInfo.category}\n")
            append("Severity: ${errorInfo.severity}\n\n")

            append("=== Error Details ===\n")
            append("User Message: ${errorInfo.userMessage}\n")
            append("Technical: ${errorInfo.technicalMessage}\n\n")

            append("=== Device Info ===\n")
            append("Memory: ${PerformanceOptimizer.getMemoryStatus(context)}\n")
            append("Available: ${PerformanceOptimizer.getAvailableMemoryMB()}MB\n\n")

            append("=== Suggestions ===\n")
            errorInfo.suggestions.forEachIndexed { index, suggestion ->
                append("${index + 1}. $suggestion\n")
            }
        }
    }
}
