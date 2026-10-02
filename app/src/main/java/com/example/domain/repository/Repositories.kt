package com.example.domain.repository

import com.example.core.error.Resource
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

data class JoinMatchResult(
  val match: MatchEntity,
  val player: MatchPlayerEntity,
  val wallet: WalletEntity,
  val transaction: TransactionEntity,
)

interface MatchRepository {
  fun getAvailableMatches(limit: Int = 20): Flow<Resource<List<MatchEntity>>>
  fun getMyJoinedMatches(userId: String): Flow<Resource<List<MatchEntity>>>
  fun getUpcomingMatches(limit: Int = 20): Flow<Resource<List<MatchEntity>>>
  fun getMatchHistory(userId: String, limit: Int = 20): Flow<Resource<List<MatchEntity>>>
  fun getMatchById(matchId: String): Flow<Resource<MatchEntity?>>
  fun getMatchPlayers(matchId: String): Flow<Resource<List<MatchPlayerEntity>>>
  suspend fun joinMatch(userId: String, matchId: String): Resource<JoinMatchResult>
  fun isUserJoinedMatch(userId: String, matchId: String): Flow<Boolean>
}

interface WalletRepository {
  fun getWallet(userId: String): Flow<Resource<WalletEntity?>>
  fun getTransactions(userId: String, limit: Int = 50): Flow<Resource<List<TransactionEntity>>>
  fun getUserDeposits(userId: String, limit: Int = 20): Flow<Resource<List<DepositEntity>>>
  fun getUserWithdrawals(userId: String, limit: Int = 20): Flow<Resource<List<WithdrawalEntity>>>
  suspend fun submitDepositRequest(
    userId: String,
    amountMinorUnits: Long,
    method: String,
    senderNumber: String,
    trxId: String,
    screenshotUrl: String,
  ): Resource<DepositEntity>
  suspend fun submitWithdrawalRequest(
    userId: String,
    amountMinorUnits: Long,
    method: String,
    recipientNumber: String,
  ): Resource<WithdrawalEntity>

  suspend fun checkAndClaimReferralBonus(userId: String): Resource<Unit>
}

interface ResultRepository {
  fun getResultForMatch(matchId: String): Flow<Resource<ResultEntity?>>
  suspend fun uploadResultScreenshot(matchId: String, imageUri: android.net.Uri): Resource<String>
  suspend fun submitMatchResult(
    matchId: String,
    winnerId: String,
    winnerName: String,
    screenshotUrl: String,
    notes: String = "",
  ): Resource<ResultEntity>
}

interface NotificationRepository {
  fun getNotifications(userId: String, limit: Int = 30): Flow<Resource<List<NotificationEntity>>>
  suspend fun markAsRead(notificationId: String): Resource<Unit>
  suspend fun markAllAsRead(userId: String): Resource<Unit> = Resource.Success(Unit)
  suspend fun createNotification(notification: NotificationEntity): Resource<Unit> = Resource.Success(Unit)
  suspend fun cleanupExpiredNotifications(cutoffTimestamp: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000L): Resource<Int> = Resource.Success(0)
}

interface AdminRepository {
  fun getAllMatches(limit: Int = 30): Flow<Resource<List<MatchEntity>>>
  fun getPendingDeposits(): Flow<Resource<List<DepositEntity>>>
  fun getPendingWithdrawals(): Flow<Resource<List<WithdrawalEntity>>>
  fun getPendingResults(): Flow<Resource<List<ResultEntity>>>
  fun getAuditLogs(limit: Int = 50): Flow<Resource<List<AuditLogEntity>>>
  fun getAdminUsers(): Flow<Resource<List<AdminUserEntity>>>
  fun checkIsAdmin(userId: String): Flow<Resource<Boolean>>
  suspend fun approveDeposit(adminUid: String, depositId: String, deposit: DepositEntity): Resource<Unit>
  suspend fun rejectDeposit(adminUid: String, depositId: String, reason: String): Resource<Unit>
  suspend fun approveWithdrawal(adminUid: String, withdrawalId: String, withdrawal: WithdrawalEntity): Resource<Unit>
  suspend fun rejectWithdrawal(adminUid: String, withdrawalId: String, withdrawal: WithdrawalEntity, reason: String): Resource<Unit>
  suspend fun adjustWalletBalance(adminUid: String, targetUserId: String, amountMinorUnits: Long, reason: String): Resource<Unit>
  suspend fun setMatchGameCode(adminUid: String, matchId: String, gameCode: String): Resource<Unit>
  suspend fun setMatchRunning(adminUid: String, matchId: String): Resource<Unit>
  suspend fun pauseMatch(adminUid: String, matchId: String, reason: String = ""): Resource<Unit>
  suspend fun disableMatch(adminUid: String, matchId: String, reason: String = ""): Resource<Unit>
  suspend fun cancelMatch(adminUid: String, matchId: String, reason: String = ""): Resource<Unit>
  suspend fun createMatch(adminUid: String, match: MatchEntity): Resource<String>
  suspend fun bulkCreateMatches(adminUid: String, matches: List<MatchEntity>): Resource<Int>
  suspend fun approveResult(adminUid: String, result: ResultEntity): Resource<Unit>
  suspend fun rejectResult(adminUid: String, resultId: String, matchId: String, reason: String): Resource<Unit>
}

interface SettingsRepository {
  fun getAppSettings(): Flow<Resource<AppSettingsEntity>>
  suspend fun updateAppSettings(settings: AppSettingsEntity): Resource<Unit>
}

interface AuthRepository {
  fun getCurrentUserId(): String?
  fun isUserSignedIn(): Boolean
  fun getCurrentUser(): Flow<UserEntity?>
  suspend fun loginWithPhone(phoneNumber: String, password: String): Resource<UserEntity>
  suspend fun registerUser(name: String, phoneNumber: String, password: String, referralCode: String = ""): Resource<UserEntity>
  suspend fun sendPasswordResetOtp(phoneNumber: String): Resource<String>
  suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<Boolean>
  suspend fun verifyOtpAndResetPassword(phoneNumber: String, otp: String, newPassword: String): Resource<Unit>
  suspend fun checkAccountStatus(userId: String): Resource<AccountStatus>
  suspend fun updateDisplayName(userId: String, newName: String): Resource<Unit>
  suspend fun updateProfilePhoto(userId: String, photoUrl: String): Resource<Unit>
  suspend fun signOut(): Resource<Unit>
}
