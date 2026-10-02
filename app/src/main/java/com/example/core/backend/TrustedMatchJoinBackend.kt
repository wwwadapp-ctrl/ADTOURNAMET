package com.example.core.backend

import com.example.core.error.AppError
import com.example.core.error.Resource
import com.example.core.finance.FinancialEngine
import com.example.domain.repository.JoinMatchResult
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.TransactionEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TrustedMatchJoinBackend {
  suspend fun joinMatch(
    userId: String,
    matchId: String,
    currentTime: Long = System.currentTimeMillis(),
  ): Resource<JoinMatchResult>
}

class AuthoritativeMatchJoinBackend(
  private val matchLookup: suspend (String) -> MatchEntity?,
  private val userLookup: suspend (String) -> UserEntity?,
  private val walletLookup: suspend (String) -> WalletEntity?,
  private val playersLookup: suspend (String) -> List<MatchPlayerEntity>,
  private val transactionsLookup: suspend (String) -> List<TransactionEntity>,
  private val commitMutation: suspend (FinancialEngine.MatchJoinResult.Success) -> Unit,
) : TrustedMatchJoinBackend {
  private val mutex = Mutex()

  override suspend fun joinMatch(
    userId: String,
    matchId: String,
    currentTime: Long,
  ): Resource<JoinMatchResult> = mutex.withLock {
    if (userId.isBlank()) {
      return Resource.Error(AppError.InvalidInput(reason = "Authentication required. User ID cannot be empty."))
    }

    val user = userLookup(userId) ?: UserEntity(uid = userId, userId = userId, role = "PLAYER", status = "ACTIVE")
    if (user.isAccountBlocked) {
      return Resource.Error(AppError.InvalidInput(reason = "Your account is blocked. Cannot join tournament."))
    }

    val match = matchLookup(matchId)
      ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found."))

    val isJoinable = match.status.equals(MatchStatus.AVAILABLE.name, ignoreCase = true)
    if (!isJoinable) {
      return Resource.Error(AppError.InvalidInput(reason = "Match is not available to join (current status: ${match.status})."))
    }

    val scheduledTime = match.effectiveScheduledAt
    if (scheduledTime > 0L && currentTime >= scheduledTime) {
      return Resource.Error(AppError.InvalidInput(reason = "Match registration deadline has passed."))
    }

    val existingPlayers = playersLookup(matchId)
    if (existingPlayers.any { it.effectiveUid == userId }) {
      return Resource.Error(AppError.InvalidInput(reason = "You have already joined this match."))
    }
    if (existingPlayers.size >= match.maxPlayers) {
      return Resource.Error(AppError.InvalidInput(reason = "Match is full. Maximum ${match.maxPlayers} players allowed."))
    }

    val currentWallet = walletLookup(userId)
      ?: return Resource.Error(AppError.ServerError("Wallet not found for user $userId."))
    val existingTxns = transactionsLookup(userId)

    val mutation = FinancialEngine.processMatchJoin(
      match = match,
      user = user,
      currentWallet = currentWallet,
      existingPlayers = existingPlayers,
      existingTransactions = existingTxns,
      currentTime = currentTime,
    )

    return when (mutation) {
      is FinancialEngine.MatchJoinResult.Success -> {
        commitMutation(mutation)
        Resource.Success(
          JoinMatchResult(
            match = mutation.updatedMatch,
            player = mutation.newPlayer,
            wallet = mutation.updatedWallet,
            transaction = mutation.transaction,
          )
        )
      }
      is FinancialEngine.MatchJoinResult.Failure -> {
        Resource.Error(AppError.InvalidInput(reason = mutation.reason))
      }
    }
  }
}
