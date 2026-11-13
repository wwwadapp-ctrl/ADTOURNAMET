package com.example.ui.autoludo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.notification.UserNotificationManager
import com.example.domain.model.NotificationEntity

/** Fires at the match start time (set by AdLudoMatchWatcher) and shows the reminder, even if the app is closed. */
class AdLudoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getStringExtra(EXTRA_MATCH_ID) ?: return
        val matchNumber = intent.getStringExtra(EXTRA_MATCH_NUMBER) ?: ""
        val opponent = intent.getStringExtra(EXTRA_OPPONENT) ?: ""
        val id = "al_reminder_$matchId"
        val entity = NotificationEntity(
            id = id,
            notificationId = id,
            title = AdLudoNotices.reminderTitleBn(),
            titleEn = AdLudoNotices.reminderTitleEn(),
            message = AdLudoNotices.reminderMessageBn(matchNumber, opponent),
            messageEn = AdLudoNotices.reminderMessageEn(matchNumber, opponent),
            body = AdLudoNotices.reminderMessageBn(matchNumber, opponent),
            type = "AUTO_LUDO_REMINDER",
            timestamp = System.currentTimeMillis()
        )
        try {
            UserNotificationManager.showSystemNotification(context, entity)
        } catch (e: Throwable) {
            android.util.Log.w("AdLudoReminderReceiver", "Could not show reminder: ${e.message}")
        }
    }

    companion object {
        const val ACTION = "com.example.AD_LUDO_REMINDER"
        const val EXTRA_MATCH_ID = "al_match_id"
        const val EXTRA_MATCH_NUMBER = "al_match_number"
        const val EXTRA_OPPONENT = "al_opponent"
    }
}
