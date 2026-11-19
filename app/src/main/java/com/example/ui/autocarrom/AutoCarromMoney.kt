package com.example.ui.autocarrom

import com.google.firebase.database.*
import com.example.ui.autocarrom.AutoCarromManager.LAST_MATCH_KEY
import com.example.ui.autocarrom.AutoCarromManager.NODE_GAME_WALLETS
import com.example.ui.autocarrom.AutoCarromManager.NODE_MATCHES
import com.example.ui.autocarrom.AutoCarromManager.getDb
import com.example.ui.autocarrom.AutoCarromManager.parseScheduledEpoch

/**
 * Auto Carrom money + match-end rules (same as Auto Ludo):
 * forfeit, prize claim, refund, cancel a solo match after its start time, the 10-minute
 * no-show rule for full matches, and the automatic refund / prize scan.
 * Every money move is ONE atomic write that the database rules check.
 */
object AutoCarromMoney {
    private val claimsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val cancelsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val refundsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val graceInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun startOf(m: AutoCarromMatchEntity) =
        if (m.scheduledTimeMillis > 0L) m.scheduledTimeMillis else parseScheduledEpoch(m.scheduledTimeFormatted, m.createdAt)

    /** The leaving player loses; the opponent wins. */
    fun forfeitMatch(matchId: String, forfeitingUid: String, onComplete: (Boolean, String?) -> Unit) {
        if (matchId.isBlank() || forfeitingUid.isBlank()) { onComplete(false, "Invalid arguments"); return }
        val db = getDb() ?: run { onComplete(false, "Database not available"); return }
        db.getReference(NODE_MATCHES).child(matchId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val m = currentData.getValue(AutoCarromMatchEntity::class.java) ?: return Transaction.abort()
                if (m.status != "IN_GAME" && m.status != "READY_COUNTDOWN" && m.status != "WAITING") return Transaction.abort()
                val opponent = if (forfeitingUid == m.player1Uid) m.player2Uid else m.player1Uid
                if (m.status == "IN_GAME" && opponent.isBlank()) return Transaction.abort()
                val winner = opponent.ifBlank { forfeitingUid }
                currentData.child("status").value = "COMPLETED"
                currentData.child("winnerUid").value = winner
                currentData.child("forfeitedByUid").value = forfeitingUid
                currentData.child("prizeClaimed").value = false
                currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                currentData.child("gameState").child("winnerUid").value = winner
                currentData.child("gameState").child("lastActionLog").value = "Player surrendered. Opponent declared winner."
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed && error == null) onComplete(true, null) else onComplete(false, error?.message ?: "Forfeit failed")
            }
        })
    }

    /** Winner takes the prize into their OWN game wallet; the rest of the pot goes to adminProfit. */
    fun claimWinPrize(matchId: String, currentUid: String, onComplete: (Boolean) -> Unit = {}) {
        if (matchId.isBlank() || currentUid.isBlank() || !claimsInFlight.add(matchId)) { onComplete(false); return }
        val db = getDb() ?: run { claimsInFlight.remove(matchId); onComplete(false); return }
        db.getReference(NODE_MATCHES).child(matchId).get().addOnSuccessListener { snap ->
            val m = snap.getValue(AutoCarromMatchEntity::class.java)
            val pot = (snap.child("pot").value as? Number)?.toDouble() ?: 0.0
            if (m == null || m.status != "COMPLETED" || m.winnerUid != currentUid || m.prizeClaimed || pot <= 0.0) {
                claimsInFlight.remove(matchId); onComplete(false); return@addOnSuccessListener
            }
            val prize = m.prizePool
            val updates = hashMapOf<String, Any?>(
                "$NODE_MATCHES/$matchId/prizeClaimed" to true,
                "$NODE_MATCHES/$matchId/pot" to 0.0,
                "$NODE_GAME_WALLETS/$currentUid/balance" to ServerValue.increment(prize),
                "$NODE_GAME_WALLETS/$currentUid/$LAST_MATCH_KEY" to matchId,
                "miniGames/vault/adminProfit" to ServerValue.increment(pot - prize),
                "userNotifications/$currentUid/${AdCarromNotices.prizeId(matchId, currentUid)}" to
                    AdCarromNotices.prizeRecord(matchId, m.matchNumber.ifBlank { matchId.takeLast(6).uppercase() }, currentUid, prize)
            )
            db.reference.updateChildren(updates).addOnCompleteListener { t ->
                claimsInFlight.remove(matchId); onComplete(t.isSuccessful)
            }
        }.addOnFailureListener { claimsInFlight.remove(matchId); onComplete(false) }
    }

    /** Entry fee back to the player's OWN game wallet when the match is CANCELLED. */
    fun claimRefund(matchId: String, currentUid: String, onComplete: (Boolean) -> Unit = {}) {
        if (matchId.isBlank() || currentUid.isBlank() || cancelsInFlight.contains(matchId) || !refundsInFlight.add(matchId)) {
            onComplete(false); return
        }
        val db = getDb() ?: run { refundsInFlight.remove(matchId); onComplete(false); return }
        db.getReference(NODE_MATCHES).child(matchId).get().addOnSuccessListener { snap ->
            try {
                val status = snap.child("status").value?.toString() ?: ""
                val fee = (snap.child("entryFee").value as? Number)?.toDouble() ?: 0.0
                val paid = snap.child("paidPlayers").child(currentUid).value == true
                val done = snap.child("refundClaimed").child(currentUid).value == true
                if (status != "CANCELLED" || fee <= 0.0 || !paid || done) {
                    refundsInFlight.remove(matchId); onComplete(false); return@addOnSuccessListener
                }
                val number = snap.child("matchNumber").value?.toString()?.ifBlank { null } ?: matchId.takeLast(6).uppercase()
                val updates = hashMapOf<String, Any?>(
                    "$NODE_MATCHES/$matchId/refundClaimed/$currentUid" to true,
                    "$NODE_MATCHES/$matchId/pot" to ServerValue.increment(-fee),
                    "$NODE_GAME_WALLETS/$currentUid/balance" to ServerValue.increment(fee),
                    "$NODE_GAME_WALLETS/$currentUid/$LAST_MATCH_KEY" to matchId,
                    "userNotifications/$currentUid/${AdCarromNotices.refundId(matchId, currentUid)}" to
                        AdCarromNotices.refundRecord(matchId, number, currentUid, fee)
                )
                db.reference.updateChildren(updates).addOnCompleteListener { t ->
                    refundsInFlight.remove(matchId); onComplete(t.isSuccessful)
                }
            } catch (e: Exception) { refundsInFlight.remove(matchId); onComplete(false) }
        }.addOnFailureListener { refundsInFlight.remove(matchId); onComplete(false) }
    }

    /** No opponent by the start time -> CANCELLED, then the creator's refund. */
    fun cancelExpiredSoloMatch(match: AutoCarromMatchEntity, onComplete: ((Boolean) -> Unit)? = null) {
        val start = startOf(match)
        if (match.matchId.isBlank() || start <= 0L || System.currentTimeMillis() < start ||
            match.status != "WAITING" || match.joinedPlayersCount >= 2 ||
            cancelsInFlight.contains(match.matchId) || refundsInFlight.contains(match.matchId)) {
            onComplete?.invoke(false); return
        }
        val db = getDb() ?: run { onComplete?.invoke(false); return }
        cancelsInFlight.add(match.matchId)
        db.getReference(NODE_MATCHES).child(match.matchId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) return Transaction.success(currentData)
                val st = currentData.child("status").getValue(String::class.java)
                val joined = currentData.child("joinedPlayersCount").getValue(Int::class.java) ?: 1
                if (st != "WAITING" || joined >= 2) return Transaction.abort()
                currentData.child("status").value = "CANCELLED"
                currentData.child("gameState").child("lastActionLog").value = "Opponent not found in time. Cancelled."
                currentData.child("updatedAt").value = System.currentTimeMillis()
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                cancelsInFlight.remove(match.matchId)
                val target = match.creatorUid.ifBlank { match.player1Uid }
                val cancelled = committed || currentData?.child("status")?.getValue(String::class.java) == "CANCELLED"
                if (cancelled && target.isNotBlank()) claimRefund(match.matchId, target) { onComplete?.invoke(it) }
                else onComplete?.invoke(committed)
            }
        })
    }

    /**
     * 10-minute rule for FULL matches: 10 minutes after the start time, if the game never began,
     * nobody ready -> CANCELLED (both get the automatic refund); exactly one ready -> that player wins.
     * Never moves money itself.
     */
    fun adjudicateExpiredFullMatch(match: AutoCarromMatchEntity, currentUid: String) {
        if (match.matchId.isBlank() || currentUid.isBlank()) return
        if (match.player1Uid != currentUid && match.player2Uid != currentUid) return
        if ((match.status != "WAITING" && match.status != "READY_COUNTDOWN") || match.joinedPlayersCount < 2) return
        val start = startOf(match)
        if (start <= 0L || System.currentTimeMillis() < start + 10 * 60 * 1000L) return
        if (!graceInFlight.add(match.matchId)) return
        val db = getDb() ?: run { graceInFlight.remove(match.matchId); return }
        db.getReference(NODE_MATCHES).child(match.matchId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) return Transaction.success(currentData)
                val st = currentData.child("status").getValue(String::class.java) ?: ""
                val joined = currentData.child("joinedPlayersCount").getValue(Int::class.java) ?: 0
                if ((st != "WAITING" && st != "READY_COUNTDOWN") || joined < 2) return Transaction.abort()
                val p1 = currentData.child("player1Uid").getValue(String::class.java) ?: ""
                val p2 = currentData.child("player2Uid").getValue(String::class.java) ?: ""
                val r1 = currentData.child("player1Ready").getValue(Boolean::class.java) == true
                val r2 = currentData.child("player2Ready").getValue(Boolean::class.java) == true
                val log = currentData.child("gameState").child("lastActionLog")
                when {
                    !r1 && !r2 -> { currentData.child("status").value = "CANCELLED"; log.value = "Both players absent. Match cancelled." }
                    r1 && !r2 && p1.isNotBlank() -> { currentData.child("status").value = "COMPLETED"; currentData.child("winnerUid").value = p1; log.value = "Opponent absent. Walkover victory declared." }
                    r2 && !r1 && p2.isNotBlank() -> { currentData.child("status").value = "COMPLETED"; currentData.child("winnerUid").value = p2; log.value = "Opponent absent. Walkover victory declared." }
                    else -> return Transaction.abort()
                }
                currentData.child("updatedAt").value = System.currentTimeMillis()
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                graceInFlight.remove(match.matchId)
            }
        })
    }

    /** Run from the lobby on every list update: expired solo matches, the 10-minute rule, refunds and prizes. */
    fun checkAndCancelExpiredSoloMatches(matches: List<AutoCarromMatchEntity>, currentUid: String) {
        val now = System.currentTimeMillis()
        matches.forEach { m ->
            val start = startOf(m)
            if (m.status == "WAITING" && m.joinedPlayersCount < 2 && start > 0L && now >= start) cancelExpiredSoloMatch(m)
            if ((m.status == "WAITING" || m.status == "READY_COUNTDOWN") && m.joinedPlayersCount >= 2) adjudicateExpiredFullMatch(m, currentUid)
        }
        if (currentUid.isBlank()) return
        matches.filter {
            it.status == "CANCELLED" && (it.player1Uid == currentUid || it.player2Uid == currentUid || it.creatorUid == currentUid) &&
                !cancelsInFlight.contains(it.matchId) && !refundsInFlight.contains(it.matchId)
        }.forEach { claimRefund(it.matchId, currentUid) }
        matches.filter { it.status == "COMPLETED" && it.winnerUid == currentUid && !it.prizeClaimed && !claimsInFlight.contains(it.matchId) }
            .forEach { claimWinPrize(it.matchId, currentUid) }
    }
}
