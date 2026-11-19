package com.example.ui.autocarrom

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Network helpers for Auto Carrom:
 *  - CarromClock: the SERVER time on every phone (Firebase .info/serverTimeOffset), so both phones count
 *    the 20-second turn the same way even when their own clocks differ.
 *  - CarromLive: tiny live node miniGames/autoCarrom/live/{matchId}/{uid} (outside the match node) storing
 *    "seen" (server time every 2 s, 0 on disconnect) and "aim" (striker x + aim vector about 10 times a second).
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

data class CarromLiveInfo(val seen: Long? = null, val aim: String? = null)

object CarromLive {
    const val BEAT_MS = 2_000L
    const val ONLINE_MS = 8_000L          // seen within 8 s = online
    const val OFFLINE_GRACE_MS = 30_000L  // up to 30 s extra when opponent is offline
    const val BACK_GRACE_MS = 5_000L      // 5 s grace when connection returns after turn timer
    private const val NODE_LIVE = "miniGames/autoCarrom/live"

    private fun ref(matchId: String, uid: String) =
        AutoCarromManager.getDb()?.getReference(NODE_LIVE)?.child(matchId)?.child(uid)

    /** Marks me online now and sets the automatic "offline" write (0) for when my connection drops. */
    fun beat(matchId: String, uid: String) {
        if (matchId.isBlank() || uid.isBlank()) return
        try {
            val r = ref(matchId, uid)?.child("seen") ?: return
            r.onDisconnect().setValue(0L)
            r.setValue(ServerValue.TIMESTAMP)
        } catch (_: Exception) { }
    }

    fun leave(matchId: String, uid: String) {
        if (matchId.isBlank() || uid.isBlank()) return
        try {
            ref(matchId, uid)?.let {
                it.child("seen").onDisconnect().cancel()
                it.child("seen").setValue(0L)
                it.child("aim").removeValue()
            }
        } catch (_: Exception) { }
    }

    /** Sends current striker position and aim vector for [shotNo] outside the match node. */
    fun sendAim(matchId: String, uid: String, shotNo: Int, x: Double, aim: DoubleArray?) {
        if (matchId.isBlank() || uid.isBlank()) return
        try {
            val encoded = "$shotNo|${Math.round(x * 10) / 10.0}|" +
                (aim?.takeIf { it.size >= 3 }?.let {
                    "${Math.round(it[0] * 1000) / 1000.0},${Math.round(it[1] * 1000) / 1000.0},${Math.round(it[2] * 1000) / 1000.0}"
                } ?: "-")
            ref(matchId, uid)?.child("aim")?.setValue(encoded)
        } catch (_: Exception) { }
    }

    /** Decodes [raw] for the expected [shotNo] into (strikerX, aimArray?). */
    fun decode(raw: String, shotNo: Int): Pair<Double, DoubleArray?>? {
        val parts = raw.split("|")
        if (parts.size < 2 || parts[0].toIntOrNull() != shotNo) return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val aim = if (parts.size >= 3 && parts[2] != "-") {
            val a = parts[2].split(",")
            if (a.size == 3) {
                val dx = a[0].toDoubleOrNull(); val dy = a[1].toDoubleOrNull(); val p = a[2].toDoubleOrNull()
                if (dx != null && dy != null && p != null) doubleArrayOf(dx, dy, p) else null
            } else null
        } else null
        return x to aim
    }

    /** uid -> live presence + aim info. */
    fun observe(matchId: String): Flow<Map<String, CarromLiveInfo>> = callbackFlow {
        val r = AutoCarromManager.getDb()?.getReference(NODE_LIVE)?.child(matchId)
        if (r == null || matchId.isBlank()) { trySend(emptyMap()); close(); return@callbackFlow }
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.associate { child ->
                    val uid = child.key ?: ""
                    val seen = (child.child("seen").value as? Number)?.toLong()
                    val aim = child.child("aim").getValue(String::class.java)
                    uid to CarromLiveInfo(seen, aim)
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        r.addValueEventListener(l)
        awaitClose { r.removeEventListener(l) }
    }
}
