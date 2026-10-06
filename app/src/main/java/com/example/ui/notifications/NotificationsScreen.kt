package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.error.Resource
import com.example.core.i18n.LocalAppStrings
import com.example.data.repository.LocalDataStore
import com.example.domain.model.NotificationEntity
import com.example.domain.repository.NotificationRepository
import com.example.ui.components.TournamentCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
  userId: String,
  onNavigateBack: () -> Unit,
  notificationRepository: NotificationRepository,
  modifier: Modifier = Modifier,
  onNavigateToTarget: ((String) -> Unit)? = null,
  viewModel: NotificationsViewModel = viewModel(
    key = "NotificationsViewModel_$userId",
    factory = NotificationsViewModel.Factory(notificationRepository, userId)
  )
) {
  val strings = LocalAppStrings.current
  val notificationsResource by viewModel.notifications.collectAsState()
  
  val initialList: List<NotificationEntity> = remember(userId) {
    LocalDataStore.localUserNotifications[userId]?.toList()?.take(50) ?: emptyList()
  }
  
  val notifications = (notificationsResource as? Resource.Success<List<NotificationEntity>>)?.data ?: initialList
  val isLoading = notificationsResource is Resource.Loading && initialList.isEmpty()
  val hasUnread = notifications.any { !it.effectiveRead }
  var selectedNotification by remember { mutableStateOf<NotificationEntity?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .testTag("notifications_screen"),
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false),
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("notifications_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = strings.notificationsTitle,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
          color = Color.White,
        )
      }

      if (hasUnread) {
        TextButton(
          onClick = {
            viewModel.markAllAsRead()
          },
          modifier = Modifier.testTag("mark_all_read_button"),
        ) {
          Text(
            text = "সব পঠিত",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )
        }
      }
    }

    when {
      isLoading -> {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("notifications_loading"),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            color = Gold400,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
          )
        }
      }
      notifications.isEmpty() -> {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(32.dp)
            .testTag("notifications_empty_state"),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MidnightNavyCard),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(36.dp),
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = strings.noNotificationsTitle,
              color = Color.White,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = strings.noNotificationsSubtext,
              color = Slate400,
              style = MaterialTheme.typography.bodySmall,
              lineHeight = 18.sp,
            )
          }
        }
      }
      else -> {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          items(notifications, key = { it.effectiveId }) { notif ->
            NotificationCard(
              notif = notif,
              onClick = {
                if (!notif.effectiveRead) {
                  viewModel.markAsRead(notif.effectiveId)
                }
                selectedNotification = notif
              },
            )
          }
        }
      }
    }

    if (selectedNotification != null) {
      val notif = selectedNotification!!
      val formattedDate = remember(notif.effectiveTimestamp) {
        if (notif.effectiveTimestamp > 0L) {
          SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.effectiveTimestamp))
        } else {
          "Recent"
        }
      }

      AlertDialog(
        onDismissRequest = { selectedNotification = null },
        modifier = Modifier.testTag("notification_detail_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = Slate900,
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Gold500.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Gold400,
                modifier = Modifier.size(20.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = notif.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = formattedDate,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = Slate400,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = notif.effectiveMessage,
              style = MaterialTheme.typography.bodyLarge,
              color = Slate200,
              lineHeight = 22.sp,
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = { selectedNotification = null },
            modifier = Modifier.testTag("notification_dialog_close_button"),
          ) {
            Text(
              text = "ঠিক আছে",
              color = Gold400,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
          }
        },
      )
    }
  }
}

@Composable
private fun NotificationCard(
  notif: NotificationEntity,
  onClick: () -> Unit,
) {
  val isRead = notif.effectiveRead
  val formattedDate = remember(notif.effectiveTimestamp) {
    if (notif.effectiveTimestamp > 0L) {
      SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.effectiveTimestamp))
    } else {
      "Recent"
    }
  }

  val (icon, iconColor, bgColor) = remember(notif.type, isRead) {
    val type = try { com.example.domain.model.NotificationType.valueOf(notif.type) } catch (_: Exception) { com.example.domain.model.NotificationType.ROOM_CODE }
    when (type) {
      com.example.domain.model.NotificationType.DEPOSIT_APPROVED -> Triple(Icons.Default.AccountBalanceWallet, Emerald400, Emerald400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.DEPOSIT_REJECTED -> Triple(Icons.Default.AccountBalanceWallet, Rose400, Rose400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.WITHDRAWAL_APPROVED -> Triple(Icons.Default.ArrowUpward, Gold400, Gold400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.WITHDRAWAL_REJECTED -> Triple(Icons.Default.ArrowUpward, Rose400, Rose400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.MATCH_WIN -> Triple(Icons.Default.EmojiEvents, Gold500, Gold500.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.MATCH_LOST -> Triple(Icons.Default.SportsEsports, Slate400, Slate400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.MATCH_PROOF_REJECTED -> Triple(Icons.Default.Gavel, Rose400, Rose400.copy(alpha = 0.15f))
      com.example.domain.model.NotificationType.ROOM_CODE -> Triple(Icons.Default.VpnKey, Cyan400, Cyan400.copy(alpha = 0.15f))
    }
  }

  TournamentCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("notification_item_${notif.effectiveId}"),
    onClick = onClick,
    backgroundColor = if (!isRead) NavySurface else MidnightNavyCard,
    borderColor = if (!isRead) Indigo600.copy(alpha = 0.8f) else MidnightNavyBorder,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(if (!isRead) bgColor else Slate800),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (!isRead) iconColor else Slate400,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = notif.title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          if (!isRead) {
            Surface(
              color = Indigo600,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = "NEW",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = notif.effectiveMessage,
          style = MaterialTheme.typography.bodySmall,
          color = Slate300,
          lineHeight = 17.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = formattedDate,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = Slate500,
        )
      }
    }
  }
}
