package com.example

import com.example.core.error.Resource
import com.example.data.repository.FirebaseWalletRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.TransactionEntity
import com.example.domain.model.TransactionType
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.domain.model.WithdrawalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class WithdrawalSafetyTest {

  private val repository = FirebaseWalletRepository()
  private val testUserId = "user_withdraw_001"

  @Before
  fun setUp() {
    LocalDataStore.localUsers[testUserId] = UserEntity(
      uid = testUserId,
      userId = testUserId,
      name = "Test User",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 1000.0,
      availableBalance = 100000L, // ৳ 1,000.00
      pendingBalance = 0L,
      lockedBalance = 0.0,
    )
    LocalDataStore.localWithdrawals.clear()
    LocalDataStore.localTransactions.clear()
    LocalDataStore.localActiveWithdrawals.clear()
  }

  // 1. Authoritative Hold: Valid withdrawal creates hold, updates balances, and creates reservation
  @Test
  fun testWithdrawalHold_validRequest_createsHoldAndDeductsAvailable() = runBlocking {
    val amount = 50000L // ৳ 500.00
    val result = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = amount,
      method = "BKASH",
      recipientNumber = "01712345678",
    )

    assertTrue("Withdrawal must succeed", result is Resource.Success)
    val withdrawal = (result as Resource.Success).data
    assertEquals(amount, withdrawal.amount)
    assertEquals("REQUESTED", withdrawal.status)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(500.0, wallet.balance, 0.001)
    assertEquals(50000L, wallet.pendingBalance)
    assertEquals(500.0, wallet.lockedBalance, 0.001)

    // Verify reservation created
    val userReservations = LocalDataStore.localActiveWithdrawals[testUserId]
    assertNotNull(userReservations)
    assertEquals(1, userReservations!!.size)
    val reservation = userReservations[withdrawal.withdrawalId]
    assertNotNull(reservation)
    assertEquals(amount, reservation!!["amountMinorUnits"])

    // Verify transaction created
    val tx = LocalDataStore.localTransactions.firstOrNull { it.referenceId == withdrawal.withdrawalId }
    assertNotNull(tx)
    assertEquals(TransactionType.WITHDRAW_HOLD.name, tx!!.type)
    assertEquals(100000L, tx.beforeBalance)
    assertEquals(50000L, tx.afterBalance)
  }

  // 2. Policy: At most ONE active withdrawal is allowed at a time
  @Test
  fun testWithdrawalHold_secondRequestBlockedWhileFirstIsActive() = runBlocking {
    val first = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 30000L, // ৳ 300.00
      method = "BKASH",
      recipientNumber = "01712345678",
    )
    assertTrue("First withdrawal should succeed", first is Resource.Success)

    // Attempt second withdrawal
    val second = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 20000L, // ৳ 200.00
      method = "NAGAD",
      recipientNumber = "01812345678",
    )
    assertTrue("Second withdrawal must be rejected", second is Resource.Error)
    val error = (second as Resource.Error).error
    assertTrue(
      "Error must mention active withdrawal request in progress",
      error.message.contains("already have an active withdrawal", ignoreCase = true),
    )

    // Balances must remain unchanged from after the first withdrawal
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(70000L, wallet.availableBalance)
    assertEquals(30000L, wallet.pendingBalance)
  }

  // 3. Insufficient balance rejects request without creating reservation
  @Test
  fun testWithdrawalHold_insufficientBalance_rejected() = runBlocking {
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 30000L, // ৳ 300.00
      balance = 300.0,
    )

    val result = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 50000L, // ৳ 500.00
      method = "BKASH",
      recipientNumber = "01712345678",
    )

    assertTrue("Request must fail due to insufficient balance", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("Insufficient", ignoreCase = true))

    // Balances must remain unchanged
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(30000L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)

    // No reservation should be present
    val reservations = LocalDataStore.localActiveWithdrawals[testUserId]
    assertTrue(reservations == null || reservations.isEmpty())
  }

  // 4. Exact balance withdrawal succeeds leaving 0 available balance
  @Test
  fun testWithdrawalHold_exactBalanceSufficiency_succeedsLeavingZero() = runBlocking {
    val result = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 100000L, // ৳ 1,000.00
      method = "BKASH",
      recipientNumber = "01712345678",
    )

    assertTrue("Exact balance withdrawal must succeed", result is Resource.Success)
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(0L, wallet.availableBalance)
    assertEquals(0.0, wallet.balance, 0.001)
    assertEquals(100000L, wallet.pendingBalance)
    assertEquals(1000.0, wallet.lockedBalance, 0.001)
  }

  // 5. One minor unit short fails
  @Test
  fun testWithdrawalHold_oneMinorUnitShort_fails() = runBlocking {
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 49999L, // ৳ 499.99
      balance = 499.99,
    )

    val result = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 50000L, // ৳ 500.00
      method = "BKASH",
      recipientNumber = "01712345678",
    )

    assertTrue("Should fail when 1 minor unit short", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("Insufficient", ignoreCase = true))

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(49999L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)
  }

  // 6. Blocked user cannot withdraw
  @Test
  fun testWithdrawalHold_blockedUser_rejected() = runBlocking {
    LocalDataStore.localUsers[testUserId] = LocalDataStore.localUsers[testUserId]!!.copy(
      status = "BLOCKED",
      isBlocked = true,
    )

    val result = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 50000L,
      method = "BKASH",
      recipientNumber = "01712345678",
    )

    assertTrue("Blocked user withdrawal must be rejected", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error.message.contains("blocked", ignoreCase = true))

    // Balance remains untouched
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(100000L, wallet.availableBalance)
  }

  // 7. Input limits: < 200 BDT or > 10,000 BDT rejected
  @Test
  fun testWithdrawalHold_amountLimits_rejected() = runBlocking {
    val belowMin = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 19900L, // 199 BDT
      method = "BKASH",
      recipientNumber = "01712345678",
    )
    assertTrue("Below minimum must fail", belowMin is Resource.Error)

    val aboveMax = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 1000100L, // 10,001 BDT
      method = "BKASH",
      recipientNumber = "01712345678",
    )
    assertTrue("Above maximum must fail", aboveMax is Resource.Error)
  }

  // 8. Invalid recipient phone number rejected
  @Test
  fun testWithdrawalHold_invalidRecipientNumber_rejected() = runBlocking {
    val shortNumber = repository.submitWithdrawalRequest(
      userId = testUserId,
      amountMinorUnits = 25000L,
      method = "BKASH",
      recipientNumber = "0171234",
    )
    assertTrue("Invalid short number must fail", shortNumber is Resource.Error)
  }

  // 9. Failure recovery / compensation: When records are absent on server, hold is safely restored
  @Test
  fun testWithdrawalHold_failureCompensation_refundsHoldWhenAbsent() = runBlocking {
    val withdrawalId = "WTH_COMP_TEST"
    val trxId = "TXN_WTH_TEST_001"
    val amount = 40000L // ৳ 400.00

    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 60000L,
      balance = 600.0,
      pendingBalance = 40000L,
      lockedBalance = 400.0,
    )

    val reservation = mapOf<String, Any>(
      "withdrawalId" to withdrawalId,
      "amountMinorUnits" to amount,
      "status" to "REQUESTED",
      "transactionId" to trxId,
      "timestamp" to System.currentTimeMillis(),
    )
    LocalDataStore.localActiveWithdrawals.getOrPut(testUserId) { ConcurrentHashMap() }[withdrawalId] = reservation

    val dummyWithdrawal = WithdrawalEntity(
      withdrawalId = withdrawalId,
      uid = testUserId,
      amount = amount,
    )
    val dummyTx = TransactionEntity(
      transactionId = trxId,
      uid = testUserId,
      amount = amount,
    )

    val outcome = repository.handleWithdrawalOutcome(
      effectiveUid = testUserId,
      userId = testUserId,
      withdrawalId = withdrawalId,
      trxId = trxId,
      amountMinorUnits = amount,
      recordsExistOnServer = false,
      withdrawal = dummyWithdrawal,
      updatedWallet = LocalDataStore.localWallets[testUserId],
      transaction = dummyTx,
    )

    assertTrue("Outcome should report Error due to network failure", outcome is Resource.Error)

    // Wallet balance must be restored
    val restoredWallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(100000L, restoredWallet.availableBalance)
    assertEquals(1000.0, restoredWallet.balance, 0.001)
    assertEquals(0L, restoredWallet.pendingBalance)
    assertEquals(0.0, restoredWallet.lockedBalance, 0.001)

    // Reservation must be cleared
    val userReservations = LocalDataStore.localActiveWithdrawals[testUserId]
    assertTrue(userReservations == null || userReservations.isEmpty())
  }

  // 10. Failure recovery: If records actually exist on server, do NOT refund
  @Test
  fun testWithdrawalHold_recordsExistOnServer_noRefund() = runBlocking {
    val withdrawalId = "WTH_EXISTS_TEST"
    val trxId = "TXN_WTH_TEST_002"
    val amount = 40000L

    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      availableBalance = 60000L,
      balance = 600.0,
      pendingBalance = 40000L,
      lockedBalance = 400.0,
    )

    val reservation = mapOf<String, Any>(
      "withdrawalId" to withdrawalId,
      "amountMinorUnits" to amount,
      "status" to "REQUESTED",
      "transactionId" to trxId,
      "timestamp" to System.currentTimeMillis(),
    )
    LocalDataStore.localActiveWithdrawals.getOrPut(testUserId) { ConcurrentHashMap() }[withdrawalId] = reservation

    val withdrawal = WithdrawalEntity(
      withdrawalId = withdrawalId,
      uid = testUserId,
      amount = amount,
    )
    val transaction = TransactionEntity(
      transactionId = trxId,
      uid = testUserId,
      amount = amount,
    )

    val outcome = repository.handleWithdrawalOutcome(
      effectiveUid = testUserId,
      userId = testUserId,
      withdrawalId = withdrawalId,
      trxId = trxId,
      amountMinorUnits = amount,
      recordsExistOnServer = true,
      withdrawal = withdrawal,
      updatedWallet = LocalDataStore.localWallets[testUserId],
      transaction = transaction,
    )

    assertTrue("Outcome must be Success when record exists", outcome is Resource.Success)

    // Balance must NOT be refunded
    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(60000L, wallet.availableBalance)
    assertEquals(40000L, wallet.pendingBalance)
  }

  // 11. Concurrent withdrawal attempts from same user (only one must succeed)
  @Test
  fun testWithdrawalHold_concurrentSubmissions_onlyOneSucceeds() = runBlocking {
    val results = coroutineScope {
      val deferred1 = async(Dispatchers.Default) {
        repository.submitWithdrawalRequest(
          userId = testUserId,
          amountMinorUnits = 30000L,
          method = "BKASH",
          recipientNumber = "01712345678",
        )
      }
      val deferred2 = async(Dispatchers.Default) {
        repository.submitWithdrawalRequest(
          userId = testUserId,
          amountMinorUnits = 30000L,
          method = "NAGAD",
          recipientNumber = "01812345678",
        )
      }
      listOf(deferred1.await(), deferred2.await())
    }

    val successes = results.count { it is Resource.Success }
    val errors = results.count { it is Resource.Error }

    assertEquals("Exactly ONE concurrent withdrawal request must succeed", 1, successes)
    assertEquals("The other concurrent request must be rejected", 1, errors)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(70000L, wallet.availableBalance)
    assertEquals(30000L, wallet.pendingBalance)
  }
}
