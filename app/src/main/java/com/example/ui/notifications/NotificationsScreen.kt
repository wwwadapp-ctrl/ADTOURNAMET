package com.example.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.error.Resource
import com.example.core.i18n.*
import com.example.data.repository.LocalDataStore
import com.example.domain.model.NotificationEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.domain.repository.NotificationRepository
import com.example.ui.components.TournamentCard
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val bengaliMonths = listOf(
  "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
  "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

private fun formatNotificationTime(timestamp: Long, strings: AppStrings): String {
  if (timestamp <= 0L) return strings.notifTimeRecent
  val now = System.currentTimeMillis()
  val diff = (now - timestamp).coerceAtLeast(0L)
  if (diff < 60_000L) {
    return strings.notifTimeJustNow
  }
  if (diff < 3600_000L) {
    val mins = (diff / 60_000L).coerceAtLeast(1)
    return strings.notifTimeMinutesAgo(strings.digits(mins.toString()))
  }
  if (diff < 86400_000L) {
    val hours = (diff / 3600_000L).coerceAtLeast(1)
    return strings.notifTimeHoursAgo(strings.digits(hours.toString()))
  }
  if (diff < 172800_000L) {
    return strings.notifTimeYesterday
  }
  return if (strings === BengaliStrings) {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = strings.digits(String.format(Locale.US, "%02d", cal.get(java.util.Calendar.DAY_OF_MONTH)))
    val monthIdx = cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)
    val month = bengaliMonths[monthIdx]
    val time = strings.digits(SimpleDateFormat("hh:mm", Locale.US).format(Date(timestamp)))
    val amPm = SimpleDateFormat("a", Locale.US).format(Date(timestamp))
    "$day $month, $time $amPm"
  } else {
    SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(timestamp))
  }
}

private fun formatNotificationDetailDate(timestamp: Long, strings: AppStrings): String {
  if (timestamp <= 0L) return strings.notifTimeRecent
  return if (strings === BengaliStrings) {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = strings.digits(String.format(Locale.US, "%02d", cal.get(java.util.Calendar.DAY_OF_MONTH)))
    val monthIdx = cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)
    val month = bengaliMonths[monthIdx]
    val year = strings.digits(cal.get(java.util.Calendar.YEAR).toString())
    val time = strings.digits(SimpleDateFormat("hh:mm", Locale.US).format(Date(timestamp)))
    val amPm = SimpleDateFormat("a", Locale.US).format(Date(timestamp))
    "$day $month $year, $time $amPm"
  } else {
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(timestamp))
  }
}

private fun getNotificationCategory(typeStr: String, strings: AppStrings): String {
  val type = try {
    com.example.domain.model.NotificationType.valueOf(typeStr)
  } catch (_: Exception) {
    null
  }
  return when (type) {
    com.example.domain.model.NotificationType.DEPOSIT_APPROVED,
    com.example.domain.model.NotificationType.DEPOSIT_REJECTED,
    com.example.domain.model.NotificationType.WITHDRAWAL_APPROVED,
    com.example.domain.model.NotificationType.WITHDRAWAL_REJECTED -> strings.notifCategoryWallet
    com.example.domain.model.NotificationType.MATCH_WIN,
    com.example.domain.model.NotificationType.MATCH_LOST,
    com.example.domain.model.NotificationType.MATCH_PROOF_REJECTED -> strings.notifCategoryMatch
    com.example.domain.model.NotificationType.ROOM_CODE -> strings.notifCategoryRoomCode
    else -> strings.notifCategorySystem
  }
}

@Composable
fun NotificationsScreen(
  userId: String,
  currentUser: UserEntity?,
  wallet: WalletEntity?,
  unreadNotificationsCount: Int,
  onWalletClick: () -> Unit,
  onNotificationClick: () -> Unit,
  onProfileClick: () -> Unit,
  onAdminClick: (() -> Unit)? = null,
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
  var selectedFilter by remember { mutableStateOf("ALL") }

  val filteredNotifications = remember(notifications, selectedFilter) {
    if (selectedFilter == "UNREAD") {
      notifications.filter { !it.effectiveRead }
    } else {
      notifications
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
        onAdminClick = onAdminClick
      )
    },
    containerColor = DeepNavyBg,
    modifier = modifier.fillMaxSize().testTag("notifications_screen"),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Header Section: Back button, Title & Subtitle, and Mark All Read CTA
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier
                .size(40.dp)
                .testTag("notifications_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.back,
                tint = Slate200
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = strings.notificationsTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                color = Color.White
              )
              Text(
                text = strings.notifSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 12.sp
                ),
                color = Slate400
              )
            }
          }

          if (hasUnread) {
            TextButton(
              onClick = { viewModel.markAllAsRead() },
              modifier = Modifier.testTag("mark_all_read_button"),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = Gold400,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = strings.notifMarkAllRead,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Gold400,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs (All / Unread)
        val unreadCount = remember(notifications) { notifications.count { !it.effectiveRead } }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedFilter == "ALL",
            onClick = { selectedFilter = "ALL" },
            label = {
              Text(
                text = strings.notifFilterAll,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (selectedFilter == "ALL") FontWeight.Bold else FontWeight.Medium
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White,
              containerColor = MidnightNavyCard,
              labelColor = Slate300
            ),
            border = BorderStroke(
              1.dp,
              if (selectedFilter == "ALL") Indigo600 else MidnightNavyBorder
            ),
            modifier = Modifier.testTag("notification_filter_all")
          )

          FilterChip(
            selected = selectedFilter == "UNREAD",
            onClick = { selectedFilter = "UNREAD" },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = strings.notifFilterUnread,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selectedFilter == "UNREAD") FontWeight.Bold else FontWeight.Medium
                  )
                )
                if (unreadCount > 0) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = CircleShape,
                    color = if (selectedFilter == "UNREAD") Gold400 else Indigo600,
                    modifier = Modifier.size(18.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = strings.digits(unreadCount.toString()),
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold
                        ),
                        color = if (selectedFilter == "UNREAD") Color.Black else Color.White
                      )
                    }
                  }
                }
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Indigo600,
              selectedLabelColor = Color.White,
              containerColor = MidnightNavyCard,
              labelColor = Slate300
            ),
            border = BorderStroke(
              1.dp,
              if (selectedFilter == "UNREAD") Indigo600 else MidnightNavyBorder
            ),
            modifier = Modifier.testTag("notification_filter_unread")
          )
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
        filteredNotifications.isEmpty() -> {
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
                text = if (selectedFilter == "UNREAD" && notifications.isNotEmpty()) {
                  strings.notifEmptyFilterTitle
                } else {
                  strings.noNotificationsTitle
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = if (selectedFilter == "UNREAD" && notifications.isNotEmpty()) {
                  strings.notifEmptyFilterSubtext
                } else {
                  strings.noNotificationsSubtext
                },
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
          ) {
            items(filteredNotifications, key = { it.effectiveId }) { notif ->
              NotificationCard(
                notif = notif,
                strings = strings,
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
                text = if (strings !== BengaliStrings && notif.titleEn.isNotBlank()) notif.titleEn else notif.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                ),
                color = Color.White,
              )
            }
          },
          text = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  color = Gold500.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f))
                ) {
                  Text(
                    text = getNotificationCategory(notif.type, strings),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold
                    ),
                    color = Gold400,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Text(
                  text = formatNotificationDetailDate(notif.effectiveTimestamp, strings),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = Slate400,
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (strings !== BengaliStrings && notif.messageEn.isNotBlank()) notif.messageEn else notif.effectiveMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 14.sp,
                  lineHeight = 22.sp
                ),
                color = Slate200,
              )
            }
          },
          confirmButton = {
            TextButton(
              onClick = { selectedNotification = null },
              modifier = Modifier.testTag("notification_dialog_close_button"),
            ) {
              Text(
                text = strings.notifDialogClose,
                color = Gold400,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              )
            }
          },
        )
      }
    }
  }
}

@Composable
private fun NotificationCard(
  notif: NotificationEntity,
  strings: AppStrings,
  onClick: () -> Unit,
) {
  val isRead = notif.effectiveRead
  val formattedTime = remember(notif.effectiveTimestamp, strings) {
    formatNotificationTime(notif.effectiveTimestamp, strings)
  }
  val categoryLabel = remember(notif.type, strings) {
    getNotificationCategory(notif.type, strings)
  }

  val (icon, iconColor, bgColor) = remember(notif.type, isRead) {
    val type = try {
      com.example.domain.model.NotificationType.valueOf(notif.type)
    } catch (_: Exception) {
      com.example.domain.model.NotificationType.ROOM_CODE
    }
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
          .size(40.dp)
          .clip(CircleShape)
          .background(if (!isRead) bgColor else Slate800),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (!isRead) iconColor else Slate400,
          modifier = Modifier.size(20.dp),
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
            text = if (strings !== BengaliStrings && notif.titleEn.isNotBlank()) notif.titleEn else notif.title,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (!isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = Indigo600,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = strings.notifBadgeNew,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (strings !== BengaliStrings && notif.messageEn.isNotBlank()) notif.messageEn else notif.effectiveMessage,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp
          ),
          color = if (!isRead) Slate200 else Slate400,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = iconColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
          ) {
            Text(
              text = categoryLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = iconColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Slate400,
          )
        }
      }
    }
  }
}
