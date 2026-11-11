package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.*
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchCard(
  match: MatchEntity,
  modifier: Modifier = Modifier,
  onCardClick: (() -> Unit)? = null,
  onJoinClick: (() -> Unit)? = null,
  onViewCodeClick: (() -> Unit)? = null,
  onSubmitProofClick: (() -> Unit)? = null,
  isJoined: Boolean = false,
  currentUserId: String = "",
) {
  val strings = LocalAppStrings.current
  val isLudo = match.gameType.equals("LUDO", ignoreCase = true)
  val isUpcoming = match.status.equals(MatchStatus.UPCOMING.name, ignoreCase = true)
  val isRunning = match.status.equals(MatchStatus.RUNNING.name, ignoreCase = true)
  val isCompleted = match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true)
  val isCancelled = match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)
  val effectiveMaxPlayers = if (match.maxPlayers > 0) match.maxPlayers else 2
  val isFull = match.joinedPlayersCount >= effectiveMaxPlayers || match.status.equals(MatchStatus.FULL.name, ignoreCase = true)
  val isCodeAdded = match.status.equals(MatchStatus.CODE_ADDED.name, ignoreCase = true) || match.gameCode.isNotBlank()
  val isResultSubmitted = match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true)
  val isPaused = match.status.equals(MatchStatus.PAUSED.name, ignoreCase = true)
  val isDisabled = match.status.equals(MatchStatus.DISABLED.name, ignoreCase = true)

  val authUid = com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.uid.orEmpty()
  val activeUser = currentUserId.ifBlank { authUid }
  val isUserWinner = isCompleted && activeUser.isNotBlank() && (
    (match.winnerUserId.isNotBlank() && (match.winnerUserId == activeUser || match.winnerUserId == currentUserId || (authUid.isNotBlank() && match.winnerUserId == authUid))) ||
    match.winnerUserId == "WINNER"
  )
  val isUserLoser = (isCompleted && isJoined && !isUserWinner) ||
    match.status.equals("LOST", ignoreCase = true) ||
    match.status.equals("REJECTED", ignoreCase = true) ||
    (isCompleted && match.winnerUserId.isNotBlank() && !isUserWinner)

  val defaultBorder = if (isJoined) Emerald500.copy(alpha = 0.6f) else MidnightNavyBorder
  val borderColor = when {
    isCompleted && isUserWinner -> Gold400.copy(alpha = 0.8f)
    isCompleted && isUserLoser -> Rose600.copy(alpha = 0.6f)
    isJoined -> Emerald500.copy(alpha = 0.8f)
    isRunning -> Indigo600
    isCompleted -> NeonEmerald.copy(alpha = 0.6f)
    isCancelled -> Rose600.copy(alpha = 0.5f)
    isPaused || isDisabled -> Amber600.copy(alpha = 0.5f)
    isUpcoming -> Amber500.copy(alpha = 0.4f)
    else -> defaultBorder
  }

  TournamentCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("match_card_${match.matchId}")
      .then(
        if (onCardClick != null) {
          Modifier.clickable { onCardClick() }
        } else {
          Modifier
        }
      ),
    backgroundColor = MidnightNavyCard,
    borderColor = borderColor,
    backgroundContent = {
      if (isLudo) {
        AnimatedLudoAsset(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 4.dp, end = 4.dp),
          size = 100.dp,
          alpha = 0.92f,
        )
      } else {
        AnimatedCarromAsset(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 4.dp, end = 4.dp),
          size = 100.dp,
          alpha = 0.92f,
        )
      }
    },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = if (isLudo) Color(0xFF1B1838) else Color(0xFF0C2234),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(0.8.dp, if (isLudo) Gold400.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.7f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isLudo) Gold400.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (isLudo) Icons.Default.Casino else Icons.Default.Album,
                contentDescription = null,
                tint = if (isLudo) Gold400 else NeonCyan,
                modifier = Modifier.size(13.dp),
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isLudo) strings.matchCardLudoChip else strings.matchCardCarromChip,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp,
                fontSize = 12.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        if (match.matchNumber.isNotBlank()) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.digits(match.matchNumber),
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            ),
            color = Slate300,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(end = 2.dp),
          )
        }
      }

      MatchStatusBadge(
        status = match.status,
        joinedPlayersCount = match.joinedPlayersCount,
        maxPlayers = match.maxPlayers,
        isJoined = isJoined,
        isWinner = isUserWinner,
        isLoser = isUserLoser,
        modifier = Modifier.padding(end = 96.dp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    val titleRegex = remember { Regex("(?i)^(ludo|carrom)\\s*1v1\\s*#(\\S+)$") }
    val displayGameName = if (isLudo) strings.filterLudo else strings.filterCarrom
    val finalTitle = remember(match.title, match.matchNumber, strings) {
      match.title.takeIf { it.isNotBlank() }?.let { title ->
        val matchResult = titleRegex.find(title)
        if (matchResult != null) {
          val game = matchResult.groupValues[1]
          val id = matchResult.groupValues[2]
          val localizedGame = if (game.equals("ludo", ignoreCase = true)) strings.filterLudo else strings.filterCarrom
          strings.historyMatchTitle(localizedGame, strings.digits(id))
        } else {
          title
        }
      } ?: run {
        val numStr = match.matchNumber.removePrefix("#").trim()
        if (numStr.isNotBlank()) {
          strings.historyMatchTitle(displayGameName, strings.digits(numStr))
        } else {
          "${displayGameName} ${strings.label1v1Battle}"
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = finalTitle,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
        ),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (onCardClick != null) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = strings.labelViewResult,
          tint = Slate400,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Slate900)
        .border(BorderStroke(0.8.dp, MidnightNavyBorder), RoundedCornerShape(10.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = strings.entryFeeLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = Slate400,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.taka(match.displayEntryFee),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
          ),
          color = Color.White,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(end = 2.dp),
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = strings.labelFormat,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = Slate400,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.label1v1Battle,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
          ),
          color = NeonCyan,
          maxLines = 1,
          softWrap = false,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = strings.prizePoolLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = Slate400,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.taka(match.displayPrizePool),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
          ),
          color = Gold400,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(end = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Thick seat status bar (Height ~28dp)
    val joinedCount = match.joinedPlayersCount
    val barColorBlue = Color(0xFF3B82F6)
    val barColorRed = Color(0xFFEF4444)

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Slate900)
        .testTag("seat_status_bar_${match.matchId}"),
      contentAlignment = Alignment.Center,
    ) {
      val seatText = when {
        isCompleted -> strings.labelMatchCompleted.uppercase()
        joinedCount >= effectiveMaxPlayers || isFull ->
          strings.matchCardSeatJoined(strings.number(effectiveMaxPlayers), strings.number(effectiveMaxPlayers))
        joinedCount > 0 ->
          strings.matchCardSeatJoined(strings.number(joinedCount), strings.number(effectiveMaxPlayers))
        else ->
          strings.matchCardSeatJoined(strings.number(0), strings.number(effectiveMaxPlayers))
      }

      when {
        isCompleted -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(NeonEmerald.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = seatText,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp,
                fontSize = 12.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        joinedCount >= effectiveMaxPlayers || isFull -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(barColorRed),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = seatText,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
                letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp,
                fontSize = 12.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        joinedCount > 0 -> {
          Row(modifier = Modifier.fillMaxSize()) {
            Box(
              modifier = Modifier
                .weight(joinedCount.toFloat())
                .fillMaxHeight()
                .background(barColorBlue)
            )
            Box(
              modifier = Modifier
                .weight((effectiveMaxPlayers - joinedCount).coerceAtLeast(1).toFloat())
                .fillMaxHeight()
                .background(barColorRed)
            )
          }
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = seatText,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
                letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp,
                fontSize = 12.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        else -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(barColorBlue),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = seatText,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
                letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp,
                fontSize = 12.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    when {
      isCompleted -> {
        val bannerBg = if (isUserWinner) Gold500.copy(alpha = 0.15f) else if (isUserLoser) Rose900.copy(alpha = 0.3f) else Emerald900.copy(alpha = 0.3f)
        val bannerBorder = if (isUserWinner) Gold400.copy(alpha = 0.6f) else if (isUserLoser) Rose500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.3f)
        val bannerTextColor = if (isUserWinner) Gold400 else if (isUserLoser) Rose400 else Emerald400
        val bannerText = when {
          isUserWinner -> strings.matchCardWinnerBanner(strings.taka(match.displayPrizePool))
          isUserLoser -> strings.matchCardDefeatBanner
          else -> strings.labelMatchCompleted
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bannerBg)
            .border(BorderStroke(1.dp, bannerBorder), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = bannerText,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            ),
            color = bannerTextColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(end = 4.dp),
          )
          if (onCardClick != null) {
            Text(
              text = "${strings.labelViewResult} →",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
              ),
              color = Gold400,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
      isCancelled -> {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Rose900.copy(alpha = 0.3f))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.matchCardCancelledBanner,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Rose400,
          )
        }
      }
      isPaused || isDisabled -> {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Amber900.copy(alpha = 0.3f))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isPaused) strings.matchCardPausedBanner else strings.matchCardDisabledBanner,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Amber400,
          )
        }
      }
      else -> {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MatchCountdownTimer(match.scheduledTime)
          when {
            isJoined -> {
              TournamentButton(
                text = strings.labelJoined,
                onClick = { onCardClick?.invoke() },
                enabled = false,
                variant = TournamentButtonVariant.JOINED,
                modifier = Modifier.height(40.dp),
                testTag = "joined_button_${match.matchId}",
              )
            }
            isCodeAdded || isRunning -> {
              TournamentButton(
                text = if (isRunning) strings.matchCardButtonInProgress else strings.matchCardButtonCodeReady,
                onClick = { onCardClick?.invoke() },
                variant = TournamentButtonVariant.SECONDARY,
                modifier = Modifier.height(40.dp),
                testTag = "in_progress_${match.matchId}",
              )
            }
            isUpcoming -> {
              TournamentButton(
                text = strings.matchCardButtonUpcoming,
                onClick = { onCardClick?.invoke() },
                enabled = false,
                variant = TournamentButtonVariant.SECONDARY,
                modifier = Modifier.height(40.dp),
                testTag = "upcoming_button_${match.matchId}",
              )
            }
            isFull -> {
              TournamentButton(
                text = strings.matchCardButtonSeatFull,
                onClick = { onCardClick?.invoke() },
                enabled = false,
                modifier = Modifier.height(40.dp),
                testTag = "match_full_${match.matchId}",
              )
            }
            else -> {
              TournamentButton(
                text = strings.actionJoinNowCta,
                onClick = { onJoinClick?.invoke() ?: onCardClick?.invoke() },
                modifier = Modifier.height(40.dp),
                testTag = "join_button_${match.matchId}",
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MatchCountdownTimer(scheduledTimeMs: Long) {
  val strings = LocalAppStrings.current
  var timeRemaining by remember(scheduledTimeMs) { mutableStateOf(scheduledTimeMs - System.currentTimeMillis()) }
  val scheduledTimeStr = remember(scheduledTimeMs, strings) {
    if (scheduledTimeMs > 0) {
      val rawTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date(scheduledTimeMs))
      strings.digits(rawTime)
    } else {
      strings.matchCardUpcoming
    }
  }

  LaunchedEffect(scheduledTimeMs) {
    while (true) {
      timeRemaining = scheduledTimeMs - System.currentTimeMillis()
      delay(1000L)
    }
  }

  val countdownText = remember(timeRemaining, strings) {
    if (timeRemaining <= 0) {
      strings.digits("00:00:00")
    } else {
      val hours = (timeRemaining / (1000 * 60 * 60)) % 24
      val minutes = (timeRemaining / (1000 * 60)) % 60
      val seconds = (timeRemaining / 1000) % 60
      val raw = if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
      } else {
        "%02d:%02d".format(minutes, seconds)
      }
      strings.digits(raw)
    }
  }

  val isExpired = timeRemaining <= 0
  val labelText = strings.matchCardTimeLeft

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = Slate950.copy(alpha = 0.88f),
    border = if (isExpired) {
      BorderStroke(1.dp, Color(0xFFEF4444))
    } else {
      BorderStroke(1.dp, Brush.horizontalGradient(listOf(Gold400, Cyan400)))
    },
    modifier = Modifier.testTag("match_time_hud_capsule")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Schedule,
        contentDescription = null,
        tint = if (isExpired) Color(0xFFEF4444) else Cyan400,
        modifier = Modifier.size(13.dp)
      )

      Text(
        text = scheduledTimeStr,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = Slate300,
        maxLines = 1,
        softWrap = false,
      )

      // Vertical Micro-Divider
      Box(
        modifier = Modifier
          .width(1.dp)
          .height(10.dp)
          .background(Color.White.copy(alpha = 0.22f))
      )

      Text(
        text = "$labelText $countdownText",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
          fontSize = 11.5.sp,
          letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp
        ),
        color = if (isExpired) Color(0xFFEF4444) else Gold400,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.padding(end = 2.dp)
      )
    }
  }
}

@Composable
fun MatchStatusBadge(
  status: String,
  joinedPlayersCount: Int,
  maxPlayers: Int = 2,
  isJoined: Boolean = false,
  isWinner: Boolean = false,
  isLoser: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val isFull = joinedPlayersCount >= maxPlayers || status.equals(MatchStatus.FULL.name, ignoreCase = true)
  val isRunning = status.equals(MatchStatus.RUNNING.name, ignoreCase = true)
  val isCompleted = status.equals(MatchStatus.COMPLETED.name, ignoreCase = true)
  val isCancelled = status.equals(MatchStatus.CANCELLED.name, ignoreCase = true)
  val isResultSubmitted = status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true)
  val isCodeAdded = status.equals(MatchStatus.CODE_ADDED.name, ignoreCase = true)
  val isPaused = status.equals(MatchStatus.PAUSED.name, ignoreCase = true)
  val isDisabled = status.equals(MatchStatus.DISABLED.name, ignoreCase = true)
  val isUpcoming = status.equals(MatchStatus.UPCOMING.name, ignoreCase = true)

  val strings = LocalAppStrings.current
  val (bgColor, textColor, label) = when {
    isCompleted && isWinner -> Triple(Gold400.copy(alpha = 0.2f), Gold400, strings.badgeVictory)
    isCompleted && (isLoser || isJoined) -> Triple(Rose600.copy(alpha = 0.2f), Rose400, strings.badgeDefeat)
    isLoser -> Triple(Rose600.copy(alpha = 0.2f), Rose400, strings.badgeDefeat)
    isWinner -> Triple(Gold400.copy(alpha = 0.2f), Gold400, strings.badgeVictory)
    isCompleted -> Triple(NeonEmerald.copy(alpha = 0.18f), NeonEmerald, strings.matchCardStatusCompleted)
    isJoined -> Triple(Emerald400.copy(alpha = 0.18f), Emerald400, strings.matchCardStatusJoined)
    isCancelled -> Triple(Rose600.copy(alpha = 0.2f), Rose400, strings.matchCardStatusCancelled)
    isPaused -> Triple(Amber600.copy(alpha = 0.2f), Amber400, strings.matchCardStatusPaused)
    isDisabled -> Triple(Slate700.copy(alpha = 0.5f), Slate400, strings.matchCardStatusDisabled)
    isUpcoming -> Triple(Amber500.copy(alpha = 0.2f), Amber400, strings.matchCardStatusUpcoming)
    isRunning -> Triple(Indigo600.copy(alpha = 0.25f), Indigo400, strings.matchCardStatusRunning)
    isResultSubmitted -> Triple(Purple600.copy(alpha = 0.25f), Purple400, strings.matchCardStatusReview)
    isCodeAdded -> Triple(NeonCyan.copy(alpha = 0.18f), NeonCyan, strings.matchCardStatusCodeReady)
    isFull -> {
      Triple(
        Rose600.copy(alpha = 0.2f),
        Rose400,
        strings.matchCardStatusFull(strings.number(joinedPlayersCount), strings.number(maxPlayers))
      )
    }
    else -> {
      Triple(
        NeonCyan.copy(alpha = 0.16f),
        NeonCyan,
        strings.matchCardStatusSlots(strings.number(joinedPlayersCount), strings.number(maxPlayers))
      )
    }
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.6f)),
    modifier = modifier
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
        letterSpacing = if (strings === BengaliStrings) 0.sp else 0.4.sp,
        fontSize = 11.5.sp,
      ),
      color = textColor,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}
