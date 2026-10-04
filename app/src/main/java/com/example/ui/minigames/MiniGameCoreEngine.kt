package com.example.ui.minigames

import com.example.core.firebase.FirebaseManager
import com.google.firebase.database.ServerValue
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Authoritative outcome of a mini-game session execution.
 */
data class GameResult(
    val isWin: Boolean,
    val multiplier: Double,
    val winAmount: Double,
    val newBalance: Double,
    val transactionId: String,
    val winnerIndex: Int = 0,
    val dice1: Int = 1,
    val dice2: Int = 1,
    val coinSide: String = "HEAD"
)

/**
 * Centralized, tamper-resistant engine for mini-game financial logic.
 * 
 * DESIGN PRINCIPLES:
 * 1. Atomicity: Multi-path updates ensure balance, vault, and ledger are updated together.
 * 2. House Integrity: Zero-loss logic prevents payouts exceeding available reserve pool.
 * 3. Concurrency Control: Mutex locking prevents double-spend or rapid-click race conditions.
 * 4. Entropy: Cryptographically strong RNG for outcome generation.
 * 5. Remote Controls: Enforces Admin App's maintenance and player-specific overrides.
 */
object MiniGameCoreEngine {
    
    private val userLocks = ConcurrentHashMap<String, Mutex>()
    private val secureRandom = SecureRandom()

    // Internal Configuration Models
    private data class GlobalConfig(
        val spinGameActive: Boolean = true,
        val diceGameActive: Boolean = true,
        val headTailGameActive: Boolean = true,
        val scratchCardGameActive: Boolean = true,
        val spinHighMultipliersLocked: Boolean = false,
        val scratchCardMaxJackpot: Double = 1000.0,
        val spinWinChanceMode: String = "NORMAL",
        val diceWinRateMode: String = "NORMAL",
        val headTailWinRateMode: String = "NORMAL"
    )

    private data class GameUserControl(
        val outcome: String = "NORMAL",
        val allowedTier: String = "ALL"
    )

    private data class UserControlSet(
        val spinWheel: GameUserControl = GameUserControl(),
        val diceRoll: GameUserControl = GameUserControl(),
        val headTail: GameUserControl = GameUserControl(),
        val scratchCard: GameUserControl = GameUserControl()
    )

    private data class EngineState(
        val balance: Double,
        val reservePool: Double,
        val global: GlobalConfig,
        val user: UserControlSet
    )

    /**
     * Fetches authoritative state from Firebase before game execution.
     */
    private suspend fun fetchEngineState(uid: String, dbRef: DatabaseReference): EngineState {
        return suspendCancellableCoroutine { cont ->
            dbRef.child("gameWallets/$uid/balance").get().addOnCompleteListener { bTask ->
                dbRef.child("miniGames/vault/reservePool").get().addOnCompleteListener { pTask ->
                    dbRef.child("miniGames/globalConfig").get().addOnCompleteListener { gTask ->
                        dbRef.child("miniGames/userControls/$uid").get().addOnCompleteListener { uTask ->
                            val b = bTask.result?.value?.toString()?.toDoubleOrNull() ?: 0.0
                            val p = pTask.result?.value?.toString()?.toDoubleOrNull() ?: 0.0
                            
                            val gSnap = gTask.result
                            val uSnap = uTask.result
                            
                            val global = GlobalConfig(
                                spinGameActive = gSnap?.child("spinGameActive")?.getValue(Boolean::class.java) ?: true,
                                diceGameActive = gSnap?.child("diceGameActive")?.getValue(Boolean::class.java) ?: true,
                                headTailGameActive = gSnap?.child("headTailGameActive")?.getValue(Boolean::class.java) ?: true,
                                scratchCardGameActive = gSnap?.child("scratchCardGameActive")?.getValue(Boolean::class.java) ?: true,
                                spinHighMultipliersLocked = gSnap?.child("spinHighMultipliersLocked")?.getValue(Boolean::class.java) ?: false,
                                scratchCardMaxJackpot = (gSnap?.child("scratchCardMaxJackpot")?.value as? Number)?.toDouble() ?: 1000.0,
                                spinWinChanceMode = gSnap?.child("spinWinChanceMode")?.getValue(String::class.java) ?: "NORMAL",
                                diceWinRateMode = gSnap?.child("diceWinRateMode")?.getValue(String::class.java) ?: "NORMAL",
                                headTailWinRateMode = gSnap?.child("headTailWinRateMode")?.getValue(String::class.java) ?: "NORMAL"
                            )
                            
                            val user = UserControlSet(
                                spinWheel = GameUserControl(
                                    outcome = uSnap?.child("spinWheel/outcome")?.getValue(String::class.java)?.uppercase()?.trim() ?: "NORMAL",
                                    allowedTier = uSnap?.child("spinWheel/allowedTier")?.getValue(String::class.java)?.uppercase()?.trim() ?: "ALL"
                                ),
                                diceRoll = GameUserControl(
                                    outcome = uSnap?.child("diceRoll/outcome")?.getValue(String::class.java)?.uppercase()?.trim() ?: "NORMAL"
                                ),
                                headTail = GameUserControl(
                                    outcome = uSnap?.child("headTail/outcome")?.getValue(String::class.java)?.uppercase()?.trim() ?: "NORMAL"
                                ),
                                scratchCard = GameUserControl(
                                    outcome = uSnap?.child("scratchCard/outcome")?.getValue(String::class.java)?.uppercase()?.trim() ?: "NORMAL"
                                )
                            )
                            
                            cont.resume(EngineState(b, p, global, user))
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes a Scratch Card game session atomically.
     */
    suspend fun playScratchCard(
        uid: String,
        stakeAmount: Double,
        availableReservePool: Double
    ): Result<GameResult> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Invalid user ID"))
        if (stakeAmount <= 0) return Result.failure(IllegalArgumentException("Stake must be greater than 0"))

        val mutex = userLocks.getOrPut(uid) { Mutex() }
        
        return mutex.withLock {
            try {
                val database = FirebaseManager.getDatabase()
                    ?: return Result.failure(IllegalStateException("Firebase Database not available"))
                val dbRef = database.reference

                // 1. Authoritative State Fetch (Balance, Pool, Admin Controls)
                val state = fetchEngineState(uid, dbRef)

                // 2. Global Maintenance Check
                if (!state.global.scratchCardGameActive) {
                    return Result.failure(IllegalStateException("এই গেমটি বর্তমানে সাময়িকভাবে বন্ধ আছে।"))
                }

                if (state.balance < stakeAmount) {
                    return Result.failure(IllegalStateException("Insufficient game balance (৳${"%.2f".format(state.balance)})"))
                }

                // 3. Outcome Determination (Admin Overridden)
                val housePoolCeiling = state.reservePool + (stakeAmount * 0.90)
                
                // Payout Table: 0x, 1x (Break-even), 2x, 5x (Jackpot)
                var possibleOutcomes = listOf(0.0, 0.0, 1.0, 1.0, 2.0, 5.0)

                // Enforce Admin Overrides
                possibleOutcomes = when (state.user.scratchCard.outcome) {
                    "FORCE_LOSS" -> listOf(0.0)
                    "FORCE_WIN" -> possibleOutcomes.filter { it > 0.0 }
                    else -> possibleOutcomes
                }

                // Filter by house liquidity and Jackpot cap
                val maxJackpot = state.global.scratchCardMaxJackpot
                val affordableOutcomes = possibleOutcomes.filter { m ->
                    m == 0.0 || ((stakeAmount * m) <= housePoolCeiling && (stakeAmount * m) <= maxJackpot)
                }

                var multiplier = if (affordableOutcomes.isEmpty()) 0.0 else {
                    affordableOutcomes[secureRandom.nextInt(affordableOutcomes.size)]
                }
                
                // Final safety clamp for multiplier
                if (stakeAmount * multiplier > maxJackpot) {
                    multiplier = Math.floor(maxJackpot / stakeAmount)
                }

                val winAmount = stakeAmount * multiplier
                val isWin = winAmount > 0
                val netChange = winAmount - stakeAmount
                
                // 4. House Distribution
                val adminCut = stakeAmount * 0.10
                val poolAdjustment = (stakeAmount * 0.90) - winAmount
                
                // 5. Atomic Multi-Path Execution
                val historyRef = dbRef.child("miniGames/history").child(uid).push()
                val historyId = historyRef.key ?: "SCR_${System.currentTimeMillis()}"
                
                val historyEntry = mapOf(
                    "historyId" to historyId,
                    "gameType" to "SCRATCH_CARD",
                    "betAmount" to stakeAmount,
                    "multiplier" to "${multiplier.toInt()}x",
                    "multiplierVal" to multiplier,
                    "winAmount" to winAmount,
                    "netProfit" to netChange,
                    "status" to if (isWin) "WIN" else "LOSS",
                    "timestamp" to ServerValue.TIMESTAMP
                )

                val updates = hashMapOf<String, Any?>(
                    "gameWallets/$uid/balance" to ServerValue.increment(netChange),
                    "miniGames/vault/adminProfit" to ServerValue.increment(adminCut),
                    "miniGames/vault/reservePool" to ServerValue.increment(poolAdjustment),
                    "miniGames/history/$uid/$historyId" to historyEntry
                )

                val transactionSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                    dbRef.updateChildren(updates) { error, _ ->
                        continuation.resume(error == null)
                    }
                }

                if (transactionSuccess) {
                    Result.success(
                        GameResult(
                            isWin = isWin,
                            multiplier = multiplier,
                            winAmount = winAmount,
                            newBalance = state.balance + netChange,
                            transactionId = historyId
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to commit game transaction to database"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a Money Wheel (Spin Wheel) session atomically.
     */
    suspend fun playSpinWheel(
        uid: String,
        stakeAmount: Double,
        availableReservePool: Double,
        slices: List<Double>
    ): Result<GameResult> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Invalid user ID"))
        if (stakeAmount <= 0) return Result.failure(IllegalArgumentException("Stake must be greater than 0"))
        if (slices.size != 24) return Result.failure(IllegalArgumentException("Wheel must have exactly 24 slices"))

        val mutex = userLocks.getOrPut(uid) { Mutex() }

        return mutex.withLock {
            try {
                val database = FirebaseManager.getDatabase()
                    ?: return Result.failure(IllegalStateException("Firebase Database not available"))
                val dbRef = database.reference

                // 1. Authoritative State Fetch
                val state = fetchEngineState(uid, dbRef)

                // 2. Global Maintenance Check
                if (!state.global.spinGameActive) {
                    return Result.failure(IllegalStateException("এই গেমটি বর্তমানে সাময়িকভাবে বন্ধ আছে।"))
                }

                if (state.balance < stakeAmount) {
                    return Result.failure(IllegalStateException("Insufficient game balance"))
                }

                // 3. Weighted Outcome Determination (Zero-Loss Safe + Admin Controls)
                val weightMap = mapOf(
                    0.0 to 0.55,
                    1.0 to 0.20,
                    2.0 to 0.10,
                    5.0 to 0.05,
                    10.0 to 0.02,
                    25.0 to 0.01,
                    50.0 to 0.005
                )

                val poolCeiling = state.reservePool + (stakeAmount * 0.90)
                
                // Identify indices where win is permitted by ALL controls
                val eligibleIndices = slices.indices.filter { i ->
                    val m = slices[i]
                    
                    // a. House Liquidity check
                    val isAffordable = m == 0.0 || (stakeAmount * m) <= poolCeiling
                    
                    // b. Global High Multiplier Lock
                    val passGlobalLock = if (state.global.spinHighMultipliersLocked) m <= 2.0 else true
                    
                    // c. User Tier restriction
                    val passTierLock = when (state.user.spinWheel.allowedTier) {
                        "LOW_ONLY" -> m <= 2.0
                        "MID" -> m <= 5.0
                        else -> true // "ALL"
                    }

                    // d. User Outcome Override (FORCE_WIN / FORCE_LOSS)
                    val passOutcomeLock = when (state.user.spinWheel.outcome) {
                        "FORCE_WIN" -> m > 0.0
                        "FORCE_LOSS" -> m == 0.0
                        else -> true
                    }

                    isAffordable && passGlobalLock && passTierLock && passOutcomeLock
                }

                // Fallback: If filtered list is empty (e.g., house broke but FORCE_WIN), force a loss (0x)
                val finalIndices = if (eligibleIndices.isEmpty()) {
                    slices.indices.filter { slices[it] == 0.0 }
                } else {
                    eligibleIndices
                }

                // Weighted Selection
                val weightedIndices = finalIndices.map { i ->
                    i to (weightMap[slices[i]] ?: 0.01)
                }
                
                val totalWeight = weightedIndices.sumOf { it.second }
                val roll = secureRandom.nextDouble() * totalWeight
                
                var cumulative = 0.0
                var winnerIndex = finalIndices.first()
                for ((index, weight) in weightedIndices) {
                    cumulative += weight
                    if (roll <= cumulative) {
                        winnerIndex = index
                        break
                    }
                }

                val multiplier = slices[winnerIndex]
                val winAmount = stakeAmount * multiplier
                val isWin = winAmount > 0
                val netChange = winAmount - stakeAmount

                // 4. House Distribution
                val adminCut = stakeAmount * 0.10
                val poolAdjustment = (stakeAmount * 0.90) - winAmount

                // 5. Atomic Multi-Path Update
                val historyRef = dbRef.child("miniGames/history").child(uid).push()
                val historyId = historyRef.key ?: "SPIN_${System.currentTimeMillis()}"

                val historyEntry = mapOf(
                    "historyId" to historyId,
                    "gameType" to "SPIN_WHEEL",
                    "betAmount" to stakeAmount,
                    "multiplier" to "${multiplier.toInt()}x",
                    "multiplierVal" to multiplier,
                    "winAmount" to winAmount,
                    "netProfit" to netChange,
                    "status" to if (isWin) "WIN" else "LOSS",
                    "timestamp" to ServerValue.TIMESTAMP
                )

                val updates = hashMapOf<String, Any?>(
                    "gameWallets/$uid/balance" to ServerValue.increment(netChange),
                    "miniGames/vault/adminProfit" to ServerValue.increment(adminCut),
                    "miniGames/vault/reservePool" to ServerValue.increment(poolAdjustment),
                    "miniGames/history/$uid/$historyId" to historyEntry
                )

                val success = suspendCancellableCoroutine<Boolean> { cont ->
                    dbRef.updateChildren(updates) { err, _ -> cont.resume(err == null) }
                }

                if (success) {
                    Result.success(
                        GameResult(
                            isWin = isWin,
                            multiplier = multiplier,
                            winAmount = winAmount,
                            newBalance = state.balance + netChange,
                            transactionId = historyId,
                            winnerIndex = winnerIndex
                        )
                    )
                } else {
                    Result.failure(Exception("Database update failed"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a Dice Roll (EVEN/ODD) session atomically.
     */
    suspend fun playDiceRoll(
        uid: String,
        stakeAmount: Double,
        choice: String,
        availableReservePool: Double
    ): Result<GameResult> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Invalid user ID"))
        if (stakeAmount <= 0) return Result.failure(IllegalArgumentException("Stake must be greater than 0"))

        val mutex = userLocks.getOrPut(uid) { Mutex() }

        return mutex.withLock {
            try {
                val database = FirebaseManager.getDatabase()
                    ?: return Result.failure(IllegalStateException("Firebase Database not available"))
                val dbRef = database.reference

                // 1. Authoritative State Fetch
                val state = fetchEngineState(uid, dbRef)

                // 2. Global Maintenance Check
                if (!state.global.diceGameActive) {
                    return Result.failure(IllegalStateException("এই গেমটি বর্তমানে সাময়িকভাবে বন্ধ আছে।"))
                }

                if (state.balance < stakeAmount) {
                    return Result.failure(IllegalStateException("Insufficient game balance"))
                }

                // 3. Outcome Determination
                val payoutMultiplier = 1.9
                val potentialPayout = stakeAmount * payoutMultiplier
                val poolCeiling = state.reservePool + (stakeAmount * 0.90)
                val canAffordWin = potentialPayout <= poolCeiling

                var d1 = secureRandom.nextInt(6) + 1
                var d2 = secureRandom.nextInt(6) + 1
                var sum = d1 + d2
                var isEven = sum % 2 == 0
                var userMatchesChoice = if (choice.equals("EVEN", true)) isEven else !isEven

                // Enforce Admin Overrides
                val finalUserWins = when (state.user.diceRoll.outcome) {
                    "FORCE_WIN" -> if (canAffordWin) true else false
                    "FORCE_LOSS" -> false
                    else -> userMatchesChoice && canAffordWin
                }

                // Adjust dice to match the final decision if needed
                if (finalUserWins != userMatchesChoice) {
                    // Flip parity by adjusting one die
                    if (d1 < 6) d1++ else d1--
                    sum = d1 + d2
                }

                val winAmount = if (finalUserWins) potentialPayout else 0.0
                val isWin = finalUserWins
                val netChange = winAmount - stakeAmount

                // 4. House Distribution
                val adminCut = stakeAmount * 0.10
                val poolAdjustment = (stakeAmount * 0.90) - winAmount

                // 5. Atomic Multi-Path Update
                val historyRef = dbRef.child("miniGames/history").child(uid).push()
                val historyId = historyRef.key ?: "DICE_${System.currentTimeMillis()}"

                val historyEntry = mapOf(
                    "historyId" to historyId,
                    "gameType" to "DICE",
                    "betAmount" to stakeAmount,
                    "choice" to choice,
                    "rolledSum" to sum,
                    "diceResult" to "$d1,$d2",
                    "multiplier" to if (isWin) "${payoutMultiplier}x" else "0x",
                    "multiplierVal" to if (isWin) payoutMultiplier else 0.0,
                    "winAmount" to winAmount,
                    "netProfit" to netChange,
                    "status" to if (isWin) "WIN" else "LOSS",
                    "timestamp" to ServerValue.TIMESTAMP
                )

                val updates = hashMapOf<String, Any?>(
                    "gameWallets/$uid/balance" to ServerValue.increment(netChange),
                    "miniGames/vault/adminProfit" to ServerValue.increment(adminCut),
                    "miniGames/vault/reservePool" to ServerValue.increment(poolAdjustment),
                    "miniGames/history/$uid/$historyId" to historyEntry
                )

                val success = suspendCancellableCoroutine<Boolean> { cont ->
                    dbRef.updateChildren(updates) { err, _ -> cont.resume(err == null) }
                }

                if (success) {
                    Result.success(
                        GameResult(
                            isWin = isWin,
                            multiplier = if (isWin) payoutMultiplier else 0.0,
                            winAmount = winAmount,
                            newBalance = state.balance + netChange,
                            transactionId = historyId,
                            dice1 = d1,
                            dice2 = d2
                        )
                    )
                } else {
                    Result.failure(Exception("Database update failed"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a Head & Tail coin flip session atomically.
     */
    suspend fun playHeadTail(
        uid: String,
        stakeAmount: Double,
        choice: String,
        availableReservePool: Double
    ): Result<GameResult> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Invalid user ID"))
        if (stakeAmount <= 0) return Result.failure(IllegalArgumentException("Stake must be greater than 0"))

        val mutex = userLocks.getOrPut(uid) { Mutex() }

        return mutex.withLock {
            try {
                val database = FirebaseManager.getDatabase()
                    ?: return Result.failure(IllegalStateException("Firebase Database not available"))
                val dbRef = database.reference

                // 1. Authoritative State Fetch
                val state = fetchEngineState(uid, dbRef)

                // 2. Global Maintenance Check
                if (!state.global.headTailGameActive) {
                    return Result.failure(IllegalStateException("এই গেমটি বর্তমানে সাময়িকভাবে বন্ধ আছে।"))
                }

                if (state.balance < stakeAmount) {
                    return Result.failure(IllegalStateException("Insufficient game balance"))
                }

                // 3. Outcome Determination
                val payoutMultiplier = 1.9
                val potentialPayout = stakeAmount * payoutMultiplier
                val poolCeiling = state.reservePool + (stakeAmount * 0.90)
                val canAffordWin = potentialPayout <= poolCeiling

                val initiallyHeads = secureRandom.nextBoolean()
                val initialResultSide = if (initiallyHeads) "HEAD" else "TAIL"
                val userMatchesChoice = choice.equals(initialResultSide, ignoreCase = true)

                // Enforce Admin Overrides
                val finalUserWins = when (state.user.headTail.outcome) {
                    "FORCE_WIN" -> if (canAffordWin) true else false
                    "FORCE_LOSS" -> false
                    else -> userMatchesChoice && canAffordWin
                }

                // Determine final side based on the decision
                val finalResultSide = if (finalUserWins) {
                    choice.uppercase()
                } else {
                    if (choice.uppercase() == "HEAD") "TAIL" else "HEAD"
                }

                val winAmount = if (finalUserWins) potentialPayout else 0.0
                val isWin = finalUserWins
                val netChange = winAmount - stakeAmount

                // 4. House Distribution
                val adminCut = stakeAmount * 0.10
                val poolAdjustment = (stakeAmount * 0.90) - winAmount

                // 5. Atomic Multi-Path Update
                val historyRef = dbRef.child("miniGames/history").child(uid).push()
                val historyId = historyRef.key ?: "HT_${System.currentTimeMillis()}"

                val historyEntry = mapOf(
                    "historyId" to historyId,
                    "gameType" to "HEAD_TAIL",
                    "betAmount" to stakeAmount,
                    "choice" to choice,
                    "flippedSide" to finalResultSide,
                    "multiplier" to if (isWin) "${payoutMultiplier}x" else "0x",
                    "multiplierVal" to if (isWin) payoutMultiplier else 0.0,
                    "winAmount" to winAmount,
                    "netProfit" to netChange,
                    "status" to if (isWin) "WIN" else "LOSS",
                    "timestamp" to ServerValue.TIMESTAMP
                )

                val updates = hashMapOf<String, Any?>(
                    "gameWallets/$uid/balance" to ServerValue.increment(netChange),
                    "miniGames/vault/adminProfit" to ServerValue.increment(adminCut),
                    "miniGames/vault/reservePool" to ServerValue.increment(poolAdjustment),
                    "miniGames/history/$uid/$historyId" to historyEntry
                )

                val success = suspendCancellableCoroutine<Boolean> { cont ->
                    dbRef.updateChildren(updates) { err, _ -> cont.resume(err == null) }
                }

                if (success) {
                    Result.success(
                        GameResult(
                            isWin = isWin,
                            multiplier = if (isWin) payoutMultiplier else 0.0,
                            winAmount = winAmount,
                            newBalance = state.balance + netChange,
                            transactionId = historyId,
                            coinSide = finalResultSide
                        )
                    )
                } else {
                    Result.failure(Exception("Database update failed"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
