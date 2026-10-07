package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.core.i18n.LocalAppStrings
import com.example.ui.components.PremiumNotificationBellButton
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val MatchEntity.completedAt: Long
  get() = if (scheduledAt > 0L) scheduledAt else scheduledTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  matches: List<MatchEntity> = emptyList(),
  currentUser: UserEntity? = null,
  wallet: WalletEntity? = null,
  currentUserId: String = "",
  onMatchClick: (String) -> Unit = {},
  onBackClick: () -> Unit = {},
  onWalletClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  unreadNotificationsCount: Int = 0,
  modifier: Modifier = Modifier,
  viewModel: HistoryViewModel = viewModel()
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()

  // Update ViewModel stats whenever matches or UID changes
  LaunchedEffect(matches, currentUserId) {
    viewModel.updateStats(matches)
  }

  // 1. User participation filtering & historical completion filter
  val userHistoricalMatches: List<MatchEntity> = remember(matches, currentUserId) {
    matches.filter { match ->
      val status = match.status.uppercase()
      status == MatchStatus.COMPLETED.name ||
      status == MatchStatus.CANCELLED.name ||
      status == MatchStatus.RESULT_SUBMITTED.name ||
      status == "UNDER_REVIEW" ||
      status == "REJECTED" ||
      status == "WON" ||
      status == "APPROVED" ||
      status == "LOST"
    }
  }

  // 2. Filter selection (ALL, LUDO, CARROM)
  var selectedFilter by remember { mutableStateOf("ALL") }

  val filteredMatches = remember(userHistoricalMatches, selectedFilter) {
    when (selectedFilter) {
      "LUDO" -> userHistoricalMatches.filter { it.gameType.equals("LUDO", ignoreCase = true) }
      "CARROM" -> userHistoricalMatches.filter { it.gameType.equals("CARROM", ignoreCase = true) }
      else -> userHistoricalMatches
    }
  }

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
    modifier = modifier.testTag("history_screen"),
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("history_match_list"),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      // 1. PAGE TITLE & SUBTITLE
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Text(
            text = strings.matchHistoryTitle,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
            ),
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = strings.matchHistorySubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
          )
        }
      }

      // 2. SUMMARY CARD
      item {
        HistorySummaryCard(
          totalPlayed = uiState.totalMatches,
          wonCount = uiState.wins,
          lostCount = uiState.losses,
          winRate = uiState.winRate,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("history_summary"),
        )
      }

      // 3. GAME FILTER PILLS
      item {
        HistoryFilterRow(
          selectedFilter = selectedFilter,
          onFilterSelected = { selectedFilter = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }

      // 4. MATCH LIST OR EMPTY STATE
      if (userHistoricalMatches.isEmpty()) {
        item {
          HistoryEmptyState(
            title = strings.noMatchHistoryTitle,
            subtitle = strings.noMatchHistorySubtext,
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp)
              .testTag("history_empty_state"),
          )
        }
      } else if (filteredMatches.isEmpty()) {
        item {
          HistoryEmptyState(
            title = strings.noFilteredMatchesTitle,
            subtitle = strings.noFilteredMatchesSubtext,
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp)
              .testTag("history_empty_state"),
          )
        }
      } else {
        items(
          items = filteredMatches,
          key = { it.matchId },
        ) { match ->
          HistoryMatchCard(
            match = match,
            currentUserId = currentUserId,
            onClick = { onMatchClick(match.matchId) },
            viewModel = viewModel,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .testTag("history_match_card"),
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// SUMMARY STAT CARD
// -----------------------------------------------------------------------------

@Composable
private fun HistorySummaryCard(
  totalPlayed: Int,
  wonCount: Int,
  lostCount: Int,
  winRate: Int,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = NavySurface,
    border = BorderStroke(1.dp, Gold400.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp, horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SummaryStatColumn(
        label = strings.totalPlayed,
        value = totalPlayed.toString(),
        valueColor = Color.White,
      )
      VerticalDivider(
        modifier = Modifier.height(36.dp),
        color = CardBorder,
      )
      SummaryStatColumn(
        label = strings.totalWon,
        value = wonCount.toString(),
        valueColor = Emerald400,
      )
      VerticalDivider(
        modifier = Modifier.height(36.dp),
        color = CardBorder,
      )
      SummaryStatColumn(
        label = strings.statLosses,
        value = lostCount.toString(),
        valueColor = Rose400,
      )
      VerticalDivider(
        modifier = Modifier.height(36.dp),
        color = CardBorder,
      )
      SummaryStatColumn(
        label = strings.winRateLabel,
        value = "$winRate%",
        valueColor = Gold400,
      )
    }
  }
}

@Composable
private fun SummaryStatColumn(
  label: String,
  value: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        fontSize = 10.sp,
      ),
      color = Slate400,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Black,
      ),
      color = valueColor,
    )
  }
}

// -----------------------------------------------------------------------------
// FILTER PILLS ROW
// -----------------------------------------------------------------------------

@Composable
private fun HistoryFilterRow(
  selectedFilter: String,
  onFilterSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val filters = listOf(
    Triple("ALL", strings.filterAll, "history_filter_all"),
    Triple("LUDO", strings.filterLudo, "history_filter_ludo"),
    Triple("CARROM", strings.filterCarrom, "history_filter_carrom"),
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    filters.forEach { (key, label, tag) ->
      val isSelected = selectedFilter == key
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Gold500.copy(alpha = 0.2f) else Slate900,
        border = BorderStroke(
          1.dp,
          if (isSelected) Gold400 else CardBorder,
        ),
        modifier = Modifier
          .clickable { onFilterSelected(key) }
          .testTag(tag),
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
          ),
          color = if (isSelected) Gold400 else Slate300,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// MATCH HISTORY CARD
// -----------------------------------------------------------------------------

@Composable
private fun HistoryMatchCard(
  match: MatchEntity,
  currentUserId: String,
  onClick: () -> Unit,
  viewModel: HistoryViewModel,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
  val activeUid = currentUserId.ifBlank { authUid }
  
  val matchResult = viewModel.getMatchResult(match, activeUid)
  val status = match.status.uppercase()
  val isRejected = status == "REJECTED"

  val (resultText, resultColor, resultBg) = when (matchResult) {
    HistoryMatchResult.WON -> Triple("বিজয়ী 🏆", Emerald400, Emerald600.copy(alpha = 0.2f))
    HistoryMatchResult.LOST -> Triple("পরাজিত", Rose400, Rose600.copy(alpha = 0.2f))
    HistoryMatchResult.PENDING -> Triple("পর্যালোচনাধীন", Gold400, Gold500.copy(alpha = 0.2f))
    HistoryMatchResult.CANCELLED -> Triple("বাতিল", Slate400, Slate600.copy(alpha = 0.2f))
  }

  val isWinner = matchResult == HistoryMatchResult.WON
  val isDefeat = matchResult == HistoryMatchResult.LOST
  val isCancelled = matchResult == HistoryMatchResult.CANCELLED
  val isUnderReview = matchResult == HistoryMatchResult.PENDING

  // Timestamp selection
  val rawTimestamp = when {
    match.completedAt > 0L -> match.completedAt
    match.scheduledAt > 0L -> match.scheduledAt
    match.createdAt > 0L -> match.createdAt
    else -> 0L
  }
  val formattedDate = remember(rawTimestamp) {
    if (rawTimestamp > 0L) {
      SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(rawTimestamp))
    } else {
      "Completed"
    }
  }

  val displayTitle = match.title.ifBlank {
    "${match.gameType} 1v1 Battle"
  }
  val matchIdShort = if (match.matchId.isNotBlank()) "#${match.matchId.takeLast(6).uppercase()}" else ""

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = NavySurface,
    border = BorderStroke(
      1.dp,
      when {
        isWinner -> Gold400.copy(alpha = 0.35f)
        isCancelled -> CardBorder
        else -> NavyCardBorder
      },
    ),
    modifier = modifier.clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      // Upper row: Icon + Title/Info + Result Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(
                when {
                  isWinner -> Gold400.copy(alpha = 0.15f)
                  isCancelled -> Slate800
                  isUnderReview -> Gold500.copy(alpha = 0.15f)
                  isRejected -> Rose600.copy(alpha = 0.15f)
                  isDefeat -> Rose600.copy(alpha = 0.12f)
                  else -> Slate800
                }
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = when {
                isWinner -> Icons.Default.EmojiEvents
                isCancelled -> Icons.Default.Refresh
                isUnderReview -> Icons.Default.HourglassEmpty
                isRejected -> Icons.Default.Cancel
                isDefeat -> Icons.Default.Close
                else -> Icons.Default.HourglassEmpty
              },
              contentDescription = resultText,
              tint = resultColor,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = displayTitle,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
              ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = buildString {
                append(formattedDate)
                if (matchIdShort.isNotBlank()) {
                  append(" • ")
                  append(matchIdShort)
                }
              },
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Result Chip
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = resultBg,
          border = BorderStroke(1.dp, resultColor.copy(alpha = 0.6f)),
        ) {
          Text(
            text = resultText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = resultColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Lower row: Match Details Strip
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate900,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Game
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${strings.labelGame} ",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
            )
            Text(
              text = if (match.gameType.equals("LUDO", ignoreCase = true)) strings.filterLudo
                     else if (match.gameType.equals("CARROM", ignoreCase = true)) strings.filterCarrom
                     else match.gameType,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Cyan400,
            )
          }

          // Entry Fee
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${strings.labelEntry} ",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
            )
            Text(
              text = "৳${match.effectiveEntryFeeMinorUnits / 100}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }

          // Prize
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${strings.labelPrize} ",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
            )
            Text(
              text = if (isCancelled) "—" else if (isDefeat) "৳ 0.00" else "৳ ${"%.2f".format(match.displayPrizePool)}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isCancelled || isDefeat) Slate400 else Gold400,
            )
          }
        }
      }

      // Cancellation Section
      if (isCancelled) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Rose600.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, Rose500.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("history_cancellation_section_${match.matchId}"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.matchCancelled,
                tint = Rose400,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = strings.matchCancelled,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                ),
                color = Rose400,
                modifier = Modifier.testTag("history_cancelled_title_${match.matchId}"),
              )
            }
            if (!match.cancelReason.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(start = 22.dp),
              ) {
                Text(
                  text = "${strings.cancellationReason}: ",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Slate300,
                )
                Text(
                  text = match.cancelReason,
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate200,
                  modifier = Modifier.testTag("history_cancelled_reason_${match.matchId}"),
                )
              }
            }
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// EMPTY STATE
// -----------------------------------------------------------------------------

@Composable
private fun HistoryEmptyState(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = CircleShape,
        color = Slate900,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.size(72.dp),
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(36.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
        ),
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
      )
    }
  }
}
