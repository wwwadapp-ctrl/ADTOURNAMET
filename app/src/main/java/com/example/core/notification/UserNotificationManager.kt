package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.domain.model.NotificationEntity
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object UserNotificationManager {
  private const val CHANNEL_ID = "user_notifications_channel_v2"
  private const val CHANNEL_NAME = "Account & Game Alerts"
  private const val PREFS_NAME = "user_notifications_prefs"
  private const val KEY_PREFIX = "notified_id_"

  private var listenerJob: Job? = null

  fun startListening(
    context: Context,
    userId: String,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope
  ) {
    if (userId.isBlank()) return
    
    // Stop existing listener if any
    stopListening()

    listenerJob = scope.launch(Dispatchers.IO) {
      notificationRepository.getNotifications(userId, 20).collectLatest { resource ->
        if (resource is com.example.core.error.Resource.Success) {
          val notifications = resource.data
          val unreadNotifications = notifications.filter { !it.effectiveRead }
          
          unreadNotifications.forEach { notif ->
            if (shouldNotify(context, notif.effectiveId)) {
              showSystemNotification(context, notif)
              markAsNotified(context, notif.effectiveId)
            }
          }
        }
      }
    }
  }

  fun stopListening() {
    listenerJob?.cancel()
    listenerJob = null
  }

  private fun shouldNotify(context: Context, notificationId: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return !prefs.contains("$KEY_PREFIX$notificationId")
  }

  private fun markAsNotified(context: Context, notificationId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean("$KEY_PREFIX$notificationId", true).apply()
  }

  fun playNotificationSoundAndVibrate(context: Context) {
    try {
      // 1. Audio: Play default notification ringtone respecting ringer mode
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
      if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, defaultSoundUri)
        ringtone?.play()
      }

      // 2. Vibration: Synchronized waveform vibration (0, 300, 200, 300)
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      if (vibrator != null && vibrator.hasVibrator()) {
        val pattern = longArrayOf(0, 300, 200, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val effect = VibrationEffect.createWaveform(pattern, -1)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.Builder()
              .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
              .build()
            vibrator.vibrate(effect, attributes)
          } else {
            vibrator.vibrate(effect)
          }
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(pattern, -1)
        }
      }
    } catch (e: Exception) {
      com.example.core.logging.AppLogger.e("UserNotificationManager", "Failed to play sound/vibrate: ${e.message}")
    }
  }

  fun showSystemNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: String = System.currentTimeMillis().toString(),
    targetScreen: String = "notifications"
  ) {
    val entity = NotificationEntity(
      id = notificationId,
      notificationId = notificationId,
      title = title,
      message = message,
      body = message,
      targetScreen = targetScreen,
      timestamp = System.currentTimeMillis()
    )
    showSystemNotification(context, entity)
  }

  fun showSystemNotification(context: Context, notification: NotificationEntity) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "All account, wallet, and match alerts"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 300, 200, 300)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .build()
        setSound(defaultSoundUri, audioAttributes)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("navigate_to", notification.targetScreen.ifBlank { "notifications" })
    }
    
    val pendingIntent = PendingIntent.getActivity(
      context,
      notification.effectiveId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val builder = try {
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_more)
        .setContentTitle(notification.title)
        .setContentText(notification.effectiveMessage)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setVibrate(longArrayOf(0, 300, 200, 300))
        .setContentIntent(pendingIntent)
    } catch (e: Exception) {
      com.example.core.logging.AppLogger.e("UserNotificationManager", "Failed to build notification: ${e.message}")
      null
    }

    builder?.let {
      try {
        notificationManager.notify(notification.effectiveId.hashCode(), it.build())
      } catch (e: Exception) {
        com.example.core.logging.AppLogger.e("UserNotificationManager", "Failed to show notification: ${e.message}")
      }
    }

    // Direct in-app sound and vibration playback
    playNotificationSoundAndVibrate(context)
  }
}
