package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.GameType
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.example.domain.repository.MatchRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MatchFilterTab(val label: String) {
  AVAILABLE("Available"),
  MY_JOINED("My Joined"),
  UPCOMING("Upcoming"),
  HISTORY("History"),
}

data class HomeUiState(
  val isLoading: Boolean = true,
  val selectedTab: MatchFilterTab = MatchFilterTab.AVAILABLE,
  val selectedGameFilter: GameType? = null,
  val matchesList: List<MatchEntity> = emptyList(),
  val filteredMatches: List<MatchEntity> = emptyList(),
  val errorMessage: String? = null,
  val joinedMatchIds: Set<String> = emptySet(),
  val myJoinedMatches: List<MatchEntity> = emptyList(),
  val appSettings: AppSettingsEntity = AppSettingsEntity(),
)

class HomeViewModel(
  private val matchRepository: MatchRepository,
  private val currentUserId: String,
  private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  val appSettings: StateFlow<AppSettingsEntity?> = (settingsRepository?.getAppSettings()
    ?: flowOf(Resource.Success(AppSettingsEntity())))
    .map { res ->
      if (res is Resource.Success) res.data else AppSettingsEntity()
    }
    .catch { emit(AppSettingsEntity()) }
    .flowOn(Dispatchers.IO)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppSettingsEntity()
    )

  // Independent Joined Matches flow with immediate emptyList() fallback
  val myJoinedMatchesState: StateFlow<List<MatchEntity>> = if (currentUserId.isNotBlank()) {
    matchRepository.getMyJoinedMatches(currentUserId)
      .map { res -> if (res is Resource.Success) res.data else emptyList() }
      .catch { emit(emptyList()) }
      .flowOn(Dispatchers.IO)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
      )
  } else {
    MutableStateFlow(emptyList())
  }

  private var fetchJob: Job? = null
  private var timeoutJob: Job? = null

  init {
    // 1. Observe joined matches independently without ever blocking match loading
    viewModelScope.launch(Dispatchers.IO) {
      myJoinedMatchesState.collect { joinedList ->
        val joinedIds = joinedList.map { it.matchId }.toSet()
        _uiState.update { current ->
          val updatedMatches = if (current.selectedTab == MatchFilterTab.AVAILABLE) {
            mergeAvailableWithJoined(current.matchesList, joinedList)
          } else {
            current.matchesList
          }
          current.copy(
            joinedMatchIds = joinedIds,
            myJoinedMatches = joinedList,
            matchesList = updatedMatches,
            filteredMatches = filterMatches(updatedMatches, current.selectedGameFilter),
          )
        }
      }
    }

    // 2. Fetch and observe active/open matches immediately
    loadMatchesForCurrentTab()

    // 3. Observe app settings
    observeAppSettings()
  }

  private fun observeAppSettings() {
    if (settingsRepository == null) return
    viewModelScope.launch(Dispatchers.IO) {
      settingsRepository.getAppSettings()
        .flowOn(Dispatchers.IO)
        .catch { /* ignore */ }
        .collect { res ->
          if (res is Resource.Success) {
            _uiState.update { it.copy(appSettings = res.data) }
          }
        }
    }
  }

  fun setTab(tab: MatchFilterTab) {
    if (_uiState.value.selectedTab == tab && !_uiState.value.isLoading && _uiState.value.errorMessage == null) return
    _uiState.update { it.copy(selectedTab = tab) }
    loadMatchesForCurrentTab()
  }

  fun setGameFilter(gameType: GameType?) {
    _uiState.update { current ->
      current.copy(
        selectedGameFilter = gameType,
        filteredMatches = filterMatches(current.matchesList, gameType),
      )
    }
  }

  fun refresh() {
    loadMatchesForCurrentTab()
  }

  fun loadMatchesForCurrentTab() {
    fetchJob?.cancel()
    timeoutJob?.cancel()

    // 1. Safety timeout: strictly set isLoading = false after 3 seconds if data hasn't arrived
    timeoutJob = viewModelScope.launch(Dispatchers.IO) {
      delay(3000L)
      _uiState.update { current ->
        if (current.isLoading) {
          com.example.core.logging.AppLogger.w("HomeViewModel", "Match loading timeout reached for tab ${current.selectedTab}")
          current.copy(isLoading = false) 
        } else current
      }
    }

    fetchJob = viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      
      try {
        val flow = when (_uiState.value.selectedTab) {
          MatchFilterTab.AVAILABLE -> matchRepository.getAvailableMatches(30)
          MatchFilterTab.MY_JOINED -> matchRepository.getMyJoinedMatches(currentUserId)
          MatchFilterTab.UPCOMING -> matchRepository.getUpcomingMatches(30)
          MatchFilterTab.HISTORY -> matchRepository.getMatchHistory(currentUserId, 30)
        }

        flow.flowOn(Dispatchers.IO)
          .catch { throwable ->
            timeoutJob?.cancel()
            _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
          }
          .collect { resource ->
            when (resource) {
              is Resource.Loading -> {
                // Keep loading = true
              }
              is Resource.Success -> {
                timeoutJob?.cancel()
                val rawMatches = resource.data
                
                // Use current state of joined matches if available, else empty
                val currentJoined = myJoinedMatchesState.value
                
                val matches = if (_uiState.value.selectedTab == MatchFilterTab.AVAILABLE) {
                  mergeAvailableWithJoined(rawMatches, currentJoined)
                } else {
                  rawMatches
                }
                
                val filtered = filterMatches(matches, _uiState.value.selectedGameFilter)
                
                _uiState.update {
                  it.copy(
                    isLoading = false,
                    matchesList = matches,
                    filteredMatches = filtered,
                    errorMessage = null,
                  )
                }
              }
              is Resource.Error -> {
                timeoutJob?.cancel()
                _uiState.update {
                  it.copy(
                    isLoading = false,
                    errorMessage = resource.error.userMessage,
                  )
                }
              }
              else -> {
                timeoutJob?.cancel()
                _uiState.update { it.copy(isLoading = false) }
              }
            }
          }
      } catch (e: Exception) {
        timeoutJob?.cancel()
        _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
      }
    }
  }

  private fun mergeAvailableWithJoined(
    availableMatches: List<MatchEntity>,
    joinedMatches: List<MatchEntity>,
  ): List<MatchEntity> {
    if (joinedMatches.isEmpty()) return availableMatches
    val activeJoined = joinedMatches.filter { match ->
      match.status in setOf(
        MatchStatus.AVAILABLE.name,
        MatchStatus.FULL.name,
        MatchStatus.CODE_ADDED.name,
        MatchStatus.RUNNING.name,
      )
    }
    val activeJoinedMap = activeJoined.associateBy { it.matchId }

    // Merge available with joined, preferring the joined version for authoritative real-time metrics (like joinedCount)
    val mergedList = availableMatches.map { match ->
      activeJoinedMap[match.matchId] ?: match
    }.toMutableList()

    val existingIds = availableMatches.map { it.matchId }.toSet()
    val missingJoined = activeJoined.filter { !existingIds.contains(it.matchId) }
    mergedList.addAll(missingJoined)

    return mergedList
  }

  private fun filterMatches(matches: List<MatchEntity>, filter: GameType?): List<MatchEntity> {
    val baseList = if (filter == null) matches else matches.filter { it.gameType.equals(filter.name, ignoreCase = true) }
    return baseList.sortedWith(
      compareBy<MatchEntity> { 
        it.matchNumber.replace(Regex("[^0-9]"), "").toIntOrNull() ?: Int.MAX_VALUE 
      }.thenBy { it.effectiveScheduledAt }
    )
  }

  override fun onCleared() {
    super.onCleared()
    fetchJob?.cancel()
    timeoutJob?.cancel()
  }

  class Factory(
    private val matchRepository: MatchRepository,
    private val currentUserId: String,
    private val settingsRepository: SettingsRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return HomeViewModel(matchRepository, currentUserId, settingsRepository) as T
    }
  }
}
