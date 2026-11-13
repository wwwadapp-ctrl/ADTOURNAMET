package com.example.ui.autoludo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.core.config.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * While the app is alive, watches the player's OWN full Auto Ludo matches:
 *  - first player (creator / first joiner) gets "your opponent is here" once, in their OWN userNotifications
 *  - both players get a phone alarm at the start time (shown even if the app is closed)
 * Writes only to the player's own userNotifications. Never touches money or matches.
 */
object AdLudoMatchWatcher {
    private var job: Job? = null
    private val scheduledStarts = mutableMapOf<String, Long>()
    private val joinedNoticeDone = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun start(context: Context, uid: String, scope: CoroutineScope) {
        if (uid.isBlank()) return
        stop()
        val app = context.applicationContext
        job = scope.launch {
            try {
                AutoLudoManager.observeMatches().collect { matches ->
                    try {
                        handle(app, uid, matches)
                    } catch (e: Throwable) {
                        android.util.Log.w("AdLudoMatchWatcher", "handle failed: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("AdLudoMatchWatcher", "watch failed: ${e.message}")
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun handle(context: Context, uid: String, matches: List<AutoLudoMatchEntity>) {
        val now = System.currentTimeMillis()
        val stillUpcoming = mutableSetOf<String>()
        for (m in matches) {
            val isP1 = m.player1Uid == uid
            val isP2 = m.player2Uid == uid
            if (!isP1 && !isP2) continue
            if (m.status != "WAITING" && m.status != "READY_COUNTDOWN") continue
            if (m.joinedPlayersCount < 2 || m.player1Uid.isBlank() || m.player2Uid.isBlank()) continue
            val start = if (m.scheduledTimeMillis > 0L) m.scheduledTimeMillis
                else AutoLudoManager.parseScheduledEpoch(m.scheduledTimeFormatted, m.createdAt)
            if (start <= now) continue

            val matchNumber = m.matchNumber.ifBlank { m.matchId.takeLast(6).uppercase() }
            val opponentName = (if (isP1) m.player2Name else m.player1Name).ifBlank { "প্রতিপক্ষ" }
            stillUpcoming.add(m.matchId)

            if (isP1) writeJoinedNoticeOnce(uid, m.matchId, matchNumber, opponentName, start)
            if (scheduledStarts[m.matchId] != start) {
                scheduleReminder(context, m.matchId, matchNumber, opponentName, start)
                scheduledStarts[m.matchId] = start
            }
        }
        // A reminder whose match is no longer upcoming (deleted or cancelled before the start): remove the alarm
        val gone = scheduledStarts.filter { (id, start) -> id !in stillUpcoming && start > now }.keys.toList()
        gone.forEach { id ->
            cancelReminder(context, id)
            scheduledStarts.remove(id)
        }
        scheduledStarts.entries.removeAll { it.value <= now }
    }

    private fun writeJoinedNoticeOnce(uid: String, matchId: String, matchNumber: String, opponentName: String, start: Long) {
        val key = "${matchId}_$uid"
        if (!joinedNoticeDone.add(key)) return
        val db = try { FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL) } catch (_: Exception) { null }
        if (db == null) {
            joinedNoticeDone.remove(key)
            return
        }
        val timeText = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(start))
        val record = AdLudoNotices.joinedRecord(matchId, matchNumber, uid, opponentName, timeText, System.currentTimeMillis())
        db.getReference("userNotifications").child(uid).child(AdLudoNotices.joinedId(matchId, uid))
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value != null) return Transaction.abort()
                    currentData.value = record
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) joinedNoticeDone.remove(key)
                }
            })
    }

    private fun pendingIntent(
        context: Context, matchId: String, matchNumber: String?, opponent: String?, create: Boolean
    ): PendingIntent? {
        val intent = Intent(context, AdLudoReminderReceiver::class.java).apply {
            action = AdLudoReminderReceiver.ACTION
            putExtra(AdLudoReminderReceiver.EXTRA_MATCH_ID, matchId)
            if (matchNumber != null) putExtra(AdLudoReminderReceiver.EXTRA_MATCH_NUMBER, matchNumber)
            if (opponent != null) putExtra(AdLudoReminderReceiver.EXTRA_OPPONENT, opponent)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, "al_rem_$matchId".hashCode(), intent, flags)
    }

    private fun scheduleReminder(context: Context, matchId: String, matchNumber: String, opponent: String, start: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, matchId, matchNumber, opponent, true) ?: return
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, pi)
        } catch (e: Exception) {
            android.util.Log.w("AdLudoMatchWatcher", "Could not set reminder: ${e.message}")
        }
    }

    private fun cancelReminder(context: Context, matchId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, matchId, null, null, false) ?: return
        am.cancel(pi)
        pi.cancel()
    }
}
