package com.example.core.logging

import android.util.Log
import com.example.core.config.AppEnvironment

object AppLogger {
  private const val GLOBAL_TAG = "AdTournament"

  private val PHONE_REGEX = Regex("(\\+?\\d{1,4}[- ]?)?\\d{10}")
  private val TOKEN_REGEX = Regex("""(?i)(bearer\s+|token["':=]\s*|key["':=]\s*)([a-zA-Z0-9_.-]{16,})""")
  private val EMAIL_REGEX = Regex("""(?i)[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+""")

  fun d(tag: String, message: String) {
    if (AppEnvironment.current.isDebugLoggingEnabled) {
      Log.d("$GLOBAL_TAG:$tag", sanitize(message))
    }
  }

  fun i(tag: String, message: String) {
    Log.i("$GLOBAL_TAG:$tag", sanitize(message))
  }

  fun w(tag: String, message: String, throwable: Throwable? = null) {
    val sanitizedMsg = sanitize(message)
    if (throwable != null) {
      if (AppEnvironment.current.isDebugLoggingEnabled) {
        Log.w("$GLOBAL_TAG:$tag", sanitizedMsg, throwable)
      } else {
        Log.w("$GLOBAL_TAG:$tag", "$sanitizedMsg (Error type: ${throwable.javaClass.simpleName})")
      }
    } else {
      Log.w("$GLOBAL_TAG:$tag", sanitizedMsg)
    }
  }

  fun e(tag: String, message: String, throwable: Throwable? = null) {
    val sanitizedMsg = sanitize(message)
    if (throwable != null) {
      if (AppEnvironment.current.isDebugLoggingEnabled) {
        Log.e("$GLOBAL_TAG:$tag", sanitizedMsg, throwable)
      } else {
        Log.e("$GLOBAL_TAG:$tag", "$sanitizedMsg (Error: ${throwable.javaClass.simpleName})")
      }
    } else {
      Log.e("$GLOBAL_TAG:$tag", sanitizedMsg)
    }
  }

  fun sanitize(input: String): String {
    var result = input
    result = TOKEN_REGEX.replace(result) { matchResult ->
      val prefix = matchResult.groupValues[1]
      "$prefix[REDACTED_SECRET]"
    }
    result = PHONE_REGEX.replace(result) { matchResult ->
      val phone = matchResult.value
      if (phone.length > 4) {
        phone.take(2) + "******" + phone.takeLast(2)
      } else {
        "****"
      }
    }
    result = EMAIL_REGEX.replace(result) { matchResult ->
      val email = matchResult.value
      val parts = email.split("@")
      if (parts.size == 2 && parts[0].length > 2) {
        "${parts[0].take(2)}***@${parts[1]}"
      } else {
        "***@${parts.getOrElse(1) { "redacted" }}"
      }
    }
    return result
  }
}
