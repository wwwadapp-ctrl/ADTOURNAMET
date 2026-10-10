package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.MatchEntity
import com.example.domain.model.TransactionEntity
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.MatchRepository
import com.example.domain.repository.WalletRepository
import com.example.data.repository.FirebaseMatchRepository
import com.example.data.repository.FirebaseWalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CareerStats(
  val totalMatches: Int = 0,
  val wins: Int = 0,
  val losses: Int = 0,
  val winRate: Int = 0,
  val resolvedJoinDate: Long = 0L,
)

data class ProfileUiState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val isLoggedOut: Boolean = false,
  val totalMatches: Int = 0,
  val wins: Int = 0,
  val losses: Int = 0,
  val winRate: Int = 0,
  val resolvedJoinDate: Long = 0L,
)

class ProfileViewModel(
  private val authRepository: AuthRepository,
  private val settingsRepository: SettingsRepository? = null,
  private val matchRepository: MatchRepository = FirebaseMatchRepository(),
  private val walletRepository: WalletRepository = FirebaseWalletRepository(),
) : ViewModel() {

  private val _internalUiState = MutableStateFlow(ProfileUiState())
  
  val currentUser: StateFlow<UserEntity?> = authRepository.getCurrentUser()
    .map { user ->
      user ?: UserEntity(
        uid = "preview_user_123",
        userId = "preview_user_123",
        name = "AD Player",
        displayName = "AD Player",
        walletBalance = 50000.0,
        totalMatches = 150,
        wins = 85
      )
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UserEntity(
        uid = "preview_user_123",
        userId = "preview_user_123",
        name = "AD Player",
        displayName = "AD Player",
        walletBalance = 50000.0,
        totalMatches = 150,
        wins = 85
      ),
    )

  private val careerStats: Flow<CareerStats> = currentUser.flatMapLatest { user ->
    if (user == null) flowOf(CareerStats())
    else {
      val userId = user.userId.ifBlank { user.uid }
      combine(
        matchRepository.getMatchHistory(userId),
        walletRepository.getTransactions(userId, limit = 50)
      ) { matchRes: Resource<List<MatchEntity>>, txRes: Resource<List<TransactionEntity>> ->
        val matches = if (matchRes is Resource.Success<List<MatchEntity>>) matchRes.data else emptyList()
        val txs = if (txRes is Resource.Success<List<TransactionEntity>>) txRes.data else emptyList()

        val completedMatches = matches.filter {
          it.status.equals("COMPLETED", ignoreCase = true) ||
          it.status.equals("RESULT_SUBMITTED", ignoreCase = true)
        }

        val totalMatchesCalc = maxOf(user.totalMatches, completedMatches.size)
        val winsCount = completedMatches.count {
          it.winnerUserId == userId || it.winnerUserId == user.uid
        }
        val winsCalc = maxOf(user.wins, winsCount)
        val lossesCalc = (totalMatchesCalc - winsCalc).coerceAtLeast(0)
        val winRateCalc = if (totalMatchesCalc > 0) (winsCalc * 100) / totalMatchesCalc else 0

        var date = if (user.createdAt > 0L) user.createdAt else user.joinDate
        if (date <= 0L) {
          val earliestMatch = matches.minByOrNull { m: MatchEntity -> 
            val ts = if (m.createdAt > 0L) m.createdAt else m.scheduledTime
            if (ts > 0L) ts else Long.MAX_VALUE
          }?.let { m -> if (m.createdAt > 0L) m.createdAt else m.scheduledTime } ?: Long.MAX_VALUE
          
          val earliestTx = txs.minByOrNull { t: TransactionEntity -> 
            if (t.effectiveTimestamp > 0L) t.effectiveTimestamp else Long.MAX_VALUE 
          }?.effectiveTimestamp ?: Long.MAX_VALUE
          date = minOf(earliestMatch, earliestTx)
          if (date == Long.MAX_VALUE) date = 0L
        }

        CareerStats(
          totalMatches = totalMatchesCalc,
          wins = winsCalc,
          losses = lossesCalc,
          winRate = winRateCalc,
          resolvedJoinDate = date
        )
      }
    }
  }

  val uiState: StateFlow<ProfileUiState> = combine(
    _internalUiState,
    careerStats
  ) { internal, stats ->
    internal.copy(
      totalMatches = stats.totalMatches,
      wins = stats.wins,
      losses = stats.losses,
      winRate = stats.winRate,
      resolvedJoinDate = stats.resolvedJoinDate
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = ProfileUiState(),
  )

  val appSettings: StateFlow<AppSettingsEntity?> = (settingsRepository?.getAppSettings()
    ?.map { if (it is Resource.Success) it.data else null } ?: flowOf(null))
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  fun updateName(userId: String, newName: String) {
    if (newName.isBlank() || userId.isBlank()) return
    viewModelScope.launch {
      _internalUiState.value = _internalUiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = authRepository.updateDisplayName(userId, newName.trim())) {
        is Resource.Success -> {
          _internalUiState.value = _internalUiState.value.copy(
            isLoading = false,
            successMessage = "Profile updated successfully",
          )
        }
        is Resource.Error -> {
          _internalUiState.value = _internalUiState.value.copy(
            isLoading = false,
            errorMessage = res.error.userMessage,
          )
        }
        else -> {
          _internalUiState.value = _internalUiState.value.copy(isLoading = false)
        }
      }
    }
  }

  fun updatePhoto(userId: String, photoUrl: String) {
    if (userId.isBlank() || photoUrl.isBlank()) return
    viewModelScope.launch {
      _internalUiState.value = _internalUiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = authRepository.updateProfilePhoto(userId, photoUrl)) {
        is Resource.Success -> {
          _internalUiState.value = _internalUiState.value.copy(
            isLoading = false,
            successMessage = "Profile photo updated successfully",
          )
        }
        is Resource.Error -> {
          _internalUiState.value = _internalUiState.value.copy(
            isLoading = false,
            errorMessage = res.error.userMessage,
          )
        }
        else -> {
          _internalUiState.value = _internalUiState.value.copy(isLoading = false)
        }
      }
    }
  }

  fun clearFeedback() {
    _internalUiState.value = _internalUiState.value.copy(errorMessage = null, successMessage = null)
  }

  fun signOut(onSignedOut: () -> Unit) {
    viewModelScope.launch {
      _internalUiState.value = _internalUiState.value.copy(isLoading = true)
      authRepository.signOut()
      _internalUiState.value = _internalUiState.value.copy(isLoading = false, isLoggedOut = true)
      onSignedOut()
    }
  }

  class Factory(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ProfileViewModel(authRepository, settingsRepository) as T
    }
  }
}
