package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.FirebaseConfig
import com.example.core.error.AppError
import com.example.core.error.Resource
import com.example.core.util.ImageCompressor
import com.example.data.repository.FirebaseAdminRepository
import com.example.data.repository.FirebaseResultRepository
import com.example.data.repository.LocalDataStore
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.MatchStatus
import com.example.domain.model.ResultEntity
import com.example.domain.model.ResultStatus
import com.example.domain.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class ScreenshotStorageTest {

  private fun createSampleVictoryScreenshot(width: Int = 1920, height: Int = 1080): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.DKGRAY)

    val paint = Paint().apply {
      color = Color.YELLOW
      textSize = 48f
      isAntiAlias = true
    }
    canvas.drawText("VICTORY! Room Code: 123456", 50f, 150f, paint)
    canvas.drawText("Winner: ProPlayer_99 (UID: user_winner_1)", 50f, 250f, paint)
    canvas.drawText("Match: #101 - Ludo King Solo", 50f, 350f, paint)
    return bitmap
  }

  @Test
  fun test1_imageCompression_downscalesAndCompressesPreservingAspect() {
    val originalWidth = 2000
    val originalHeight = 1000
    val bitmap = createSampleVictoryScreenshot(originalWidth, originalHeight)

    val maxDimension = 1280
    val result = ImageCompressor.compressBitmap(bitmap, maxDimension = maxDimension)

    assertTrue("Expected compression to succeed", result is Resource.Success)
    val success = (result as Resource.Success).data

    // Width was 2000, scaled to maxDimension 1280
    assertEquals(1280, success.width)
    // Height scaled down proportionally (1000 * 1280 / 2000 = 640)
    assertEquals(640, success.height)

    // Result should be well under the 800 KB limit
    assertTrue(success.sizeInBytes < ImageCompressor.MAX_PAYLOAD_SIZE_BYTES)
    assertTrue(success.sizeInBytes > 0)
  }

  @Test
  fun test2_base64Conversion_producesValidDataUriAndDecodableJpeg() {
    val bitmap = createSampleVictoryScreenshot(800, 600)
    val result = ImageCompressor.compressBitmap(bitmap)

    assertTrue(result is Resource.Success)
    val data = (result as Resource.Success).data

    // Base64 string is non-empty
    assertTrue(data.base64String.isNotBlank())

    // Data URI format check
    assertTrue(data.dataUri.startsWith("data:image/jpeg;base64,"))
    assertTrue(data.dataUri.endsWith(data.base64String))

    // Decode back to bytes
    val decodedBytes = ImageCompressor.decodeBase64(data.dataUri)
    assertTrue("Decoded bytes must not be empty", decodedBytes.isNotEmpty())

    // Check JPEG magic numbers: 0xFF, 0xD8 (SOI - Start of Image)
    assertEquals(0xFF.toByte(), decodedBytes[0])
    assertEquals(0xD8.toByte(), decodedBytes[1])
  }

  @Test
  fun test3_emptyAndInvalidImageHandling_failsGracefully() {
    // 1. Empty stream
    val emptyStream = ByteArrayInputStream(ByteArray(0))
    val emptyResult = ImageCompressor.compressFromStream(emptyStream)
    assertTrue("Empty stream should return Error", emptyResult is Resource.Error)
    val emptyErr = (emptyResult as Resource.Error).error
    assertTrue(emptyErr is AppError.InvalidInput)

    // 2. Corrupted bytes stream
    val corruptBytes = "NotAnImageRandomStringCorruptData".toByteArray()
    val corruptStream = ByteArrayInputStream(corruptBytes)
    val corruptResult = ImageCompressor.compressFromStream(corruptStream)
    assertTrue("Corrupted stream should return Error", corruptResult is Resource.Error)

    // 3. Blank / empty Uri
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val blankUriResult = ImageCompressor.compressScreenshot(context, Uri.EMPTY)
    assertTrue("Empty Uri should return Error", blankUriResult is Resource.Error)

    // 4. Non-existent file Uri
    val fakeUri = Uri.fromFile(File(context.cacheDir, "non_existent_screenshot_${System.currentTimeMillis()}.png"))
    val nonExistentResult = ImageCompressor.compressScreenshot(context, fakeUri)
    assertTrue("Non-existent Uri should return Error", nonExistentResult is Resource.Error)
  }

  @Test
  fun test4_payloadSizeGuard_enforcesMaxSizeBytesLimitGracefully() {
    val largeBitmap = createSampleVictoryScreenshot(1920, 1080)

    // Set an artificially tiny size limit of 1 KB (1024 bytes) that a full screenshot cannot meet
    val tinyLimit = 1024
    val result = ImageCompressor.compressBitmap(largeBitmap, maxSizeBytes = tinyLimit)

    assertTrue("Expected to fail gracefully when image cannot fit size limit", result is Resource.Error)
    val error = (result as Resource.Error).error
    assertTrue(error is AppError.InvalidInput)
    val errorMsg = error.message.orEmpty()
    assertTrue(
      "Error message should mention safe size limit: $errorMsg",
      errorMsg.contains("800 KB") || errorMsg.contains("সাইজ") || errorMsg.contains("সীমা")
    )
  }

  @Test
  fun test5_endToEndUriCompression_usingRealFile() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val tempFile = File(context.cacheDir, "victory_proof_test.jpg")
    val originalBitmap = createSampleVictoryScreenshot(1280, 720)

    FileOutputStream(tempFile).use { out ->
      originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    val fileUri = Uri.fromFile(tempFile)
    val result = ImageCompressor.compressScreenshot(context, fileUri)

    assertTrue(result is Resource.Success)
    val compressionResult = (result as Resource.Success).data
    assertTrue(compressionResult.dataUri.startsWith("data:image/jpeg;base64,"))
    assertTrue(compressionResult.sizeInBytes < ImageCompressor.MAX_PAYLOAD_SIZE_BYTES)

    // Clean up
    tempFile.delete()
  }

  @Test
  fun test6_authenticatedPlayerResultSubmission_savesBase64ScreenshotAndUpdatesMatchStatus() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = FirebaseResultRepository(context, forceLocalOnly = true)

    val playerId = "winner_player_101"
    val matchId = "match_test_202"

    LocalDataStore.localUsers[playerId] = UserEntity(
      uid = playerId,
      userId = playerId,
      name = "Top Champion",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localMatches[matchId] = MatchEntity(
      matchId = matchId,
      title = "Tournament Final",
      gameType = "FREE_FIRE",
      status = MatchStatus.RUNNING.name,
    )
    LocalDataStore.localMatchPlayers[matchId] = java.util.concurrent.CopyOnWriteArrayList(listOf(
      MatchPlayerEntity(
        matchPlayerId = "mp_$playerId",
        matchId = matchId,
        userId = playerId,
        username = "Top Champion",
        status = "JOINED",
      )
    ))

    val bitmap = createSampleVictoryScreenshot(800, 600)
    val compressResult = ImageCompressor.compressBitmap(bitmap)
    assertTrue("Compression must succeed", compressResult is Resource.Success)
    val base64DataUri = (compressResult as Resource.Success).data.dataUri

    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = playerId,
      winnerName = "Top Champion",
      screenshotUrl = base64DataUri,
      notes = "Room Code: 998877",
    )

    assertTrue("Result submission must succeed: $submissionResult", submissionResult is Resource.Success)
    val resultEntity = (submissionResult as Resource.Success).data

    // Verify ResultEntity fields
    assertEquals(matchId, resultEntity.matchId)
    assertEquals(playerId, resultEntity.submittedByUserId)
    assertEquals(playerId, resultEntity.userId)
    assertEquals(playerId, resultEntity.claimedWinnerUserId)
    assertEquals(ResultStatus.PENDING_REVIEW.name, resultEntity.status)
    assertEquals(base64DataUri, resultEntity.proofScreenshotUrl)
    assertTrue(resultEntity.proofScreenshotUrl.startsWith("data:image/jpeg;base64,"))

    // Verify Match status is updated
    val updatedMatch = LocalDataStore.localMatches[matchId]
    assertNotNull(updatedMatch)
    assertEquals(MatchStatus.RESULT_SUBMITTED.name, updatedMatch?.status)

    // Verify stored in local results
    val storedResult = LocalDataStore.localResults.find { it.resultId == resultEntity.resultId }
    assertNotNull("Result must be saved in repository data store", storedResult)
    assertEquals(playerId, storedResult?.submittedByUserId)
    assertEquals(playerId, storedResult?.userId)
    assertEquals(base64DataUri, storedResult?.proofScreenshotUrl)
  }

  @Test
  fun test7_playerResultSubmission_doesNotAttemptAuditLogWrite() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = FirebaseResultRepository(context, forceLocalOnly = true)

    val playerId = "player_no_audit_log"
    val matchId = "match_no_audit_log"

    LocalDataStore.localUsers[playerId] = UserEntity(
      uid = playerId,
      userId = playerId,
      name = "Clean Player",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localMatches[matchId] = MatchEntity(
      matchId = matchId,
      status = MatchStatus.RUNNING.name,
    )
    LocalDataStore.localMatchPlayers[matchId] = java.util.concurrent.CopyOnWriteArrayList(listOf(
      MatchPlayerEntity(
        matchPlayerId = "mp_$playerId",
        matchId = matchId,
        userId = playerId,
        username = "Clean Player",
        status = "JOINED",
      )
    ))

    // Track existing audit logs before submission
    val beforeAuditLogCount = LocalDataStore.localAuditLogs.size

    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = playerId,
      winnerName = "Clean Player",
      screenshotUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      notes = "Proof submitted",
    )

    assertTrue("Submission must succeed without auditLogs write", submissionResult is Resource.Success)
    // No audit log entries should have been created on the client/player side
    assertEquals("Player result submission must not create audit log entries", beforeAuditLogCount, LocalDataStore.localAuditLogs.size)
  }

  @Test
  fun test8_databaseRules_disallowNormalPlayerAuditLogWritesAndPermitOwnResultSubmission() {
    val rulesFile = File("database.rules.json")
    val content = if (rulesFile.exists()) rulesFile.readText() else File("app/database.rules.json").readText()
    val json = JSONObject(content)
    val rules = json.getJSONObject("rules")

    // 1. Audit logs: only SUPER_ADMIN or adminUsers can write
    val auditLogs = rules.getJSONObject("auditLogs")
    val auditWriteRule = auditLogs.getString(".write")
    assertTrue("Audit logs write rule must require SUPER_ADMIN or adminUsers",
      auditWriteRule.contains("SUPER_ADMIN") || auditWriteRule.contains("adminUsers"))
    assertFalse("Normal users should not have unconditional write access to auditLogs",
      auditWriteRule.contains("auth.uid == \$uid") || auditWriteRule == "auth != null")

    // 2. Results: player can submit own result with submittedByUserId
    val results = rules.getJSONObject("results")
    val resultIdNode = results.getJSONObject("\$resultId")
    val resultWriteRule = resultIdNode.getString(".write")
    assertTrue("Result write rule must check submittedByUserId",
      resultWriteRule.contains("submittedByUserId"))
    assertTrue("Result write rule must restrict to authenticated user",
      resultWriteRule.contains("auth != null"))
  }

  @Test
  fun test9_adminCanReadAndApprovePendingResultWithScreenshot() = runBlocking {
    val adminRepository = FirebaseAdminRepository(forceLocalOnly = true)
    val testMatchId = "match_for_admin_read"
    val testWinnerId = "winner_for_admin"
    val testScreenshot = "data:image/jpeg;base64,/9j/4AAQSkZJRg_SAMPLE_DATA_URI"

    LocalDataStore.localUsers[testWinnerId] = UserEntity(
      uid = testWinnerId,
      userId = testWinnerId,
      name = "Winner Player",
      role = "PLAYER",
      status = "ACTIVE",
    )
    LocalDataStore.localWallets[testWinnerId] = com.example.domain.model.WalletEntity(
      uid = testWinnerId,
      userId = testWinnerId,
      balance = 0.0,
      availableBalance = 0L,
    )
    LocalDataStore.localMatches[testMatchId] = MatchEntity(
      matchId = testMatchId,
      status = MatchStatus.RESULT_SUBMITTED.name,
      prizePool = 100.0,
      prizeMinorUnits = 10000L,
    )

    val newResult = ResultEntity(
      resultId = "RES_ADMIN_TEST_${System.currentTimeMillis()}",
      matchId = testMatchId,
      submittedByUserId = testWinnerId,
      claimedWinnerUserId = testWinnerId,
      proofScreenshotUrl = testScreenshot,
      status = ResultStatus.PENDING_REVIEW.name,
      submittedAt = System.currentTimeMillis(),
    )
    LocalDataStore.localResults.add(newResult)

    // Admin inspects pending results
    val pendingResults = LocalDataStore.localResults.filter { it.status == ResultStatus.PENDING_REVIEW.name }
    val found = pendingResults.find { it.resultId == newResult.resultId }
    assertNotNull("Admin must be able to read the pending result", found)
    assertEquals(testScreenshot, found?.proofScreenshotUrl)

    // Admin approves result
    val approveRes = adminRepository.approveResult("admin_uid_001", found!!)
    assertTrue("Admin approve result must succeed", approveRes is Resource.Success)
    val remainingResult = LocalDataStore.localResults.find { it.resultId == newResult.resultId }
    assertNull("Temporary winning proof result must be removed upon approval", remainingResult)
    assertEquals(MatchStatus.COMPLETED.name, LocalDataStore.localMatches[testMatchId]?.status)
  }

  @Test
  fun test10_resultSubmission_containsBothSubmittedByUserIdAndUserId_fromAuthenticatedUser() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val authUid = "firebase_auth_user_777"
    val repository = FirebaseResultRepository(
      context = context,
      forceLocalOnly = true,
      authUidProvider = { authUid },
    )

    val matchId = "match_auth_test_10"
    LocalDataStore.localMatches[matchId] = MatchEntity(
      matchId = matchId,
      status = MatchStatus.RUNNING.name,
      prizeMinorUnits = 5000L,
    )
    LocalDataStore.localMatchPlayers[matchId] = java.util.concurrent.CopyOnWriteArrayList(
      listOf(
        MatchPlayerEntity(matchId = matchId, userId = authUid, slot = "PLAYER_1", status = "JOINED"),
        MatchPlayerEntity(matchId = matchId, userId = "opponent_player", slot = "PLAYER_2", status = "JOINED"),
      )
    )

    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = "ignored_param_uid", // Should use the authenticated UID!
      winnerName = "Champion",
      screenshotUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      notes = "Room: 123",
    )

    assertTrue("Result submission must succeed: $submissionResult", submissionResult is Resource.Success)
    val resultEntity = (submissionResult as Resource.Success).data

    // Both submittedByUserId and userId must be set to the authenticated user's UID
    assertEquals(authUid, resultEntity.submittedByUserId)
    assertEquals(authUid, resultEntity.userId)
    assertEquals(authUid, resultEntity.claimedWinnerUserId)

    // Stored local result must also contain both
    val storedResult = LocalDataStore.localResults.find { it.resultId == resultEntity.resultId }
    assertNotNull("Result must be saved", storedResult)
    assertEquals(authUid, storedResult?.submittedByUserId)
    assertEquals(authUid, storedResult?.userId)
  }

  @Test
  fun test11_resultSubmission_failsSafely_whenAuthUserIsNull() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = FirebaseResultRepository(
      context = context,
      forceLocalOnly = true,
      authUidProvider = { null },
    )

    val matchId = "match_unauthenticated"
    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = "some_winner_id",
      winnerName = "Player",
      screenshotUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      notes = "Proof",
    )

    assertTrue("Submission must fail with error when user is not authenticated", submissionResult is Resource.Error)
    val error = (submissionResult as Resource.Error).error
    assertTrue("Error should be authentication error: ${error.message}", error.message.contains("authenticated", ignoreCase = true))
  }

  @Test
  fun test12_resultSubmission_failsSafely_whenAuthUserUidIsBlank() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repository = FirebaseResultRepository(
      context = context,
      forceLocalOnly = true,
      authUidProvider = { "   " },
    )

    val matchId = "match_blank_uid"
    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = "some_winner_id",
      winnerName = "Player",
      screenshotUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      notes = "Proof",
    )

    assertTrue("Submission must fail with error when UID is blank", submissionResult is Resource.Error)
    val error = (submissionResult as Resource.Error).error
    assertTrue("Error should be authentication error: ${error.message}", error.message.contains("authenticated", ignoreCase = true))
  }

  @Test
  fun test13_buildSubmitResultUpdates_hasNoAncestorDescendantConflict() {
    val resultId = "RES_999999_match_777"
    val matchId = "match_777"
    val authUid = "auth_player_99"
    val timestamp = 1700000000000L

    val entity = ResultEntity(
      resultId = resultId,
      matchId = matchId,
      submittedByUserId = authUid,
      userId = authUid,
      claimedWinnerUserId = authUid,
      proofScreenshotUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      status = ResultStatus.PENDING_REVIEW.name,
      matchNumber = "M-777",
      gameType = "FREE_FIRE",
      createdAt = timestamp,
    )

    val updates = FirebaseResultRepository.buildSubmitResultUpdates(
      resultEntity = entity,
      matchId = matchId,
      effectiveUid = authUid,
      timestamp = timestamp,
    )

    // 1. Verify exact result root path is used
    val expectedResultPath = "${FirebaseConfig.NODE_RESULTS}/$matchId/$authUid"
    assertTrue("Updates must contain the result root path", updates.containsKey(expectedResultPath))
    assertSame("Result object must be placed at the result root path", entity, updates[expectedResultPath])

    // 2. Verify no child subpaths of the result exist in updates (prevents ancestor/descendant conflict)
    val childPrefix = "$expectedResultPath/"
    val conflictingChildKeys = updates.keys.filter { it.startsWith(childPrefix) }
    assertTrue(
      "No descendant paths of $expectedResultPath may exist in updates, but found: $conflictingChildKeys",
      conflictingChildKeys.isEmpty()
    )

    // 3. General ancestor/descendant conflict check across ALL keys
    val normalizedPaths = updates.keys.map { it.trim().trimStart('/') }
    for (p1 in normalizedPaths) {
      for (p2 in normalizedPaths) {
        if (p1 != p2) {
          assertFalse(
            "Firebase update path conflict detected: '$p1' is an ancestor of '$p2'",
            p2.startsWith("$p1/")
          )
        }
      }
    }

    // 4. Verify all required fields inside the result object are preserved
    val resultInMap = updates[expectedResultPath] as ResultEntity
    assertEquals(authUid, resultInMap.submittedByUserId)
    assertEquals(authUid, resultInMap.userId)
    assertEquals(authUid, resultInMap.claimedWinnerUserId)
    assertEquals(matchId, resultInMap.matchId)
    assertEquals("M-777", resultInMap.matchNumber)
    assertEquals("FREE_FIRE", resultInMap.gameType)
    assertEquals(ResultStatus.PENDING_REVIEW.name, resultInMap.status)
    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", resultInMap.proofScreenshotUrl)
    assertEquals(timestamp, resultInMap.createdAt)
  }

  @Test
  fun test14_submitMatchResult_populatesAllPreservedFields_inSavedEntity() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val authUid = "firebase_auth_user_888"
    val repository = FirebaseResultRepository(
      context = context,
      forceLocalOnly = true,
      authUidProvider = { authUid },
    )

    val matchId = "match_full_fields_14"
    LocalDataStore.localMatches[matchId] = MatchEntity(
      matchId = matchId,
      matchNumber = "M-888",
      gameType = "LUDO",
      status = MatchStatus.RUNNING.name,
      prizeMinorUnits = 6000L,
    )
    LocalDataStore.localMatchPlayers[matchId] = java.util.concurrent.CopyOnWriteArrayList(
      listOf(
        MatchPlayerEntity(matchId = matchId, userId = authUid, slot = "PLAYER_1", status = "JOINED"),
        MatchPlayerEntity(matchId = matchId, userId = "opponent_player", slot = "PLAYER_2", status = "JOINED"),
      )
    )

    val submissionResult = repository.submitMatchResult(
      matchId = matchId,
      winnerId = authUid,
      winnerName = "Pro Gamer",
      screenshotUrl = "data:image/jpeg;base64,DATA_URI_STRING",
      notes = "Room: 456",
    )

    assertTrue("Result submission must succeed", submissionResult is Resource.Success)
    val entity = (submissionResult as Resource.Success).data

    assertEquals(authUid, entity.submittedByUserId)
    assertEquals(authUid, entity.userId)
    assertEquals(authUid, entity.claimedWinnerUserId)
    assertEquals(matchId, entity.matchId)
    assertEquals("M-888", entity.matchNumber)
    assertEquals("LUDO", entity.gameType)
    assertEquals("data:image/jpeg;base64,DATA_URI_STRING", entity.proofScreenshotUrl)
    assertEquals(ResultStatus.PENDING_REVIEW.name, entity.status)
    assertTrue("createdAt must be positive", entity.createdAt > 0L)
  }
}
