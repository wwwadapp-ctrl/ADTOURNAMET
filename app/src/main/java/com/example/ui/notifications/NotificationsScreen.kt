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
import com.example.ui.wallet.WlEmptyState
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.wlBackground
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
        .wlBackground()
        .padding(innerPadding),
    ) {
      // Header Section: Back button, Title & Subtitle, and Mark All Read CTA
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        NfHeader(
          title = strings.notificationsTitle,
          subtitle = strings.notifSubtitle,
          backLabel = strings.back,
          onBack = onNavigateBack,
          markAllText = if (hasUnread) strings.notifMarkAllRead else null,
          onMarkAll = { viewModel.markAllAsRead() },
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Tabs (All / Unread)
        val unreadCount = remember(notifications) { notifications.count { !it.effectiveRead } }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          NfTab(
            text = strings.notifFilterAll,
            selected = selectedFilter == "ALL",
            onClick = { selectedFilter = "ALL" },
            tag = "notification_filter_all",
            modifier = Modifier.weight(1f),
          )
          NfTab(
            text = strings.notifFilterUnread,
            selected = selectedFilter == "UNREAD",
            onClick = { selectedFilter = "UNREAD" },
            tag = "notification_filter_unread",
            modifier = Modifier.weight(1f),
            badge = if (unreadCount > 0) strings.digits(unreadCount.toString()) else null,
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
            WlFrame(gold = true, pad = 14.dp) {
              WlEmptyState(
                title = if (selectedFilter == "UNREAD" && notifications.isNotEmpty()) {
                  strings.notifEmptyFilterTitle
                } else {
                  strings.noNotificationsTitle
                },
                description = if (selectedFilter == "UNREAD" && notifications.isNotEmpty()) {
                  strings.notifEmptyFilterSubtext
                } else {
                  strings.noNotificationsSubtext
                },
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

        val (dPicture, dRing) = nfLook(notif.type)
        NfDetailWindow(
          picture = dPicture,
          ring = dRing,
          title = if (strings !== BengaliStrings && notif.titleEn.isNotBlank()) notif.titleEn else notif.title,
          category = getNotificationCategory(notif.type, strings),
          date = formatNotificationDetailDate(notif.effectiveTimestamp, strings),
          message = if (strings !== BengaliStrings && notif.messageEn.isNotBlank()) notif.messageEn else notif.effectiveMessage,
          closeText = strings.notifDialogClose,
          onClose = { selectedNotification = null },
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

  val (picture, ring) = remember(notif.type) { nfLook(notif.type) }

  NfCard(
    picture = picture,
    ring = ring,
    title = if (strings !== BengaliStrings && notif.titleEn.isNotBlank()) notif.titleEn else notif.title,
    message = if (strings !== BengaliStrings && notif.messageEn.isNotBlank()) notif.messageEn else notif.effectiveMessage,
    unread = !isRead,
    newText = strings.notifBadgeNew,
    category = categoryLabel,
    time = formattedTime,
    onClick = onClick,
    modifier = Modifier.testTag("notification_item_${notif.effectiveId}"),
  )
}
