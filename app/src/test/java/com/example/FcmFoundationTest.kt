package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.notification.AppFirebaseMessagingService
import com.example.core.notification.FcmTokenManager
import com.example.core.notification.NotificationPermissionHelper
import com.example.data.repository.LocalDataStore
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FcmFoundationTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    FcmTokenManager.clearForTesting()
  }

  @Test
  fun test1_fcmTokenManager_updatesTokenSafely() {
    assertNull(FcmTokenManager.getCachedToken())

    val testToken = "test_fcm_token_foundation_xyz"
    FcmTokenManager.updateToken(testToken)

    assertEquals(testToken, FcmTokenManager.getCachedToken())
    assertEquals(testToken, FcmTokenManager.currentToken.value)

    // Blank token should be ignored
    FcmTokenManager.updateToken("   ")
    assertEquals(testToken, FcmTokenManager.getCachedToken())
  }

  @Test
  fun test2_fcmTokenManager_safeRetrievalWhenFirebaseUnavailable() {
    var callbackInvoked = false
    var retrievedToken: String? = "initial"

    // Simulate Firebase unavailable
    FcmTokenManager.isAvailableOverride = { false }

    FcmTokenManager.retrieveToken { token ->
      callbackInvoked = true
      retrievedToken = token
    }

    assertTrue(callbackInvoked)
    assertNull(retrievedToken)
  }

  @Test
  fun test3_firebaseMessagingService_handlesTokenRefresh() {
    val service = AppFirebaseMessagingService()
    val refreshedToken = "refreshed_sample_token_456"

    service.onNewToken(refreshedToken)

    assertEquals(refreshedToken, FcmTokenManager.getCachedToken())
  }

  @Test
  fun test4_firebaseMessagingService_onMessageReceivedSafelyWithoutSideEffects() {
    val service = AppFirebaseMessagingService()

    val initialNotifsCount = LocalDataStore.localUserNotifications.values.sumOf { it.size }
    val initialMatchesCount = LocalDataStore.localMatches.size
    val initialWalletsCount = LocalDataStore.localWallets.size

    val message = RemoteMessage.Builder("test_sender")
      .setMessageId("msg_001")
      .addData("type", "generic")
      .addData("body", "Test message")
      .build()

    // Must execute cleanly without exceptions
    service.onMessageReceived(message)

    // Verify no side-effects on notifications, matches, or financial data
    val afterNotifsCount = LocalDataStore.localUserNotifications.values.sumOf { it.size }
    val afterMatchesCount = LocalDataStore.localMatches.size
    val afterWalletsCount = LocalDataStore.localWallets.size

    assertEquals(initialNotifsCount, afterNotifsCount)
    assertEquals(initialMatchesCount, afterMatchesCount)
    assertEquals(initialWalletsCount, afterWalletsCount)
  }

  @Test
  fun test5_notificationPermissionHelper_runsSafely() {
    // Should run safely without throwing exception on Android context
    val result = NotificationPermissionHelper.hasNotificationPermission(context)
    assertNotNull(result)
  }

  @Test
  fun test6_fcmTokenManager_deterministicTokenId() {
    val token = "fcm_sample_token_abc123"
    val id1 = FcmTokenManager.deriveTokenId(token)
    val id2 = FcmTokenManager.deriveTokenId(token)
    assertEquals(id1, id2)
    assertFalse(id1.isBlank())
  }
}
