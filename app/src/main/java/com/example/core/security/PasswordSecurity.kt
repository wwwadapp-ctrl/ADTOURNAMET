package com.example.core.security

object PasswordSecurity {
  fun isPasswordValid(password: String): Boolean {
    return password.length in 6..64 && !password.all { it.isWhitespace() }
  }

  fun isValidMobileNumber(phone: String): Boolean {
    return AuthValidator.validateMobileNumber(phone).isValid
  }

  @Deprecated(
    message = "Do not use client-side SHA-256 as substitute for proper authentication. Passwords must be verified server-side by Firebase Auth.",
    replaceWith = ReplaceWith("password")
  )
  fun hashPassword(rawPassword: String): String = rawPassword
}
