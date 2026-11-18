package com.example.ui.matches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.error.Resource
import com.example.domain.model.GameType
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.domain.repository.MatchRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.AppStrings
import com.example.ui.wallet.wlBackground
import com.example.ui.home.AdGameFilterChip

enum class MatchesTab {
  AVAILABLE,
  MY_JOINED,
  UPCOMING,
  HISTORY,
}

@Composable
fun getTabLabel(tab: MatchesTab, strings: AppStrings): String {
  return when (tab) {
    MatchesTab.AVAILABLE -> strings.tabAvailable
    MatchesTab.MY_JOINED -> strings.tabMyJoined
    MatchesTab.UPCOMING -> strings.tabUpcoming
    MatchesTab.HISTORY -> strings.navHistory
  }
}

enum class GameFilter(val gameType: GameType?) {
  ALL(null),
  LUDO(GameType.LUDO),
  CARROM(GameType.CARROM),
}

@Composable
fun getFilterLabel(filter: GameFilter, strings: AppStrings): String {
  return when (filter) {
    GameFilter.ALL -> strings.filterAllGames
    GameFilter.LUDO -> strings.filterLudo1v1
    GameFilter.CARROM -> strings.filterCarrom1v1
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
  userId: String,
  currentUser: UserEntity?,
  wallet: WalletEntity?,
  unreadNotificationsCount: Int = 0,
  onNavigateToMatchDetails: (String) -> Unit,
  onWalletClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  onHistoryClick: () -> Unit = {},
  matchRepository: MatchRepository,
  modifier: Modifier = Modifier,
  initialTab: MatchesTab = MatchesTab.AVAILABLE,
  viewModel: MatchesViewModel = viewModel(
    key = "MatchesViewModel_${userId}_${initialTab.name}",
    factory = MatchesViewModel.Factory(matchRepository, userId, initialTab),
  ),
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()
  val selectedTab = uiState.selectedTab
  val selectedGameFilter = uiState.selectedGameFilter

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = onWalletClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick,
      )
    },
    containerColor = DeepNavyBg,
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .wlBackground()
        .padding(innerPadding),
    ) {
      // 1. Primary Filter Tabs: Available, My Joined, Upcoming, History
      PrimaryTabRow(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.setTab(it) },
        strings = strings,
      )

      // 2. Game Filter Chips: All, Ludo 1v1, Carrom 1v1
      GameFilterRow(
        selectedFilter = selectedGameFilter,
        onFilterSelected = { viewModel.setGameFilter(it) },
        strings = strings,
      )

      // 3. Match List / Loading / Error / Empty Content
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
      ) {
        if (uiState.isLoading) {
          LoadingState(message = strings.loadingTabMatches(getTabLabel(selectedTab, strings).lowercase()))
        } else if (uiState.errorMessage != null) {
          ErrorState(
            message = uiState.errorMessage ?: strings.failedToLoadMatches,
            onRetry = {
              viewModel.retry()
            },
          )
        } else {
          val filteredMatches = uiState.filteredMatches

          if (filteredMatches.isEmpty()) {
            val emptyStatePair = when (selectedTab) {
              MatchesTab.AVAILABLE -> Pair(strings.emptyAvailableTitleLabel, strings.emptyAvailableDescLabel)
              MatchesTab.MY_JOINED -> Pair(strings.emptyJoinedTitleLabel, strings.emptyJoinedDescLabel)
              MatchesTab.UPCOMING -> Pair(strings.emptyUpcomingTitleLabel, strings.emptyUpcomingDescLabel)
              MatchesTab.HISTORY -> Pair(strings.emptyHistoryTitleLabel, strings.emptyHistoryDescLabel)
            }
            val emptyTitle = emptyStatePair.first
            val emptyMsg = emptyStatePair.second
            AdMatchesEmpty(
              title = emptyTitle,
              description = emptyMsg,
              actionText = if (selectedTab != MatchesTab.AVAILABLE) strings.browseAvailableCta else null,
              onAction = if (selectedTab != MatchesTab.AVAILABLE) {
                { viewModel.setTab(MatchesTab.AVAILABLE) }
              } else null,
              modifier = Modifier.padding(top = 14.dp),
            )
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("matches_list"),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              contentPadding = PaddingValues(vertical = 14.dp),
            ) {
              items(
                items = filteredMatches,
                key = { it.matchId },
              ) { match ->
                MatchCard(
                  match = match,
                  isJoined = uiState.joinedMatchIds.contains(match.matchId),
                  currentUserId = userId,
                  onCardClick = { onNavigateToMatchDetails(match.matchId) },
                  onJoinClick = { onNavigateToMatchDetails(match.matchId) },
                  onViewCodeClick = { onNavigateToMatchDetails(match.matchId) },
                  onSubmitProofClick = { onNavigateToMatchDetails(match.matchId) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrimaryTabRow(
  selectedTab: MatchesTab,
  onTabSelected: (MatchesTab) -> Unit,
  strings: com.example.core.i18n.AppStrings,
) {
  AdMatchesTabBar(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
    MatchesTab.values().forEach { tab ->
      AdMatchesTab(
        text = getTabLabel(tab, strings),
        selected = selectedTab == tab,
        onClick = { onTabSelected(tab) },
        tag = "tab_${tab.name.lowercase()}",
      )
    }
  }
}

@Composable
private fun GameFilterRow(
  selectedFilter: GameFilter,
  onFilterSelected: (GameFilter) -> Unit,
  strings: com.example.core.i18n.AppStrings,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    GameFilter.values().forEach { filter ->
      AdGameFilterChip(
        label = when (filter) {
          GameFilter.ALL -> strings.filterAllGames
          GameFilter.LUDO -> strings.filterLudo
          GameFilter.CARROM -> strings.filterCarrom
        },
        selected = selectedFilter == filter,
        iconKind = when (filter) {
          GameFilter.ALL -> 0
          GameFilter.LUDO -> 1
          GameFilter.CARROM -> 2
        },
        onClick = { onFilterSelected(filter) },
        tag = "filter_${filter.name.lowercase()}",
        modifier = Modifier.weight(1f),
      )
    }
  }
}
