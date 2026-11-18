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
import com.example.core.i18n.*
import com.example.ui.components.PremiumNotificationBellButton
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.wallet.WlPageTitle
import com.example.ui.wallet.WlTone
import com.example.ui.wallet.wlBackground
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val MatchEntity.completedAt: Long
  get() = if (scheduledAt > 0L) scheduledAt else scheduledTime

/** Auto games are known by their title ("Auto Ludo || Match No:- #1") or game type. Used only to sort and name cards. */
private fun historyAutoKind(match: MatchEntity): String? {
  val t = match.title.lowercase(Locale.US).replace(" ", "")
  val g = match.gameType.uppercase(Locale.US).replace(" ", "").replace("_", "")
  return when {
    t.startsWith("autoludo") || g == "AUTOLUDO" -> "AUTO_LUDO"
    t.startsWith("autocarrom") || g == "AUTOCARROM" -> "AUTO_CARROM"
    else -> null
  }
}

private fun historyAutoCarromLabel(strings: AppStrings): String =
  if (strings === BengaliStrings) "অটো ক্যারম" else "Auto Carrom"

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

  // 2. Filter selection (ALL, LUDO, CARROM, AUTO_LUDO, AUTO_CARROM)
  var selectedFilter by remember { mutableStateOf("ALL") }

  val filteredMatches = remember(userHistoricalMatches, selectedFilter) {
    when (selectedFilter) {
      "LUDO" -> userHistoricalMatches.filter { historyAutoKind(it) == null && it.gameType.equals("LUDO", ignoreCase = true) }
      "CARROM" -> userHistoricalMatches.filter { historyAutoKind(it) == null && it.gameType.equals("CARROM", ignoreCase = true) }
      "AUTO_LUDO" -> userHistoricalMatches.filter { historyAutoKind(it) == "AUTO_LUDO" }
      "AUTO_CARROM" -> userHistoricalMatches.filter { historyAutoKind(it) == "AUTO_CARROM" }
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
        .wlBackground()
        .padding(innerPadding)
        .testTag("history_match_list"),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      // 1. PAGE TITLE & SUBTITLE
      item {
        WlPageTitle(
          title = strings.matchHistoryTitle,
          subtitle = strings.matchHistorySubtitle,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
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
              .padding(horizontal = 16.dp, vertical = 12.dp)
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
              .padding(horizontal = 16.dp, vertical = 12.dp)
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
  HsSummaryCard(
    totalLabel = strings.totalPlayed,
    totalText = strings.number(totalPlayed),
    wonLabel = strings.totalWon,
    wonText = strings.number(wonCount),
    lostLabel = strings.statLosses,
    lostText = strings.number(lostCount),
    rateLabel = strings.winRateLabel,
    rateText = strings.digits("$winRate%"),
    rateFraction = winRate / 100f,
    modifier = modifier,
  )
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
  val autoBadge = if (strings === BengaliStrings) "⚡ অটো" else "⚡ AUTO"
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      HsFilterCard(strings.filterAll, selectedFilter == "ALL", 0, { onFilterSelected("ALL") }, "history_filter_all", Modifier.weight(1f))
      HsFilterCard(strings.filterLudo, selectedFilter == "LUDO", 1, { onFilterSelected("LUDO") }, "history_filter_ludo", Modifier.weight(1f))
      HsFilterCard(strings.filterCarrom, selectedFilter == "CARROM", 2, { onFilterSelected("CARROM") }, "history_filter_carrom", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      HsFilterCard(strings.homeAutoLudoTitle, selectedFilter == "AUTO_LUDO", 1, { onFilterSelected("AUTO_LUDO") }, "history_filter_auto_ludo", Modifier.weight(1f), badge = autoBadge)
      HsFilterCard(historyAutoCarromLabel(strings), selectedFilter == "AUTO_CARROM", 2, { onFilterSelected("AUTO_CARROM") }, "history_filter_auto_carrom", Modifier.weight(1f), badge = autoBadge)
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
    HistoryMatchResult.WON -> Triple(strings.historyResultWon, Emerald400, Emerald600.copy(alpha = 0.2f))
    HistoryMatchResult.LOST -> Triple(strings.historyResultLost, Rose400, Rose600.copy(alpha = 0.2f))
    HistoryMatchResult.PENDING -> Triple(strings.historyResultPending, Gold400, Gold500.copy(alpha = 0.2f))
    HistoryMatchResult.CANCELLED -> Triple(strings.historyResultCancelled, Slate400, Slate600.copy(alpha = 0.2f))
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
  val formattedDate = remember(rawTimestamp, strings) {
    if (rawTimestamp > 0L) {
      if (strings === BengaliStrings) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = rawTimestamp }
        val day = strings.digits(String.format(Locale.US, "%02d", cal.get(java.util.Calendar.DAY_OF_MONTH)))
        val monthIdx = cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)
        val month = listOf(
          "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
          "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )[monthIdx]
        val year = strings.digits(cal.get(java.util.Calendar.YEAR).toString())
        val time = strings.digits(SimpleDateFormat("hh:mm", Locale.US).format(Date(rawTimestamp)))
        val amPm = SimpleDateFormat("a", Locale.US).format(Date(rawTimestamp))
        "$day $month $year, $time $amPm"
      } else {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(rawTimestamp))
      }
    } else {
      strings.historyDateCompleted
    }
  }

  val displayGameName = if (match.gameType.equals("LUDO", ignoreCase = true)) strings.filterLudo else strings.filterCarrom
  val matchIdShort = if (match.matchId.isNotBlank()) match.matchId.takeLast(6).uppercase() else ""
  
  val displayTitle = match.title.ifBlank {
    strings.historyMatchTitle(displayGameName, matchIdShort)
  }
  
  // Rebuild title if it matches: <Ludo|Carrom> 1v1 #<number>
  val titleRegex = Regex("(?i)^(ludo|carrom)\\s*1v1\\s*#(\\S+)$")
  val finalDisplayTitle = match.title.takeIf { it.isNotBlank() }?.let { title ->
      val matchResult = titleRegex.find(title)
      if (matchResult != null) {
          val game = matchResult.groupValues[1]
          val id = matchResult.groupValues[2]
          val localizedGame = if (game.equals("ludo", ignoreCase = true)) strings.filterLudo else strings.filterCarrom
          strings.historyMatchTitle(localizedGame, strings.digits(id))
      } else {
          title
      }
  } ?: strings.historyMatchTitle(displayGameName, strings.digits(matchIdShort))

  val matchIdDisplay = if (matchIdShort.isNotBlank()) "#$matchIdShort" else ""

  // Auto games: own name and title (look only)
  val autoKind = historyAutoKind(match)
  val autoLabel = when (autoKind) {
    "AUTO_LUDO" -> strings.homeAutoLudoTitle
    "AUTO_CARROM" -> historyAutoCarromLabel(strings)
    else -> null
  }
  val autoNumber = Regex("#\\s*(\\S+)\\s*$").find(match.title)?.groupValues?.get(1).orEmpty()
  val cardTitle = when {
    autoKind == "AUTO_LUDO" && autoNumber.isNotBlank() -> strings.autoLudoCardTitle(strings.digits(autoNumber))
    autoLabel != null && autoNumber.isNotBlank() -> "$autoLabel · #${strings.digits(autoNumber)}"
    autoLabel != null -> autoLabel
    else -> finalDisplayTitle
  }
  val isLudoGame = autoKind == "AUTO_LUDO" || (autoKind == null && match.gameType.equals("LUDO", ignoreCase = true))
  val kind = when (matchResult) {
    HistoryMatchResult.WON -> HsKind.WON
    HistoryMatchResult.LOST -> HsKind.LOST
    HistoryMatchResult.PENDING -> HsKind.PENDING
    HistoryMatchResult.CANCELLED -> HsKind.CANCELLED
  }
  val picture = when (kind) {
    HsKind.WON -> R.drawable.wl_trophy
    HsKind.PENDING -> R.drawable.wl_hourglass
    else -> if (isLudoGame) R.drawable.mg_dice else R.drawable.mg_carrom
  }

  HsMatchCard(
    kind = kind,
    picture = picture,
    resultText = resultText,
    title = cardTitle,
    dateLine = buildString {
      append(formattedDate)
      if (matchIdDisplay.isNotBlank()) {
        append(" • ")
        append(matchIdDisplay)
      }
    },
    gameLabel = strings.historyGameLabel,
    gameText = autoLabel
      ?: if (match.gameType.equals("LUDO", ignoreCase = true)) strings.filterLudo
      else if (match.gameType.equals("CARROM", ignoreCase = true)) strings.filterCarrom
      else match.gameType,
    entryLabel = strings.historyEntryLabel,
    entryText = strings.takaFromMinor(match.effectiveEntryFeeMinorUnits),
    prizeLabel = strings.historyPrizeLabel,
    prizeText = if (isCancelled) "—" else if (isDefeat) strings.taka(0.0) else strings.taka(match.displayPrizePool),
    prizeTone = if (isCancelled) WlTone.GREY else if (isDefeat) WlTone.RED else WlTone.GREEN,
    onClick = onClick,
    modifier = modifier,
    extra = {
      if (isCancelled) {
        HsCancelBox(strings.matchCancelled, strings.cancellationReason, match.cancelReason, match.matchId)
      }
    },
  )
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
  HsEmptyState(title = title, subtitle = subtitle, modifier = modifier)
}
