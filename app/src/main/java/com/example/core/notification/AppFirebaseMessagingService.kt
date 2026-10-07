package com.example.core.notification

import com.example.core.logging.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service for the User App.
 *
 * FCM Foundation:
 * - Safely receives FCM messages and token refreshes.
 * - Delegates incoming push payloads to UserNotificationManager for system tray, sound, and vibration.
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
    AppLogger.d(TAG, "FCM message received safely from: ${remoteMessage.from}")

    val title = remoteMessage.notification?.title
      ?: remoteMessage.data["title"]
      ?: "AD TOURNAMENT"

    val body = remoteMessage.notification?.body
      ?: remoteMessage.data["message"]
      ?: remoteMessage.data["body"]
      ?: ""

    if (body.isNotBlank() || remoteMessage.notification?.title != null) {
      val targetScreen = remoteMessage.data["targetScreen"] ?: "notifications"
      val notifId = remoteMessage.data["id"]
        ?: remoteMessage.data["notificationId"]
        ?: remoteMessage.messageId
        ?: System.currentTimeMillis().toString()

      UserNotificationManager.showSystemNotification(
        context = applicationContext,
        title = title,
        message = body,
        notificationId = notifId,
        targetScreen = targetScreen
      )
    }
  }

  companion object {
    private const val TAG = "AppFirebaseMessaging"
  }
}
