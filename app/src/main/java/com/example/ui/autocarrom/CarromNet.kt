package com.example.ui.autocarrom

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Network helpers for Auto Carrom:
 *  - CarromClock: the SERVER time on every phone (Firebase .info/serverTimeOffset), so both phones count
 *    the 20-second turn the same way even when their own clocks differ.
 *  - presence: each player writes "last seen" (server time) every 5 s while in the game; Firebase writes 0
 *    by itself when the connection drops. The other phone then knows the player is offline and waits longer.
 */
object CarromClock {
    @Volatile private var offset = 0L
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        try {
            AutoCarromManager.getDb()?.getReference(".info/serverTimeOffset")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { offset = (snapshot.value as? Number)?.toLong() ?: 0L }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (_: Exception) { started = false }
    }

    /** Server time in milliseconds. */
    fun now(): Long = System.currentTimeMillis() + offset
}

object CarromPresence {
    const val ONLINE_MS = 12_000L     // seen within 12 s = online
    const val OFFLINE_GRACE_MS = 30_000L

    private fun ref(matchId: String, uid: String) =
        AutoCarromManager.getDb()?.getReference(AutoCarromManager.NODE_MATCHES)?.child(matchId)?.child("presence")?.child(uid)

    /** Marks me online now and sets the automatic "offline" write for when my connection drops. */
    fun beat(matchId: String, uid: String) {
        if (matchId.isBlank() || uid.isBlank()) return
        try {
            val r = ref(matchId, uid) ?: return
            r.onDisconnect().setValue(0L)
            r.setValue(ServerValue.TIMESTAMP)
        } catch (_: Exception) { }
    }

    fun leave(matchId: String, uid: String) {
        try { ref(matchId, uid)?.let { it.onDisconnect().cancel(); it.setValue(0L) } } catch (_: Exception) { }
    }

    /** uid -> last seen (server time, 0 = offline). */
    fun observe(matchId: String): Flow<Map<String, Long>> = callbackFlow {
        val r = AutoCarromManager.getDb()?.getReference(AutoCarromManager.NODE_MATCHES)?.child(matchId)?.child("presence")
        if (r == null || matchId.isBlank()) { trySend(emptyMap()); close(); return@callbackFlow }
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.associate { (it.key ?: "") to ((it.value as? Number)?.toLong() ?: 0L) })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        r.addValueEventListener(l)
        awaitClose { r.removeEventListener(l) }
    }
}
