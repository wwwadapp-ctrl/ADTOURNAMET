package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.core.notification.RoomCodeNotificationManager
import com.example.data.repository.FirebaseNotificationRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.MatchEntity
import com.example.ui.notifications.NotificationsScreen
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RoomCodeNotificationTest {

  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val notificationRepository = FirebaseNotificationRepository()

  @Before
  fun setup() {
    LocalDataStore.localUserNotifications.clear()
    RoomCodeNotificationManager.clearCache()
  }

  @Test
  fun joinedParticipant_receivesRoomCodeNotification_andAppearsInNotificationCenter() {
    runBlocking {
      val userId = "participant_user_1"
      val match = MatchEntity(
        matchId = "match_cuj_1",
        matchNumber = "12",
        title = "Free Fire Daily Clash",
        gameCode = "ROOM-9921",
      )

      // Process Room Code event for verified joined participant
      val handled = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = match,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )

      assertTrue("Participant must receive notification", handled)

      // Verify stored notification entity
      val userNotifications = LocalDataStore.localUserNotifications[userId]
      assertEquals(1, userNotifications?.size)
      val notif = userNotifications?.first()
      assertEquals("ম্যাচ #12 এর রুম কোড দেওয়া হয়েছে", notif?.title)
      assertTrue(notif?.body?.contains("ROOM-9921") == true)
      assertFalse(notif?.isRead ?: true)

      // Verify it renders properly in Notification Center
      composeTestRule.setContent {
        NotificationsScreen(
          userId = userId,
          onNavigateBack = {},
          notificationRepository = notificationRepository,
        )
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithText("ম্যাচ #12 এর রুম কোড দেওয়া হয়েছে").assertIsDisplayed()
      composeTestRule.onNodeWithText("NEW").assertIsDisplayed()
    }
  }

  @Test
  fun nonParticipant_doesNotReceiveRoomCodeNotification() {
    runBlocking {
      val userId = "non_participant_user"
      val match = MatchEntity(
        matchId = "match_cuj_2",
        matchNumber = "15",
        title = "PUBG Erangel Battle",
        gameCode = "PUBG-5544",
      )

      // Process Room Code for user who has NOT joined the match
      val handled = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = match,
        isUserParticipant = false,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )

      assertFalse("Non-participant must not receive notification", handled)
      val userNotifications = LocalDataStore.localUserNotifications[userId]
      assertTrue(userNotifications.isNullOrEmpty())

      // Notification Center should show empty state
      composeTestRule.setContent {
        NotificationsScreen(
          userId = userId,
          onNavigateBack = {},
          notificationRepository = notificationRepository,
        )
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
      composeTestRule.onNodeWithText("কোনো নতুন নোটিফিকেশন নেই").assertIsDisplayed()
    }
  }

  @Test
  fun sameRoomCodeEvent_isNotDuplicatedOnSubsequentReads() {
    runBlocking {
      val userId = "participant_user_dup"
      val match = MatchEntity(
        matchId = "match_cuj_3",
        matchNumber = "20",
        gameCode = "CODE-1234",
      )

      // First detection: notification created
      val firstAttempt = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = match,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )
      assertTrue("First detection should succeed", firstAttempt)
      assertEquals(1, LocalDataStore.localUserNotifications[userId]?.size)

      // Second detection with identical Room Code: must NOT duplicate
      val secondAttempt = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = match,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )
      assertFalse("Duplicate detection must return false", secondAttempt)
      assertEquals("Should still have only 1 notification", 1, LocalDataStore.localUserNotifications[userId]?.size)

      // Third batch process with list containing same match: must NOT duplicate
      val processedCount = RoomCodeNotificationManager.processMatches(
        userId = userId,
        joinedMatchIds = setOf("match_cuj_3"),
        matches = listOf(match),
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )
      assertEquals("0 new notifications should be processed", 0, processedCount)
      assertEquals("Should strictly remain 1 notification", 1, LocalDataStore.localUserNotifications[userId]?.size)
    }
  }

  @Test
  fun newRoomCodeEvent_canCreateNewNotification() {
    runBlocking {
      val userId = "participant_user_updates"
      val matchV1 = MatchEntity(
        matchId = "match_cuj_4",
        matchNumber = "44",
        gameCode = "INITIAL-CODE-1",
      )

      // 1. Initial code
      val v1Success = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = matchV1,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )
      assertTrue(v1Success)
      assertEquals(1, LocalDataStore.localUserNotifications[userId]?.size)

      // 2. Admin releases updated/new Room Code
      val matchV2 = matchV1.copy(gameCode = "UPDATED-CODE-2")
      val v2Success = RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = matchV2,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )
      assertTrue("New Room Code event must trigger a new notification", v2Success)
      assertEquals("Should now have 2 notifications", 2, LocalDataStore.localUserNotifications[userId]?.size)

      val latest = LocalDataStore.localUserNotifications[userId]?.first()
      assertTrue(latest?.body?.contains("UPDATED-CODE-2") == true)
    }
  }

  @Test
  fun notificationState_remainsCompatibleWithNotificationCenter_readFlow() {
    runBlocking {
      val userId = "participant_user_read_flow"
      val match = MatchEntity(
        matchId = "match_cuj_5",
        matchNumber = "55",
        gameCode = "CODE-READ-TEST",
      )

      RoomCodeNotificationManager.processRoomCodeForMatch(
        userId = userId,
        match = match,
        isUserParticipant = true,
        notificationRepository = notificationRepository,
        triggerAlert = false,
      )

      val notifId = RoomCodeNotificationManager.getStableNotificationId("match_cuj_5", "CODE-READ-TEST")

      composeTestRule.setContent {
        NotificationsScreen(
          userId = userId,
          onNavigateBack = {},
          notificationRepository = notificationRepository,
        )
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("notification_item_$notifId").assertIsDisplayed()
      composeTestRule.onNodeWithText("NEW").assertIsDisplayed()

      // Clicking marks as read
      composeTestRule.onNodeWithTag("notification_item_$notifId").performClick()
      composeTestRule.waitForIdle()

      // Confirm isRead updated
      val updated = LocalDataStore.localUserNotifications[userId]?.firstOrNull { it.notificationId == notifId }
      assertTrue("Notification should be marked as read", updated?.isRead == true)
    }
  }
}
