package com.example.ui.autocarrom

import com.google.firebase.database.*
import com.example.ui.autocarrom.AutoCarromManager.NODE_MATCHES
import com.example.ui.autocarrom.AutoCarromManager.getDb
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Auto Carrom in-game moves. Each one is a transaction on the match, so two phones can never
 * both apply the same shot. The shooter's phone runs the physics; the result is written here and
 * the other phone replays the shot from [CarromGameState.lastShot].
 */
object AutoCarromPlay {

    private fun result(cont: kotlin.coroutines.Continuation<Result<Unit>>, err: DatabaseError?, committed: Boolean, what: String) {
        if (committed && err == null) cont.resume(Result.success(Unit))
        else cont.resume(Result.failure(err?.toException() ?: Exception(what)))
    }

    private fun afterEnd(matchId: String, winner: String, me: String) {
        if (winner.isNotBlank() && winner == me) AutoCarromMoney.claimWinPrize(matchId, me)
    }

    /**
     * Saves one finished shot. [shotNo] must equal the current shot number (stops double writes).
     * [shotInput] = "shotNo|uid|strikerX|strikerY|dirX|dirY|power".
     */
    suspend fun submitShot(
        matchId: String, playerUid: String, shotNo: Int, shotInput: String,
        finalBoard: String, pocketed: String, strikerIn: Boolean
    ): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        var winner = ""
        return suspendCancellableCoroutine { cont ->
            db.getReference(NODE_MATCHES).child(matchId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val m = currentData.getValue(AutoCarromMatchEntity::class.java) ?: return Transaction.abort()
                    val st = m.gameState
                    if (m.status != "IN_GAME" || st.currentTurnUid != playerUid || st.shotNo != shotNo) return Transaction.abort()
                    val now = CarromClock.now()
                    val ends = if (m.gameStartedAt > 0L) m.gameStartedAt + CarromRules.GAME_MS else 0L
                    val out = CarromRules.resolve(st, m.player1Uid, m.player2Uid, playerUid, finalBoard, pocketed, strikerIn, shotInput, ends, now)
                    currentData.child("gameState").value = out.state
                    currentData.child("status").value = out.status
                    if (out.status == "COMPLETED") currentData.child("winnerUid").value = out.winnerUid
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    winner = out.winnerUid
                    return Transaction.success(currentData)
                }
                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) afterEnd(matchId, winner, playerUid)
                    result(cont, err, committed, "Shot failed")
                }
            })
        }
    }

    /** Sprinkles powder at (x, y) mm. Only the shooter, before the shot, max 3 per player. */
    suspend fun usePowder(matchId: String, playerUid: String, shotNo: Int, x: Double, y: Double): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        return suspendCancellableCoroutine { cont ->
            db.getReference(NODE_MATCHES).child(matchId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val m = currentData.getValue(AutoCarromMatchEntity::class.java) ?: return Transaction.abort()
                    val st = m.gameState
                    if (m.status != "IN_GAME" || st.currentTurnUid != playerUid || st.shotNo != shotNo) return Transaction.abort()
                    val isP1 = playerUid == m.player1Uid
                    val left = if (isP1) st.player1Powder else st.player2Powder
                    if (left <= 0) return Transaction.abort()
                    val cx = x.coerceIn(30.0, CarromRules.S - 30.0)
                    val cy = y.coerceIn(30.0, CarromRules.S - 30.0)
                    val patch = "${Math.round(cx * 10)},${Math.round(cy * 10)},100"
                    val next = st.copy(
                        powder = if (st.powder.isBlank()) patch else st.powder + ";" + patch,
                        player1Powder = if (isP1) left - 1 else st.player1Powder,
                        player2Powder = if (isP1) st.player2Powder else left - 1,
                        lastMoveAt = CarromClock.now(),
                        lastActionLog = "Powder."
                    )
                    currentData.child("gameState").value = next
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    return Transaction.success(currentData)
                }
                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) = result(cont, err, committed, "Powder failed")
            })
        }
    }

    /**
     * Shot timer ran out. Either phone may call it (so a game never gets stuck when one player is offline).
     * Applied only if the turn really lasted >= 19 s: +1 miss for the shooter, turn passes;
     * 3 misses = the other player wins. Also checks the 10-minute game time.
     */
    suspend fun passTurnTimeout(matchId: String, callerUid: String, shotNo: Int): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        var winner = ""
        return suspendCancellableCoroutine { cont ->
            db.getReference(NODE_MATCHES).child(matchId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val m = currentData.getValue(AutoCarromMatchEntity::class.java) ?: return Transaction.abort()
                    val st = m.gameState
                    if (m.status != "IN_GAME" || st.shotNo != shotNo) return Transaction.abort()
                    if (callerUid != m.player1Uid && callerUid != m.player2Uid) return Transaction.abort()
                    val now = CarromClock.now()
                    if (now - st.actionStartedAt < CarromRules.TURN_MS - 1000L) return Transaction.abort()
                    val shooterIsP1 = st.currentTurnUid == m.player1Uid
                    var s1 = st.player1Strikes
                    var s2 = st.player2Strikes
                    if (shooterIsP1) s1++ else s2++
                    var status = "IN_GAME"
                    var log = "Time up! Miss +1"
                    if (s1 >= CarromRules.MAX_STRIKES || s2 >= CarromRules.MAX_STRIKES) {
                        winner = if (s1 >= CarromRules.MAX_STRIKES) m.player2Uid else m.player1Uid
                        status = "COMPLETED"; log = "Player Disqualified (3 Misses)!"
                    } else {
                        val ends = if (m.gameStartedAt > 0L) m.gameStartedAt + CarromRules.GAME_MS else 0L
                        if ((ends in 1..now) || st.suddenDeath) {
                            if (st.player1Pocketed != st.player2Pocketed) {
                                winner = if (st.player1Pocketed > st.player2Pocketed) m.player1Uid else m.player2Uid
                            } else if (!st.suddenDeath && st.queenOwnerUid.isNotBlank()) winner = st.queenOwnerUid
                            if (winner.isNotBlank()) { status = "COMPLETED"; log = "Time over." }
                        }
                    }
                    val next = st.copy(
                        currentTurnUid = if (shooterIsP1) m.player2Uid else m.player1Uid,
                        shotNo = st.shotNo + 1,
                        player1Strikes = s1,
                        player2Strikes = s2,
                        suddenDeath = st.suddenDeath || (status == "IN_GAME" && m.gameStartedAt > 0L && now >= m.gameStartedAt + CarromRules.GAME_MS),
                        actionStartedAt = now,
                        winnerUid = winner,
                        lastMoveAt = now,
                        lastActionLog = log
                    )
                    currentData.child("gameState").value = next
                    currentData.child("status").value = status
                    if (status == "COMPLETED") currentData.child("winnerUid").value = winner
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    return Transaction.success(currentData)
                }
                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) afterEnd(matchId, winner, callerUid)
                    result(cont, err, committed, "Pass failed")
                }
            })
        }
    }
}
