package com.example.core.notification

import com.example.core.logging.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service for the User App.
 *
 * FCM Foundation:
 * - Safely receives FCM messages and token refreshes.
 * - Room Code notification creation is NOT implemented here yet.
 * - Does not duplicate messages into Notification Center.
 * - Does not execute financial, match, or database write operations.
 */
@Suppress("DEPRECATION")
class AppFirebaseMessagingService : FirebaseMessagingService() {

  @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    // Safely update in-memory token state without exposing secret in logs
    FcmTokenManager.updateToken(token)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    // Foundation step: Safely handle incoming message without Room Code or duplicate injection
    AppLogger.d(TAG, "FCM message received safely from: ${remoteMessage.from}")
  }

  companion object {
    private const val TAG = "AppFirebaseMessaging"
  }
}
