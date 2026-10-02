package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.core.error.Resource
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.EnglishStrings
import com.example.domain.model.GameType
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserMatchHistoryEntity
import com.example.domain.repository.JoinMatchResult
import com.example.domain.repository.MatchRepository
import com.example.ui.history.HistoryScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
class MatchHistoryTest {

  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  private val currentUserId = "user_alpha"
  private val otherUserId = "user_beta"

  /**
   * Test repository mirroring the permanent /userMatches resolution logic with legacy fallback.
   */
  class TestMatchHistoryRepository : MatchRepository {
    val userMatchesNode = mutableMapOf<String, MutableMap<String, UserMatchHistoryEntity>>()
    val matchPlayersNode = mutableMapOf<String, MutableMap<String, MatchPlayerEntity>>()
    val matchesNode = mutableMapOf<String, MatchEntity>()

    override fun getMatchHistory(userId: String, limit: Int): Flow<Resource<List<MatchEntity>>> = flow {
      emit(Resource.Loading)
      val requestedUserId = userId.trim().ifBlank { null }

      if (requestedUserId.isNullOrBlank()) {
        emit(Resource.Success(emptyList()))
        return@flow
      }

      val userMatches = userMatchesNode[requestedUserId]?.values?.toList()
      if (userMatches != null && userMatches.isNotEmpty()) {
        val mapped = userMatches.mapNotNull { umh ->
          val m = matchesNode[umh.matchId]
          m ?: MatchEntity(
            matchId = umh.matchId,
            matchNumber = umh.matchNumber,
            title = umh.title,
            gameType = umh.gameType,
            status = umh.status,
            winnerUserId = if (umh.isWinner == true) requestedUserId else "",
            entryFeeMinorUnits = umh.entryFeeMinorUnits,
            prizeMinorUnits = umh.prizeMinorUnits,
          )
        }.filter { match ->
          match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true) ||
          match.status.equals("UNDER_REVIEW", ignoreCase = true) ||
          match.status.equals("REJECTED", ignoreCase = true)
        }.sortedByDescending { it.effectiveScheduledAt.coerceAtLeast(it.createdAt) }
        emit(Resource.Success(if (limit > 0) mapped.take(limit) else mapped))
        return@flow
      }

      // Legacy fallback
      val joinedIds = mutableSetOf<String>()
      for ((matchIdKey, playersMap) in matchPlayersNode) {
        for ((playerKey, player) in playersMap) {
          val pUid = player.uid.trim()
          val pUserId = player.userId.trim()
          val pEffectiveUid = player.effectiveUid.trim()

          val matchesUser = requestedUserId == playerKey ||
              (pUid.isNotBlank() && requestedUserId == pUid) ||
              (pUserId.isNotBlank() && requestedUserId == pUserId) ||
              (pEffectiveUid.isNotBlank() && requestedUserId == pEffectiveUid)

          if (matchesUser) {
            val resolvedMatchId = player.matchId.trim().ifBlank { null } ?: matchIdKey
            joinedIds.add(resolvedMatchId)
            break
          }
        }
      }

      if (joinedIds.isEmpty()) {
        emit(Resource.Success(emptyList()))
        return@flow
      }

      val historicalMatches = joinedIds.mapNotNull { matchesNode[it] }
        .filter { match ->
          match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true) ||
          match.status.equals("UNDER_REVIEW", ignoreCase = true) ||
          match.status.equals("REJECTED", ignoreCase = true)
        }
        .sortedByDescending { it.effectiveScheduledAt.coerceAtLeast(it.createdAt) }
        .let { if (limit > 0) it.take(limit) else it }

      emit(Resource.Success(historicalMatches))
    }

    override fun getAvailableMatches(limit: Int): Flow<Resource<List<MatchEntity>>> = flow {
      emit(Resource.Success(emptyList()))
    }
    override fun getMyJoinedMatches(userId: String): Flow<Resource<List<MatchEntity>>> = flow {
      emit(Resource.Success(emptyList()))
    }
    override fun getUpcomingMatches(limit: Int): Flow<Resource<List<MatchEntity>>> = flow {
      emit(Resource.Success(emptyList()))
    }
    override fun getMatchById(matchId: String): Flow<Resource<MatchEntity?>> = flow {
      emit(Resource.Success(matchesNode[matchId]))
    }
    override fun getMatchPlayers(matchId: String): Flow<Resource<List<MatchPlayerEntity>>> = flow {
      emit(Resource.Success(matchPlayersNode[matchId]?.values?.toList() ?: emptyList()))
    }
    override suspend fun joinMatch(userId: String, matchId: String): Resource<JoinMatchResult> {
      throw UnsupportedOperationException()
    }
    override fun isUserJoinedMatch(userId: String, matchId: String): Flow<Boolean> = flow {
      emit(false)
    }
  }

  private val repository = TestMatchHistoryRepository()

  @Before
  fun setUp() {
    repository.matchPlayersNode.clear()
    repository.matchesNode.clear()
    repository.userMatchesNode.clear()
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 1: New user with no participation -> History is empty
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario1_newUserWithNoParticipation_historyIsEmpty() {
    runBlocking {
      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertTrue(list.isEmpty())

      composeTestRule.setContent {
        HistoryScreen(
          matches = list,
          currentUserId = currentUserId,
        )
      }

      composeTestRule.onNodeWithTag("history_empty_state").assertIsDisplayed()
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 2: User joined an active match -> does NOT appear in history
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario2_userJoinedActiveMatch_doesNotAppearInHistory() {
    runBlocking {
      val activeMatchId = "match_running_1"
      repository.matchPlayersNode[activeMatchId] = mutableMapOf(
        currentUserId to MatchPlayerEntity(matchId = activeMatchId, userId = currentUserId)
      )
      repository.matchesNode[activeMatchId] = MatchEntity(
        matchId = activeMatchId,
        title = "Active Battle",
        status = MatchStatus.RUNNING.name,
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertTrue("Active match with status RUNNING must not appear in history", list.isEmpty())
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 3: User participated in a COMPLETED match -> appears in history
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario3_userParticipatedInCompletedMatch_appearsInHistory() {
    runBlocking {
      val completedMatchId = "match_completed_1"
      repository.matchPlayersNode[completedMatchId] = mutableMapOf(
        currentUserId to MatchPlayerEntity(matchId = completedMatchId, userId = currentUserId),
        otherUserId to MatchPlayerEntity(matchId = completedMatchId, userId = otherUserId)
      )
      repository.matchesNode[completedMatchId] = MatchEntity(
        matchId = completedMatchId,
        title = "Championship Final",
        gameType = GameType.LUDO.name,
        status = MatchStatus.COMPLETED.name,
        winnerUserId = currentUserId,
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertEquals(1, list.size)
      assertEquals(completedMatchId, list.first().matchId)
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 4: User participated in a CANCELLED match -> appears in history
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario4_userParticipatedInCancelledMatch_appearsInHistory() {
    runBlocking {
      val cancelledMatchId = "match_cancelled_1"
      repository.matchPlayersNode[cancelledMatchId] = mutableMapOf(
        currentUserId to MatchPlayerEntity(matchId = cancelledMatchId, userId = currentUserId)
      )
      repository.matchesNode[cancelledMatchId] = MatchEntity(
        matchId = cancelledMatchId,
        title = "Cancelled Clash",
        gameType = GameType.CARROM.name,
        status = MatchStatus.CANCELLED.name,
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertEquals(1, list.size)
      assertEquals(cancelledMatchId, list.first().matchId)
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 5: Another user's COMPLETED match -> must NOT appear
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario5_otherUserCompletedMatch_doesNotAppearForCurrentUser() {
    runBlocking {
      val otherMatchId = "match_other_1"
      repository.matchPlayersNode[otherMatchId] = mutableMapOf(
        otherUserId to MatchPlayerEntity(matchId = otherMatchId, userId = otherUserId),
        "user_gamma" to MatchPlayerEntity(matchId = otherMatchId, userId = "user_gamma")
      )
      repository.matchesNode[otherMatchId] = MatchEntity(
        matchId = otherMatchId,
        title = "Other Battle",
        status = MatchStatus.COMPLETED.name,
        winnerUserId = otherUserId,
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertTrue("Another user's match must not appear for current user", list.isEmpty())
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 6: winnerUserId equals current user but current user has NO /matchPlayers participation -> must NOT appear
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario6_winnerEqualsCurrentUserWithoutMatchPlayersRecord_doesNotAppear() {
    runBlocking {
      val ghostWinnerMatchId = "match_ghost_1"
      // Other users are in matchPlayers, current user is NOT in matchPlayers
      repository.matchPlayersNode[ghostWinnerMatchId] = mutableMapOf(
        otherUserId to MatchPlayerEntity(matchId = ghostWinnerMatchId, userId = otherUserId)
      )
      repository.matchesNode[ghostWinnerMatchId] = MatchEntity(
        matchId = ghostWinnerMatchId,
        title = "Ghost Winner Match",
        status = MatchStatus.COMPLETED.name,
        winnerUserId = currentUserId, // Has winnerUserId equal to current user, but no player participation record
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertTrue("Match with winnerUserId set to current user without participation record must NOT appear", list.isEmpty())
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 7: No sample or random matches appear
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario7_noSampleOrRandomMatchesAppear() {
    runBlocking {
      // If user has not joined, history returns purely empty, no fallback to sample matches
      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertEquals(0, list.size)
      assertFalse(list.any { it.matchId.contains("AD-USER-9481") })
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 8: Existing History UI still builds and renders correctly
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario8_existingHistoryUI_rendersCorrectly() {
    val match1 = MatchEntity(
      matchId = "m_ludo_win",
      title = "Grand Ludo Duel",
      gameType = GameType.LUDO.name,
      status = MatchStatus.COMPLETED.name,
      winnerUserId = currentUserId,
      entryFeeMinorUnits = 5000L,
      prizeMinorUnits = 9000L,
    )
    val match2 = MatchEntity(
      matchId = "m_carrom_cancel",
      title = "Carrom Clash",
      gameType = GameType.CARROM.name,
      status = MatchStatus.CANCELLED.name,
      entryFeeMinorUnits = 3000L,
    )

    composeTestRule.setContent {
      HistoryScreen(
        matches = listOf(match1, match2),
        currentUserId = currentUserId,
      )
    }

    // Verify main screen and match items are displayed
    composeTestRule.onNodeWithTag("history_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_summary").assertIsDisplayed()
    composeTestRule.onNodeWithText("Grand Ludo Duel").assertIsDisplayed()
    composeTestRule.onNodeWithText("Carrom Clash").assertIsDisplayed()

    // Test game filter pill: Carrom only
    composeTestRule.onNodeWithTag("history_filter_carrom").performClick()
    composeTestRule.onNodeWithText("Carrom Clash").assertIsDisplayed()

    // Test game filter pill: All
    composeTestRule.onNodeWithTag("history_filter_all").performClick()
    composeTestRule.onNodeWithText("Grand Ludo Duel").assertIsDisplayed()
    composeTestRule.onNodeWithText("Carrom Clash").assertIsDisplayed()
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 9: CANCELLED match with real cancelReason displays reason
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario9_cancelledMatchWithRealCancelReason_displaysReason() {
    val cancelledMatch = MatchEntity(
      matchId = "m_cancelled_with_reason",
      title = "Cancelled Duel",
      gameType = GameType.LUDO.name,
      status = MatchStatus.CANCELLED.name,
      cancelReason = "Server technical maintenance by admin",
      entryFeeMinorUnits = 2000L,
    )

    composeTestRule.setContent {
      HistoryScreen(
        matches = listOf(cancelledMatch),
        currentUserId = currentUserId,
      )
    }

    // Verify cancellation section, title, and real reason are displayed
    composeTestRule.onNodeWithTag("history_cancellation_section_m_cancelled_with_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_title_m_cancelled_with_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_reason_m_cancelled_with_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Server technical maintenance by admin").assertIsDisplayed()
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 10: CANCELLED match with null cancelReason remains valid without reason
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario10_cancelledMatchWithNullCancelReason_remainsValid() {
    val cancelledMatch = MatchEntity(
      matchId = "m_cancelled_null_reason",
      title = "Cancelled Null Reason",
      gameType = GameType.LUDO.name,
      status = MatchStatus.CANCELLED.name,
      cancelReason = null,
      entryFeeMinorUnits = 2000L,
    )

    composeTestRule.setContent {
      HistoryScreen(
        matches = listOf(cancelledMatch),
        currentUserId = currentUserId,
      )
    }

    composeTestRule.onNodeWithTag("history_cancellation_section_m_cancelled_null_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_title_m_cancelled_null_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_reason_m_cancelled_null_reason", useUnmergedTree = true).assertDoesNotExist()
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 11: CANCELLED match with blank cancelReason remains valid without reason
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario11_cancelledMatchWithBlankCancelReason_remainsValid() {
    val cancelledMatch = MatchEntity(
      matchId = "m_cancelled_blank_reason",
      title = "Cancelled Blank Reason",
      gameType = GameType.LUDO.name,
      status = MatchStatus.CANCELLED.name,
      cancelReason = "   ",
      entryFeeMinorUnits = 2000L,
    )

    composeTestRule.setContent {
      HistoryScreen(
        matches = listOf(cancelledMatch),
        currentUserId = currentUserId,
      )
    }

    composeTestRule.onNodeWithTag("history_cancellation_section_m_cancelled_blank_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_title_m_cancelled_blank_reason", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_cancelled_reason_m_cancelled_blank_reason", useUnmergedTree = true).assertDoesNotExist()
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 12: User who did NOT participate in a CANCELLED match does NOT see it
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario12_userNotParticipatedInCancelledMatch_doesNotAppear() {
    runBlocking {
      val otherCancelledMatchId = "match_other_cancelled_99"
      repository.matchPlayersNode[otherCancelledMatchId] = mutableMapOf(
        otherUserId to MatchPlayerEntity(matchId = otherCancelledMatchId, userId = otherUserId)
      )
      repository.matchesNode[otherCancelledMatchId] = MatchEntity(
        matchId = otherCancelledMatchId,
        title = "Other User Cancelled Match",
        status = MatchStatus.CANCELLED.name,
        cancelReason = "Admin test cancel",
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertTrue("Cancelled match joined by other user must not appear for current user", list.isEmpty())
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 13: Bengali/English cancellation labels resolve correctly
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario13_bengaliAndEnglishCancellationLabels_resolveCorrectly() {
    assertEquals("ম্যাচ বাতিল হয়েছে", BengaliStrings.matchCancelled)
    assertEquals("বাতিলের কারণ", BengaliStrings.cancellationReason)
    assertEquals("Match Cancelled", EnglishStrings.matchCancelled)
    assertEquals("Cancellation Reason", EnglishStrings.cancellationReason)
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 14: Bengali/English Under Review and Rejected badges resolve correctly
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario14_bengaliAndEnglishUnderReviewAndRejectedLabels_resolveCorrectly() {
    assertEquals("পর্যালোচনাধীন", BengaliStrings.badgeUnderReview)
    assertEquals("প্রত্যাখ্যাত", BengaliStrings.badgeRejected)
    assertEquals("UNDER REVIEW", EnglishStrings.badgeUnderReview)
    assertEquals("REJECTED", EnglishStrings.badgeRejected)
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 15: Permanent userMatches index serves match history
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario15_permanentUserMatchesIndex_servesHistory() {
    runBlocking {
      val matchId = "m_permanent_test_1"
      val now = System.currentTimeMillis()
      repository.userMatchesNode[currentUserId] = mutableMapOf(
        matchId to UserMatchHistoryEntity(
          matchId = matchId,
          userId = currentUserId,
          status = MatchStatus.COMPLETED.name,
          isWinner = true,
          joinedAt = now,
          updatedAt = now,
          title = "Permanent History Duel",
          gameType = GameType.LUDO.name,
          entryFeeMinorUnits = 5000L,
          prizeMinorUnits = 9000L,
        )
      )

      val result = repository.getMatchHistory(currentUserId).first { it !is Resource.Loading }
      assertTrue(result is Resource.Success)
      val list = (result as Resource.Success).data
      assertEquals(1, list.size)
      assertEquals(matchId, list.first().matchId)
      assertEquals(currentUserId, list.first().winnerUserId)
    }
  }

  // ---------------------------------------------------------------------------
  // SCENARIO 16: Under Review and Rejected matches render appropriate status in UI
  // ---------------------------------------------------------------------------
  @Test
  fun testScenario16_underReviewAndRejectedMatches_renderCorrectly() {
    val reviewMatch = MatchEntity(
      matchId = "m_review_1",
      title = "Proof Under Review",
      gameType = GameType.LUDO.name,
      status = MatchStatus.RESULT_SUBMITTED.name,
      entryFeeMinorUnits = 2500L,
    )
    val rejectedMatch = MatchEntity(
      matchId = "m_rejected_1",
      title = "Proof Rejected Clash",
      gameType = GameType.CARROM.name,
      status = "REJECTED",
      entryFeeMinorUnits = 2000L,
    )

    composeTestRule.setContent {
      HistoryScreen(
        matches = listOf(reviewMatch, rejectedMatch),
        currentUserId = currentUserId,
      )
    }

    composeTestRule.onNodeWithText("Proof Under Review").assertIsDisplayed()
    composeTestRule.onNodeWithText("Proof Rejected Clash").assertIsDisplayed()
  }
}
