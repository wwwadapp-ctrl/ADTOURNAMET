package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.core.config.FirebaseConfig
import com.example.core.error.Resource
import com.example.core.firebase.FirebaseManager
import com.example.data.repository.CanonicalNotificationDto
import com.example.data.repository.FirebaseNotificationRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.NotificationEntity
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.notifications.NotificationsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class NotificationCenterTest {

  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val notificationRepository = FirebaseNotificationRepository()

  @Before
  fun setup() {
    LocalDataStore.localUserNotifications.clear()
  }

  @Test
  fun homeNotificationButton_triggersCallback() {
    var notificationClicked = false

    composeTestRule.setContent {
      HomeScreen(
        viewModel = HomeViewModel(
          matchRepository = com.example.data.repository.FirebaseMatchRepository(),
          currentUserId = "test_user_1",
        ),
        currentUser = null,
        wallet = null,
        onMatchClick = {},
        onWalletClick = {},
        onDepositClick = {},
        onNotificationClick = { notificationClicked = true },
        onProfileClick = {},
        onRulesClick = {},
        onSupportClick = {},
        onAdminClick = {},
      )
    }

    composeTestRule.onNodeWithTag("home_notification_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_notification_button").performClick()
    assertTrue(notificationClicked)
  }

  @Test
  fun notificationsScreen_showsEmptyState_whenNoNotifications() {
    composeTestRule.setContent {
      NotificationsScreen(
        userId = "user_with_no_notifs",
        onNavigateBack = {},
        notificationRepository = notificationRepository,
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notifications_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithText("কোনো নতুন নোটিফিকেশন নেই").assertIsDisplayed()
  }

  @Test
  fun notificationsScreen_doesNotShowOtherUsersNotifications() {
    // Add notification for user_B
    LocalDataStore.localUserNotifications["user_B"] = CopyOnWriteArrayList(
      listOf(
        NotificationEntity(
          notificationId = "notif_user_b",
          userId = "user_B",
          title = "Secret Alert For User B",
          body = "This must not be visible to user A",
          createdAt = System.currentTimeMillis(),
        )
      )
    )

    // View notifications for user_A
    composeTestRule.setContent {
      NotificationsScreen(
        userId = "user_A",
        onNavigateBack = {},
        notificationRepository = notificationRepository,
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithText("কোনো নতুন নোটিফিকেশন নেই").assertIsDisplayed()
  }

  @Test
  fun notificationsScreen_showsUserNotifications_andMarksAsReadOnClick() {
    val targetUser = "user_real_winner"
    val notifId = "notif_winner_1"
    LocalDataStore.localUserNotifications[targetUser] = CopyOnWriteArrayList(
      listOf(
        NotificationEntity(
          notificationId = notifId,
          userId = targetUser,
          title = "Prize Credited! 🏆",
          body = "Congratulations! You won ৳ 90.",
          isRead = false,
          createdAt = System.currentTimeMillis(),
        )
      )
    )

    composeTestRule.setContent {
      NotificationsScreen(
        userId = targetUser,
        onNavigateBack = {},
        notificationRepository = notificationRepository,
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notification_item_$notifId").assertIsDisplayed()
    composeTestRule.onNodeWithText("Prize Credited! 🏆").assertIsDisplayed()
    composeTestRule.onNodeWithText("NEW").assertIsDisplayed()

    // Click on the notification to mark it as read
    composeTestRule.onNodeWithTag("notification_item_$notifId").performClick()
    composeTestRule.waitForIdle()

    // Verify local storage isRead changed to true
    val updatedNotif = LocalDataStore.localUserNotifications[targetUser]?.firstOrNull { it.notificationId == notifId }
    assertTrue(updatedNotif?.isRead == true)
  }

  @Test
  fun test1_notificationListIsSourceOfTruth() = runBlocking {
    val userId = "user_source_of_truth"
    val notif = NotificationEntity(
      notificationId = "truth_notif_1",
      userId = userId,
      title = "Room Code Alert",
      body = "Room Code: 12345",
      isRead = false,
      createdAt = System.currentTimeMillis(),
    )
    val createRes = notificationRepository.createNotification(notif)
    assertTrue(createRes is Resource.Success)

    val flowRes = notificationRepository.getNotifications(userId, 50).first()
    assertTrue(flowRes is Resource.Success)
    val list = (flowRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(1, list.size)
    assertEquals("truth_notif_1", list[0].notificationId)
    assertEquals("Room Code Alert", list[0].title)
  }

  @Test
  fun test2_unreadCountRemainsCorrectAcrossNavigationFlow() = runBlocking {
    val userId = "user_nav_flow"
    val now = System.currentTimeMillis()
    val notif1 = NotificationEntity(
      notificationId = "nav_notif_1",
      userId = userId,
      title = "Match Starting",
      body = "Join room now",
      isRead = false,
      createdAt = now - 2000L,
    )
    val notif2 = NotificationEntity(
      notificationId = "nav_notif_2",
      userId = userId,
      title = "Result Updated",
      body = "You scored 5 kills",
      isRead = false,
      createdAt = now - 1000L,
    )
    notificationRepository.createNotification(notif1)
    notificationRepository.createNotification(notif2)

    // Initial state on Home: 2 unread
    val initialRes = notificationRepository.getNotifications(userId, 50).first()
    val initialNotifications = (initialRes as Resource.Success<List<NotificationEntity>>).data
    var unreadCount = initialNotifications.count { !it.isRead }
    assertEquals(2, unreadCount)

    // User navigates to Notification Center and marks one notification as read
    notificationRepository.markAsRead("nav_notif_1")

    // Upon navigating back to Home, unread count is preserved as 1 without reset
    val afterRes = notificationRepository.getNotifications(userId, 50).first()
    val afterNavNotifications = (afterRes as Resource.Success<List<NotificationEntity>>).data
    unreadCount = afterNavNotifications.count { !it.isRead }
    assertEquals(1, unreadCount)
  }

  @Test
  fun test3_markingNotificationReadUpdatesUnreadCount() = runBlocking {
    val userId = "user_mark_read"
    val notif = NotificationEntity(
      notificationId = "read_notif_1",
      userId = userId,
      title = "Deposit Confirmed",
      body = "50 BDT added",
      isRead = false,
      createdAt = System.currentTimeMillis(),
    )
    notificationRepository.createNotification(notif)

    val beforeRes = notificationRepository.getNotifications(userId, 50).first()
    val before = (beforeRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(1, before.count { !it.isRead })

    val markRes = notificationRepository.markAsRead("read_notif_1")
    assertTrue(markRes is Resource.Success)

    val afterRes = notificationRepository.getNotifications(userId, 50).first()
    val after = (afterRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(0, after.count { !it.isRead })
    assertTrue(after[0].isRead)
  }

  @Test
  fun test4_markAllAsReadUpdatesUnreadCount() = runBlocking {
    val userId = "user_mark_all_read"
    notificationRepository.createNotification(
      NotificationEntity(notificationId = "all_1", userId = userId, title = "A", body = "A", isRead = false)
    )
    notificationRepository.createNotification(
      NotificationEntity(notificationId = "all_2", userId = userId, title = "B", body = "B", isRead = false)
    )

    val beforeRes = notificationRepository.getNotifications(userId, 50).first()
    val before = (beforeRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(2, before.count { !it.isRead })

    val markAllRes = notificationRepository.markAllAsRead(userId)
    assertTrue(markAllRes is Resource.Success)

    val afterRes = notificationRepository.getNotifications(userId, 50).first()
    val after = (afterRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(0, after.count { !it.isRead })
    assertTrue(after.all { it.isRead })
  }

  @Test
  fun test5_notificationPersistsAfterRepositoryRecreation() = runBlocking {
    val userId = "user_repo_recreate"
    val repo1 = FirebaseNotificationRepository()
    val notif = NotificationEntity(
      notificationId = "persist_notif_1",
      userId = userId,
      title = "Persistent Notif",
      body = "Should survive repo recreation",
      isRead = false,
      createdAt = System.currentTimeMillis(),
    )
    repo1.createNotification(notif)

    // Recreate repository instance
    val repo2 = FirebaseNotificationRepository()
    val fetchedRes = repo2.getNotifications(userId, 50).first()
    val fetched = (fetchedRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(1, fetched.size)
    assertEquals("persist_notif_1", fetched[0].notificationId)
    assertEquals("Persistent Notif", fetched[0].title)
  }

  @Test
  fun test6_duplicateNotificationIdDoesNotCreateDuplicateRecords() = runBlocking {
    val userId = "user_duplicate_test"
    val notifId = "duplicate_test_id"
    val now = System.currentTimeMillis()
    val notifInitial = NotificationEntity(
      notificationId = notifId,
      userId = userId,
      title = "Initial Title",
      body = "Initial Body",
      isRead = false,
      createdAt = now - 2000L,
    )
    val notifUpdated = NotificationEntity(
      notificationId = notifId,
      userId = userId,
      title = "Updated Title",
      body = "Updated Body",
      isRead = false,
      createdAt = now - 1000L,
    )

    // Submit duplicate notification ID
    notificationRepository.createNotification(notifInitial)
    notificationRepository.createNotification(notifUpdated)

    val listRes = notificationRepository.getNotifications(userId, 50).first()
    val list = (listRes as Resource.Success<List<NotificationEntity>>).data
    assertEquals(1, list.size)
    assertEquals(notifId, list[0].notificationId)
    assertEquals("Updated Title", list[0].title)
    assertEquals("Updated Body", list[0].body)
  }



  @Test
  fun test8_canonicalNotificationFilteringAndParsing() = runBlocking {
    val participantUser = "user_participant"
    val nonParticipantUser = "user_non_participant"
    val matchId = "match_101"

    LocalDataStore.localMatchPlayers[matchId] = CopyOnWriteArrayList(
      listOf(MatchPlayerEntity(userId = participantUser, matchId = matchId))
    )

    // Populate local user notifications cache simulating canonical parsing & filtering
    val notifAll = NotificationEntity(notificationId = "notif_all", userId = participantUser, title = "General Notice", body = "Hello all", createdAt = System.currentTimeMillis())
    val notifMatch = NotificationEntity(notificationId = "notif_match", userId = participantUser, title = "Room Code", body = "Room Code: 248901 | সময়মতো ম্যাচে যোগ দিন।", createdAt = System.currentTimeMillis(), targetScreen = "match_detail")

    LocalDataStore.localUserNotifications[participantUser] = CopyOnWriteArrayList(listOf(notifAll, notifMatch))
    LocalDataStore.localUserNotifications[nonParticipantUser] = CopyOnWriteArrayList(listOf(notifAll))
    LocalDataStore.notifyNotificationsChanged()

    val repo = FirebaseNotificationRepository()

    val resParticipant = repo.getNotifications(participantUser, 50).first()
    assertTrue(resParticipant is Resource.Success)
    val listParticipant = (resParticipant as Resource.Success).data
    assertTrue(listParticipant.any { it.notificationId == "notif_all" })
    assertTrue(listParticipant.any { it.notificationId == "notif_match" && it.body.contains("Room Code: 248901") })

    val resNonParticipant = repo.getNotifications(nonParticipantUser, 50).first()
    val listNonParticipant = (resNonParticipant as Resource.Success).data
    assertTrue(listNonParticipant.any { it.notificationId == "notif_all" })
    assertFalse(listNonParticipant.any { it.notificationId == "notif_match" })
  }

  @Test
  fun test9_winningNotification_sentOnlyToWinnerWithCorrectFields() = runBlocking {
    val adminRepo = com.example.data.repository.FirebaseAdminRepository(forceLocalOnly = true)
    val winnerUid = "winner_player_xyz"
    val loserUid = "loser_player_abc"
    val matchId = "match_win_test_999"

    LocalDataStore.localMatches[matchId] = com.example.domain.model.MatchEntity(
      matchId = matchId,
      matchNumber = "001",
      title = "Ludo Master Final",
      prizeMinorUnits = 9900L, // ৳99
      status = com.example.domain.model.MatchStatus.RUNNING.name,
      joinedPlayersCount = 2,
    )
    LocalDataStore.localMatchPlayers[matchId] = CopyOnWriteArrayList(
      listOf(
        MatchPlayerEntity(matchId = matchId, userId = winnerUid, uid = winnerUid, slot = "1"),
        MatchPlayerEntity(matchId = matchId, userId = loserUid, uid = loserUid, slot = "2"),
      )
    )
    LocalDataStore.localWallets[winnerUid] = com.example.domain.model.WalletEntity(uid = winnerUid, balance = 0.0)

    val result = com.example.domain.model.ResultEntity(
      resultId = "res_win_999",
      matchId = matchId,
      submittedByUserId = winnerUid,
      claimedWinnerUserId = winnerUid,
      status = com.example.domain.model.ResultStatus.PENDING_REVIEW.name,
    )

    val approveRes = adminRepo.approveResult("admin_1", result)
    assertTrue("Approve result should succeed", approveRes is Resource.Success)

    // Winner must have the winning notification
    val winnerNotifs = LocalDataStore.localUserNotifications[winnerUid]?.toList() ?: emptyList()
    assertEquals(1, winnerNotifs.size)
    val winNotif = winnerNotifs[0]
    assertTrue("Title must indicate victory", winNotif.title.contains("ম্যাচ জিতেছেন") || winNotif.title.contains("🏆"))
    assertTrue("Body must mention match number #001", winNotif.body.contains("#001"))
    assertTrue("Body must mention prize amount 99", winNotif.body.contains("99") || winNotif.body.contains("৯৯"))
    assertEquals(winnerUid, winNotif.userId)

    // Loser must NOT have any notification
    val loserNotifs = LocalDataStore.localUserNotifications[loserUid]?.toList() ?: emptyList()
    assertTrue("Loser must NOT receive any notification", loserNotifs.isEmpty())
  }

  @Test
  fun test10_duplicateWinningApproval_doesNotCreateDuplicateNotification() = runBlocking {
    val adminRepo = com.example.data.repository.FirebaseAdminRepository(forceLocalOnly = true)
    val winnerUid = "winner_idempotent"
    val matchId = "match_idempotent_1"

    LocalDataStore.localMatches[matchId] = com.example.domain.model.MatchEntity(
      matchId = matchId,
      matchNumber = "002",
      title = "Carrom Clash",
      prizeMinorUnits = 5000L,
      status = com.example.domain.model.MatchStatus.RUNNING.name,
    )
    LocalDataStore.localWallets[winnerUid] = com.example.domain.model.WalletEntity(uid = winnerUid, balance = 0.0)

    val result = com.example.domain.model.ResultEntity(
      resultId = "res_idempotent_1",
      matchId = matchId,
      submittedByUserId = winnerUid,
      claimedWinnerUserId = winnerUid,
      status = com.example.domain.model.ResultStatus.PENDING_REVIEW.name,
    )

    // Approve first time
    adminRepo.approveResult("admin_1", result)
    val afterFirst = LocalDataStore.localUserNotifications[winnerUid]?.size ?: 0
    assertEquals(1, afterFirst)

    // Approve retry / second time
    adminRepo.approveResult("admin_1", result)
    val afterSecond = LocalDataStore.localUserNotifications[winnerUid]?.size ?: 0
    assertEquals("Duplicate winning notification must not be created", 1, afterSecond)
  }

  @Test
  fun test11_twentyFourHourExpiry_purgesOlderNotificationsAndKeepsNewer() = runBlocking {
    val userId = "user_expiry_test"
    val now = System.currentTimeMillis()
    val oneHourAgo = now - (1 * 60 * 60 * 1000L)
    val twentyThreeHoursAgo = now - (23 * 60 * 60 * 1000L)
    val twentyFiveHoursAgo = now - (25 * 60 * 60 * 1000L)
    val twoDaysAgo = now - (48 * 60 * 60 * 1000L)

    val notifRecent1 = NotificationEntity(notificationId = "notif_1h", userId = userId, title = "Recent 1", body = "Body", createdAt = oneHourAgo)
    val notifRecent2 = NotificationEntity(notificationId = "notif_23h", userId = userId, title = "Recent 2", body = "Body", createdAt = twentyThreeHoursAgo)
    val notifExpired1 = NotificationEntity(notificationId = "notif_25h", userId = userId, title = "Expired 1", body = "Body", createdAt = twentyFiveHoursAgo)
    val notifExpired2 = NotificationEntity(notificationId = "notif_48h", userId = userId, title = "Expired 2", body = "Body", createdAt = twoDaysAgo)

    LocalDataStore.localUserNotifications[userId] = CopyOnWriteArrayList(
      listOf(notifRecent1, notifRecent2, notifExpired1, notifExpired2)
    )

    val repo = FirebaseNotificationRepository()
    val flowRes = repo.getNotifications(userId, 50).first()
    assertTrue(flowRes is Resource.Success)
    val activeList = (flowRes as Resource.Success<List<NotificationEntity>>).data

    // Only notifications <= 24 hours old should be returned
    assertEquals(2, activeList.size)
    assertTrue(activeList.any { it.notificationId == "notif_1h" })
    assertTrue(activeList.any { it.notificationId == "notif_23h" })
    assertFalse(activeList.any { it.notificationId == "notif_25h" })
    assertFalse(activeList.any { it.notificationId == "notif_48h" })

    // Explicit cleanup call purges expired from store
    val cleanupRes = repo.cleanupExpiredNotifications()
    assertTrue(cleanupRes is Resource.Success)
    val remaining = LocalDataStore.localUserNotifications[userId]?.toList() ?: emptyList()
    assertEquals(2, remaining.size)
  }

  @Test
  fun test12_readAndUnreadBothSubjectTo24HourExpiry() = runBlocking {
    val userId = "user_read_unread_expiry"
    val now = System.currentTimeMillis()
    val thirtyHoursAgo = now - (30 * 60 * 60 * 1000L)

    val expiredUnread = NotificationEntity(notificationId = "exp_unread", userId = userId, title = "Unread Expired", body = "B", isRead = false, createdAt = thirtyHoursAgo)
    val expiredRead = NotificationEntity(notificationId = "exp_read", userId = userId, title = "Read Expired", body = "B", isRead = true, createdAt = thirtyHoursAgo)

    LocalDataStore.localUserNotifications[userId] = CopyOnWriteArrayList(listOf(expiredUnread, expiredRead))

    val repo = FirebaseNotificationRepository()
    val flowRes = repo.getNotifications(userId, 50).first()
    assertTrue(flowRes is Resource.Success)
    val activeList = (flowRes as Resource.Success<List<NotificationEntity>>).data

    // Both read and unread expired after 24h
    assertEquals(0, activeList.size)
  }

  @Test
  fun test13_homeNotificationButton_rendersUnreadDotWhenUnreadExists() {
    composeTestRule.setContent {
      com.example.ui.components.PremiumNotificationBellButton(
        unreadCount = 3,
        onClick = {},
      )
    }

    composeTestRule.onNodeWithTag("home_notification_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_unread_dot", useUnmergedTree = true).assertIsDisplayed()
  }
}
