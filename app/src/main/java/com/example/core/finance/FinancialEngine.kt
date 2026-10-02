package com.example.core.finance

import com.example.domain.model.*

object FinancialEngine {
  const val MIN_DEPOSIT_MINOR_UNITS: Long = 5000L
  const val MAX_DEPOSIT_MINOR_UNITS: Long = 2500000L
  const val MIN_WITHDRAW_MINOR_UNITS: Long = 20000L
  const val MAX_WITHDRAW_MINOR_UNITS: Long = 1000000L

  const val COMMISSION_TIER_1_THRESHOLD: Long = 10000L
  const val COMMISSION_TIER_2_THRESHOLD: Long = 20000L
  const val COMMISSION_TIER_1_AMOUNT: Long = 1000L
  const val COMMISSION_TIER_2_AMOUNT: Long = 2000L
  const val COMMISSION_TIER_3_AMOUNT: Long = 4000L

  fun calculateCommission(entryFeeMinorUnits: Long): Long {
    require(entryFeeMinorUnits > 0L) { "Entry fee must be greater than zero" }
    return when {
      entryFeeMinorUnits <= COMMISSION_TIER_1_THRESHOLD -> COMMISSION_TIER_1_AMOUNT
      entryFeeMinorUnits <= COMMISSION_TIER_2_THRESHOLD -> COMMISSION_TIER_2_AMOUNT
      else -> COMMISSION_TIER_3_AMOUNT
    }
  }

  data class MatchPrizeCalculation(
    val totalPool: Long,
    val commissionAmount: Long,
    val winnerPrize: Long,
  )

  fun calculateMatchPrize(
    entryFeeMinorUnits: Long,
    playerCount: Int = 2,
  ): MatchPrizeCalculation {
    require(entryFeeMinorUnits > 0L) { "Entry fee must be greater than zero" }
    require(playerCount == 2) { "Match requires exactly 2 players" }

    val totalPool = entryFeeMinorUnits * playerCount
    val commissionAmount = calculateCommission(entryFeeMinorUnits)
    val winnerPrize = maxOf(0L, totalPool - commissionAmount)

    return MatchPrizeCalculation(
      totalPool = totalPool,
      commissionAmount = commissionAmount,
      winnerPrize = winnerPrize,
    )
  }

  sealed interface MutationResult {
    data class Success(
      val wallet: WalletEntity,
      val transaction: TransactionEntity,
      val isAlreadyProcessed: Boolean = false,
    ) : MutationResult

    data class Failure(
      val reason: String,
      val errorCode: String,
    ) : MutationResult
  }

  fun processDepositApproval(
    currentWallet: WalletEntity,
    deposit: DepositEntity,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "DEP_${deposit.depositId}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    if (deposit.amount < MIN_DEPOSIT_MINOR_UNITS) {
      return MutationResult.Failure("Deposit amount below minimum limit", "ERR_MIN_DEPOSIT")
    }
    if (deposit.amount > MAX_DEPOSIT_MINOR_UNITS) {
      return MutationResult.Failure("Deposit amount exceeds maximum limit", "ERR_MAX_DEPOSIT")
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance + deposit.amount

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      balance = afterBalance / 100.0,
      totalDeposited = currentWallet.totalDeposited + deposit.amount,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_${timestamp}_${deposit.depositId}",
      uid = currentWallet.uid,
      amount = deposit.amount,
      type = TransactionType.DEPOSIT.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "${deposit.method}_DEPOSIT",
      referenceId = mutationKey,
      description = "Deposit approved via ${deposit.method} (TrxID: ${deposit.trxId})",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processWithdrawalHold(
    currentWallet: WalletEntity,
    withdrawal: WithdrawalEntity,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "WTH_HOLD_${withdrawal.withdrawalId}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    if (withdrawal.amount < MIN_WITHDRAW_MINOR_UNITS) {
      return MutationResult.Failure("Withdrawal amount below minimum limit (200 BDT)", "ERR_MIN_WITHDRAW")
    }
    if (withdrawal.amount > MAX_WITHDRAW_MINOR_UNITS) {
      return MutationResult.Failure("Withdrawal amount exceeds maximum limit (10,000 BDT)", "ERR_MAX_WITHDRAW")
    }
    if (currentWallet.availableBalance < withdrawal.amount) {
      return MutationResult.Failure("Insufficient available balance for withdrawal", "ERR_INSUFFICIENT_BALANCE")
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance - withdrawal.amount

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      pendingBalance = currentWallet.pendingBalance + withdrawal.amount,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_HOLD_${timestamp}_${withdrawal.withdrawalId}",
      uid = currentWallet.uid,
      amount = withdrawal.amount,
      type = TransactionType.WITHDRAW_HOLD.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "${withdrawal.method}_PAYOUT_HOLD",
      referenceId = mutationKey,
      description = "Withdrawal hold for payout to ${withdrawal.recipientNumber}",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processWithdrawalCompletion(
    currentWallet: WalletEntity,
    withdrawal: WithdrawalEntity,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "WTH_CMP_${withdrawal.withdrawalId}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    if (currentWallet.pendingBalance < withdrawal.amount) {
      return MutationResult.Failure("Pending balance is less than withdrawal amount", "ERR_INVALID_PENDING")
    }

    val newPendingBalance = currentWallet.pendingBalance - withdrawal.amount
    val newTotalWithdrawn = currentWallet.totalWithdrawn + withdrawal.amount
    val updatedWallet = currentWallet.copy(
      pendingBalance = newPendingBalance,
      totalWithdrawn = newTotalWithdrawn,
      balance = currentWallet.availableBalance / 100.0,
      lockedBalance = newPendingBalance / 100.0,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_CMP_${timestamp}_${withdrawal.withdrawalId}",
      uid = currentWallet.uid,
      amount = withdrawal.amount,
      type = TransactionType.WITHDRAW.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = currentWallet.availableBalance,
      afterBalance = currentWallet.availableBalance,
      source = "${withdrawal.method}_PAYOUT_COMPLETE",
      referenceId = mutationKey,
      description = "Withdrawal completed via ${withdrawal.method} to ${withdrawal.recipientNumber}",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processWithdrawalRejection(
    currentWallet: WalletEntity,
    withdrawal: WithdrawalEntity,
    reason: String,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "WTH_REL_${withdrawal.withdrawalId}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    if (currentWallet.pendingBalance < withdrawal.amount) {
      return MutationResult.Failure("Pending balance is less than withdrawal release amount", "ERR_INVALID_PENDING")
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance + withdrawal.amount
    val newPendingBalance = currentWallet.pendingBalance - withdrawal.amount

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      pendingBalance = newPendingBalance,
      balance = afterBalance / 100.0,
      lockedBalance = newPendingBalance / 100.0,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_REL_${timestamp}_${withdrawal.withdrawalId}",
      uid = currentWallet.uid,
      amount = withdrawal.amount,
      type = TransactionType.WITHDRAW_RELEASE.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "WITHDRAWAL_REJECTED",
      referenceId = mutationKey,
      description = "Withdrawal rejected & released: $reason",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processMatchEntryDeduction(
    currentWallet: WalletEntity,
    matchId: String,
    entryFeeMinorUnits: Long,
    matchTitle: String,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "MATCH_ENTRY_${matchId}_${currentWallet.uid}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    if (currentWallet.availableBalance < entryFeeMinorUnits) {
      return MutationResult.Failure("Insufficient balance to join match", "ERR_INSUFFICIENT_BALANCE")
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance - entryFeeMinorUnits

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      balance = afterBalance / 100.0,
      pendingBalance = currentWallet.pendingBalance + entryFeeMinorUnits,
      lockedBalance = (currentWallet.pendingBalance + entryFeeMinorUnits) / 100.0,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_ENTRY_${timestamp}_$matchId",
      uid = currentWallet.uid,
      amount = entryFeeMinorUnits,
      type = TransactionType.MATCH_ENTRY.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "CONTEST_ESCROW",
      referenceId = mutationKey,
      description = "Entry fee held for match $matchTitle",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processMatchPrizeCredit(
    currentWallet: WalletEntity,
    matchId: String,
    prizeMinorUnits: Long,
    heldEntryFeeMinorUnits: Long,
    matchTitle: String,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "MATCH_PRIZE_${matchId}_${currentWallet.uid}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance + prizeMinorUnits
    val newPendingBalance = (currentWallet.pendingBalance - heldEntryFeeMinorUnits).coerceAtLeast(0L)

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      pendingBalance = newPendingBalance,
      totalWinnings = currentWallet.totalWinnings + prizeMinorUnits,
      winningBalance = (currentWallet.totalWinnings + prizeMinorUnits) / 100.0,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_PRIZE_${timestamp}_$matchId",
      uid = currentWallet.uid,
      amount = prizeMinorUnits,
      type = TransactionType.MATCH_PRIZE.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "MATCH_VICTORY",
      referenceId = mutationKey,
      description = "Prize credited for victory in $matchTitle",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processMatchRefund(
    currentWallet: WalletEntity,
    matchId: String,
    entryFeeMinorUnits: Long,
    reason: String,
    existingTransactions: List<TransactionEntity> = emptyList(),
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val mutationKey = "MATCH_REFUND_${matchId}_${currentWallet.uid}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }
    if (existingTxn != null) {
      return MutationResult.Success(
        wallet = currentWallet,
        transaction = existingTxn,
        isAlreadyProcessed = true,
      )
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance + entryFeeMinorUnits
    val newPendingBalance = (currentWallet.pendingBalance - entryFeeMinorUnits).coerceAtLeast(0L)

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      balance = afterBalance / 100.0,
      pendingBalance = newPendingBalance,
      updatedAt = timestamp,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_REFUND_${timestamp}_$matchId",
      uid = currentWallet.uid,
      amount = entryFeeMinorUnits,
      type = TransactionType.MATCH_REFUND.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "MATCH_REFUND",
      referenceId = mutationKey,
      description = "Refund for match: $reason",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  fun processAdminAdjustment(
    currentWallet: WalletEntity,
    adminUid: String,
    adjustmentMinorUnits: Long,
    reason: String,
    timestamp: Long = System.currentTimeMillis(),
  ): MutationResult {
    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance + adjustmentMinorUnits
    if (afterBalance < 0L) {
      return MutationResult.Failure("Admin debit would result in negative balance", "ERR_NEGATIVE_BALANCE")
    }

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      balance = afterBalance / 100.0,
      updatedAt = timestamp,
    )

    val isCredit = adjustmentMinorUnits >= 0L
    val transaction = TransactionEntity(
      transactionId = "TXN_ADJ_${timestamp}_${currentWallet.uid}",
      uid = currentWallet.uid,
      amount = kotlin.math.abs(adjustmentMinorUnits),
      type = if (isCredit) TransactionType.ADMIN_CREDIT.name else TransactionType.ADMIN_DEBIT.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "ADMIN_ADJUSTMENT",
      referenceId = "ADMIN_$adminUid",
      description = "Admin adjustment ($reason)",
      createdAt = timestamp,
      processedAt = timestamp,
    )

    return MutationResult.Success(updatedWallet, transaction)
  }

  sealed class MatchJoinResult {
    data class Success(
      val updatedMatch: MatchEntity,
      val newPlayer: MatchPlayerEntity,
      val updatedWallet: WalletEntity,
      val transaction: TransactionEntity,
      val isAlreadyProcessed: Boolean = false,
    ) : MatchJoinResult() {
      val match: MatchEntity get() = updatedMatch
      val matchPlayer: MatchPlayerEntity get() = newPlayer
      val wallet: WalletEntity get() = updatedWallet
    }

    data class Failure(
      val reason: String,
      val errorCode: String,
    ) : MatchJoinResult()
  }

  sealed class AdminMatchResult {
    data class Success(
      val updatedMatch: MatchEntity,
    ) : AdminMatchResult()

    data class Failure(
      val reason: String,
      val errorCode: String,
    ) : AdminMatchResult()
  }

  fun processMatchJoin(
    match: MatchEntity,
    user: UserEntity,
    currentWallet: WalletEntity,
    existingPlayers: List<MatchPlayerEntity>,
    existingTransactions: List<TransactionEntity> = emptyList(),
    currentTime: Long = System.currentTimeMillis(),
  ): MatchJoinResult {
    if (user.isAccountBlocked) {
      return MatchJoinResult.Failure(
        reason = "Your account is blocked. Cannot join tournament.",
        errorCode = "ERR_BLOCKED_USER",
      )
    }

    if (match.matchId.isBlank()) {
      return MatchJoinResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }

    val existingPlayer = existingPlayers.find {
      it.effectiveUid == user.uid || it.effectiveUid == user.userId
    }
    val mutationKey = "MATCH_ENTRY_${match.matchId}_${user.uid}"
    val existingTxn = existingTransactions.find {
      it.referenceId == mutationKey && it.status == TransactionStatus.COMPLETED.name
    }

    if (existingPlayer != null) {
      if (existingTxn != null) {
        return MatchJoinResult.Success(
          updatedMatch = match,
          newPlayer = existingPlayer,
          updatedWallet = currentWallet,
          transaction = existingTxn,
          isAlreadyProcessed = true,
        )
      }
      return MatchJoinResult.Failure(
        reason = "You have already joined this match.",
        errorCode = "ERR_ALREADY_JOINED",
      )
    }

    if (existingTxn != null) {
      return MatchJoinResult.Failure(
        reason = "A completed entry transaction already exists for this match.",
        errorCode = "ERR_DUPLICATE_TXN",
      )
    }

    val isMatchFull = existingPlayers.size >= match.maxPlayers ||
        match.status.equals(MatchStatus.FULL.name, ignoreCase = true)
    if (isMatchFull) {
      return MatchJoinResult.Failure(
        reason = "Match is full. Maximum ${match.maxPlayers} players allowed.",
        errorCode = "ERR_MATCH_FULL",
      )
    }

    val isAvailable = match.status.equals(MatchStatus.AVAILABLE.name, ignoreCase = true)
    if (!isAvailable) {
      return MatchJoinResult.Failure(
        reason = "Match is not available to join (current status: ${match.status}).",
        errorCode = "ERR_MATCH_UNAVAILABLE",
      )
    }

    val scheduledTime = match.effectiveScheduledAt
    if (scheduledTime > 0L && currentTime >= scheduledTime) {
      return MatchJoinResult.Failure(
        reason = "Match registration deadline has passed.",
        errorCode = "ERR_CUTOFF_PASSED",
      )
    }

    val entryFeeMinorUnits = match.effectiveEntryFeeMinorUnits
    if (entryFeeMinorUnits < 0L) {
      return MatchJoinResult.Failure(
        reason = "Invalid entry fee on match record.",
        errorCode = "ERR_INVALID_FEE",
      )
    }

    // 10% Bonus Usage Logic
    val bonusBalancePaisa = currentWallet.bonusBalance.toLong()
    val maxBonusAllowedPaisa = (entryFeeMinorUnits * 0.10).toLong()
    val usableBonusPaisa = kotlin.math.min(bonusBalancePaisa, maxBonusAllowedPaisa)
    val remainingFeePaisa = entryFeeMinorUnits - usableBonusPaisa

    if (currentWallet.availableBalance < remainingFeePaisa) {
      return MatchJoinResult.Failure(
        reason = "পর্যাপ্ত মেইন ব্যালেন্স নেই! ডিপোজিট করুন।",
        errorCode = "ERR_INSUFFICIENT_MAIN_BALANCE",
      )
    }

    val beforeBalance = currentWallet.availableBalance
    val afterBalance = beforeBalance - remainingFeePaisa
    val newBonusBalance = (currentWallet.bonusBalance - usableBonusPaisa).coerceAtLeast(0.0)

    val updatedWallet = currentWallet.copy(
      availableBalance = afterBalance,
      balance = afterBalance / 100.0,
      bonusBalance = newBonusBalance,
      pendingBalance = currentWallet.pendingBalance + entryFeeMinorUnits,
      lockedBalance = (currentWallet.pendingBalance + entryFeeMinorUnits) / 100.0,
      updatedAt = currentTime,
    )

    val transaction = TransactionEntity(
      transactionId = "TXN_ENTRY_${currentTime}_${match.matchId}_${user.uid}",
      uid = user.uid,
      amount = entryFeeMinorUnits,
      type = TransactionType.MATCH_ENTRY.name,
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = beforeBalance,
      afterBalance = afterBalance,
      source = "MATCH_ENTRY",
      referenceId = mutationKey,
      description = "Entry fee for ${match.title.ifBlank { match.matchNumber }} (Used Bonus: ৳${"%.2f".format(usableBonusPaisa / 100.0)})",
      createdAt = currentTime,
      processedAt = currentTime,
    )

    val slot = if (existingPlayers.isEmpty()) PlayerSlot.PLAYER_1.name else PlayerSlot.PLAYER_2.name
    val newPlayer = MatchPlayerEntity(
      matchPlayerId = "MP_${match.matchId}_${user.uid}",
      matchId = match.matchId,
      uid = user.uid,
      userId = user.uid,
      username = user.effectiveName,
      slot = slot,
      joinedAt = currentTime,
      entryFeeMinorUnits = entryFeeMinorUnits,
      status = "JOINED",
      isReady = true,
    )

    val newCount = existingPlayers.size + 1
    val newStatus = if (newCount >= match.maxPlayers) MatchStatus.FULL.name else MatchStatus.AVAILABLE.name
    val updatedMatch = match.copy(
      joinedPlayersCount = newCount,
      status = newStatus,
      updatedAt = currentTime,
    )

    return MatchJoinResult.Success(
      updatedMatch = updatedMatch,
      newPlayer = newPlayer,
      updatedWallet = updatedWallet,
      transaction = transaction,
    )
  }

  fun processAdminSetGameCode(
    match: MatchEntity,
    adminUser: UserEntity,
    gameCode: String,
    currentTime: Long = System.currentTimeMillis(),
  ): AdminMatchResult {
    if (!adminUser.role.equals("ADMIN", ignoreCase = true) && !adminUser.role.equals("SUPER_ADMIN", ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Only Super Admin is authorized to set game room code.",
        errorCode = "ERR_UNAUTHORIZED",
      )
    }
    if (match.matchId.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }
    if (match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
        match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Cannot add room code to a completed or cancelled match.",
        errorCode = "ERR_INVALID_STATUS",
      )
    }
    val trimmedCode = gameCode.trim()
    if (trimmedCode.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Game room code cannot be blank.",
        errorCode = "ERR_EMPTY_CODE",
      )
    }

    val updatedMatch = match.copy(
      gameCode = trimmedCode,
      isCodeVisible = true,
      status = MatchStatus.CODE_ADDED.name,
      updatedAt = currentTime,
    )
    return AdminMatchResult.Success(updatedMatch)
  }

  fun processAdminSetMatchRunning(
    match: MatchEntity,
    adminUser: UserEntity,
    currentTime: Long = System.currentTimeMillis(),
  ): AdminMatchResult {
    if (!adminUser.role.equals("ADMIN", ignoreCase = true) && !adminUser.role.equals("SUPER_ADMIN", ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Only Super Admin is authorized to start a match.",
        errorCode = "ERR_UNAUTHORIZED",
      )
    }
    if (match.matchId.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }
    val canStart = match.status.equals(MatchStatus.CODE_ADDED.name, ignoreCase = true) ||
        (match.status.equals(MatchStatus.FULL.name, ignoreCase = true) && match.gameCode.isNotBlank())
    if (!canStart) {
      return AdminMatchResult.Failure(
        reason = "Match cannot transition to RUNNING from ${match.status}.",
        errorCode = "ERR_INVALID_TRANSITION",
      )
    }

    val updatedMatch = match.copy(
      status = MatchStatus.RUNNING.name,
      startedAt = match.startedAt ?: currentTime,
      updatedAt = currentTime,
    )
    return AdminMatchResult.Success(updatedMatch)
  }

  fun processAdminPauseMatch(
    match: MatchEntity,
    adminUser: UserEntity,
    reason: String = "",
    currentTime: Long = System.currentTimeMillis(),
  ): AdminMatchResult {
    if (!adminUser.role.equals("ADMIN", ignoreCase = true) && !adminUser.role.equals("SUPER_ADMIN", ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Only Super Admin is authorized to pause a match.",
        errorCode = "ERR_UNAUTHORIZED",
      )
    }
    if (match.matchId.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }
    if (match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
        match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Cannot pause a ${match.status} match.",
        errorCode = "ERR_INVALID_TRANSITION",
      )
    }

    val updatedMatch = match.copy(
      status = MatchStatus.PAUSED.name,
      updatedAt = currentTime,
    )
    return AdminMatchResult.Success(updatedMatch)
  }

  fun processAdminDisableMatch(
    match: MatchEntity,
    adminUser: UserEntity,
    reason: String = "",
    currentTime: Long = System.currentTimeMillis(),
  ): AdminMatchResult {
    if (!adminUser.role.equals("ADMIN", ignoreCase = true) && !adminUser.role.equals("SUPER_ADMIN", ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Only Super Admin is authorized to disable a match.",
        errorCode = "ERR_UNAUTHORIZED",
      )
    }
    if (match.matchId.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }
    if (match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
        match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Cannot disable a ${match.status} match.",
        errorCode = "ERR_INVALID_TRANSITION",
      )
    }

    val updatedMatch = match.copy(
      status = MatchStatus.DISABLED.name,
      updatedAt = currentTime,
    )
    return AdminMatchResult.Success(updatedMatch)
  }

  fun processAdminCancelMatch(
    match: MatchEntity,
    adminUser: UserEntity,
    reason: String = "",
    currentTime: Long = System.currentTimeMillis(),
  ): AdminMatchResult {
    if (!adminUser.role.equals("ADMIN", ignoreCase = true) && !adminUser.role.equals("SUPER_ADMIN", ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Only Super Admin is authorized to cancel a match.",
        errorCode = "ERR_UNAUTHORIZED",
      )
    }
    if (match.matchId.isBlank()) {
      return AdminMatchResult.Failure(
        reason = "Match not found.",
        errorCode = "ERR_MATCH_NOT_FOUND",
      )
    }
    if (match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
        match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)) {
      return AdminMatchResult.Failure(
        reason = "Cannot cancel a ${match.status} match.",
        errorCode = "ERR_INVALID_TRANSITION",
      )
    }

    val updatedMatch = match.copy(
      status = MatchStatus.CANCELLED.name,
      updatedAt = currentTime,
    )
    return AdminMatchResult.Success(updatedMatch)
  }

  fun getPermittedGameRoomCode(
    match: MatchEntity,
    userId: String,
    isJoined: Boolean,
    isAdmin: Boolean,
  ): String? {
    if (!isJoined && !isAdmin) return null
    if (match.gameCode.isBlank()) return null
    return match.gameCode
  }
}
