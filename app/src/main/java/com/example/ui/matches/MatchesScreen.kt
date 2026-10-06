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
      )
    },
    containerColor = DeepNavyBg,
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MidnightBackgroundGradient)
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
            EmptyState(
              title = emptyTitle,
              description = emptyMsg,
              actionButtonText = if (selectedTab != MatchesTab.AVAILABLE) strings.browseAvailableCta else null,
              onActionClick = if (selectedTab != MatchesTab.AVAILABLE) {
                { viewModel.setTab(MatchesTab.AVAILABLE) }
              } else null,
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
  Surface(
    color = MidnightNavyCard,
    border = BorderStroke(0.8.dp, MidnightNavyBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    ScrollableTabRow(
      selectedTabIndex = selectedTab.ordinal,
      containerColor = Color.Transparent,
      contentColor = Gold400,
      edgePadding = 16.dp,
      divider = {},
    ) {
      MatchesTab.values().forEach { tab ->
        val isSelected = selectedTab == tab
        Tab(
          selected = isSelected,
          onClick = { onTabSelected(tab) },
          text = {
            Text(
              text = getTabLabel(tab, strings),
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
              ),
              color = if (isSelected) Gold400 else Slate400,
            )
          },
          modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
        )
      }
    }
  }
}

@Composable
private fun GameFilterRow(
  selectedFilter: GameFilter,
  onFilterSelected: (GameFilter) -> Unit,
  strings: com.example.core.i18n.AppStrings,
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(GameFilter.values()) { filter ->
      val isSelected = selectedFilter == filter
      val accentColor = when (filter) {
        GameFilter.ALL -> Indigo400
        GameFilter.LUDO -> Gold400
        GameFilter.CARROM -> NeonCyan
      }

      Surface(
        color = if (isSelected) Color.Transparent else MidnightNavyCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
          0.8.dp,
          if (isSelected) Gold400 else MidnightNavyBorder,
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .then(
            if (isSelected) {
              Modifier.background(ElectricAmberGradient)
            } else {
              Modifier
            }
          )
          .testTag("filter_${filter.name.lowercase()}"),
        onClick = { onFilterSelected(filter) },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // 3D Isometric Tactile Icon Container
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(RoundedCornerShape(6.dp))
              .then(
                if (isSelected) {
                  Modifier.background(Slate950.copy(alpha = 0.25f))
                } else {
                  Modifier.background(
                    Brush.radialGradient(
                      colors = listOf(
                        accentColor.copy(alpha = 0.3f),
                        Color.Transparent,
                      )
                    )
                  )
                }
              )
              .border(
                0.8.dp,
                if (isSelected) Slate950.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = when (filter) {
                GameFilter.ALL -> Icons.Default.SportsEsports
                GameFilter.LUDO -> Icons.Default.Casino
                GameFilter.CARROM -> Icons.Default.Album
              },
              contentDescription = null,
              tint = if (isSelected) Slate950 else accentColor,
              modifier = Modifier.size(14.dp),
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = getFilterLabel(filter, strings),
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
              fontSize = 12.sp,
              letterSpacing = 0.3.sp,
            ),
            color = if (isSelected) Slate950 else Color.White,
          )
        }
      }
    }
  }
}
