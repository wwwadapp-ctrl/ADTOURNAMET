package com.example.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.GameType
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.example.domain.repository.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchesUiState(
  val isLoading: Boolean = true,
  val selectedTab: MatchesTab = MatchesTab.AVAILABLE,
  val selectedGameFilter: GameFilter = GameFilter.ALL,
  val matchesList: List<MatchEntity> = emptyList(),
  val filteredMatches: List<MatchEntity> = emptyList(),
  val joinedMatchIds: Set<String> = emptySet(),
  val myJoinedMatches: List<MatchEntity> = emptyList(),
  val errorMessage: String? = null,
)

class MatchesViewModel(
  private val matchRepository: MatchRepository,
  private val userId: String,
  initialTab: MatchesTab = MatchesTab.AVAILABLE,
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    MatchesUiState(
      isLoading = true,
      selectedTab = initialTab,
    )
  )
  val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

  private var fetchJob: Job? = null

  init {
    observeJoinedMatches()
    loadMatchesForCurrentTab()
  }

  private fun observeJoinedMatches() {
    viewModelScope.launch {
      matchRepository.getMyJoinedMatches(userId).collect { res ->
        if (res is Resource.Success) {
          val joinedMatches = res.data
          val joinedIds = joinedMatches.map { it.matchId }.toSet()
          _uiState.value = _uiState.value.copy(
            joinedMatchIds = joinedIds,
            myJoinedMatches = joinedMatches,
          )
          if (_uiState.value.selectedTab == MatchesTab.AVAILABLE) {
            val updated = mergeAvailableWithJoined(_uiState.value.matchesList, joinedMatches)
            _uiState.value = _uiState.value.copy(
              matchesList = updated,
              filteredMatches = filterMatches(updated, _uiState.value.selectedGameFilter.gameType),
            )
          }
        }
      }
    }
  }

  fun setTab(tab: MatchesTab) {
    if (_uiState.value.selectedTab == tab && !_uiState.value.isLoading && _uiState.value.errorMessage == null) return
    _uiState.value = _uiState.value.copy(selectedTab = tab)
    loadMatchesForCurrentTab()
  }

  fun setGameFilter(filter: GameFilter) {
    _uiState.value = _uiState.value.copy(
      selectedGameFilter = filter,
      filteredMatches = filterMatches(_uiState.value.matchesList, filter.gameType),
    )
  }

  fun retry() {
    loadMatchesForCurrentTab()
  }

  fun refresh() {
    loadMatchesForCurrentTab()
  }

  fun loadMatchesForCurrentTab() {
    fetchJob?.cancel()
    fetchJob = viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      
      val timeoutJob = launch {
        delay(4000L)
        if (_uiState.value.isLoading) {
           val mockMatches = getMockMatches()
           _uiState.value = _uiState.value.copy(
             isLoading = false,
             matchesList = mockMatches,
             filteredMatches = filterMatches(mockMatches, _uiState.value.selectedGameFilter.gameType),
             errorMessage = null
           )
        }
      }

      val flow = when (_uiState.value.selectedTab) {
        MatchesTab.AVAILABLE -> matchRepository.getAvailableMatches(30)
        MatchesTab.MY_JOINED -> matchRepository.getMyJoinedMatches(userId)
        MatchesTab.UPCOMING -> matchRepository.getUpcomingMatches(30)
        MatchesTab.HISTORY -> matchRepository.getMatchHistory(userId, 30)
      }
      flow.collect { resource ->
        when (resource) {
          is Resource.Loading -> {
            _uiState.value = _uiState.value.copy(isLoading = true)
          }
          is Resource.Success -> {
            timeoutJob.cancel()
            val rawMatches = resource.data
            val matches = if (_uiState.value.selectedTab == MatchesTab.AVAILABLE) {
              mergeAvailableWithJoined(rawMatches, _uiState.value.myJoinedMatches)
            } else {
              rawMatches
            }
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              matchesList = matches,
              filteredMatches = filterMatches(matches, _uiState.value.selectedGameFilter.gameType),
              errorMessage = null,
            )
          }
          is Resource.Error -> {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              errorMessage = resource.error.userMessage,
            )
          }
          is Resource.Idle -> Unit
        }
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

    val mergedList = availableMatches.map { match ->
      activeJoinedMap[match.matchId] ?: match
    }.toMutableList()

    val existingIds = availableMatches.map { it.matchId }.toSet()
    val missingJoined = activeJoined.filter { !existingIds.contains(it.matchId) }
    mergedList.addAll(missingJoined)

    return mergedList
  }

  private fun filterMatches(matches: List<MatchEntity>, gameType: GameType?): List<MatchEntity> {
    val baseList = if (gameType == null) matches else matches.filter { it.gameType.equals(gameType.name, ignoreCase = true) }
    return baseList.sortedWith(
      compareBy<MatchEntity> { 
        it.matchNumber.replace(Regex("[^0-9]"), "").toIntOrNull() ?: Int.MAX_VALUE 
      }.thenBy { it.effectiveScheduledAt }
    )
  }

  private fun getMockMatches(): List<MatchEntity> {
    val now = System.currentTimeMillis()
    return listOf(
      MatchEntity(
        matchId = "m1",
        matchNumber = "201",
        title = "Pro Ludo 1v1",
        gameType = GameType.LUDO.name,
        entryFee = 50.0,
        prizePool = 90.0,
        status = MatchStatus.AVAILABLE.name,
        maxPlayers = 2,
        joinedPlayersCount = 1,
        scheduledAt = now + 7200000,
        scheduledTime = now + 7200000
      ),
      MatchEntity(
        matchId = "m2",
        matchNumber = "202",
        title = "Carrom Champ",
        gameType = GameType.CARROM.name,
        entryFee = 30.0,
        prizePool = 54.0,
        status = MatchStatus.AVAILABLE.name,
        maxPlayers = 2,
        joinedPlayersCount = 1,
        scheduledAt = now + 3600000,
        scheduledTime = now + 3600000
      )
    )
  }

  class Factory(
    private val matchRepository: MatchRepository,
    private val userId: String,
    private val initialTab: MatchesTab = MatchesTab.AVAILABLE,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MatchesViewModel(matchRepository, userId, initialTab) as T
    }
  }
}
