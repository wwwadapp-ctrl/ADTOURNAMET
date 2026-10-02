package com.example

import com.example.core.config.FirebaseConfig
import com.example.core.error.Resource
import com.example.data.repository.FirebaseWalletRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.DepositEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DepositSafetyTest {

  private val repository = FirebaseWalletRepository()
  private val testUserId = "deposit_test_user_001"

  @Before
  fun setUp() {
    LocalDataStore.localDeposits.clear()
    LocalDataStore.localUsers[testUserId] = UserEntity(
      uid = testUserId,
      userId = testUserId,
      name = "Deposit Test Player",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localWallets[testUserId] = WalletEntity(
      uid = testUserId,
      userId = testUserId,
      balance = 250.0,
      availableBalance = 25000L,
      pendingBalance = 0L,
    )
  }

  @Test
  fun test1_firstValidRequestIsAllowed() = runBlocking {
    val result = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRX111111",
      screenshotUrl = "",
    )

    assertTrue("First request must succeed: $result", result is Resource.Success)
    val deposit = (result as Resource.Success).data
    assertEquals("01711111111", deposit.senderNumber)
    assertEquals("TRX111111", deposit.trxId)
    assertEquals(5000L, deposit.amount)
    assertEquals("PENDING", deposit.status)
    assertEquals(1, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test2_secondDistinctRequestWithin20MinutesIsAllowed() = runBlocking {
    val res1 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRX111111",
      screenshotUrl = "",
    )
    assertTrue("First request must succeed", res1 is Resource.Success)

    val res2 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 6000L,
      method = "NAGAD",
      senderNumber = "01822222222",
      trxId = "TRX222222",
      screenshotUrl = "",
    )
    assertTrue("Second distinct request within 20 mins must succeed", res2 is Resource.Success)
    assertEquals(2, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test3_thirdDistinctRequestWithin20MinutesIsAllowed() = runBlocking {
    val res1 = repository.submitDepositRequest(testUserId, 5000L, "BKASH", "01711111111", "TRX111111", "")
    val res2 = repository.submitDepositRequest(testUserId, 6000L, "NAGAD", "01822222222", "TRX222222", "")
    val res3 = repository.submitDepositRequest(testUserId, 7000L, "BKASH", "01933333333", "TRX333333", "")

    assertTrue(res1 is Resource.Success)
    assertTrue(res2 is Resource.Success)
    assertTrue("Third distinct request within 20 mins must succeed", res3 is Resource.Success)
    assertEquals(3, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test4_fourthDistinctRequestWithin20MinutesIsRejected() = runBlocking {
    repository.submitDepositRequest(testUserId, 5000L, "BKASH", "01711111111", "TRX111111", "")
    repository.submitDepositRequest(testUserId, 6000L, "NAGAD", "01822222222", "TRX222222", "")
    repository.submitDepositRequest(testUserId, 7000L, "BKASH", "01933333333", "TRX333333", "")

    val res4 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 8000L,
      method = "BKASH",
      senderNumber = "01644444444",
      trxId = "TRX444444",
      screenshotUrl = "",
    )

    assertTrue("Fourth request within 20 mins must be rejected", res4 is Resource.Error)
    val error = res4 as Resource.Error
    assertTrue(
      "Expected 20-minute limit error, got: ${error.error.message}",
      error.error.message.contains("3 deposit requests within 20 minutes", ignoreCase = true)
    )
    assertEquals("No 4th record should be created", 3, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test5_exactDuplicateIsRejected() = runBlocking {
    val res1 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRXABC123",
      screenshotUrl = "",
    )
    assertTrue(res1 is Resource.Success)

    // Exact duplicate with same sender, same trxId (different casing/whitespace), and same amount
    val duplicateRes = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = " 01711111111 ",
      trxId = " trxabc123 ",
      screenshotUrl = "",
    )

    assertTrue("Exact duplicate must be rejected", duplicateRes is Resource.Error)
    val error = duplicateRes as Resource.Error
    assertTrue(
      "Expected duplicate error, got: ${error.error.message}",
      error.error.message.contains("same sender number, transaction ID, and amount", ignoreCase = true)
    )
    assertEquals("Duplicate must not create an extra record", 1, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test6_sameTrxIdDifferentAmountIsTreatedAccordingToExactDuplicateRule() = runBlocking {
    // Exact duplicate rule requires (senderNumber + trxId + amount).
    // If trxId is identical but amount is different, it is NOT an exact duplicate and is treated as a distinct request.
    val res1 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRXSHARED01",
      screenshotUrl = "",
    )
    assertTrue(res1 is Resource.Success)

    val res2 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 10000L, // Different amount
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRXSHARED01",
      screenshotUrl = "",
    )
    assertTrue("Different amount is not an exact duplicate; treated as distinct request", res2 is Resource.Success)
    assertEquals(2, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test7_sameAmountAndSenderDifferentTrxIdIsTreatedAsDistinct() = runBlocking {
    val res1 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L,
      method = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRX111111",
      screenshotUrl = "",
    )
    assertTrue(res1 is Resource.Success)

    val res2 = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 5000L, // Same amount
      method = "BKASH",
      senderNumber = "01711111111", // Same sender
      trxId = "TRX999999", // Different trxId
      screenshotUrl = "",
    )
    assertTrue("Different trxId is distinct", res2 is Resource.Success)
    assertEquals(2, LocalDataStore.localDeposits.count { it.uid == testUserId })
  }

  @Test
  fun test8_requestOlderThan20MinutesDoesNotCountTowardLimit() = runBlocking {
    val oldTime = System.currentTimeMillis() - 25 * 60 * 1000L // 25 minutes ago
    val oldDeposit = DepositEntity(
      depositId = "DEP_OLD_01",
      uid = testUserId,
      userId = testUserId,
      amount = 5000L,
      method = "BKASH",
      paymentMethod = "BKASH",
      senderNumber = "01711111111",
      trxId = "TRXOLD001",
      screenshotUrl = "",
      status = "PENDING",
      createdAt = oldTime,
      processedAt = 0L,
    )
    LocalDataStore.localDeposits.add(oldDeposit)

    // User submits 3 new requests within current window
    val r1 = repository.submitDepositRequest(testUserId, 5000L, "BKASH", "01711111111", "TRXNEW001", "")
    val r2 = repository.submitDepositRequest(testUserId, 6000L, "BKASH", "01711111111", "TRXNEW002", "")
    val r3 = repository.submitDepositRequest(testUserId, 7000L, "BKASH", "01711111111", "TRXNEW003", "")

    assertTrue("1st new request allowed", r1 is Resource.Success)
    assertTrue("2nd new request allowed", r2 is Resource.Success)
    assertTrue("3rd new request allowed despite older >20m request existing", r3 is Resource.Success)

    // A 4th request in current window should be rejected
    val r4 = repository.submitDepositRequest(testUserId, 8000L, "BKASH", "01711111111", "TRXNEW004", "")
    assertTrue(r4 is Resource.Error)
  }

  @Test
  fun test9_validationDoesNotModifyWalletBalance() = runBlocking {
    val initialWallet = LocalDataStore.localWallets[testUserId]!!
    val initialBalance = initialWallet.balance
    val initialAvailable = initialWallet.availableBalance
    val initialPending = initialWallet.pendingBalance

    // Submit valid request
    repository.submitDepositRequest(testUserId, 5000L, "BKASH", "01711111111", "TRX111111", "")

    // Submit duplicate request (rejected)
    repository.submitDepositRequest(testUserId, 5000L, "BKASH", "01711111111", "TRX111111", "")

    // Submit rate-limited request
    repository.submitDepositRequest(testUserId, 6000L, "BKASH", "01711111111", "TRX222222", "")
    repository.submitDepositRequest(testUserId, 7000L, "BKASH", "01711111111", "TRX333333", "")
    repository.submitDepositRequest(testUserId, 8000L, "BKASH", "01711111111", "TRX444444", "")

    val finalWallet = LocalDataStore.localWallets[testUserId]!!
    assertEquals("Wallet balance must be untouched", initialBalance, finalWallet.balance, 0.001)
    assertEquals("Wallet availableBalance must be untouched", initialAvailable, finalWallet.availableBalance)
    assertEquals("Wallet pendingBalance must be untouched", initialPending, finalWallet.pendingBalance)
  }

  @Test
  fun test10_existingDepositSchemaAndPathsRemainUnchanged() = runBlocking {
    assertEquals("deposits", FirebaseConfig.NODE_DEPOSITS)
    assertEquals("userDeposits", FirebaseConfig.NODE_USER_DEPOSITS)

    val res = repository.submitDepositRequest(
      userId = testUserId,
      amountMinorUnits = 7500L,
      method = "BKASH",
      senderNumber = "01799999999",
      trxId = "TRXSCHEMA01",
      screenshotUrl = "",
    )
    assertTrue(res is Resource.Success)
    val deposit = (res as Resource.Success).data

    assertNotNull(deposit.depositId)
    assertTrue(deposit.depositId.isNotBlank())
    assertEquals(testUserId, deposit.uid)
    assertEquals(testUserId, deposit.userId)
    assertEquals(7500L, deposit.amount)
    assertEquals(75.0, deposit.amountInCurrency, 0.001)
    assertEquals("BKASH", deposit.method)
    assertEquals("BKASH", deposit.paymentMethod)
    assertEquals("01799999999", deposit.senderNumber)
    assertEquals("TRXSCHEMA01", deposit.trxId)
    assertEquals("TRXSCHEMA01", deposit.transactionReference)
    assertEquals("", deposit.screenshotUrl)
    assertEquals("", deposit.screenshotProofUrl)
    assertEquals("PENDING", deposit.status)
    assertTrue("createdAt must be positive", deposit.createdAt > 0L)
    assertEquals(0L, deposit.processedAt)
    assertEquals(0L, deposit.approvedAt)
  }
}
