package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.ui.home.adPressScale
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

  // Card edge colour (look only)
  val frame = when {
    isCompleted && isUserWinner -> AdMatchFrame.WIN
    isCompleted && isUserLoser -> AdMatchFrame.RED
    isJoined -> AdMatchFrame.GREEN
    isCompleted -> AdMatchFrame.GREEN
    isCancelled -> AdMatchFrame.RED
    isPaused || isDisabled -> AdMatchFrame.AMBER
    isRunning || isCodeAdded -> AdMatchFrame.CYAN
    isFull -> AdMatchFrame.RED
    else -> AdMatchFrame.GOLD
  }
  val dimArt = isCancelled || isDisabled || (isFull && !isJoined && !isCompleted && !isRunning && !isCodeAdded)
  val press = remember { MutableInteractionSource() }

  AdMatchCardFrame(
    frame = frame,
    modifier = modifier
      .then(if (onCardClick != null) Modifier.adPressScale(press, 0.97f) else Modifier)
      .fillMaxWidth()
      .testTag("match_card_${match.matchId}")
      .then(
        if (onCardClick != null) {
          Modifier.clickable(interactionSource = press, indication = LocalIndication.current) { onCardClick() }
        } else {
          Modifier
        }
      ),
  ) {
    AdMatchGameArt(isLudo = isLudo, dim = dimArt, modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-4).dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(end = 80.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AdMatchGameChip(isLudo = isLudo, label = if (isLudo) strings.matchCardLudoChip else strings.matchCardCarromChip)
        if (match.matchNumber.isNotBlank()) {
          Spacer(modifier = Modifier.width(7.dp))
          Text(
            text = strings.digits(match.matchNumber),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xBFFFF0C8),
            maxLines = 1,
            softWrap = false,
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        MatchStatusBadge(
          status = match.status,
          joinedPlayersCount = match.joinedPlayersCount,
          maxPlayers = match.maxPlayers,
          isJoined = isJoined,
          isWinner = isUserWinner,
          isLoser = isUserLoser,
          premium = true,
        )
      }

      Spacer(modifier = Modifier.height(7.dp))

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

      Text(
        text = finalTitle,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        style = androidx.compose.ui.text.TextStyle(color = Color.White, shadow = androidx.compose.ui.graphics.Shadow(Color(0x80000000), androidx.compose.ui.geometry.Offset(0f, 2f), 4f)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(end = 80.dp),
      )

      Spacer(modifier = Modifier.height(12.dp))

      AdMatchStats(
        entryLabel = strings.entryFeeLabel,
        entry = strings.taka(match.displayEntryFee),
        formatLabel = strings.labelFormat,
        format = strings.label1v1Battle,
        prizeLabel = strings.prizePoolLabel,
        prize = strings.taka(match.displayPrizePool),
      )

      Spacer(modifier = Modifier.height(10.dp))

      val joinedCount = match.joinedPlayersCount
      val seatText = when {
        isCompleted -> strings.labelMatchCompleted.uppercase()
        joinedCount >= effectiveMaxPlayers || isFull ->
          strings.matchCardSeatJoined(strings.number(effectiveMaxPlayers), strings.number(effectiveMaxPlayers))
        joinedCount > 0 ->
          strings.matchCardSeatJoined(strings.number(joinedCount), strings.number(effectiveMaxPlayers))
        else ->
          strings.matchCardSeatJoined(strings.number(0), strings.number(effectiveMaxPlayers))
      }
      val seatFill = when {
        isCompleted -> AdSeatFill.GREEN
        joinedCount >= effectiveMaxPlayers || isFull -> if (isJoined) AdSeatFill.GREEN else AdSeatFill.RED
        joinedCount > 0 -> AdSeatFill.GOLD
        else -> AdSeatFill.EMPTY
      }
      AdMatchSeatBar(
        joined = if (isCompleted || isFull) effectiveMaxPlayers else joinedCount,
        maxSeats = effectiveMaxPlayers,
        text = seatText,
        fill = seatFill,
        modifier = Modifier.testTag("seat_status_bar_${match.matchId}"),
      )

      Spacer(modifier = Modifier.height(11.dp))

      when {
        isCompleted -> {
          AdMatchBanner(
            kind = if (isUserWinner) AdMatchBannerKind.WIN else if (isUserLoser) AdMatchBannerKind.LOSE else AdMatchBannerKind.DONE,
            text = when {
              isUserWinner -> strings.matchCardWinnerBanner(strings.taka(match.displayPrizePool))
              isUserLoser -> strings.matchCardDefeatBanner
              else -> strings.labelMatchCompleted
            },
            linkText = if (onCardClick != null) "${strings.labelViewResult} →" else null,
          )
        }
        isCancelled -> {
          AdMatchBanner(kind = AdMatchBannerKind.CANCEL, text = strings.matchCardCancelledBanner, linkText = null)
        }
        isPaused || isDisabled -> {
          AdMatchBanner(
            kind = AdMatchBannerKind.PAUSE,
            text = if (isPaused) strings.matchCardPausedBanner else strings.matchCardDisabledBanner,
            linkText = null,
          )
        }
        else -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            MatchCountdownTimer(match.scheduledTime)
            Spacer(modifier = Modifier.width(8.dp))
            when {
              isJoined -> AdMatchStateButton(
                text = strings.labelJoined,
                kind = AdMatchButtonKind.JOINED,
                tag = "joined_button_${match.matchId}",
                onClick = null,
              )
              isCodeAdded || isRunning -> AdMatchStateButton(
                text = if (isRunning) strings.matchCardButtonInProgress else strings.matchCardButtonCodeReady,
                kind = AdMatchButtonKind.LIVE,
                tag = "in_progress_${match.matchId}",
                onClick = { onCardClick?.invoke() },
              )
              isUpcoming -> AdMatchStateButton(
                text = strings.matchCardButtonUpcoming,
                kind = AdMatchButtonKind.UPCOMING,
                tag = "upcoming_button_${match.matchId}",
                onClick = null,
              )
              isFull -> AdMatchStateButton(
                text = strings.matchCardButtonSeatFull,
                kind = AdMatchButtonKind.FULL,
                tag = "match_full_${match.matchId}",
                onClick = null,
              )
              else -> AdMatchJoinButton(
                text = strings.actionJoinNowCta,
                onClick = { onJoinClick?.invoke() ?: onCardClick?.invoke() },
                tag = "join_button_${match.matchId}",
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

  val secondAngle = (((scheduledTimeMs - timeRemaining) / 1000L) % 60L).toFloat() * 6f
  AdMatchClock(
    timeText = scheduledTimeStr,
    leftLabel = labelText,
    countdown = countdownText,
    expired = isExpired,
    secondAngle = secondAngle,
  )
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
  premium: Boolean = false,
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

  if (premium) {
    AdMatchBadge(label = label, color = textColor, modifier = modifier)
    return
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
