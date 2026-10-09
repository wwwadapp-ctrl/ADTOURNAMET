package com.example.ui.autoludo

import com.example.core.config.FirebaseConfig
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LudoGameState(
    val currentTurnUid: String = "",
    val diceValue: Int = 1,
    val isDiceRolled: Boolean = false,
    val player1Pawns: List<Int> = listOf(-1, -1, -1, -1), // -1 = Yard, 0-51 = Track, 52-56 = Home Runway, 57 = Goal
    val player2Pawns: List<Int> = listOf(-1, -1, -1, -1),
    val consecutiveSixesCount: Int = 0,
    val winnerUid: String = "",
    val lastMoveAt: Long = 0L,
    val lastActionLog: String = ""
)

data class AutoLudoMatchEntity(
    val matchId: String = "",
    val matchNumber: String = "",
    val title: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val creatorUid: String = "",
    val creatorName: String = "",
    val player1Uid: String = "",
    val player1Name: String = "",
    val player1Avatar: String = "",
    val player2Uid: String = "",
    val player2Name: String = "",
    val player2Avatar: String = "",
    val status: String = "WAITING", // WAITING, READY_COUNTDOWN, IN_GAME, COMPLETED, CANCELLED
    val joinedPlayersCount: Int = 1,
    val maxPlayers: Int = 2,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scheduledTimeFormatted: String = "Today",
    val readyTimerStartedAt: Long = 0L,
    val countdownStartedAt: Long = 0L,
    val player1Ready: Boolean = false,
    val player2Ready: Boolean = false,
    val gameStartedAt: Long = 0L,
    val gameState: LudoGameState = LudoGameState()
)

object AutoLudoManager {
    // Authorized path under miniGames node to ensure Firebase Security Rule compliance
    private const val NODE_MATCHES = "miniGames/autoLudo/matches"
    private const val NODE_GAME_WALLETS = "gameWallets"

    private fun getDb(): FirebaseDatabase? {
        return try {
            FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Observes live Auto Ludo matches in real-time under miniGames/autoLudo/matches.
     */
    fun observeMatches(): Flow<List<AutoLudoMatchEntity>> = callbackFlow {
        val db = getDb()
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.getReference(NODE_MATCHES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matches = mutableListOf<AutoLudoMatchEntity>()
                for (child in snapshot.children) {
                    val match = child.getValue(AutoLudoMatchEntity::class.java)
                    if (match != null) {
                        matches.add(match)
                    }
                }
                matches.sortByDescending { it.createdAt }
                trySend(matches)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Atomically validates balance, deducts entry fee from gameWallets, and creates match.
     */
    suspend fun createMatch(
        creatorUid: String,
        creatorName: String,
        entryFee: Double,
        scheduledTimeFormatted: String
    ): Result<String> {
        if (creatorUid.isBlank()) return Result.failure(IllegalArgumentException("User ID is required"))
        if (entryFee < 10.0 || entryFee > 10000.0) {
            return Result.failure(IllegalArgumentException("এন্ট্রি ফি অবশ্যই ১০ থেকে ১০,০০০ টাকার মধ্যে হতে হবে"))
        }

        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val walletBalanceRef = db.getReference(NODE_GAME_WALLETS).child(creatorUid).child("balance")

        // Step 1: Atomic Transaction on gameWallets/$uid/balance with proper NULL handling
        val deductionResult = suspendCancellableCoroutine<Pair<Boolean, String?>> { cont ->
            walletBalanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    // CRITICAL FIX: If currentData is null, return success to let Firebase fetch authoritative server value
                    if (currentData.value == null) {
                        return Transaction.success(currentData)
                    }

                    val rawBal = currentData.value
                    val currentBal = when (rawBal) {
                        is Number -> rawBal.toDouble()
                        is String -> rawBal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    if (currentBal < entryFee) {
                        return Transaction.abort() // Legitimate insufficient balance from server snapshot
                    }

                    // Deduct entry fee
                    currentData.value = currentBal - entryFee
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (committed && error == null) {
                        cont.resume(Pair(true, null))
                    } else {
                        val errMsg = error?.message ?: "অপর্যাপ্ত গেম ওয়ালেট ব্যালেন্স"
                        cont.resume(Pair(false, errMsg))
                    }
                }
            })
        }

        if (!deductionResult.first) {
            return Result.failure(IllegalStateException(deductionResult.second ?: "ব্যালেন্স ডিডাকশন ব্যর্থ হয়েছে"))
        }

        // Step 2: Write Match to miniGames/autoLudo/matches/$matchId
        val matchRef = db.getReference(NODE_MATCHES).push()
        val matchId = matchRef.key ?: "AL_${System.currentTimeMillis()}"
        val matchNumber = (100..999).random().toString()
        val prizePool = (entryFee * 2.0) * 0.90 // 10% commission, 1.8x payout

        val matchData = hashMapOf<String, Any?>(
            "matchId" to matchId,
            "matchNumber" to matchNumber,
            "title" to "Auto Ludo || Match No:- $matchNumber",
            "entryFee" to entryFee,
            "prizePool" to prizePool,
            "creatorUid" to creatorUid,
            "creatorName" to creatorName.ifBlank { "Player" },
            "player1Uid" to creatorUid,
            "player1Name" to creatorName.ifBlank { "Player" },
            "player2Uid" to "",
            "player2Name" to "",
            "status" to "WAITING",
            "joinedPlayersCount" to 1,
            "maxPlayers" to 2,
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP,
            "scheduledTimeFormatted" to scheduledTimeFormatted,
            "readyTimerStartedAt" to 0L
        )

        return try {
            var firebaseErrorMsg: String? = null
            val writeSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                matchRef.setValue(matchData) { err, _ ->
                    if (err != null) {
                        firebaseErrorMsg = "${err.message} (Code: ${err.code})"
                        android.util.Log.e("AutoLudoError", "FIREBASE WRITE REJECTED: $firebaseErrorMsg")
                    }
                    cont.resume(err == null)
                }
            }

            if (writeSuccess) {
                Result.success(matchId)
            } else {
                // Atomic refund rollback if match write fails
                compensateRefund(walletBalanceRef, entryFee)
                val finalError = if (firebaseErrorMsg != null) {
                    "ম্যাচ সংরক্ষণে সমস্যা: $firebaseErrorMsg. ব্যালেন্স ফেরত দেওয়া হয়েছে।"
                } else {
                    "ম্যাচ তৈরিতে সমস্যা হয়েছে, ব্যালেন্স ফেরত দেওয়া হয়েছে"
                }
                Result.failure(IllegalStateException(finalError))
            }
        } catch (e: Exception) {
            compensateRefund(walletBalanceRef, entryFee)
            Result.failure(e)
        }
    }

    /**
     * Secures Player join flow: validates, deducts fee, and updates match state.
     */
    suspend fun joinMatch(
        matchId: String,
        playerUid: String,
        playerName: String
    ): Result<Unit> {
        if (playerUid.isBlank() || matchId.isBlank()) return Result.failure(IllegalArgumentException("Invalid arguments"))

        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val matchRef = db.getReference(NODE_MATCHES).child(matchId)
        
        // Fetch current match state
        val matchSnapshot = suspendCancellableCoroutine<DataSnapshot> { cont ->
            matchRef.get().addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resumeWith(Result.failure(it)) }
        }
        
        val match = matchSnapshot.getValue(AutoLudoMatchEntity::class.java)
            ?: return Result.failure(IllegalStateException("ম্যাচটি পাওয়া যায়নি"))

        // 1. Validation Guards
        if (match.status != "WAITING" || match.joinedPlayersCount >= 2) {
            return Result.failure(IllegalStateException("ম্যাচটি ইতোমধ্যে পূর্ণ হয়ে গেছে"))
        }
        if (match.player1Uid == playerUid || match.player2Uid == playerUid) {
            return Result.failure(IllegalStateException("আপনি ইতোমধ্যে এই ম্যাচে যুক্ত আছেন"))
        }

        val entryFee = match.entryFee
        val walletBalanceRef = db.getReference(NODE_GAME_WALLETS).child(playerUid).child("balance")

        // 2. Atomic Wallet Deduction
        val deductionResult = suspendCancellableCoroutine<Pair<Boolean, String?>> { cont ->
            walletBalanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value == null) return Transaction.success(currentData)
                    val rawBal = currentData.value
                    val cur = when (rawBal) {
                        is Number -> rawBal.toDouble()
                        is String -> rawBal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    if (cur < entryFee) return Transaction.abort()
                    currentData.value = cur - entryFee
                    return Transaction.success(currentData)
                }
                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) cont.resume(Pair(true, null))
                    else cont.resume(Pair(false, err?.message ?: "অপর্যাপ্ত গেম ওয়ালেট ব্যালেন্স"))
                }
            })
        }

        if (!deductionResult.first) {
            return Result.failure(IllegalStateException(deductionResult.second ?: "ব্যালেন্স ডিডাকশন ব্যর্থ"))
        }

        // 3. Slot Allocation & Status Transition
        val isSlot1Empty = match.player1Uid.isBlank()
        val isSlot2Empty = match.player2Uid.isBlank()
        val newCount = match.joinedPlayersCount + 1

        val updates = hashMapOf<String, Any?>(
            "joinedPlayersCount" to newCount,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        if (isSlot1Empty) {
            updates["player1Uid"] = playerUid
            updates["player1Name"] = playerName.ifBlank { "Player 1" }
        } else if (isSlot2Empty) {
            updates["player2Uid"] = playerUid
            updates["player2Name"] = playerName.ifBlank { "Player 2" }
        }

        // Trigger Countdown if 2/2 full
        if (newCount >= 2) {
            updates["status"] = "READY_COUNTDOWN"
            updates["countdownStartedAt"] = ServerValue.TIMESTAMP
        }

        return try {
            val updateSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                matchRef.updateChildren(updates) { err, _ -> cont.resume(err == null) }
            }
            if (updateSuccess) {
                Result.success(Unit)
            } else {
                compensateRefund(walletBalanceRef, entryFee)
                Result.failure(IllegalStateException("ম্যাচে যুক্ত হতে ব্যর্থ, ব্যালেন্স ফেরত দেওয়া হয়েছে"))
            }
        } catch (e: Exception) {
            compensateRefund(walletBalanceRef, entryFee)
            Result.failure(e)
        }
    }

    /**
     * Observes a specific match in real-time.
     */
    fun observeMatch(matchId: String): Flow<AutoLudoMatchEntity?> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val db = getDb()
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val ref = db.getReference(NODE_MATCHES).child(matchId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(AutoLudoMatchEntity::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Authoritatively sets a player's ready status and transitions to IN_GAME if both ready.
     */
    suspend fun setPlayerReady(matchId: String, playerUid: String): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val matchRef = db.getReference(NODE_MATCHES).child(matchId)

        return suspendCancellableCoroutine { cont ->
            matchRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val match = currentData.getValue(AutoLudoMatchEntity::class.java)
                        ?: return Transaction.abort()

                    val isPlayer1 = match.player1Uid == playerUid
                    val isPlayer2 = match.player2Uid == playerUid

                    if (!isPlayer1 && !isPlayer2) return Transaction.abort()

                    val p1Ready = if (isPlayer1) true else match.player1Ready
                    val p2Ready = if (isPlayer2) true else match.player2Ready

                    currentData.child("player1Ready").value = p1Ready
                    currentData.child("player2Ready").value = p2Ready
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP

                    // Transition to IN_GAME if both are ready
                    if (p1Ready && p2Ready) {
                        currentData.child("status").value = "IN_GAME"
                        currentData.child("gameStartedAt").value = ServerValue.TIMESTAMP
                        
                        // Initialize Game State
                        val initialState = LudoGameState(
                            currentTurnUid = match.player1Uid,
                            lastMoveAt = System.currentTimeMillis()
                        )
                        currentData.child("gameState").setValue(initialState)
                    }

                    return Transaction.success(currentData)
                }

                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) {
                        cont.resume(Result.success(Unit))
                    } else {
                        cont.resume(Result.failure(err?.toException() ?: Exception("Failed to set ready")))
                    }
                }
            })
        }
    }

    /**
     * Updates the dice value for the current turn with 3x Six Guard.
     */
    suspend fun rollDice(matchId: String, playerUid: String, value: Int): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val matchRef = db.getReference(NODE_MATCHES).child(matchId)
        
        return suspendCancellableCoroutine { cont ->
            matchRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val match = currentData.getValue(AutoLudoMatchEntity::class.java) ?: return Transaction.abort()
                    val state = match.gameState
                    
                    if (state.currentTurnUid != playerUid || state.isDiceRolled) return Transaction.abort()
                    
                    var nextConsecutiveSixes = if (value == 6) state.consecutiveSixesCount + 1 else 0
                    var nextTurnUid = state.currentTurnUid
                    var diceValue = value
                    var isDiceRolled = true
                    var actionLog = ""

                    // Anti-3x-Six Dice Guard
                    if (nextConsecutiveSixes >= 3) {
                        nextTurnUid = if (playerUid == match.player1Uid) match.player2Uid else match.player1Uid
                        nextConsecutiveSixes = 0
                        isDiceRolled = false
                        diceValue = value
                        actionLog = "3 Sixes! Turn Passed"
                    }

                    val newState = state.copy(
                        diceValue = diceValue,
                        isDiceRolled = isDiceRolled,
                        consecutiveSixesCount = nextConsecutiveSixes,
                        currentTurnUid = nextTurnUid,
                        lastMoveAt = System.currentTimeMillis(),
                        lastActionLog = actionLog
                    )
                    
                    currentData.child("gameState").value = newState
                    return Transaction.success(currentData)
                }

                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(err?.toException() ?: Exception("Roll failed")))
                }
            })
        }
    }

    /**
     * Moves a pawn, handles captures, bonus turns, and safe zones.
     */
    suspend fun movePawn(matchId: String, playerUid: String, pawnIndex: Int, newPos: Int): Result<Unit> {
        val db = getDb() ?: return Result.failure(IllegalStateException("Database not available"))
        val matchRef = db.getReference(NODE_MATCHES).child(matchId)

        return suspendCancellableCoroutine { cont ->
            matchRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val match = currentData.getValue(AutoLudoMatchEntity::class.java) ?: return Transaction.abort()
                    val state = match.gameState
                    
                    if (state.currentTurnUid != playerUid || !state.isDiceRolled) return Transaction.abort()
                    
                    val isPlayer1 = match.player1Uid == playerUid
                    val myPawns = (if (isPlayer1) state.player1Pawns else state.player2Pawns).toMutableList()
                    val oppPawns = (if (isPlayer1) state.player2Pawns else state.player1Pawns).toMutableList()
                    
                    if (pawnIndex !in 0..3) return Transaction.abort()
                    
                    // 1. Update pawn position
                    myPawns[pawnIndex] = newPos
                    
                    var captured = false
                    var bonusRoll = state.diceValue == 6 || newPos == 57 // Bonus for 6 or Goal

                    // 2. Collision & Capture (Cutting) Engine
                    // Check only if newPos is on the track (0-51) and NOT a safe zone
                    if (newPos in 0..51) {
                        val isSafe = isSafeCell(newPos, isPlayer1)
                        if (!isSafe) {
                            // Map my track position to global track position to compare with opponent
                            val myGlobalPos = getGlobalTrackPos(newPos, isPlayer1)
                            
                            oppPawns.forEachIndexed { idx, oppPos ->
                                if (oppPos in 0..51) {
                                    val oppGlobalPos = getGlobalTrackPos(oppPos, !isPlayer1)
                                    if (myGlobalPos == oppGlobalPos) {
                                        // CAPTURE!
                                        oppPawns[idx] = -1
                                        captured = true
                                        bonusRoll = true
                                    }
                                }
                            }
                        }
                    }

                    // 3. Victory Check
                    val allAtGoal = myPawns.all { it == 57 }
                    var winnerUid = state.winnerUid
                    var status = match.status
                    if (allAtGoal) {
                        winnerUid = playerUid
                        status = "COMPLETED"
                    }

                    // 4. Turn Passing
                    val nextTurnUid = if (bonusRoll || allAtGoal) {
                        playerUid
                    } else {
                        if (isPlayer1) match.player2Uid else match.player1Uid
                    }

                    val newState = state.copy(
                        player1Pawns = if (isPlayer1) myPawns else oppPawns,
                        player2Pawns = if (isPlayer1) oppPawns else myPawns,
                        currentTurnUid = nextTurnUid,
                        isDiceRolled = false,
                        winnerUid = winnerUid,
                        lastMoveAt = System.currentTimeMillis(),
                        lastActionLog = if (captured) "Captured!" else if (newPos == 57) "Goal!" else ""
                    )
                    
                    currentData.child("gameState").value = newState
                    currentData.child("status").value = status
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    
                    return Transaction.success(currentData)
                }

                override fun onComplete(err: DatabaseError?, committed: Boolean, d: DataSnapshot?) {
                    if (committed && err == null) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(err?.toException() ?: Exception("Move failed")))
                }
            })
        }
    }

    private fun isSafeCell(pos: Int, isPlayer1: Boolean): Boolean {
        // Safe cells in player-relative coordinates
        // 0 (entry), 8, 13, 21, 26, 34, 39, 47
        return pos == 0 || pos == 8 || pos == 13 || pos == 21 || pos == 26 || pos == 34 || pos == 39 || pos == 47
    }

    private fun getGlobalTrackPos(pos: Int, isPlayer1: Boolean): Int {
        val offset = if (isPlayer1) 0 else 26
        return (pos + offset) % 52
    }

    private suspend fun compensateRefund(balanceRef: DatabaseReference, amount: Double) {
        suspendCancellableCoroutine<Unit> { cont ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value == null) return Transaction.success(currentData)
                    val rawBal = currentData.value
                    val cur = when (rawBal) {
                        is Number -> rawBal.toDouble()
                        is String -> rawBal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    currentData.value = cur + amount
                    return Transaction.success(currentData)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, d: DataSnapshot?) {
                    cont.resume(Unit)
                }
            })
        }
    }
}
