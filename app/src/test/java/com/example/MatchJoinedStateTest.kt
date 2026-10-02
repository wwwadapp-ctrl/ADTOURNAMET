package com.example

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.core.error.Resource
import com.example.data.repository.FirebaseMatchRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.MatchCard
import com.example.ui.match.MatchDetailScreen
import com.example.ui.match.MatchDetailViewModel
import com.example.ui.matches.MatchesTab
import com.example.ui.matches.MatchesViewModel
import com.example.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
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
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MatchJoinedStateTest {

  @Rule
  @JvmField
  val composeTestRule = createComposeRule()

  class TestMatchRepository(
    private val delegate: FirebaseMatchRepository = FirebaseMatchRepository(),
  ) : MatchRepository by delegate {
    override fun getMatchById(matchId: String): Flow<Resource<MatchEntity?>> = flow {
      emit(Resource.Success(LocalDataStore.localMatches[matchId]))
    }
    override fun getMatchPlayers(matchId: String): Flow<Resource<List<MatchPlayerEntity>>> = flow {
      emit(Resource.Success(LocalDataStore.localMatchPlayers[matchId] ?: emptyList()))
    }
  }

  private val repository = TestMatchRepository()
  private val currentUser = "user_alpha"
  private val otherUser = "user_beta"

  private val matchId1 = "match_101"
  private val matchId2 = "match_202"
  private val runningMatchId = "match_running_303"
  private val fullMatchId = "match_full_404"

  @Before
  fun setUp() {
    LocalDataStore.localUsers.clear()
    LocalDataStore.localWallets.clear()
    LocalDataStore.localMatches.clear()
    LocalDataStore.localMatchPlayers.clear()
    LocalDataStore.localTransactions.clear()

    LocalDataStore.localUsers[currentUser] = UserEntity(
      uid = currentUser,
      userId = currentUser,
      name = "Player Alpha",
    )
    LocalDataStore.localUsers[otherUser] = UserEntity(
      uid = otherUser,
      userId = otherUser,
      name = "Player Beta",
    )

    LocalDataStore.localWallets[currentUser] = WalletEntity(
      uid = currentUser,
      userId = currentUser,
      balance = 200.0,
      availableBalance = 20000L,
    )
    LocalDataStore.localWallets[otherUser] = WalletEntity(
      uid = otherUser,
      userId = otherUser,
      balance = 200.0,
      availableBalance = 20000L,
    )

    // Match 1: Available
    LocalDataStore.localMatches[matchId1] = MatchEntity(
      matchId = matchId1,
      matchNumber = "#M101",
      title = "Tournament 101",
      status = MatchStatus.AVAILABLE.name,
      entryFee = 25.0,
      entryFeeMinorUnits = 2500L,
      joinedPlayersCount = 0,
      maxPlayers = 2,
    )
    LocalDataStore.localMatchPlayers[matchId1] = CopyOnWriteArrayList()

    // Match 2: Available
    LocalDataStore.localMatches[matchId2] = MatchEntity(
      matchId = matchId2,
      matchNumber = "#M202",
      title = "Tournament 202",
      status = MatchStatus.AVAILABLE.name,
      entryFee = 25.0,
      entryFeeMinorUnits = 2500L,
      joinedPlayersCount = 0,
      maxPlayers = 2,
    )
    LocalDataStore.localMatchPlayers[matchId2] = CopyOnWriteArrayList()

    // Running match with visible game room code
    LocalDataStore.localMatches[runningMatchId] = MatchEntity(
      matchId = runningMatchId,
      matchNumber = "#M303",
      title = "Running Battle 303",
      status = MatchStatus.RUNNING.name,
      gameCode = "ROOM9999",
      isCodeVisible = true,
      entryFee = 25.0,
      entryFeeMinorUnits = 2500L,
      joinedPlayersCount = 2,
      maxPlayers = 2,
    )
    val runningPlayers = CopyOnWriteArrayList<MatchPlayerEntity>()
    runningPlayers.add(MatchPlayerEntity(matchId = runningMatchId, userId = "p1", uid = "p1", slot = "1"))
    runningPlayers.add(MatchPlayerEntity(matchId = runningMatchId, userId = "p2", uid = "p2", slot = "2"))
    LocalDataStore.localMatchPlayers[runningMatchId] = runningPlayers

    // Full match
    LocalDataStore.localMatches[fullMatchId] = MatchEntity(
      matchId = fullMatchId,
      matchNumber = "#M404",
      title = "Full Battle 404",
      status = MatchStatus.FULL.name,
      entryFee = 25.0,
      entryFeeMinorUnits = 2500L,
      joinedPlayersCount = 2,
      maxPlayers = 2,
    )
    val fullPlayers = CopyOnWriteArrayList<MatchPlayerEntity>()
    fullPlayers.add(MatchPlayerEntity(matchId = fullMatchId, userId = "pA", uid = "pA", slot = "1"))
    fullPlayers.add(MatchPlayerEntity(matchId = fullMatchId, userId = "pB", uid = "pB", slot = "2"))
    LocalDataStore.localMatchPlayers[fullMatchId] = fullPlayers
  }

  // Case 1: User has not joined -> JOIN NOW appears on MatchCard
  @Test
  fun testCase1_userHasNotJoined_showsJoinNow() {
    val match = LocalDataStore.localMatches[matchId1]!!
    composeTestRule.setContent {
      MatchCard(
        match = match,
        isJoined = false,
      )
    }

    composeTestRule.onNodeWithTag("join_button_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("JOIN").assertIsDisplayed()
  }

  // Case 2: User successfully joins -> JOINED appears on MatchCard and is disabled
  @Test
  fun testCase2_userSuccessfullyJoins_showsJoinedDisabled() {
    runBlocking {
      val joinRes = repository.joinMatch(currentUser, matchId1)
      assertTrue("Join should succeed", joinRes is Resource.Success)
    }

    val match = LocalDataStore.localMatches[matchId1]!!
    composeTestRule.setContent {
      MatchCard(
        match = match,
        isJoined = true,
      )
    }

    composeTestRule.onNodeWithTag("joined_button_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("joined_button_$matchId1").assertIsNotEnabled()
  }

  // Case 3: Same user revisits the match -> JOINED still appears
  @Test
  fun testCase3_sameUserRevisits_showsJoined() {
    runBlocking {
      repository.joinMatch(currentUser, matchId1)
    }

    val detailVm = MatchDetailViewModel(
      matchRepository = repository,
      resultRepository = com.example.data.repository.FirebaseResultRepository(),
      matchId = matchId1,
      currentUserId = currentUser,
    )

    composeTestRule.setContent {
      MatchDetailScreen(
        viewModel = detailVm,
        onNavigateBack = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("detail_joined_button").assertExists()
    composeTestRule.onNodeWithTag("detail_joined_button").assertIsNotEnabled()
  }

  // Case 4: Another user joins a different match -> current user still sees JOIN NOW for that other match
  @Test
  fun testCase4_otherUserJoinsMatch2_currentUserSeesJoinNow() {
    runBlocking {
      // Other user joins match 2
      val res = repository.joinMatch(otherUser, matchId2)
      assertTrue(res is Resource.Success)
    }

    // Current user checks match 2
    val match2 = LocalDataStore.localMatches[matchId2]!!
    // Current user has NOT joined match 2
    val isCurrentUserJoinedMatch2 = LocalDataStore.localMatchPlayers[matchId2]!!
      .any { it.effectiveUid == currentUser }

    assertFalse("Current user should not be joined in match 2", isCurrentUserJoinedMatch2)

    composeTestRule.setContent {
      MatchCard(
        match = match2,
        isJoined = isCurrentUserJoinedMatch2,
      )
    }

    composeTestRule.onNodeWithTag("join_button_$matchId2").assertIsDisplayed()
    composeTestRule.onNodeWithText("JOIN").assertIsDisplayed()
  }

  // Case 5: A running match with visible room code -> must NOT automatically become JOINED for a user who never joined
  @Test
  fun testCase5_runningMatchWithVisibleCode_notJoinedShowsInProgress() {
    val runningMatch = LocalDataStore.localMatches[runningMatchId]!!
    // current user never joined
    val isCurrentUserJoined = LocalDataStore.localMatchPlayers[runningMatchId]!!
      .any { it.effectiveUid == currentUser }

    assertFalse(isCurrentUserJoined)

    composeTestRule.setContent {
      MatchCard(
        match = runningMatch,
        isJoined = isCurrentUserJoined,
      )
    }

    // Should show IN PROGRESS, NOT JOINED!
    composeTestRule.onNodeWithTag("in_progress_$runningMatchId").assertIsDisplayed()
    composeTestRule.onNodeWithText("IN PROGRESS").assertIsDisplayed()
  }

  // Case 6: A FULL match -> existing FULL behavior remains for users who did not join (SEAT FULL, disabled)
  @Test
  fun testCase6_fullMatch_showsMatchFullDisabled() {
    val fullMatch = LocalDataStore.localMatches[fullMatchId]!!
    val isCurrentUserJoined = LocalDataStore.localMatchPlayers[fullMatchId]!!
      .any { it.effectiveUid == currentUser }

    assertFalse(isCurrentUserJoined)

    composeTestRule.setContent {
      MatchCard(
        match = fullMatch,
        isJoined = isCurrentUserJoined,
      )
    }

    composeTestRule.onNodeWithTag("match_full_$fullMatchId").assertIsDisplayed()
    composeTestRule.onNodeWithTag("match_full_$fullMatchId").assertIsNotEnabled()
    composeTestRule.onNodeWithText("SEAT FULL").assertIsDisplayed()
  }

  // Case 8: Seat status bar is rendered at the bottom of the card for matches
  @Test
  fun testCase8_seatStatusBar_renderedOnCard() {
    val match0of2 = LocalDataStore.localMatches[matchId1]!!.copy(joinedPlayersCount = 0)
    composeTestRule.setContent {
      MatchCard(
        match = match0of2,
        isJoined = false,
      )
    }

    composeTestRule.onNodeWithTag("seat_status_bar_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("0/2 JOINED").assertIsDisplayed()
    composeTestRule.onNodeWithTag("join_button_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("JOIN").assertIsDisplayed()
  }

  @Test
  fun testCase9_seatStatusBar_1of2And2of2_states() {
    val match1of2 = LocalDataStore.localMatches[matchId1]!!.copy(joinedPlayersCount = 1)
    composeTestRule.setContent {
      MatchCard(
        match = match1of2,
        isJoined = false,
      )
    }

    composeTestRule.onNodeWithTag("seat_status_bar_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("1/2 JOINED").assertIsDisplayed()
    composeTestRule.onNodeWithTag("join_button_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("JOIN").assertIsDisplayed()
  }

  @Test
  fun testCase10_seatStatusBar_fullState() {
    val match2of2 = LocalDataStore.localMatches[matchId1]!!.copy(joinedPlayersCount = 2)
    composeTestRule.setContent {
      MatchCard(
        match = match2of2,
        isJoined = false,
      )
    }

    composeTestRule.onNodeWithTag("seat_status_bar_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2/2 JOINED").assertIsDisplayed()
    composeTestRule.onNodeWithTag("match_full_$matchId1").assertIsDisplayed()
    composeTestRule.onNodeWithText("SEAT FULL").assertIsDisplayed()
  }

  // Case 7: Existing registered user cannot accidentally trigger join again
  @Test
  fun testCase7_existingRegisteredUser_cannotTriggerJoinAgain() {
    runBlocking {
      val firstJoin = repository.joinMatch(currentUser, matchId1)
      assertTrue(firstJoin is Resource.Success)

      // Second join attempt at repository level is rejected
      val secondJoin = repository.joinMatch(currentUser, matchId1)
      assertTrue("Second join must fail", secondJoin is Resource.Error)
    }

    // In MatchDetailScreen, button is JOINED and not clickable
    val detailVm = MatchDetailViewModel(
      matchRepository = repository,
      resultRepository = com.example.data.repository.FirebaseResultRepository(),
      matchId = matchId1,
      currentUserId = currentUser,
    )

    composeTestRule.setContent {
      MatchDetailScreen(
        viewModel = detailVm,
        onNavigateBack = {},
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("detail_joined_button").assertExists()
    composeTestRule.onNodeWithTag("detail_joined_button").assertIsNotEnabled()
  }
}
