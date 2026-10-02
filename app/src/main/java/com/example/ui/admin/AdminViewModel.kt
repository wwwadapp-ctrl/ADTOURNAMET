package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.core.finance.FinancialEngine
import com.example.domain.model.*
import com.example.domain.repository.AdminRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminTab {
  DEPOSITS,
  WITHDRAWALS,
  MATCHES,
  RESULTS,
  AUDIT_LOGS,
  SETTINGS,
}

data class AdminUiState(
  val isLoading: Boolean = false,
  val selectedTab: AdminTab = AdminTab.DEPOSITS,
  val pendingDeposits: List<DepositEntity> = emptyList(),
  val pendingWithdrawals: List<WithdrawalEntity> = emptyList(),
  val processingWithdrawalIds: Set<String> = emptySet(),
  val pendingResults: List<ResultEntity> = emptyList(),
  val allMatches: List<MatchEntity> = emptyList(),
  val auditLogs: List<AuditLogEntity> = emptyList(),
  val appSettings: AppSettingsEntity = AppSettingsEntity(),
  val isSavingSettings: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

class AdminViewModel(
  private val adminRepository: AdminRepository,
  val adminUid: String,
  private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminUiState())
  val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
  private val inFlightWithdrawals = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

  init {
    loadAllData()
  }

  fun setTab(tab: AdminTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }

  fun loadAllData() {
    viewModelScope.launch {
      adminRepository.getPendingDeposits().collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(pendingDeposits = res.data)
        }
      }
    }
    viewModelScope.launch {
      adminRepository.getPendingWithdrawals().collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(pendingWithdrawals = res.data)
        }
      }
    }
    viewModelScope.launch {
      adminRepository.getPendingResults().collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(pendingResults = res.data)
        }
      }
    }
    viewModelScope.launch {
      adminRepository.getAllMatches().collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(allMatches = res.data)
        }
      }
    }
    viewModelScope.launch {
      adminRepository.getAuditLogs().collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(auditLogs = res.data)
        }
      }
    }
    if (settingsRepository != null) {
      viewModelScope.launch {
        settingsRepository.getAppSettings().collect { res ->
          if (res is Resource.Success) {
            _uiState.value = _uiState.value.copy(appSettings = res.data)
          }
        }
      }
    }
  }

  fun approveDeposit(deposit: DepositEntity) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.approveDeposit(adminUid, deposit.depositId, deposit)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Deposit approved successfully!")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun rejectDeposit(depositId: String, reason: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.rejectDeposit(adminUid, depositId, reason)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Deposit rejected.")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun approveWithdrawal(withdrawal: WithdrawalEntity) {
    val id = withdrawal.withdrawalId
    if (!inFlightWithdrawals.add(id)) {
      return
    }
    _uiState.value = _uiState.value.copy(
      isLoading = true,
      processingWithdrawalIds = inFlightWithdrawals.toSet(),
      errorMessage = null,
    )
    viewModelScope.launch {
      try {
        when (val res = adminRepository.approveWithdrawal(adminUid, id, withdrawal)) {
          is Resource.Success -> {
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Withdrawal payout approved!")
            loadAllData()
          }
          is Resource.Error -> {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
          }
          else -> _uiState.value = _uiState.value.copy(isLoading = false)
        }
      } finally {
        inFlightWithdrawals.remove(id)
        _uiState.value = _uiState.value.copy(processingWithdrawalIds = inFlightWithdrawals.toSet())
      }
    }
  }

  fun rejectWithdrawal(withdrawal: WithdrawalEntity, reason: String) {
    val id = withdrawal.withdrawalId
    if (!inFlightWithdrawals.add(id)) {
      return
    }
    _uiState.value = _uiState.value.copy(
      isLoading = true,
      processingWithdrawalIds = inFlightWithdrawals.toSet(),
      errorMessage = null,
    )
    viewModelScope.launch {
      try {
        when (val res = adminRepository.rejectWithdrawal(adminUid, id, withdrawal, reason)) {
          is Resource.Success -> {
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Withdrawal rejected & refunded.")
            loadAllData()
          }
          is Resource.Error -> {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
          }
          else -> _uiState.value = _uiState.value.copy(isLoading = false)
        }
      } finally {
        inFlightWithdrawals.remove(id)
        _uiState.value = _uiState.value.copy(processingWithdrawalIds = inFlightWithdrawals.toSet())
      }
    }
  }

  fun setMatchGameCode(matchId: String, code: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.setMatchGameCode(adminUid, matchId, code)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Room code updated!")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun startMatch(matchId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.setMatchRunning(adminUid, matchId)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Match started!")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun cancelMatch(matchId: String, reason: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.cancelMatch(adminUid, matchId, reason)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Match cancelled and entry fees refunded.")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun approveResult(result: ResultEntity) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.approveResult(adminUid, result)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Victory result approved and prize credited!")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun rejectResult(resultId: String, matchId: String, reason: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = adminRepository.rejectResult(adminUid, resultId, matchId, reason)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Result rejected.")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun createSingleMatch(title: String, gameType: String, entryFeeBDT: Double, scheduledTime: Long) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      val entryFeeMinor = (entryFeeBDT * 100).toLong()
      val prizeCalc = FinancialEngine.calculateMatchPrize(entryFeeMinor, 2)
      val prizeMinor = prizeCalc.winnerPrize
      val now = System.currentTimeMillis()
      val randomId = (1000..9999).random()

      val newMatch = MatchEntity(
        matchId = "M_${now}_$randomId",
        matchNumber = "AD-#$randomId",
        title = title.ifBlank { "$gameType Duel #$randomId" },
        gameType = gameType,
        entryFee = entryFeeBDT,
        entryFeeMinorUnits = entryFeeMinor,
        prizePool = prizeMinor / 100.0,
        prizeMinorUnits = prizeMinor,
        status = MatchStatus.AVAILABLE.name,
        maxPlayers = 2,
        joinedPlayersCount = 0,
        scheduledTime = if (scheduledTime > 0L) scheduledTime else now + 3600_000L,
        scheduledAt = if (scheduledTime > 0L) scheduledTime else now + 3600_000L,
        createdAt = now,
        createdByAdminId = adminUid,
      )

      when (val res = adminRepository.createMatch(adminUid, newMatch)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Match created: ${newMatch.title}")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun bulkCreateMatches(gameType: String, count: Int = 50, entryFeeBDT: Double = 50.0) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      val entryFeeMinor = (entryFeeBDT * 100).toLong()
      val prizeCalc = FinancialEngine.calculateMatchPrize(entryFeeMinor, 2)
      val prizeMinor = prizeCalc.winnerPrize
      val now = System.currentTimeMillis()

      val matches = (1..count).map { idx ->
        val mId = "M_${now}_$idx"
        val scheduleOffset = idx * (15 * 60 * 1000L) // every 15 minutes
        MatchEntity(
          matchId = mId,
          matchNumber = "AD-#${1000 + idx}",
          title = "$gameType Championship #$idx",
          gameType = gameType,
          entryFee = entryFeeBDT,
          entryFeeMinorUnits = entryFeeMinor,
          prizePool = prizeMinor / 100.0,
          prizeMinorUnits = prizeMinor,
          status = MatchStatus.AVAILABLE.name,
          maxPlayers = 2,
          joinedPlayersCount = 0,
          scheduledTime = now + scheduleOffset,
          scheduledAt = now + scheduleOffset,
          createdAt = now,
          createdByAdminId = adminUid,
        )
      }

      when (val res = adminRepository.bulkCreateMatches(adminUid, matches)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Created ${res.data} matches in bulk!")
          loadAllData()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.error.message)
        }
        else -> _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun updateSettings(settings: AppSettingsEntity) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSavingSettings = true, errorMessage = null)
      val res = settingsRepository?.updateAppSettings(settings) ?: Resource.Success(Unit)
      if (res is Resource.Success) {
        _uiState.value = _uiState.value.copy(
          isSavingSettings = false,
          appSettings = settings,
          successMessage = "App settings and payment numbers updated successfully!"
        )
      } else if (res is Resource.Error) {
        _uiState.value = _uiState.value.copy(
          isSavingSettings = false,
          errorMessage = res.error.message
        )
      }
    }
  }

  class Factory(
    private val adminRepository: AdminRepository,
    private val adminUid: String,
    private val settingsRepository: SettingsRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AdminViewModel(adminRepository, adminUid, settingsRepository) as T
    }
  }
}
