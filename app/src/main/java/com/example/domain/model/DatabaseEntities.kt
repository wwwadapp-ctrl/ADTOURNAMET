package com.example.domain.model

import com.google.firebase.database.IgnoreExtraProperties

enum class AccountStatus {
  ACTIVE,
  BLOCKED,
  BANNED
}

@IgnoreExtraProperties
data class UserEntity(
  val uid: String = "",
  val name: String = "",
  val mobileNumber: String = "",
  val joinDate: Long = 0L,
  val status: String = AccountStatus.ACTIVE.name,
  val profilePhotoUrl: String? = null,
  val userId: String = uid,
  val fullName: String = name,
  val profilePhoto: String = profilePhotoUrl ?: "",
  val accountStatus: String = status,
  val totalMatches: Int = 0,
  val wins: Int = 0,
  val losses: Int = 0,
  val role: String = "PLAYER",
  val lastActiveAt: Long = 0L,
  val savedBkashNumber: String = "",
  val savedNagadNumber: String = "",
  val displayName: String = name.ifEmpty { fullName },
  val phoneNumber: String = mobileNumber,
  val avatarUrl: String = profilePhotoUrl ?: profilePhoto,
  val isBlocked: Boolean = status.equals(AccountStatus.BLOCKED.name, ignoreCase = true) ||
      accountStatus.equals(AccountStatus.BLOCKED.name, ignoreCase = true) ||
      status.equals(AccountStatus.BANNED.name, ignoreCase = true) ||
      accountStatus.equals(AccountStatus.BANNED.name, ignoreCase = true),
  @get:com.google.firebase.database.Exclude
  val blocked: Any? = false,
  val createdAt: Long = joinDate,
  // Raw fields from Firebase (stored strictly as Paisa/Minor Units)
  val walletBalance: Double = 0.0,
  val totalWinnings: Double = 0.0,
  val referralCode: String = "",
  val totalRefers: Int = 0,
  val referredBy: String = "",
  val firstDepositBonusClaimed: Boolean = false,
) {
  val effectiveName: String get() = name.ifEmpty { fullName.ifEmpty { displayName.ifEmpty { "Player" } } }
  val effectiveMobile: String get() = mobileNumber.ifEmpty { phoneNumber }
  val effectivePhoto: String get() = profilePhotoUrl ?: profilePhoto.ifEmpty { avatarUrl }
  
  private val isBlockedDynamic: Boolean 
    get() = when (val b = blocked) {
      is Boolean -> b
      is String -> b.equals("true", ignoreCase = true) || b == "1"
      is Number -> b.toInt() == 1
      else -> false
    }

  val isAccountBlocked: Boolean get() = isBlocked ||
      isBlockedDynamic ||
      status.equals(AccountStatus.BLOCKED.name, ignoreCase = true) ||
      status.equals(AccountStatus.BANNED.name, ignoreCase = true) ||
      accountStatus.equals(AccountStatus.BLOCKED.name, ignoreCase = true) ||
      accountStatus.equals(AccountStatus.BANNED.name, ignoreCase = true)

  // Derived properties that always return BDT (Taka) by dividing minor units by 100.0
  val walletBalanceAmount: Double get() = walletBalance / 100.0
  val totalWinningsAmount: Double get() = totalWinnings / 100.0
  
  // Public accessors for UI to use
  fun getDisplayWalletBalance(): Double = walletBalanceAmount
  fun getDisplayTotalWinnings(): Double = totalWinningsAmount
}

@IgnoreExtraProperties
data class WalletEntity(
  val uid: String = "",
  val availableBalance: Long = 0L,
  val pendingBalance: Long = 0L,
  val totalDeposited: Long = 0L,
  val totalWithdrawn: Long = 0L,
  val totalWinnings: Long = 0L,
  val updatedAt: Long = 0L,
  val walletId: String = uid,
  val userId: String = uid,
  // Internal fields to capture Firebase fields (stored strictly as Paisa/Minor Units)
  val balance: Double = 0.0,
  val winningBalance: Double = 0.0,
  val lockedBalance: Double = 0.0,
  val bonusBalance: Double = 0.0,
  val lockedBonus: Double = 0.0,
) {
  val availableAmount: Double get() = availableBalance / 100.0
  val pendingAmount: Double get() = pendingBalance / 100.0
  val totalDepositedAmount: Double get() = totalDeposited / 100.0
  val totalWithdrawnAmount: Double get() = totalWithdrawn / 100.0
  
  // Correctly derived BDT (Taka) amounts by dividing minor units by 100.0
  val balanceAmount: Double get() = availableBalance / 100.0
  val winningsAmount: Double get() = totalWinnings / 100.0
  val bonusAmount: Double get() = bonusBalance / 100.0
  val lockedBonusAmount: Double get() = lockedBonus / 100.0
  
  val effectiveTotalWinnings: Double get() = winningsAmount
  val totalWinningsAmount: Double get() = winningsAmount
}

enum class TransactionType {
  DEPOSIT,
  WITHDRAW,
  WITHDRAW_HOLD,
  WITHDRAW_RELEASE,
  MATCH_ENTRY,
  MATCH_JOIN,
  MATCH_REFUND,
  MATCH_PRIZE,
  ADMIN_CREDIT,
  ADMIN_DEBIT,
  COMMISSION,
  WITHDRAWAL,
  MATCH_ENTRY_FEE,
  MATCH_PRIZE_CREDIT,
  REFUND,
  ADMIN_ADJUSTMENT
}

enum class TransactionStatus {
  PENDING,
  COMPLETED,
  FAILED,
  CANCELLED,
  REJECTED
}

@IgnoreExtraProperties
data class TransactionEntity(
  val transactionId: String = "",
  val uid: String = "",
  val amount: Long = 0L,
  val type: String = TransactionType.DEPOSIT.name,
  val status: String = TransactionStatus.PENDING.name,
  val beforeBalance: Long = 0L,
  val afterBalance: Long = 0L,
  val source: String = "APP",
  val referenceId: String = "",
  val description: String = "",
  val createdAt: Long = 0L,
  val processedAt: Long = 0L,
  val walletId: String = uid,
  val userId: String = uid,
  // Schema alignment for /userTransactions/{userId}/{id}
  val id: String = "",
  val title: String = "",
  val timestamp: Long = 0L,
) {
  val effectiveId: String get() = id.ifBlank { transactionId.ifBlank { referenceId } }
  val effectiveTitle: String get() = title.ifBlank {
    val typeUpper = type.uppercase()
    when {
      typeUpper.contains("WITHDRAW") -> "উইথড্র"
      typeUpper.contains("DEPOSIT") -> "ডিপোজিট"
      typeUpper.contains("PRIZE") || typeUpper.contains("WIN") -> "পুরস্কার"
      typeUpper.contains("REFUND") -> "ফেরত"
      else -> type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
  }
  val effectiveTimestamp: Long get() = if (timestamp > 0L) timestamp else (if (createdAt > 0L) createdAt else processedAt)
  val amountInCurrency: Double get() = amount / 100.0
  val beforeBalanceInCurrency: Double get() = beforeBalance / 100.0
  val afterBalanceInCurrency: Double get() = afterBalance / 100.0
}

enum class GameType {
  LUDO,
  CARROM
}

enum class MatchStatus {
  AVAILABLE,
  FULL,
  CODE_ADDED,
  RUNNING,
  RESULT_SUBMITTED,
  COMPLETED,
  PAUSED,
  DISABLED,
  CANCELLED,
  UPCOMING
}

@IgnoreExtraProperties
data class MatchEntity(
  val matchId: String = "",
  val matchNumber: String = "",
  val title: String = "",
  val gameType: String = GameType.LUDO.name,
  val entryFee: Double = 0.0,
  val prizePool: Double = 0.0,
  val status: String = MatchStatus.AVAILABLE.name,
  val gameCode: String = "",
  val isCodeVisible: Boolean = false,
  val scheduledTime: Long = 0L,
  val maxPlayers: Int = 2,
  val joinedPlayersCount: Int = 0,
  val winnerUserId: String = "",
  val createdByAdminId: String = "",
  val createdAt: Long = 0L,
  val entryFeeMinorUnits: Long = 0L,
  val prizeMinorUnits: Long = 0L,
  val scheduledAt: Long = 0L,
  val updatedAt: Long = 0L,
  val startedAt: Long? = null,
  val cancelReason: String? = null,
  val isLoyaltyFree: Boolean = false,
  val requiredMatches24h: Int = 0,
) {
  val effectiveEntryFeeMinorUnits: Long
    get() = if (entryFeeMinorUnits > 0L) entryFeeMinorUnits else (entryFee).toLong()
  val effectivePrizeMinorUnits: Long
    get() = if (prizeMinorUnits > 0L) prizeMinorUnits else (prizePool).toLong()
  val effectiveScheduledAt: Long
    get() = if (scheduledAt > 0L) scheduledAt else scheduledTime
  val effectiveUpdatedAt: Long
    get() = if (updatedAt > 0L) updatedAt else createdAt
  val displayEntryFee: Double
    get() = effectiveEntryFeeMinorUnits / 100.0
  val displayPrizePool: Double
    get() = effectivePrizeMinorUnits / 100.0
}

enum class PlayerSlot {
  PLAYER_1,
  PLAYER_2
}

@IgnoreExtraProperties
data class MatchPlayerEntity(
  val matchPlayerId: String = "",
  val matchId: String = "",
  val userId: String = "",
  val username: String = "",
  val slot: String = PlayerSlot.PLAYER_1.name,
  val joinedAt: Long = 0L,
  val isReady: Boolean = false,
  val uid: String = userId,
  val entryFeeMinorUnits: Long = 0L,
  val status: String = "JOINED",
) {
  val effectiveUid: String get() = uid.ifEmpty { userId }
}

@IgnoreExtraProperties
data class UserMatchHistoryEntity(
  val matchId: String = "",
  val userId: String = "",
  val uid: String = userId,
  val joinedAt: Long = 0L,
  val status: String = "JOINED", // JOINED, RESULT_SUBMITTED, COMPLETED, REJECTED, CANCELLED
  val isWinner: Boolean? = null,
  val updatedAt: Long = 0L,
  val gameType: String = GameType.LUDO.name,
  val matchNumber: String = "",
  val title: String = "",
  val entryFeeMinorUnits: Long = 0L,
  val prizeMinorUnits: Long = 0L,
  val opponentName: String = "",
  val opponentUserId: String = "",
) {
  val effectiveUid: String get() = uid.ifEmpty { userId }
}

enum class ResultStatus {
  PENDING_REVIEW,
  APPROVED,
  REJECTED
}

@IgnoreExtraProperties
data class ResultEntity(
  val resultId: String = "",
  val matchId: String = "",
  val submittedByUserId: String = "",
  val userId: String = submittedByUserId,
  val claimedWinnerUserId: String = "",
  val proofScreenshotUrl: String = "",
  val status: String = ResultStatus.PENDING_REVIEW.name,
  val reviewNotes: String = "",
  val reviewedByAdminId: String = "",
  val submittedAt: Long = 0L,
  val reviewedAt: Long = 0L,
  val matchNumber: String = "",
  val gameType: String = "",
  val createdAt: Long = submittedAt,
)

enum class DepositStatus {
  SUBMITTED,
  PENDING,
  APPROVED,
  REJECTED,
  VERIFYING
}

@IgnoreExtraProperties
data class DepositEntity(
  val depositId: String = "",
  val uid: String = "",
  val amount: Long = 0L,
  val method: String = "BKASH",
  val senderNumber: String = "",
  val trxId: String = "",
  val screenshotUrl: String = "",
  val status: String = DepositStatus.SUBMITTED.name,
  val createdAt: Long = 0L,
  val processedAt: Long = 0L,
  val rejectionReason: String = "",
  val adminUid: String = "",
  val userId: String = uid,
  val paymentMethod: String = method,
  val transactionReference: String = trxId,
  val screenshotProofUrl: String = screenshotUrl,
  val approvedAt: Long = processedAt,
) {
  val amountInCurrency: Double get() = amount / 100.0
}

enum class WithdrawalStatus {
  REQUESTED,
  PENDING,
  APPROVED,
  REJECTED,
  PROCESSING,
  COMPLETED
}

@IgnoreExtraProperties
data class WithdrawalEntity(
  val withdrawalId: String = "",
  val uid: String = "",
  val amount: Long = 0L,
  val method: String = "BKASH",
  val recipientNumber: String = "",
  val status: String = WithdrawalStatus.REQUESTED.name,
  val createdAt: Long = 0L,
  val processedAt: Long = 0L,
  val rejectionReason: String = "",
  val adminUid: String = "",
  val userId: String = uid,
  val paymentMethod: String = method,
  val paymentDetails: String = recipientNumber,
  val requestedAt: Long = createdAt,
) {
  val amountInCurrency: Double get() = amount / 100.0
}

enum class NotificationType {
  DEPOSIT_APPROVED,
  DEPOSIT_REJECTED,
  WITHDRAWAL_APPROVED,
  WITHDRAWAL_REJECTED,
  MATCH_WIN,
  MATCH_LOST,
  MATCH_PROOF_REJECTED,
  ROOM_CODE
}

@IgnoreExtraProperties
data class NotificationEntity(
  val id: String = "",
  val userId: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = "",
  val timestamp: Long = 0L,
  val read: Boolean = false,
  // Keep legacy fields for compatibility if needed
  val notificationId: String = "",
  val body: String = "",
  val targetScreen: String = "",
  val isRead: Boolean = false,
  val createdAt: Long = 0L,
) {
  val effectiveId: String get() = id.ifBlank { notificationId }
  val effectiveMessage: String get() = message.ifBlank { body }
  val effectiveTimestamp: Long get() = if (timestamp > 0L) timestamp else createdAt
  val effectiveRead: Boolean get() = read || isRead
}

@IgnoreExtraProperties
data class AdminUserEntity(
  val adminId: String = "",
  val userId: String = "",
  val email: String = "",
  val role: String = "MODERATOR",
  val permissions: List<String> = emptyList(),
  val isActive: Boolean = true,
  val createdAt: Long = 0L,
)

@IgnoreExtraProperties
data class AuditLogEntity(
  val logId: String = "",
  val action: String = "",
  val adminUid: String = "",
  val targetUid: String = "",
  val beforeState: String = "",
  val afterState: String = "",
  val timestamp: Long = 0L,
  val ipAddress: String = "",
  val performedByUserId: String = adminUid,
  val targetEntityId: String = targetUid,
  val targetEntityType: String = "",
  val details: String = "$beforeState -> $afterState",
)

@IgnoreExtraProperties
data class FinanceSettingsEntity(
  val minDeposit: Long = 5000L,
  val maxDeposit: Long = 2500000L,
  val minWithdraw: Long = 20000L,
  val maxWithdraw: Long = 1000000L,
  val depositFeePercent: Double = 0.0,
  val withdrawFeePercent: Double = 0.0,
  val commissionRate: Double = 10.0,
  val bkashNumber: String = "01700000000",
  val nagadNumber: String = "01800000000",
  val depositInstructions: String = "Send Money (Personal) to the official bKash/Nagad number, then submit your transaction ID and proof.",
) {
  val minDepositAmount: Double get() = minDeposit / 100.0
  val maxDepositAmount: Double get() = maxDeposit / 100.0
  val minWithdrawAmount: Double get() = minWithdraw / 100.0
  val maxWithdrawAmount: Double get() = maxWithdraw / 100.0
}

@IgnoreExtraProperties
data class AppSettingsEntity(
  val minDepositAmount: Double = 5000.0,
  val minWithdrawalAmount: Double = 20000.0,
  val platformCommissionPercent: Double = 10.0,
  val maintenanceMode: Boolean = false,
  val supportWhatsappNumber: String = "",
  val whatsapp: String = "",
  val whatsappNumber: String = "",
  val supportTelegramUrl: String = "",
  val telegram: String = "",
  val telegramUrl: String = "",
  val appVersionCode: Int = 1,
  val finance: FinanceSettingsEntity = FinanceSettingsEntity(),
  val bkashNumber: String = "01700000000",
  val nagadNumber: String = "01800000000",
  val depositInstructions: String = "",
  val withdrawalInstructions: String = "",
  val howToJoinVideoUrl: String = "",
  val howToDepositVideoUrl: String = "",
  val howToSubmitResultVideoUrl: String = "",
  val tournamentRulesVideoUrl: String = "",
  val customBannerImageUrl: String = "",
  val depositBannerImageUrl: String = "",
  val matchJoinBannerImageUrl: String = "",
  val resultSubmitBannerImageUrl: String = "",
  val rulesBannerImageUrl: String = "",
  val announcementMessage: String = "",
  val announcementActive: Boolean = false,
) {
  val activeWhatsappNumber: String
    get() = supportWhatsappNumber.trim().ifEmpty {
      whatsappNumber.trim().ifEmpty {
        whatsapp.trim()
      }
    }

  val activeTelegramUrl: String
    get() = supportTelegramUrl.trim().ifEmpty {
      telegramUrl.trim().ifEmpty {
        telegram.trim()
      }
    }

  val effectiveBkashNumber: String
    get() = bkashNumber.ifBlank { finance.bkashNumber }.ifBlank { "01700000000" }

  val effectiveNagadNumber: String
    get() = nagadNumber.ifBlank { finance.nagadNumber }.ifBlank { "01800000000" }

  // Derived BDT (Taka) amounts by dividing minor units by 100.0
  val minDepositTaka: Double get() = minDepositAmount / 100.0
  val minWithdrawalTaka: Double get() = minWithdrawalAmount / 100.0
}

