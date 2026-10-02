package com.example

import com.example.core.error.Resource
import com.example.data.repository.FirebaseAdminRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class AdminWithdrawalSettlementTest {

  private val adminRepository = FirebaseAdminRepository()
  private val testAdminUid = "admin_001"
  private val testUserId = "user_wth_001"

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
      balance = 500.0,
      availableBalance = 50000L, // ৳ 500.00
      pendingBalance = 50000L,   // ৳ 500.00 on hold
      lockedBalance = 500.0,
      totalWithdrawn = 0L,
    )
    LocalDataStore.localWithdrawals.clear()
    LocalDataStore.localTransactions.clear()
    LocalDataStore.localAuditLogs.clear()
    LocalDataStore.localActiveWithdrawals.clear()
    LocalDataStore.localActiveJoins.clear()
  }

  private fun createPendingWithdrawal(
    withdrawalId: String,
    amountMinorUnits: Long = 50000L,
  ): WithdrawalEntity {
    val withdrawal = WithdrawalEntity(
      withdrawalId = withdrawalId,
      uid = testUserId,
      userId = testUserId,
      amount = amountMinorUnits,
      method = "BKASH",
      recipientNumber = "01712345678",
      status = WithdrawalStatus.REQUESTED.name,
      createdAt = System.currentTimeMillis(),
    )
    LocalDataStore.localWithdrawals.add(withdrawal)

    val userReservations = LocalDataStore.localActiveWithdrawals.getOrPut(testUserId) { ConcurrentHashMap() }
    userReservations[withdrawalId] = mapOf(
      "withdrawalId" to withdrawalId,
      "amountMinorUnits" to amountMinorUnits,
      "amount" to amountMinorUnits,
      "status" to WithdrawalStatus.REQUESTED.name,
      "createdAt" to System.currentTimeMillis(),
    )
    return withdrawal
  }

  // 1. Valid Approval: Settles wallet, zeroes pending balance, removes reservation, updates status
  @Test
  fun testApproveWithdrawal_validReservation_settlesCorrectly() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_001", 50000L)

    val result = adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal)
    assertTrue("Approval must succeed", result is Resource.Success)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)
    assertEquals(50000L, wallet.totalWithdrawn)
    assertEquals(500.0, wallet.balance, 0.001)
    assertEquals(0.0, wallet.lockedBalance, 0.001)

    // Reservation must be removed
    val reservations = LocalDataStore.localActiveWithdrawals[testUserId]
    assertNull("Reservation must be removed", reservations?.get("WTH_001"))

    // Withdrawal status updated
    val updatedWithdrawal = LocalDataStore.localWithdrawals.first { it.withdrawalId == "WTH_001" }
    assertEquals(WithdrawalStatus.COMPLETED.name, updatedWithdrawal.status)

    // Transaction & Audit Log created
    val txn = LocalDataStore.localTransactions.firstOrNull { it.referenceId == "WTH_CMP_WTH_001" }
    assertNotNull("Completion transaction must exist", txn)
    assertEquals(TransactionType.WITHDRAW.name, txn?.type)
    assertEquals(50000L, txn?.amount)

    val log = LocalDataStore.localAuditLogs.firstOrNull { it.action == "APPROVE_WITHDRAWAL" }
    assertNotNull("Audit log must exist", log)
    assertEquals(testAdminUid, log?.adminUid)
  }

  // 2. Missing Reservation: Aborts approval and leaves wallet untouched
  @Test
  fun testApproveWithdrawal_missingReservation_failsAndDoesNotMutateWallet() = runBlocking {
    val withdrawal = WithdrawalEntity(
      withdrawalId = "WTH_GHOST",
      uid = testUserId,
      userId = testUserId,
      amount = 50000L,
      method = "BKASH",
      recipientNumber = "01712345678",
      status = WithdrawalStatus.REQUESTED.name,
    )
    LocalDataStore.localWithdrawals.add(withdrawal)
    // Do NOT add to localActiveWithdrawals

    val result = adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal)
    assertTrue("Approval must fail when reservation is missing", result is Resource.Error)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(50000L, wallet.pendingBalance)
    assertEquals(0L, wallet.totalWithdrawn)
  }

  // 3. Amount Mismatch: Aborts approval when reservation amount does not match withdrawal
  @Test
  fun testApproveWithdrawal_amountMismatch_failsAndDoesNotMutateWallet() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_TAMPERED", 50000L)
    // Tamper with withdrawal object passed to repository
    val tamperedWithdrawal = withdrawal.copy(amount = 99999L)

    val result = adminRepository.approveWithdrawal(testAdminUid, tamperedWithdrawal.withdrawalId, tamperedWithdrawal)
    assertTrue("Approval must fail on amount mismatch", result is Resource.Error)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(50000L, wallet.pendingBalance)
    assertEquals(0L, wallet.totalWithdrawn)
  }

  // 4. Insufficient Pending Balance: Aborts when pending balance is less than withdrawal
  @Test
  fun testApproveWithdrawal_insufficientPendingBalance_fails() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_UNDER", 50000L)
    // Tamper wallet pending balance to be less than withdrawal
    LocalDataStore.localWallets[testUserId] = LocalDataStore.localWallets[testUserId]!!.copy(
      pendingBalance = 10000L,
    )

    val result = adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal)
    assertTrue("Approval must fail if pending balance is insufficient", result is Resource.Error)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(10000L, wallet.pendingBalance)
    assertEquals(0L, wallet.totalWithdrawn)
  }

  // 5. Valid Rejection: Refunds available balance, zeroes pending balance, removes reservation
  @Test
  fun testRejectWithdrawal_validReservation_releasesHoldCorrectly() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_REJ_001", 50000L)
    val reason = "Incorrect Bkash account number provided"

    val result = adminRepository.rejectWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal, reason)
    assertTrue("Rejection must succeed", result is Resource.Success)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(100000L, wallet.availableBalance) // 50000 + 50000 refunded
    assertEquals(0L, wallet.pendingBalance)
    assertEquals(0L, wallet.totalWithdrawn)
    assertEquals(1000.0, wallet.balance, 0.001)
    assertEquals(0.0, wallet.lockedBalance, 0.001)

    // Reservation removed
    val reservations = LocalDataStore.localActiveWithdrawals[testUserId]
    assertNull("Reservation must be removed", reservations?.get("WTH_REJ_001"))

    // Status updated with reason
    val updatedWithdrawal = LocalDataStore.localWithdrawals.first { it.withdrawalId == "WTH_REJ_001" }
    assertEquals(WithdrawalStatus.REJECTED.name, updatedWithdrawal.status)
    assertEquals(reason, updatedWithdrawal.rejectionReason)

    // Release transaction created
    val txn = LocalDataStore.localTransactions.firstOrNull { it.referenceId == "WTH_REL_WTH_REJ_001" }
    assertNotNull("Release transaction must exist", txn)
    assertEquals(TransactionType.WITHDRAW_RELEASE.name, txn?.type)
    assertEquals(50000L, txn?.amount)
    assertEquals(100000L, txn?.afterBalance)

    // Audit log created
    val log = LocalDataStore.localAuditLogs.firstOrNull { it.action == "REJECT_WITHDRAWAL" }
    assertNotNull("Audit log must exist", log)
    assertEquals(testAdminUid, log?.adminUid)
  }

  // 6. Blank Rejection Reason: Fails validation
  @Test
  fun testRejectWithdrawal_blankReason_fails() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_BLANK", 50000L)

    val result = adminRepository.rejectWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal, "   ")
    assertTrue("Blank rejection reason must fail", result is Resource.Error)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(50000L, wallet.pendingBalance)
  }

  // 7. Idempotency: Repeated approval attempts cannot double-settle
  @Test
  fun testIdempotency_repeatedApprove_cannotDoubleSettle() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_IDEM", 50000L)

    val first = adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal)
    assertTrue("First approval must succeed", first is Resource.Success)

    val completedWithdrawal = LocalDataStore.localWithdrawals.first { it.withdrawalId == "WTH_IDEM" }
    val second = adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, completedWithdrawal)
    assertTrue("Second approval must fail", second is Resource.Error)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(50000L, wallet.availableBalance)
    assertEquals(0L, wallet.pendingBalance)
    assertEquals(50000L, wallet.totalWithdrawn)
  }

  // 8. Concurrent Settlement: Exactly one of approve/reject succeeds
  @Test
  fun testConcurrency_concurrentApproveAndReject_onlyOneSucceeds() = runBlocking {
    val withdrawal = createPendingWithdrawal("WTH_RACE", 50000L)

    val (res1, res2) = coroutineScope {
      val d1 = async(Dispatchers.Default) {
        adminRepository.approveWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal)
      }
      val d2 = async(Dispatchers.Default) {
        adminRepository.rejectWithdrawal(testAdminUid, withdrawal.withdrawalId, withdrawal, "Declined")
      }
      Pair(d1.await(), d2.await())
    }

    val successCount = listOf(res1, res2).count { it is Resource.Success }
    val errorCount = listOf(res1, res2).count { it is Resource.Error }

    assertEquals("Exactly one operation must succeed", 1, successCount)
    assertEquals("Exactly one operation must fail", 1, errorCount)

    val wallet = LocalDataStore.localWallets[testUserId]!!
    // Either approved (pending=0, totalWithdrawn=50000, avail=50000)
    // or rejected (pending=0, totalWithdrawn=0, avail=100000)
    assertEquals(0L, wallet.pendingBalance)
    assertTrue(
      "Wallet must be in one of the valid settled states",
      (wallet.availableBalance == 50000L && wallet.totalWithdrawn == 50000L) ||
      (wallet.availableBalance == 100000L && wallet.totalWithdrawn == 0L)
    )
  }

  // 9. Isolation: Other active reservations and active joins are preserved
  @Test
  fun testIsolation_otherReservationsAndJoinsUntouched() = runBlocking {
    // Setup two active withdrawals and one active match join
    LocalDataStore.localWallets[testUserId] = LocalDataStore.localWallets[testUserId]!!.copy(
      availableBalance = 30000L,
      pendingBalance = 70000L, // 30000 (WTH_A) + 40000 (WTH_B)
      lockedBalance = 700.0,
    )
    val wA = createPendingWithdrawal("WTH_A", 30000L)
    val wB = createPendingWithdrawal("WTH_B", 40000L)

    // Add an active match join
    val userJoins = LocalDataStore.localActiveJoins.getOrPut(testUserId) { ConcurrentHashMap() }
    userJoins["MATCH_999"] = mapOf("matchId" to "MATCH_999", "fee" to 10000L)

    // Approve WTH_A
    val result = adminRepository.approveWithdrawal(testAdminUid, wA.withdrawalId, wA)
    assertTrue("Approval for WTH_A must succeed", result is Resource.Success)

    val reservations = LocalDataStore.localActiveWithdrawals[testUserId]!!
    assertNull("WTH_A must be removed", reservations["WTH_A"])
    assertNotNull("WTH_B must remain untouched", reservations["WTH_B"])

    val joins = LocalDataStore.localActiveJoins[testUserId]!!
    assertNotNull("Match join must remain untouched", joins["MATCH_999"])

    val wallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals(30000L, wallet.availableBalance)
    assertEquals(40000L, wallet.pendingBalance)
    assertEquals(30000L, wallet.totalWithdrawn)
  }
}
