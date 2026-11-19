package com.example.ui.autocarrom

import com.example.core.config.FirebaseConfig
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Auto Carrom lobby engine (same flow/money moves as AutoLudoManager, under miniGames/autoCarrom).
object AutoCarromManager {
    internal const val NODE_MATCHES = "miniGames/autoCarrom/matches"
    internal const val NODE_GAME_WALLETS = "gameWallets"
    internal const val NODE_METADATA = "miniGames/autoCarrom/metadata"
    internal const val LAST_MATCH_KEY = "lastAutoCarromMatch"

    internal fun getDb(): FirebaseDatabase? = try {
        FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
    } catch (e: Exception) { null }

    fun observeMatches(): Flow<List<AutoCarromMatchEntity>> = callbackFlow {
        val db = getDb()
        if (db == null) { trySend(emptyList()); close(); return@callbackFlow }
        val ref = db.getReference(NODE_MATCHES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(AutoCarromMatchEntity::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { trySend(emptyList()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeMatch(matchId: String): Flow<AutoCarromMatchEntity?> = callbackFlow {
        val db = getDb()
        if (matchId.isBlank() || db == null) { trySend(null); close(); return@callbackFlow }
        val ref = db.getReference(NODE_MATCHES).child(matchId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.getValue(AutoCarromMatchEntity::class.java)) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private suspend fun readBalance(db: FirebaseDatabase, uid: String): Double =
        suspendCancellableCoroutine { cont ->
            db.getReference(NODE_GAME_WALLETS).child(uid).child("balance").get()
                .addOnSuccessListener { snap ->
                    val v = snap.value
                    cont.resume(when (v) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0 })
                }
                .addOnFailureListener { cont.resume(-1.0) }
        }

    suspend fun createMatch(
        creatorUid: String,
        creatorName: String,
        entryFee: Double,
        scheduledTimeFormatted: String,
        scheduledTimeMillis: Long = 0L
    ): Result<String> {
        if (creatorUid.isBlank()) return Result.failure(IllegalArgumentException("User ID is required"))
        if (entryFee < 10.0 || entryFee > 500.0) {
            return Result.failure(IllegalArgumentException("এন্ট্রি ফি অবশ্যই ১০ থেকে ৫০০ টাকার মধ্যে হতে হবে"))
        }
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        if (readBalance(db, creatorUid) < entryFee) return Result.failure(IllegalStateException("অপর্যাপ্ত গেম ওয়ালেট ব্যালেন্স"))

        val counterRef = db.getReference(NODE_METADATA).child("matchCounter")
        val nextNumber = suspendCancellableCoroutine<Long> { cont ->
            counterRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val cur = when (val v = currentData.value) { is Number -> v.toLong(); is String -> v.toLongOrNull() ?: 0L; else -> 0L }
                    currentData.value = cur + 1L
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed && error == null) cont.resume((currentData?.value as? Number)?.toLong() ?: 1L)
                    else cont.resume(System.currentTimeMillis() % 10000L)
                }
            })
        }

        val matchRef = db.getReference(NODE_MATCHES).push()
        val matchId = matchRef.key ?: "AC_${System.currentTimeMillis()}"
        val matchNumber = nextNumber.toString()
        val prizePool = (entryFee * 2.0) * 0.90 // 10% commission, 1.8x payout
        val schedMillis = if (scheduledTimeMillis > 0L) scheduledTimeMillis else parseScheduledEpoch(scheduledTimeFormatted)
        val name = creatorName.ifBlank { "Player" }

        val matchData = hashMapOf<String, Any?>(
            "matchId" to matchId,
            "matchNumber" to matchNumber,
            "title" to "Auto Carrom || Match No:- #$matchNumber",
            "entryFee" to entryFee,
            "prizePool" to prizePool,
            "pot" to entryFee,
            "paidPlayers" to mapOf(creatorUid to true),
            "creatorUid" to creatorUid,
            "creatorName" to name,
            "player1Uid" to creatorUid,
            "player1Name" to name,
            "player2Uid" to "",
            "player2Name" to "",
            "status" to "WAITING",
            "joinedPlayersCount" to 1,
            "maxPlayers" to 2,
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP,
            "scheduledTimeFormatted" to scheduledTimeFormatted,
            "scheduledTimeMillis" to schedMillis,
            "readyTimerStartedAt" to 0L
        )
        val updates = hashMapOf<String, Any?>(
            "$NODE_GAME_WALLETS/$creatorUid/balance" to ServerValue.increment(-entryFee),
            "$NODE_GAME_WALLETS/$creatorUid/$LAST_MATCH_KEY" to matchId,
            "$NODE_MATCHES/$matchId" to matchData
        )
        return try {
            var errMsg: String? = null
            val ok = suspendCancellableCoroutine<Boolean> { cont ->
                db.reference.updateChildren(updates) { err, _ ->
                    if (err != null) {
                        errMsg = "${err.message} (Code: ${err.code})"
                        android.util.Log.e("AutoCarromError", "FIREBASE WRITE REJECTED: $errMsg")
                    }
                    cont.resume(err == null)
                }
            }
            if (ok) Result.success(matchId)
            else Result.failure(IllegalStateException("ম্যাচ তৈরিতে সমস্যা হয়েছে${errMsg?.let { ": $it" } ?: ""}। ব্যালেন্স কাটা হয়নি।"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun joinMatch(matchId: String, playerUid: String, playerName: String): Result<Unit> {
        if (playerUid.isBlank() || matchId.isBlank()) return Result.failure(IllegalArgumentException("Invalid arguments"))
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val snap = suspendCancellableCoroutine<DataSnapshot?> { cont ->
            db.getReference(NODE_MATCHES).child(matchId).get()
                .addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
        } ?: return Result.failure(IllegalStateException("নেটওয়ার্ক সমস্যা, আবার চেষ্টা করুন"))
        val match = snap.getValue(AutoCarromMatchEntity::class.java)
            ?: return Result.failure(IllegalStateException("ম্যাচটি পাওয়া যায়নি"))

        if (match.status != "WAITING" || match.joinedPlayersCount >= 2) return Result.failure(IllegalStateException("ম্যাচটি ইতোমধ্যে পূর্ণ হয়ে গেছে"))
        if (match.player1Uid == playerUid || match.player2Uid == playerUid) return Result.failure(IllegalStateException("আপনি ইতোমধ্যে এই ম্যাচে যুক্ত আছেন"))
        val start = if (match.scheduledTimeMillis > 0L) match.scheduledTimeMillis else parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
        if (start > 0L && System.currentTimeMillis() >= start) return Result.failure(IllegalStateException("ম্যাচ শুরুর সময় উত্তীর্ণ হয়ে গেছে!"))
        val fee = match.entryFee
        if (readBalance(db, playerUid) < fee) return Result.failure(IllegalStateException("অপর্যাপ্ত গেম ওয়ালেট ব্যালেন্স"))

        val base = "$NODE_MATCHES/$matchId"
        val newCount = match.joinedPlayersCount + 1
        val updates = hashMapOf<String, Any?>(
            "$base/joinedPlayersCount" to newCount,
            "$base/updatedAt" to ServerValue.TIMESTAMP,
            "$base/pot" to ServerValue.increment(fee),
            "$base/paidPlayers/$playerUid" to true,
            "$NODE_GAME_WALLETS/$playerUid/balance" to ServerValue.increment(-fee),
            "$NODE_GAME_WALLETS/$playerUid/$LAST_MATCH_KEY" to matchId
        )
        if (match.player1Uid.isBlank()) {
            updates["$base/player1Uid"] = playerUid
            updates["$base/player1Name"] = playerName.ifBlank { "Player 1" }
        } else if (match.player2Uid.isBlank()) {
            updates["$base/player2Uid"] = playerUid
            updates["$base/player2Name"] = playerName.ifBlank { "Player 2" }
        }
        if (newCount >= 2) {
            updates["$base/status"] = "READY_COUNTDOWN"
            updates["$base/countdownStartedAt"] = ServerValue.TIMESTAMP
        }
        return try {
            val ok = suspendCancellableCoroutine<Boolean> { cont ->
                db.reference.updateChildren(updates) { err, _ -> cont.resume(err == null) }
            }
            if (ok) Result.success(Unit) else Result.failure(IllegalStateException("ম্যাচে যুক্ত হতে ব্যর্থ। ব্যালেন্স কাটা হয়নি।"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Both ready -> IN_GAME, fresh board, player 1 (white) shoots first.
    suspend fun setPlayerReady(matchId: String, playerUid: String): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        return suspendCancellableCoroutine { cont ->
            db.getReference(NODE_MATCHES).child(matchId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val match = currentData.getValue(AutoCarromMatchEntity::class.java) ?: return Transaction.abort()
                    val isP1 = match.player1Uid == playerUid
                    val isP2 = match.player2Uid == playerUid
                    if (!isP1 && !isP2) return Transaction.abort()
                    val p1Ready = isP1 || match.player1Ready
                    val p2Ready = isP2 || match.player2Ready
                    currentData.child("player1Ready").value = p1Ready
                    currentData.child("player2Ready").value = p2Ready
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    if (p1Ready && p2Ready && match.status != "IN_GAME" && match.status != "COMPLETED") {
                        val now = System.currentTimeMillis()
                        currentData.child("status").value = "IN_GAME"
                        currentData.child("gameStartedAt").value = now
                        currentData.child("gameState").value = CarromGameState(
                            currentTurnUid = match.player1Uid,
                            board = CarromRules.initialBoard(),
                            actionStartedAt = now,
                            lastMoveAt = now,
                            lastActionLog = "Game started."
                        )
                    }
                    return Transaction.success(currentData)
                }
                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(err?.toException() ?: Exception("Failed to set ready")))
                }
            })
        }
    }

    fun parseScheduledEpoch(formatted: String, createdAt: Long = 0L): Long = try {
        val parts = formatted.replace("Today", "").trim().split(" ")
        val hm = parts[0].split(":")
        val h = hm[0].toInt(); val m = hm[1].toInt()
        val amPm = parts.getOrNull(1) ?: "PM"
        val hour24 = if (amPm.equals("AM", true)) (if (h == 12) 0 else h) else (if (h == 12) 12 else h + 12)
        val cal = java.util.Calendar.getInstance()
        if (createdAt > 0L) cal.timeInMillis = createdAt
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
        cal.set(java.util.Calendar.MINUTE, m)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (e: Exception) { 0L }
}
