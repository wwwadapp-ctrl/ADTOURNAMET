package com.example.core.security

data class ValidationResult(
  val isValid: Boolean,
  val errorMessage: String? = null,
)

object AuthValidator {
  private val VALID_BANGLADESH_PREFIXES = setOf("013", "014", "015", "016", "017", "018", "019")
  const val ERROR_INVALID_MOBILE = "Enter a valid 11-digit Bangladeshi mobile number starting with 013 019."

  fun validateMobileNumber(phone: String): ValidationResult {
    if (phone.isEmpty()) {
      return ValidationResult(false, "Mobile number cannot be empty")
    }
    if (phone.length != 11 || !phone.all { it.isDigit() }) {
      return ValidationResult(false, ERROR_INVALID_MOBILE)
    }
    val prefix = phone.take(3)
    if (prefix !in VALID_BANGLADESH_PREFIXES) {
      return ValidationResult(false, ERROR_INVALID_MOBILE)
    }
    return ValidationResult(true)
  }

  fun normalizeMobileNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length == 11 && digits.startsWith("0")) {
      digits.substring(1)
    } else {
      digits
    }
  }

  fun validatePassword(password: String): ValidationResult {
    return when {
      password.isEmpty() -> ValidationResult(false, "Password cannot be empty")
      password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
      password.length > 64 -> ValidationResult(false, "Password must not exceed 64 characters")
      password.all { it.isWhitespace() } -> ValidationResult(false, "Password cannot consist only of whitespace")
      else -> ValidationResult(true)
    }
  }

  fun validateFullName(name: String): ValidationResult {
    val trimmed = name.trim()
    return when {
      trimmed.isEmpty() -> ValidationResult(false, "Full name cannot be empty")
      trimmed.length < 2 -> ValidationResult(false, "Full name must be at least 2 characters")
      trimmed.length > 50 -> ValidationResult(false, "Full name cannot exceed 50 characters")
      !trimmed.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '-' } ->
        ValidationResult(false, "Full name can only contain letters, spaces, dots, or hyphens")
      else -> ValidationResult(true)
    }
  }

  fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
    return when {
      confirm.isEmpty() -> ValidationResult(false, "Please confirm your password")
      password != confirm -> ValidationResult(false, "Passwords do not match")
      else -> ValidationResult(true)
    }
  }

  fun validateOtp(otp: String): ValidationResult {
    val clean = otp.filter { it.isDigit() }
    return when {
      clean.isEmpty() -> ValidationResult(false, "Please enter the 6-digit OTP")
      clean.length != 6 -> ValidationResult(false, "OTP must be exactly 6 digits")
      else -> ValidationResult(true)
    }
  }
}
