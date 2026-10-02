package com.example

import com.example.core.error.Resource
import com.example.data.repository.FirebaseMatchRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.TransactionEntity
import com.example.domain.model.TransactionStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class MatchJoinClientTest {

  private val repository = FirebaseMatchRepository()
  private val testUserId = "player_test_001"
  private val testMatchId = "match_test_101"

  @Before
  fun setUp() {
    LocalDataStore.localUsers[testUserId] = UserEntity(
      uid = testUserId,
      userId = testUserId,
      name = "Test Player",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 100.0,
      availableBalance = 10000L,
    )
    LocalDataStore.localMatches[testMatchId] = MatchEntity(
      matchId = testMatchId,
      matchNumber = "#M101",
      title = "Test Tournament",
      status = MatchStatus.AVAILABLE.name,
      entryFee = 50.0,
      entryFeeMinorUnits = 5000L,
      joinedPlayersCount = 0,
      maxPlayers = 2,
    )
    LocalDataStore.localMatchPlayers[testMatchId] = CopyOnWriteArrayList()
    LocalDataStore.localTransactions.clear()
    LocalDataStore.localActiveJoins.clear()
  }

  // --- Requirement 12 - Test A: Normal successful join ---
  @Test
  fun testJoinMatch_normalSuccessfulJoin() = runBlocking {
    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Expected Success, got $result", result is Resource.Success)

    val updatedMatch = LocalDataStore.localMatches[testMatchId]!!
    assertEquals(1, updatedMatch.joinedPlayersCount)
    assertEquals(MatchStatus.AVAILABLE.name, updatedMatch.status)

    val updatedWallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(5000L, updatedWallet.availableBalance)
    assertEquals(50.0, updatedWallet.balance, 0.01)
    assertEquals(5000L, updatedWallet.pendingBalance)
    assertEquals(50.0, updatedWallet.lockedBalance, 0.01)

    val players = LocalDataStore.localMatchPlayers[testMatchId]!!
    assertEquals(1, players.size)
    assertEquals(testUserId, players[0].effectiveUid)

    val activeReservation = LocalDataStore.localActiveJoins[testUserId]?.get(testMatchId)
    assertNotNull("Active reservation should be recorded", activeReservation)
    assertEquals(testUserId, activeReservation?.get("userId"))
    assertEquals(testMatchId, activeReservation?.get("matchId"))
    assertEquals(5000L, activeReservation?.get("entryFeeMinorUnits"))
  }

  // --- Requirement 12 - Test B: Insufficient balance ---
  @Test
  fun testJoinMatch_insufficientBalance() = runBlocking {
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 10.0,
      availableBalance = 1000L,
    )

    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Expected Error due to low balance, got $result", result is Resource.Error)

    val match = LocalDataStore.localMatches[testMatchId]!!
    assertEquals(0, match.joinedPlayersCount)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(1000L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)
  }

  // --- Requirement 12 - Test C: Same-match concurrent attempts -> one reservation/deduction only ---
  @Test
  fun testJoinMatch_sameMatchConcurrentAttempts_oneReservationAndDeductionOnly() = runBlocking {
    val results = coroutineScope {
      val deferredResults = (1..5).map {
        async(Dispatchers.Default) {
          repository.joinMatch(testUserId, testMatchId)
        }
      }
      deferredResults.map { it.await() }
    }
    val successCount = results.count { it is Resource.Success }
    val failureCount = results.count { it is Resource.Error }

    assertEquals("Exactly one join attempt must succeed", 1, successCount)
    assertEquals("All other duplicate attempts must fail", 4, failureCount)

    val updatedWallet = LocalDataStore.localWallets[testUserId]!!
    // Starting was 10000L, entry fee 5000L -> must be exactly 5000L, never deducted multiple times
    assertEquals(5000L, updatedWallet.availableBalance)
    assertEquals(50.0, updatedWallet.balance, 0.001)
    assertEquals(5000L, updatedWallet.pendingBalance)
    assertEquals(50.0, updatedWallet.lockedBalance, 0.001)

    val players = LocalDataStore.localMatchPlayers[testMatchId]!!
    assertEquals(1, players.size)

    val activeReservations = LocalDataStore.localActiveJoins[testUserId]
    assertEquals(1, activeReservations?.size)
  }

  // --- Requirement 12 - Test D: Different-match concurrent attempts -> balance cannot go negative/double-spend ---
  @Test
  fun testJoinMatch_differentMatchConcurrentAttempts_balanceCannotGoNegativeOrDoubleSpend() = runBlocking {
    // User has exactly 5000L (৳50).
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 50.0,
      availableBalance = 5000L,
    )

    val match2Id = "match_test_102"
    LocalDataStore.localMatches[match2Id] = MatchEntity(
      matchId = match2Id,
      matchNumber = "#M102",
      title = "Concurrent Match 2",
      status = MatchStatus.AVAILABLE.name,
      entryFee = 50.0,
      entryFeeMinorUnits = 5000L,
      joinedPlayersCount = 0,
      maxPlayers = 2,
    )
    LocalDataStore.localMatchPlayers[match2Id] = CopyOnWriteArrayList()

    val results = coroutineScope {
      val res1 = async(Dispatchers.Default) {
        repository.joinMatch(testUserId, testMatchId)
      }
      val res2 = async(Dispatchers.Default) {
        repository.joinMatch(testUserId, match2Id)
      }
      listOf(res1.await(), res2.await())
    }

    val successCount = results.count { it is Resource.Success }
    val failureCount = results.count { it is Resource.Error }

    assertEquals("Only 1 match can be joined with balance for 1", 1, successCount)
    assertEquals("The other match join must fail due to insufficient balance", 1, failureCount)

    val updatedWallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals("Balance cannot drop below 0", 0L, updatedWallet.availableBalance)
    assertEquals(0.0, updatedWallet.balance, 0.001)
    assertEquals(5000L, updatedWallet.pendingBalance)
    assertEquals(50.0, updatedWallet.lockedBalance, 0.001)
  }

  // --- Requirement 12 - Test E: updateChildren timeout but player exists -> NO refund ---
  @Test
  fun testJoinMatch_updateChildrenTimeout_playerExists_noRefund() = runBlocking {
    // Simulate: Wallet was deducted, player record was created on server
    val now = System.currentTimeMillis()
    val trxId = "TXN_JOIN_${now}_${testMatchId}_001"
    val entryFee = 5000L

    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 5000L,
      balance = 50.0,
      pendingBalance = 5000L,
      lockedBalance = 50.0,
      updatedAt = now,
    )

    val testPlayer = MatchPlayerEntity(
      matchPlayerId = "MP_${testMatchId}_$testUserId",
      matchId = testMatchId,
      uid = testUserId,
      userId = testUserId,
      username = "Test Player",
      joinedAt = now,
      entryFeeMinorUnits = entryFee,
      status = "JOINED",
    )

    val updatedMatch = LocalDataStore.localMatches[testMatchId]!!.copy(joinedPlayersCount = 1)
    val updatedWallet = LocalDataStore.localWallets[testUserId]!!
    val transaction = TransactionEntity(
      transactionId = trxId,
      uid = testUserId,
      userId = testUserId,
      amount = entryFee,
      type = "MATCH_JOIN",
      status = TransactionStatus.COMPLETED.name,
      beforeBalance = 10000L,
      afterBalance = 5000L,
      referenceId = testMatchId,
      createdAt = now,
      processedAt = now,
    )

    // Player exists on server (playerExistsOnServer = true)
    val outcome = repository.handleJoinUpdateOutcome(
      effectiveUid = testUserId,
      userId = testUserId,
      matchId = testMatchId,
      trxId = trxId,
      entryFeeMinorUnits = entryFee,
      playerExistsOnServer = true,
      playerConfirmedAbsentOnServer = false,
      updatedMatch = updatedMatch,
      newPlayer = testPlayer,
      updatedWallet = updatedWallet,
      transaction = transaction,
    )

    assertTrue("Outcome must be Success when player exists on server", outcome is Resource.Success)

    // Wallet balance MUST NOT be refunded
    val walletAfter = LocalDataStore.localWallets[testUserId]!!
    assertEquals(5000L, walletAfter.availableBalance)
    assertEquals(5000L, walletAfter.pendingBalance)
    assertEquals(50.0, walletAfter.balance, 0.001)

    // Player record must be present
    val players = LocalDataStore.localMatchPlayers[testMatchId]!!
    assertEquals(1, players.size)
    assertEquals(testUserId, players[0].effectiveUid)
  }

  // --- Requirement 12 - Test F: updateChildren failure and player absent + matching active reservation -> exactly one compensation ---
  @Test
  fun testJoinMatch_updateChildrenFailure_playerAbsent_matchingReservation_compensatesOnce() = runBlocking {
    val now = System.currentTimeMillis()
    val trxId = "TXN_JOIN_${now}_${testMatchId}_001"
    val entryFee = 5000L

    // Setup active reservation and deducted wallet
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 5000L,
      balance = 50.0,
      pendingBalance = 5000L,
      lockedBalance = 50.0,
      updatedAt = now,
    )

    val reservation = mapOf(
      "userId" to testUserId,
      "matchId" to testMatchId,
      "entryFeeMinorUnits" to entryFee,
      "transactionId" to trxId,
      "timestamp" to now,
    )
    LocalDataStore.localActiveJoins.getOrPut(testUserId) { ConcurrentHashMap() }[testMatchId] = reservation

    // Execute compensation
    val compensated = repository.compensateFailedJoin(
      effectiveUid = testUserId,
      matchId = testMatchId,
      trxId = trxId,
      entryFeeMinorUnits = entryFee,
    )

    assertTrue("Compensation should succeed for matching active reservation", compensated)

    val walletAfter = LocalDataStore.localWallets[testUserId]!!
    assertEquals(10000L, walletAfter.availableBalance)
    assertEquals(100.0, walletAfter.balance, 0.001)
    assertEquals(0L, walletAfter.pendingBalance)
    assertEquals(0.0, walletAfter.lockedBalance, 0.001)

    // Active reservation should be removed
    val activeReservation = LocalDataStore.localActiveJoins[testUserId]?.get(testMatchId)
    assertEquals(null, activeReservation)
  }

  // --- Requirement 12 - Test G: compensation repeated -> second attempt must NOT refund again ---
  @Test
  fun testJoinMatch_compensationRepeated_doesNotRefundAgain() = runBlocking {
    val now = System.currentTimeMillis()
    val trxId = "TXN_JOIN_${now}_${testMatchId}_001"
    val entryFee = 5000L

    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 5000L,
      balance = 50.0,
      pendingBalance = 5000L,
      lockedBalance = 50.0,
      updatedAt = now,
    )

    val reservation = mapOf(
      "userId" to testUserId,
      "matchId" to testMatchId,
      "entryFeeMinorUnits" to entryFee,
      "transactionId" to trxId,
      "timestamp" to now,
    )
    LocalDataStore.localActiveJoins.getOrPut(testUserId) { ConcurrentHashMap() }[testMatchId] = reservation

    // First compensation attempt
    val firstAttempt = repository.compensateFailedJoin(
      effectiveUid = testUserId,
      matchId = testMatchId,
      trxId = trxId,
      entryFeeMinorUnits = entryFee,
    )
    assertTrue("First compensation should succeed", firstAttempt)

    val balanceAfterFirst = LocalDataStore.localWallets[testUserId]!!.availableBalance
    assertEquals(10000L, balanceAfterFirst)

    // Second compensation attempt (repeated)
    val secondAttempt = repository.compensateFailedJoin(
      effectiveUid = testUserId,
      matchId = testMatchId,
      trxId = trxId,
      entryFeeMinorUnits = entryFee,
    )
    assertFalse("Second compensation must be rejected / aborted", secondAttempt)

    val balanceAfterSecond = LocalDataStore.localWallets[testUserId]!!.availableBalance
    assertEquals("Wallet must NOT be refunded a second time", 10000L, balanceAfterSecond)
  }

  // --- Requirement 12 - Test H: active reservation prevents duplicate join ---
  @Test
  fun testJoinMatch_activeReservationPreventsDuplicateJoin() = runBlocking {
    val now = System.currentTimeMillis()
    val reservation = mapOf(
      "userId" to testUserId,
      "matchId" to testMatchId,
      "entryFeeMinorUnits" to 5000L,
      "transactionId" to "TXN_PREV",
      "timestamp" to now,
    )
    LocalDataStore.localActiveJoins.getOrPut(testUserId) { ConcurrentHashMap() }[testMatchId] = reservation

    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Should fail due to active reservation", result is Resource.Error)

    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("already joined", ignoreCase = true))

    // Balance should remain unmutated (10000L)
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(10000L, wallet.availableBalance)
  }

  // --- Requirement 12 - Test I: balance/pending/locked consistency ---
  @Test
  fun testJoinMatch_balanceAndLockedBalanceConsistency() = runBlocking {
    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue(result is Resource.Success)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(wallet.availableBalance / 100.0, wallet.balance, 0.0001)
    assertEquals(wallet.pendingBalance / 100.0, wallet.lockedBalance, 0.0001)
  }

  // Additional match lifecycle tests
  @Test
  fun testJoinMatch_becomesFullWhenMaxPlayersReached() = runBlocking {
    val opponentId = "player_test_002"
    LocalDataStore.localUsers[opponentId] = UserEntity(uid = opponentId, userId = opponentId)
    LocalDataStore.localWallets[opponentId] = WalletEntity(uid = opponentId, userId = opponentId, balance = 100.0, availableBalance = 10000L)

    val res1 = repository.joinMatch(testUserId, testMatchId)
    assertTrue(res1 is Resource.Success)

    val res2 = repository.joinMatch(opponentId, testMatchId)
    assertTrue(res2 is Resource.Success)

    val updatedMatch = LocalDataStore.localMatches[testMatchId]!!
    assertEquals(2, updatedMatch.joinedPlayersCount)
    assertEquals(MatchStatus.FULL.name, updatedMatch.status)
  }

  @Test
  fun testJoinMatch_alreadyJoinedFails() = runBlocking {
    val res1 = repository.joinMatch(testUserId, testMatchId)
    assertTrue(res1 is Resource.Success)

    val res2 = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Expected Error for duplicate join, got $res2", res2 is Resource.Error)
  }

  @Test
  fun testJoinMatch_upcomingStatusFails() = runBlocking {
    LocalDataStore.localMatches[testMatchId] = LocalDataStore.localMatches[testMatchId]!!.copy(
      status = MatchStatus.UPCOMING.name,
    )

    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Expected Error when joining UPCOMING match, got $result", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("not available to join", ignoreCase = true))

    val match = LocalDataStore.localMatches[testMatchId]!!
    assertEquals(0, match.joinedPlayersCount)
  }

  @Test
  fun testJoinMatch_exactBalanceSufficiency_succeedsLeavingZero() = runBlocking {
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 50.0,
      availableBalance = 5000L,
    )

    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Exact balance match must succeed", result is Resource.Success)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(0L, wallet.availableBalance)
    assertEquals(0.0, wallet.balance, 0.001)
    assertEquals(5000L, wallet.pendingBalance)
    assertEquals(50.0, wallet.lockedBalance, 0.001)
  }

  @Test
  fun testJoinMatch_oneMinorUnitShort_fails() = runBlocking {
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 49.99,
      availableBalance = 4999L,
    )

    val result = repository.joinMatch(testUserId, testMatchId)
    assertTrue("Should fail when 1 minor unit short", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("Insufficient", ignoreCase = true))

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(4999L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)
  }
}
