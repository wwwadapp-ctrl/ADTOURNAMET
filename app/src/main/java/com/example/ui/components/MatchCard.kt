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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.LocalAppStrings
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
              text = if (isLudo) "LUDO 1v1" else "CARROM 1v1",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = match.matchNumber,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = Slate300,
        )
      }

      MatchStatusBadge(
        status = match.status,
        joinedPlayersCount = match.joinedPlayersCount,
        maxPlayers = match.maxPlayers,
        isJoined = isJoined,
        isWinner = isUserWinner,
        isLoser = isUserLoser,
        modifier = Modifier.padding(end = 116.dp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = match.title.ifBlank { "${if (isLudo) "Ludo" else "Carrom"} 1v1 Battle" },
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier.weight(1f),
      )
      if (onCardClick != null) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = "View Details",
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
      val strings = LocalAppStrings.current
      Column {
        Text(text = strings.entryFeeLabel, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
          text = "৳ ${"%.2f".format(match.displayEntryFee)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = strings.labelFormat, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
          text = strings.label1v1Battle,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = NeonCyan,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(text = strings.prizePoolLabel, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
          text = "৳ ${"%.2f".format(match.displayPrizePool)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
          color = Gold400,
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
      val strings = LocalAppStrings.current
      when {
        isCompleted -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(NeonEmerald.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = strings.labelMatchCompleted.uppercase(),
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
          }
        }
        joinedCount >= 2 || isFull -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(barColorRed),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "2/2 JOINED",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
          }
        }
        joinedCount == 1 -> {
          Row(modifier = Modifier.fillMaxSize()) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(barColorBlue)
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(barColorRed)
            )
          }
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "1/2 JOINED",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
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
              text = "0/2 JOINED",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    when {
      isCompleted -> {
        val strings = LocalAppStrings.current
        val bannerBg = if (isUserWinner) Gold500.copy(alpha = 0.15f) else if (isUserLoser) Rose900.copy(alpha = 0.3f) else Emerald900.copy(alpha = 0.3f)
        val bannerBorder = if (isUserWinner) Gold400.copy(alpha = 0.6f) else if (isUserLoser) Rose500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.3f)
        val bannerTextColor = if (isUserWinner) Gold400 else if (isUserLoser) Rose400 else Emerald400
        val bannerText = when {
          isUserWinner -> "${strings.labelMatchCompleted} • ${strings.badgeVictory} (৳ ${"%.2f".format(match.displayPrizePool)})"
          isUserLoser -> "${strings.labelMatchCompleted} • ${strings.badgeDefeat}"
          else -> strings.labelMatchCompleted
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bannerBg)
            .border(BorderStroke(1.dp, bannerBorder), RoundedCornerShape(8.dp))
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = bannerText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = bannerTextColor,
          )
          if (onCardClick != null) {
            Text(
              text = "${strings.labelViewResult} →",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Gold400,
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
            text = "Match Cancelled • Entry fees refunded",
            style = MaterialTheme.typography.bodySmall,
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
            text = if (isPaused) "Match Paused by Super Admin" else "Match Disabled",
            style = MaterialTheme.typography.bodySmall,
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
          val strings = LocalAppStrings.current
          val timeFormatted = remember(match.scheduledTime) {
            if (match.scheduledTime > 0) {
              SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(match.scheduledTime))
            } else {
              "Upcoming"
            }
          }
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
                text = if (isRunning) "IN PROGRESS" else "CODE READY",
                onClick = { onCardClick?.invoke() },
                variant = TournamentButtonVariant.SECONDARY,
                modifier = Modifier.height(40.dp),
                testTag = "in_progress_${match.matchId}",
              )
            }
            isUpcoming -> {
              TournamentButton(
                text = "UPCOMING",
                onClick = { onCardClick?.invoke() },
                enabled = false,
                variant = TournamentButtonVariant.SECONDARY,
                modifier = Modifier.height(40.dp),
                testTag = "upcoming_button_${match.matchId}",
              )
            }
            isFull -> {
              TournamentButton(
                text = "SEAT FULL",
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
  val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
  val scheduledTimeStr = remember(scheduledTimeMs) {
    if (scheduledTimeMs > 0) timeFormat.format(Date(scheduledTimeMs)) else "Upcoming"
  }

  LaunchedEffect(scheduledTimeMs) {
    while (true) {
      timeRemaining = scheduledTimeMs - System.currentTimeMillis()
      delay(1000L)
    }
  }

  val countdownText = remember(timeRemaining) {
    if (timeRemaining <= 0) {
      "00:00:00"
    } else {
      val hours = (timeRemaining / (1000 * 60 * 60)) % 24
      val minutes = (timeRemaining / (1000 * 60)) % 60
      val seconds = (timeRemaining / 1000) % 60
      if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
      } else {
        "%02d:%02d".format(minutes, seconds)
      }
    }
  }

  val isExpired = timeRemaining <= 0
  val labelText = if (strings is com.example.core.i18n.BengaliStrings) "বাকি" else "Left"

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
          fontSize = 10.5.sp
        ),
        color = Slate300,
        maxLines = 1
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
          fontWeight = FontWeight.Black,
          fontSize = 11.sp,
          letterSpacing = 0.5.sp
        ),
        color = if (isExpired) Color(0xFFEF4444) else Gold400,
        maxLines = 1
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
  val isBN = strings is com.example.core.i18n.BengaliStrings
  val (bgColor, textColor, label) = when {
    isCompleted && isWinner -> Triple(Gold400.copy(alpha = 0.2f), Gold400, strings.badgeVictory)
    isCompleted && (isLoser || isJoined) -> Triple(Rose600.copy(alpha = 0.2f), Rose400, strings.badgeDefeat)
    isLoser -> Triple(Rose600.copy(alpha = 0.2f), Rose400, strings.badgeDefeat)
    isWinner -> Triple(Gold400.copy(alpha = 0.2f), Gold400, strings.badgeVictory)
    isCompleted -> Triple(NeonEmerald.copy(alpha = 0.18f), NeonEmerald, if (isBN) "সম্পন্ন" else "COMPLETED")
    isJoined -> Triple(Emerald400.copy(alpha = 0.18f), Emerald400, if (isBN) "যুক্ত" else "JOINED")
    isCancelled -> Triple(Rose600.copy(alpha = 0.2f), Rose400, if (isBN) "বাতিল" else "CANCELLED")
    isPaused -> Triple(Amber600.copy(alpha = 0.2f), Amber400, if (isBN) "স্থগিত" else "PAUSED")
    isDisabled -> Triple(Slate700.copy(alpha = 0.5f), Slate400, if (isBN) "অক্ষম" else "DISABLED")
    isUpcoming -> Triple(Amber500.copy(alpha = 0.2f), Amber400, if (isBN) "আসন্ন" else "UPCOMING")
    isRunning -> Triple(Indigo600.copy(alpha = 0.25f), Indigo400, if (isBN) "চলমান" else "RUNNING")
    isResultSubmitted -> Triple(Purple600.copy(alpha = 0.25f), Purple400, if (isBN) "রিভিউ" else "REVIEW")
    isCodeAdded -> Triple(NeonCyan.copy(alpha = 0.18f), NeonCyan, if (isBN) "কোড রেডি" else "CODE READY")
    isFull -> {
      val countText = if (isBN) {
        val bnCount = joinedPlayersCount.toString().map { if (it.isDigit()) '০' + (it - '0') else it }.joinToString("")
        val bnMax = maxPlayers.toString().map { if (it.isDigit()) '০' + (it - '0') else it }.joinToString("")
        "$bnCount/$bnMax"
      } else {
        "$joinedPlayersCount/$maxPlayers"
      }
      Triple(Rose600.copy(alpha = 0.2f), Rose400, if (isBN) "পূর্ণ ($countText)" else "FULL ($countText)")
    }
    else -> {
      val countText = if (isBN) {
        val bnCount = joinedPlayersCount.toString().map { if (it.isDigit()) '০' + (it - '0') else it }.joinToString("")
        val bnMax = maxPlayers.toString().map { if (it.isDigit()) '০' + (it - '0') else it }.joinToString("")
        "$bnCount/$bnMax"
      } else {
        "$joinedPlayersCount/$maxPlayers"
      }
      Triple(NeonCyan.copy(alpha = 0.16f), NeonCyan, if (isBN) "$countText স্লট" else "$countText SLOTS")
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
        fontWeight = FontWeight.Black,
        letterSpacing = 0.4.sp,
      ),
      color = textColor,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}
