package com.example.core.error

sealed class AppError(
  val userMessage: String,
  val technicalCode: String,
) {
  val message: String get() = userMessage
  data class NetworkUnavailable(
    val customMessage: String = "No internet connection. Please check your network and try again.",
  ) : AppError(customMessage, "ERR_NETWORK_UNAVAILABLE")

  data class FirebaseUnavailable(
    val customMessage: String = "Tournament service is temporarily unavailable. Please try again in a few moments.",
  ) : AppError(customMessage, "ERR_FIREBASE_UNAVAILABLE")

  data class Timeout(
    val customMessage: String = "The request timed out. Please try again.",
  ) : AppError(customMessage, "ERR_TIMEOUT")

  data class InvalidInput(
    val fieldName: String = "",
    val reason: String = "Please review the entered information and try again.",
  ) : AppError(
    if (fieldName.isNotBlank()) "Invalid $fieldName: $reason" else reason,
    "ERR_INVALID_INPUT"
  )

  data class PermissionDenied(
    val customMessage: String = "You do not have permission to perform this tournament action.",
  ) : AppError(customMessage, "ERR_PERMISSION_DENIED")

  data class AuthenticationRequired(
    val customMessage: String = "Please sign in to access tournament features.",
  ) : AppError(customMessage, "ERR_AUTH_REQUIRED")

  data class ServerError(
    val customMessage: String = "An unexpected server issue occurred. Our team has been notified.",
  ) : AppError(customMessage, "ERR_SERVER")

  data class Unknown(
    val customMessage: String = "Something went wrong. Please try again.",
  ) : AppError(customMessage, "ERR_UNKNOWN")
}
