package com.example.core.notification

import com.example.core.firebase.FirebaseManager
import com.example.core.logging.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Manages FCM registration token lifecycle safely.
 *
 * Rules:
 * - Does not display token to the user.
 * - Does not hard-code tokens.
 * - Does not log sensitive token values.
 * - Persists token to Realtime Database under userFcmTokens/{uid}/{tokenId} when signed in.
 */
@Suppress("DEPRECATION")
object FcmTokenManager {
  private const val TAG = "FcmTokenManager"

  internal var isAvailableOverride: (() -> Boolean)? = null

  private val isRunningInTest: Boolean by lazy {
    try {
      android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
        android.os.Build.HARDWARE.contains("robolectric", ignoreCase = true) ||
        System.getProperty("java.vm.name")?.contains("Robolectric", ignoreCase = true) == true
    } catch (_: Exception) {
      false
    }
  }

  private fun isFirebaseAvailable(): Boolean =
    isAvailableOverride?.invoke() ?: (FirebaseManager.isAvailable() && !isRunningInTest)

  private val _currentToken = MutableStateFlow<String?>(null)
  val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

  /**
   * Safely retrieves the current registration token using FirebaseMessaging.
   * Can be invoked asynchronously or with a callback.
   */
  fun retrieveToken(onComplete: ((String?) -> Unit)? = null) {
    if (!isFirebaseAvailable()) {
      AppLogger.d(TAG, "Firebase unavailable, skipping FCM token retrieval")
      onComplete?.invoke(null)
      return
    }

    try {
      FirebaseMessaging.getInstance().token
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            val token = task.result
            if (!token.isNullOrBlank()) {
              _currentToken.value = token
              AppLogger.i(TAG, "FCM registration token retrieved successfully")
              onComplete?.invoke(token)
              CoroutineScope(Dispatchers.IO).launch {
                persistToken(token)
              }
            } else {
              AppLogger.w(TAG, "FCM token was empty or null")
              onComplete?.invoke(null)
            }
          } else {
            AppLogger.w(TAG, "FCM token task was not successful: ${task.exception?.message}")
            onComplete?.invoke(null)
          }
        }
    } catch (e: Exception) {
      AppLogger.w(TAG, "Exception during FCM token retrieval: ${e.message}")
      onComplete?.invoke(null)
    }
  }

  /**
   * Safely updates token when refreshed by Firebase.
   */
  fun updateToken(newToken: String) {
    if (newToken.isNotBlank()) {
      _currentToken.value = newToken
      AppLogger.i(TAG, "FCM token refreshed successfully")
      CoroutineScope(Dispatchers.IO).launch {
        persistToken(newToken)
      }
    }
  }

  fun persistToken(token: String, customAuth: FirebaseAuth? = null, customDb: FirebaseDatabase? = null) {
    if (token.isBlank()) return
    val auth = customAuth ?: FirebaseManager.getAuth()
    val currentUser = auth?.currentUser
    if (currentUser == null) {
      AppLogger.d(TAG, "No authenticated user, skipping FCM token persistence")
      return
    }

    val uid = currentUser.uid
    val tokenId = deriveTokenId(token)

    val db = customDb ?: FirebaseManager.getDatabase()
    if (db == null) {
      AppLogger.w(TAG, "Firebase database unavailable, skipping FCM token persistence")
      return
    }

    try {
      val ref = db.getReference("userFcmTokens").child(uid).child(tokenId)
      val record = mapOf(
        "token" to token,
        "updatedAt" to System.currentTimeMillis()
      )
      ref.setValue(record).addOnCompleteListener { task ->
        if (task.isSuccessful) {
          AppLogger.i(TAG, "FCM token persisted successfully")
        } else {
          AppLogger.w(TAG, "Failed to persist FCM token: ${task.exception?.message}")
        }
      }
    } catch (e: Exception) {
      AppLogger.w(TAG, "Exception during FCM token persistence: ${e.message}")
    }
  }

  fun deriveTokenId(token: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun getCachedToken(): String? = _currentToken.value

  /**
   * Clears token state (e.g. for testing)
   */
  fun clearForTesting() {
    _currentToken.value = null
    isAvailableOverride = null
  }
}
