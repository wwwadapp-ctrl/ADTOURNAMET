package com.example.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import kotlinx.coroutines.Job
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.DepositEntity
import com.example.domain.model.TransactionEntity
import com.example.domain.model.WalletEntity
import com.example.domain.model.WithdrawalEntity
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.domain.model.UserEntity

data class WalletUiState(
  val isLoading: Boolean = true,
  val wallet: WalletEntity? = null,
  val mainBalance: Double = 0.0,
  val bonusBalance: Double = 0.0,
  val transactions: List<TransactionEntity> = emptyList(),
  val deposits: List<DepositEntity> = emptyList(),
  val withdrawals: List<WithdrawalEntity> = emptyList(),
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val isSubmitting: Boolean = false,
  val depositSubmitted: Boolean = false,
  val withdrawalSubmitted: Boolean = false,
)

class WalletViewModel(
  private val walletRepository: WalletRepository,
  private val userId: String,
  private val settingsRepository: SettingsRepository? = null,
  private val authRepository: AuthRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(WalletUiState())
  val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

  val appSettings: StateFlow<AppSettingsEntity?> = (settingsRepository?.getAppSettings()
    ?: flowOf(Resource.Success(AppSettingsEntity())))
    .map { res ->
      if (res is Resource.Success) res.data else AppSettingsEntity()
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppSettingsEntity()
    )

  private var walletJob: Job? = null
  private var transactionsJob: Job? = null

  init {
    loadWalletData()
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(
      errorMessage = null,
      successMessage = null,
      depositSubmitted = false,
      withdrawalSubmitted = false,
    )
  }

  fun loadWalletData() {
    walletJob?.cancel()
    walletJob = viewModelScope.launch {
      if (_uiState.value.wallet == null) {
        _uiState.value = _uiState.value.copy(isLoading = true)
      }
      
      // Auto-unlock referral bonus if eligible
      walletRepository.checkAndClaimReferralBonus(userId)
      
      val walletFlow = walletRepository.getWallet(userId)
      val userFlow = authRepository?.getCurrentUser() ?: flowOf(null)

      combine(walletFlow, userFlow) { walletRes, user ->
        Pair(walletRes, user)
      }.collect { (walletRes, user) ->
        if (walletRes is Resource.Success) {
          val wallet = walletRes.data
          // 1. Primary: Use wallet.availableAmount (or availableBalance converted to Taka)
          val mainBal: Double = wallet?.let { w ->
            if (w.availableAmount > 0.0) w.availableAmount else w.availableBalance / 100.0
          } ?: 0.0

          // 2. Bonus Balance Calculation:
          val bonusBal: Double = wallet?.let { w ->
            if (w.bonusAmount > 0.0) w.bonusAmount else w.bonusBalance / 100.0
          } ?: 0.0

          _uiState.value = _uiState.value.copy(
            wallet = wallet,
            mainBalance = mainBal,
            bonusBalance = bonusBal,
            isLoading = false
          )
        } else if (walletRes is Resource.Error) {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = walletRes.error.message)
        }
      }
    }

    transactionsJob?.cancel()
    transactionsJob = viewModelScope.launch {
      walletRepository.getTransactions(userId).collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(transactions = res.data)
        }
      }
    }
  }

  fun submitDeposit(amountMinorUnits: Long, method: String, senderNumber: String, trxId: String) {
    if (_uiState.value.isSubmitting) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
      when (val res = walletRepository.submitDepositRequest(userId, amountMinorUnits, method, senderNumber, trxId, "")) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            depositSubmitted = true,
            successMessage = "Deposit request submitted! Please wait for Admin approval.",
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            errorMessage = res.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun submitWithdrawal(amountMinorUnits: Long, method: String, recipientNumber: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
      when (val res = walletRepository.submitWithdrawalRequest(userId, amountMinorUnits, method, recipientNumber)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            withdrawalSubmitted = true,
            successMessage = "Withdrawal request of ৳ ${"%.2f".format(amountMinorUnits / 100.0)} submitted successfully.",
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            errorMessage = res.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  class Factory(
    private val walletRepository: WalletRepository,
    private val userId: String,
    private val settingsRepository: SettingsRepository? = null,
    private val authRepository: AuthRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return WalletViewModel(walletRepository, userId, settingsRepository, authRepository) as T
    }
  }
}
