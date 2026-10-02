package com.example.core.notification

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.model.MatchEntity
import com.example.domain.model.NotificationEntity
import com.example.domain.repository.NotificationRepository
import java.util.concurrent.ConcurrentHashMap

object RoomCodeNotificationManager {

  private const val PREFS_NAME = "tournament_room_code_notifs"
  private const val KEY_PREFIX = "notified_room_code_"

  // In-memory fallback tracking for duplicate prevention across test runs or memory cache
  private val inMemoryNotifiedCodes = ConcurrentHashMap<String, String>()

  fun getNotificationKey(userId: String, matchId: String): String =
    "$KEY_PREFIX${userId}_$matchId"

  fun getStableNotificationId(matchId: String, gameCode: String): String =
    "room_code_${matchId}_${gameCode.trim()}"

  suspend fun processRoomCodeForMatch(
    userId: String,
    match: MatchEntity,
    isUserParticipant: Boolean,
    notificationRepository: NotificationRepository,
    context: Context? = null,
    sharedPreferences: SharedPreferences? = null,
    triggerAlert: Boolean = true,
  ): Boolean {
    val trimmedCode = match.gameCode.trim()
    // 1. Must have a real Room Code
    if (trimmedCode.isBlank()) {
      return false
    }

    // 2. Only users who are verified participants of that match receive the notification
    if (!isUserParticipant) {
      return false
    }

    val memoryKey = "${userId}_${match.matchId}"
    val prefsKey = getNotificationKey(userId, match.matchId)

    // 4. Duplicate prevention: atomic check-and-set
    val prefs = sharedPreferences ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastNotifiedCode = inMemoryNotifiedCodes[memoryKey] ?: prefs?.getString(prefsKey, null)

    if (lastNotifiedCode == trimmedCode) {
      // User has already been notified for this specific Room Code event
      return false
    }

    if (inMemoryNotifiedCodes.put(memoryKey, trimmedCode) == trimmedCode) {
      return false
    }
    prefs?.edit()?.putString(prefsKey, trimmedCode)?.apply()

    // 3. Create the in-app notification for Notification Center
    val matchTitle = if (match.matchNumber.isNotBlank()) {
      match.matchNumber
    } else if (match.title.isNotBlank()) {
      match.title
    } else {
      match.matchId.takeLast(4)
    }

    val notificationId = getStableNotificationId(match.matchId, trimmedCode)
    val notification = NotificationEntity(
      id = notificationId,
      notificationId = notificationId,
      userId = userId,
      title = "রুম কোড তৈরি হয়েছে",
      message = "ম্যাচ #$matchTitle এর রুম কোড: $trimmedCode. দ্রুত গেমে প্রবেশ করুন!",
      body = "ম্যাচ #$matchTitle এর রুম কোড: $trimmedCode. দ্রুত গেমে প্রবেশ করুন!",
      type = "ROOM_CODE",
      timestamp = System.currentTimeMillis(),
      createdAt = System.currentTimeMillis(),
      targetScreen = "MATCHES",
      read = false,
      isRead = false,
    )

    notificationRepository.createNotification(notification)

    // 5 & 6. Sound & Vibration
    if (triggerAlert && context != null) {
      playRoomCodeAlert(context)
    }

    return true
  }

  suspend fun processMatches(
    userId: String,
    joinedMatchIds: Set<String>,
    matches: List<MatchEntity>,
    notificationRepository: NotificationRepository,
    context: Context? = null,
    sharedPreferences: SharedPreferences? = null,
    triggerAlert: Boolean = true,
  ): Int {
    if (userId.isBlank()) return 0
    var count = 0
    matches.forEach { match ->
      val isParticipant = joinedMatchIds.contains(match.matchId)
      if (processRoomCodeForMatch(
          userId = userId,
          match = match,
          isUserParticipant = isParticipant,
          notificationRepository = notificationRepository,
          context = context,
          sharedPreferences = sharedPreferences,
          triggerAlert = triggerAlert,
        )
      ) {
        count++
      }
    }
    return count
  }

  fun playRoomCodeAlert(context: Context) {
    try {
      // 5. Sound: standard notification ringtone respecting ringer mode
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
      val ringerMode = audioManager?.ringerMode ?: android.media.AudioManager.RINGER_MODE_NORMAL
      if (ringerMode != android.media.AudioManager.RINGER_MODE_SILENT) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, soundUri)
        ringtone?.play()
      }

      // 6. Vibration: synchronized standard vibration (one-shot 300ms) with notification usage
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val effect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
          val attributes = android.os.VibrationAttributes.Builder()
            .setUsage(android.os.VibrationAttributes.USAGE_NOTIFICATION)
            .build()
          vibrator.vibrate(effect, attributes)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(300)
        }
      }
    } catch (_: Exception) {
      // Graceful fallback if device lacks hardware vibrator or sound in test/headless environment
    }
  }

  fun clearCache() {
    inMemoryNotifiedCodes.clear()
  }
}
