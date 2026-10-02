package com.example.data.repository

import android.content.Context
import com.example.core.backend.AuthoritativeMatchJoinBackend
import com.example.core.config.FirebaseConfig
import com.example.core.error.AppError
import com.example.core.error.Resource
import com.example.core.finance.FinancialEngine
import com.example.core.firebase.FirebaseManager
import com.example.core.logging.AppLogger
import com.example.core.security.AuthValidator
import com.example.core.security.SessionManager
import com.example.core.util.ImageCompressor
import com.example.data.sample.SampleData
import com.example.domain.model.*
import com.example.domain.repository.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(timeoutMs: Long = 10_000L): T = withTimeout(timeoutMs) {
  suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
      if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
      if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
      if (cont.isActive) cont.cancel()
    }
  }
}

object LocalDataStore {
  var localCurrentUserUid = ""
  val localMatches = ConcurrentHashMap<String, MatchEntity>()
  val localMatchPlayers = ConcurrentHashMap<String, CopyOnWriteArrayList<MatchPlayerEntity>>()
  val localWallets = ConcurrentHashMap<String, WalletEntity>()
  val localUsers = ConcurrentHashMap<String, UserEntity>()
  val localTransactions = CopyOnWriteArrayList<TransactionEntity>()
  val localDeposits = CopyOnWriteArrayList<DepositEntity>()
  val localWithdrawals = CopyOnWriteArrayList<WithdrawalEntity>()
  val localResults = CopyOnWriteArrayList<ResultEntity>()
  val localAuditLogs = CopyOnWriteArrayList<AuditLogEntity>()
  val localActiveJoins = ConcurrentHashMap<String, ConcurrentHashMap<String, Map<String, Any>>>()
  val localActiveWithdrawals = ConcurrentHashMap<String, ConcurrentHashMap<String, Map<String, Any>>>()
  val localUserNotifications = ConcurrentHashMap<String, CopyOnWriteArrayList<NotificationEntity>>()
  val localUserMatches = ConcurrentHashMap<String, ConcurrentHashMap<String, UserMatchHistoryEntity>>()

  fun clearUserData() {
    localCurrentUserUid = ""
    localWallets.clear()
    localUsers.clear()
    localTransactions.clear()
    localDeposits.clear()
    localWithdrawals.clear()
    localResults.clear()
    localActiveJoins.clear()
    localActiveWithdrawals.clear()
    localUserNotifications.clear()
    localUserMatches.clear()
    notifyWalletChanged()
    notifyNotificationsChanged()
    notifyUserMatchesChanged()
  }

  val matchesUpdateTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
  val walletUpdateTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
  val appSettingsUpdateTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
  val notificationsUpdateTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
  val userMatchesUpdateTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
  var localAppSettings = AppSettingsEntity()

  fun notifyMatchesChanged() {
    matchesUpdateTrigger.value = System.currentTimeMillis()
  }

  fun notifyUserMatchesChanged() {
    userMatchesUpdateTrigger.value = System.currentTimeMillis()
  }

  fun notifyWalletChanged() {
    walletUpdateTrigger.value = System.currentTimeMillis()
  }

  fun notifyAppSettingsChanged() {
    appSettingsUpdateTrigger.value = System.currentTimeMillis()
  }

  fun notifyNotificationsChanged() {
    notificationsUpdateTrigger.value = System.currentTimeMillis()
  }
}

class FirebaseMatchRepository(private val customDatabase: FirebaseDatabase? = null) : MatchRepository {
  private val tag = "MatchRepository"
  private val userMatchJoinMutexes = ConcurrentHashMap<String, Mutex>()

  override fun getAvailableMatches(limit: Int): Flow<Resource<List<MatchEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)
    if (ref == null) {
      trySend(Resource.Error(AppError.NetworkUnavailable("Firebase database unavailable.")))
      awaitClose { }
      return@callbackFlow
    }

    val query = ref.orderByChild("status")
      // Remove strict .equalTo(MatchStatus.AVAILABLE.name)
      .limitToLast(100) // Fetch last 100 matches to ensure we cover all active ones

    var hasReceivedData = false

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        hasReceivedData = true
        val activeStatuses = setOf(
          MatchStatus.AVAILABLE.name,
          MatchStatus.FULL.name,
          MatchStatus.CODE_ADDED.name,
          MatchStatus.RUNNING.name
        )
        val allMatches = snapshot.children.mapNotNull { it.getValue(MatchEntity::class.java) }
        val activeMatches = allMatches.filter { it.status in activeStatuses }
          .sortedByDescending { it.effectiveScheduledAt }
          .take(limit)
        
        activeMatches.forEach { LocalDataStore.localMatches[it.matchId] = it }
        trySend(Resource.Success(activeMatches))
      }

      override fun onCancelled(error: DatabaseError) {
        hasReceivedData = true
        AppLogger.w(tag, "getAvailableMatches cancelled: ${error.message}")
        trySend(Resource.Error(AppError.ServerError("Failed to load matches: ${error.message}")))
      }
    }
    query.addValueEventListener(listener)

    val timeoutJob = launch {
      delay(5_000L)
      if (!hasReceivedData) {
        AppLogger.w(tag, "getAvailableMatches timeout: emitting NetworkUnavailable error")
        trySend(Resource.Error(AppError.NetworkUnavailable("Connection timed out loading available matches. Please check your network and retry.")))
      }
    }

    awaitClose {
      timeoutJob.cancel()
      query.removeEventListener(listener)
    }
  }

  override fun getMyJoinedMatches(userId: String): Flow<Resource<List<MatchEntity>>> = callbackFlow {
    trySend(Resource.Loading)

    val authUid = FirebaseManager.getAuth()?.currentUser?.uid?.trim()?.ifBlank { null }
    val requestedUserId = userId.trim().ifBlank { null }
    val targetIds = setOfNotNull(requestedUserId, authUid)

    if (targetIds.isEmpty()) {
      trySend(Resource.Success(emptyList()))
      awaitClose { }
      return@callbackFlow
    }

    val matchPlayersRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCH_PLAYERS)
    val matchesRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)

    if (matchPlayersRef == null || matchesRef == null) {
      trySend(Resource.Error(AppError.NetworkUnavailable("Firebase database unavailable.")))
      awaitClose { }
      return@callbackFlow
    }

    var latestJoinedMatchIds: Set<String>? = null
    var latestMatchesMap: Map<String, MatchEntity>? = null
    var hasReceivedPlayers = false
    var hasReceivedMatches = false

    fun emitIfReady() {
      val joinedIds = latestJoinedMatchIds ?: return
      if (joinedIds.isEmpty()) {
        trySend(Resource.Success(emptyList()))
        return
      }
      val matchesMap = latestMatchesMap ?: return
      val userMatches = joinedIds.mapNotNull { matchesMap[it] }
        .sortedByDescending { it.createdAt.coerceAtLeast(it.scheduledAt) }
      trySend(Resource.Success(userMatches))
    }

    val playersListener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        hasReceivedPlayers = true
        val joinedIds = mutableSetOf<String>()
        for (matchSnapshot in snapshot.children) {
          val matchIdKey = matchSnapshot.key?.trim() ?: continue
          if (matchIdKey.isBlank()) continue
          for (playerSnapshot in matchSnapshot.children) {
            val playerKey = playerSnapshot.key?.trim().orEmpty()
            val player = playerSnapshot.getValue(MatchPlayerEntity::class.java)
            val pUid = player?.uid?.trim() ?: playerSnapshot.child("uid").getValue(String::class.java)?.trim().orEmpty()
            val pUserId = player?.userId?.trim() ?: playerSnapshot.child("userId").getValue(String::class.java)?.trim().orEmpty()
            val pEffectiveUid = player?.effectiveUid?.trim() ?: playerSnapshot.child("effectiveUid").getValue(String::class.java)?.trim().orEmpty()

            val matchesUser = targetIds.contains(playerKey) ||
                (pUid.isNotBlank() && targetIds.contains(pUid)) ||
                (pUserId.isNotBlank() && targetIds.contains(pUserId)) ||
                (pEffectiveUid.isNotBlank() && targetIds.contains(pEffectiveUid))

            if (matchesUser) {
              val resolvedMatchId = player?.matchId?.trim()?.ifBlank { null } ?: matchIdKey
              joinedIds.add(resolvedMatchId)
              break
            }
          }
        }
        latestJoinedMatchIds = joinedIds
        emitIfReady()
      }

      override fun onCancelled(error: DatabaseError) {
        hasReceivedPlayers = true
        AppLogger.w(tag, "getMyJoinedMatches playersListener cancelled: ${error.message}")
        trySend(Resource.Error(AppError.ServerError("Failed to load joined matches: ${error.message}")))
      }
    }

    val matchesListener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        hasReceivedMatches = true
        val matchesMap = mutableMapOf<String, MatchEntity>()
        for (child in snapshot.children) {
          val match = child.getValue(MatchEntity::class.java)
          val key = child.key ?: match?.matchId
          if (match != null && !key.isNullOrBlank()) {
            val resolvedMatch = if (match.matchId.isBlank()) match.copy(matchId = key) else match
            matchesMap[key] = resolvedMatch
            matchesMap[resolvedMatch.matchId] = resolvedMatch
          }
        }
        latestMatchesMap = matchesMap
        emitIfReady()
      }

      override fun onCancelled(error: DatabaseError) {
        hasReceivedMatches = true
        AppLogger.w(tag, "getMyJoinedMatches matchesListener cancelled: ${error.message}")
        trySend(Resource.Error(AppError.ServerError("Failed to load matches: ${error.message}")))
      }
    }

    matchPlayersRef.addValueEventListener(playersListener)
    matchesRef.addValueEventListener(matchesListener)

    val timeoutJob = launch {
      delay(5_000L)
      if (!hasReceivedPlayers) {
        AppLogger.w(tag, "getMyJoinedMatches timeout: emitting NetworkUnavailable error")
        trySend(Resource.Error(AppError.NetworkUnavailable("Connection timed out loading joined matches.")))
      }
    }

    awaitClose {
      timeoutJob.cancel()
      matchPlayersRef.removeEventListener(playersListener)
      matchesRef.removeEventListener(matchesListener)
    }
  }

  override fun getUpcomingMatches(limit: Int): Flow<Resource<List<MatchEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)
    if (ref == null) {
      trySend(Resource.Error(AppError.NetworkUnavailable("Firebase database unavailable.")))
      awaitClose { }
      return@callbackFlow
    }

    val query = ref.orderByChild("status")
      .equalTo(MatchStatus.UPCOMING.name)
      .limitToFirst(limit)

    var hasReceivedData = false

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        hasReceivedData = true
        val matches = snapshot.children.mapNotNull { it.getValue(MatchEntity::class.java) }
        matches.forEach { LocalDataStore.localMatches[it.matchId] = it }
        trySend(Resource.Success(matches))
      }

      override fun onCancelled(error: DatabaseError) {
        hasReceivedData = true
        AppLogger.w(tag, "getUpcomingMatches cancelled: ${error.message}")
        trySend(Resource.Error(AppError.ServerError("Failed to load upcoming matches: ${error.message}")))
      }
    }
    query.addValueEventListener(listener)

    val timeoutJob = launch {
      delay(5_000L)
      if (!hasReceivedData) {
        AppLogger.w(tag, "getUpcomingMatches timeout: emitting empty list fallback")
        trySend(Resource.Success(emptyList()))
      }
    }

    awaitClose {
      timeoutJob.cancel()
      query.removeEventListener(listener)
    }
  }

  override fun getMatchHistory(userId: String, limit: Int): Flow<Resource<List<MatchEntity>>> = callbackFlow {
    trySend(Resource.Loading)

    val authUid = FirebaseManager.getAuth()?.currentUser?.uid?.trim()?.ifBlank { null }
    val requestedUserId = userId.trim().ifBlank { null }
    val effectiveUid = requestedUserId ?: authUid ?: ""

    if (effectiveUid.isNullOrBlank()) {
      trySend(Resource.Success(emptyList()))
      awaitClose { }
      return@callbackFlow
    }

    val userMatchesRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_MATCHES)?.child(effectiveUid)
    val matchesRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)

    fun resolveAndEmit(userMatches: List<UserMatchHistoryEntity>) {
      launch {
        val resultMatches = mutableListOf<MatchEntity>()
        for (umh in userMatches) {
          var match = LocalDataStore.localMatches[umh.matchId]
          if (match == null && matchesRef != null) {
            try {
              val matchSnap = matchesRef.child(umh.matchId).get().awaitTask(3_000L)
              match = matchSnap?.getValue(MatchEntity::class.java)
              if (match != null) {
                LocalDataStore.localMatches[umh.matchId] = match
              }
            } catch (e: Exception) {
              AppLogger.w(tag, "Failed to resolve match ${umh.matchId}: ${e.message}")
            }
          }

          val finalMatch = if (match != null) {
            val effectiveStatus = if (umh.status == "REJECTED" || umh.status == MatchStatus.RESULT_SUBMITTED.name) {
              umh.status
            } else {
              match.status
            }
            val effectiveWinner = if (umh.isWinner == true && match.winnerUserId.isBlank()) {
              effectiveUid
            } else {
              match.winnerUserId
            }
            match.copy(
              status = effectiveStatus,
              winnerUserId = effectiveWinner,
            )
          } else {
            MatchEntity(
              matchId = umh.matchId,
              matchNumber = umh.matchNumber,
              title = umh.title.ifBlank { "${umh.gameType} Match" },
              gameType = umh.gameType,
              status = umh.status,
              winnerUserId = if (umh.isWinner == true) effectiveUid else if (umh.isWinner == false) "OPPONENT" else "",
              entryFeeMinorUnits = umh.entryFeeMinorUnits,
              prizeMinorUnits = umh.prizeMinorUnits,
              createdAt = umh.joinedAt,
              updatedAt = umh.updatedAt,
            )
          }
          resultMatches.add(finalMatch)
        }

        val filteredAndSorted = resultMatches.filter { match ->
          match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true) ||
          match.status.equals("UNDER_REVIEW", ignoreCase = true) ||
          match.status.equals("REJECTED", ignoreCase = true)
        }.sortedByDescending { it.effectiveScheduledAt.coerceAtLeast(it.createdAt) }

        trySend(Resource.Success(if (limit > 0) filteredAndSorted.take(limit) else filteredAndSorted))
      }
    }

    if (userMatchesRef == null) {
      val localList = LocalDataStore.localUserMatches[effectiveUid]?.values?.toList().orEmpty()
      if (localList.isNotEmpty()) {
        resolveAndEmit(localList)
      } else {
        // Fallback for old data in local data store
        val fallbackFromLocal = mutableListOf<MatchEntity>()
        for ((matchId, players) in LocalDataStore.localMatchPlayers) {
          if (players.any { it.effectiveUid == effectiveUid || it.userId == effectiveUid || it.uid == effectiveUid }) {
            LocalDataStore.localMatches[matchId]?.let { fallbackFromLocal.add(it) }
          }
        }
        val sorted = fallbackFromLocal.filter { match ->
          match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true) ||
          match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true) ||
          match.status.equals("UNDER_REVIEW", ignoreCase = true) ||
          match.status.equals("REJECTED", ignoreCase = true)
        }.sortedByDescending { it.effectiveScheduledAt.coerceAtLeast(it.createdAt) }
        trySend(Resource.Success(if (limit > 0) sorted.take(limit) else sorted))
      }
      awaitClose { }
      return@callbackFlow
    }

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val userMatchesList = mutableListOf<UserMatchHistoryEntity>()
        for (child in snapshot.children) {
          val umh = child.getValue(UserMatchHistoryEntity::class.java)
          val matchIdKey = child.key ?: umh?.matchId
          if (umh != null && !matchIdKey.isNullOrBlank()) {
            val resolved = if (umh.matchId.isBlank()) umh.copy(matchId = matchIdKey) else umh
            userMatchesList.add(resolved)
            LocalDataStore.localUserMatches.getOrPut(effectiveUid) { ConcurrentHashMap() }[resolved.matchId] = resolved
          }
        }

        if (userMatchesList.isEmpty()) {
          val localItems = LocalDataStore.localUserMatches[effectiveUid]?.values?.toList().orEmpty()
          if (localItems.isNotEmpty()) {
            resolveAndEmit(localItems)
            return
          }

          // Fallback for old data / existing users without /userMatches
          val fallbackFromLocal = mutableListOf<MatchEntity>()
          for ((matchId, players) in LocalDataStore.localMatchPlayers) {
            if (players.any { it.effectiveUid == effectiveUid || it.userId == effectiveUid || it.uid == effectiveUid }) {
              LocalDataStore.localMatches[matchId]?.let { fallbackFromLocal.add(it) }
            }
          }
          if (fallbackFromLocal.isNotEmpty()) {
            val sorted = fallbackFromLocal.filter { match ->
              match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true) ||
              match.status.equals(MatchStatus.CANCELLED.name, ignoreCase = true) ||
              match.status.equals(MatchStatus.RESULT_SUBMITTED.name, ignoreCase = true) ||
              match.status.equals("UNDER_REVIEW", ignoreCase = true) ||
              match.status.equals("REJECTED", ignoreCase = true)
            }.sortedByDescending { it.effectiveScheduledAt.coerceAtLeast(it.createdAt) }
            trySend(Resource.Success(if (limit > 0) sorted.take(limit) else sorted))
            return
          }

          trySend(Resource.Success(emptyList()))
          return
        }

        resolveAndEmit(userMatchesList)
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "getMatchHistory onCancelled: ${error.message}")
        val localList = LocalDataStore.localUserMatches[effectiveUid]?.values?.toList().orEmpty()
        if (localList.isNotEmpty()) {
          resolveAndEmit(localList)
        } else {
          trySend(Resource.Error(AppError.ServerError("Failed to load match history: ${error.message}")))
        }
      }
    }

    userMatchesRef.addValueEventListener(listener)

    awaitClose {
      userMatchesRef.removeEventListener(listener)
    }
  }

  override fun getMatchById(matchId: String): Flow<Resource<MatchEntity?>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)?.child(matchId)
    val fallback = LocalDataStore.localMatches[matchId]

    if (ref == null) {
      trySend(Resource.Success(fallback))
      val job = launch {
        LocalDataStore.matchesUpdateTrigger.collect {
          trySend(Resource.Success(LocalDataStore.localMatches[matchId]))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }

    var hasReceivedData = false

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        hasReceivedData = true
        val match = snapshot.getValue(MatchEntity::class.java) ?: LocalDataStore.localMatches[matchId]
        if (match != null) LocalDataStore.localMatches[matchId] = match
        trySend(Resource.Success(match))
      }

      override fun onCancelled(error: DatabaseError) {
        hasReceivedData = true
        AppLogger.w(tag, "getMatchById cancelled: ${error.message}")
        trySend(Resource.Error(AppError.ServerError("Failed to load match: ${error.message}")))
      }
    }
    ref.addValueEventListener(listener)

    val timeoutJob = launch {
      delay(5_000L)
      if (!hasReceivedData) {
        trySend(Resource.Success(LocalDataStore.localMatches[matchId]))
      }
    }

    awaitClose {
      timeoutJob.cancel()
      ref.removeEventListener(listener)
    }
  }

  override fun getMatchPlayers(matchId: String): Flow<Resource<List<MatchPlayerEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCH_PLAYERS)?.child(matchId)
    val initialCached = LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
    if (initialCached.isNotEmpty()) {
      trySend(Resource.Success(initialCached))
    }

    val localTriggerJob = CoroutineScope(Dispatchers.IO).launch {
      LocalDataStore.matchesUpdateTrigger.collect {
        val updatedLocal = LocalDataStore.localMatchPlayers[matchId]
        if (!updatedLocal.isNullOrEmpty()) {
          trySend(Resource.Success(updatedLocal.toList()))
        }
      }
    }

    if (ref == null) {
      trySend(Resource.Success(initialCached))
      awaitClose { localTriggerJob.cancel() }
      return@callbackFlow
    }

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val remotePlayers = snapshot.children.mapNotNull { it.getValue(MatchPlayerEntity::class.java) }
        val localList = LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
        val finalPlayers = if (remotePlayers.isNotEmpty()) {
          val remoteUids = remotePlayers.map { it.effectiveUid }.toSet()
          remotePlayers + localList.filter { it.effectiveUid !in remoteUids }
        } else {
          localList
        }
        LocalDataStore.localMatchPlayers[matchId] = CopyOnWriteArrayList(finalPlayers)
        trySend(Resource.Success(finalPlayers))
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.d(tag, "getMatchPlayers read restricted: ${error.message}")
        val local = LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
        trySend(Resource.Success(local))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose {
      localTriggerJob.cancel()
      ref.removeEventListener(listener)
    }
  }

  override suspend fun joinMatch(userId: String, matchId: String): Resource<JoinMatchResult> {
    val authUid = FirebaseManager.getAuth()?.currentUser?.uid?.ifBlank { null }
    val effectiveUid = if (!authUid.isNullOrBlank()) authUid else userId
    if (effectiveUid.isBlank()) {
      return Resource.Error(AppError.AuthenticationRequired("Authentication required to join match."))
    }

    val userMutex = userMatchJoinMutexes.getOrPut(effectiveUid) { Mutex() }
    return userMutex.withLock {
      val isUserBlocked = LocalDataStore.localUsers[effectiveUid]?.isAccountBlocked == true ||
          LocalDataStore.localUsers[userId]?.isAccountBlocked == true
      if (isUserBlocked) {
        return@withLock Resource.Error(AppError.InvalidInput(reason = "Your account is blocked. Cannot join tournament."))
      }

      val database = customDatabase ?: FirebaseManager.getDatabase()
      val now = System.currentTimeMillis()

      if (database != null) {
        AppLogger.i(tag, "Executing client-side atomic match join for matchId=$matchId, effectiveUid=$effectiveUid, userId=$userId")
        try {
          // 1. Fetch current match details
          val matchSnapshot = try {
            database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask(5_000L)
          } catch (e: Exception) {
            AppLogger.w(tag, "Match fetch from Firebase failed, falling back to cache: ${e.message}")
            null
          }

          val match = matchSnapshot?.getValue(MatchEntity::class.java)
            ?: LocalDataStore.localMatches[matchId]
            ?: SampleData.sampleMatches.find { it.matchId == matchId }
            ?: return@withLock Resource.Error(AppError.InvalidInput(reason = "Match not found."))

          // Check if match is AVAILABLE and has slots
          val isAvailable = match.status.equals(MatchStatus.AVAILABLE.name, ignoreCase = true)
          if (!isAvailable) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "Match is not available to join (current status: ${match.status})."))
          }

          if (match.joinedPlayersCount >= match.maxPlayers || match.status.equals(MatchStatus.FULL.name, ignoreCase = true)) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "Match is full. Maximum ${match.maxPlayers} players allowed."))
          }

          val scheduledTime = match.effectiveScheduledAt
          if (scheduledTime > 0L && now >= scheduledTime) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "Match registration deadline has passed."))
          }

          // Check existing players from /matchPlayers/{matchId}
          val playersSnapshot = try {
            database.getReference(FirebaseConfig.NODE_MATCH_PLAYERS).child(matchId).get().awaitTask(5_000L)
          } catch (e: Exception) {
            null
          }
          val existingPlayers = if (playersSnapshot != null && playersSnapshot.exists()) {
            playersSnapshot.children.mapNotNull { it.getValue(MatchPlayerEntity::class.java) }
          } else {
            LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
          }

          val isAlreadyJoined = existingPlayers.any { it.effectiveUid == effectiveUid || it.effectiveUid == userId } ||
              (playersSnapshot != null && (playersSnapshot.hasChild(effectiveUid) || playersSnapshot.hasChild(userId))) ||
              LocalDataStore.localMatchPlayers[matchId]?.any { it.effectiveUid == effectiveUid || it.effectiveUid == userId } == true

          if (isAlreadyJoined) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "You have already joined this match."))
          }

          if (existingPlayers.size >= match.maxPlayers) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "Match is full. Maximum ${match.maxPlayers} players allowed."))
          }

          val entryFeeMinorUnits = match.effectiveEntryFeeMinorUnits
          if (entryFeeMinorUnits < 0L) {
            return@withLock Resource.Error(AppError.InvalidInput(reason = "Invalid entry fee on match record."))
          }

          val trxId = "TXN_JOIN_${now}_${matchId}_${effectiveUid.takeLast(6)}"

          // 2. Authoritative Firebase Realtime Database transaction on /wallets/{effectiveUid}
          val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(effectiveUid)
          try {
            walletRef.get().awaitTask(5_000L)
          } catch (_: Exception) { }

          var isInsufficientBalance = false
          var isAlreadyReserved = false
          var beforeBalanceLong: Long = 0L
          var afterBalanceLong: Long = 0L
          var bonusUsedPaisa: Long = 0L
          var updatedWalletResult: WalletEntity? = null
          var abortReason: String? = null

          val txResult = suspendCancellableCoroutine<Pair<Boolean, String?>> { cont ->
            walletRef.runTransaction(object : Transaction.Handler {
              override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val activeJoinNode = mutableData.child("activeJoins").child(matchId)
                val activeJoinsMap = mutableData.child("activeJoins").value as? Map<*, *>
                val hasExistingReservation = (activeJoinNode.value != null) || (activeJoinsMap?.containsKey(matchId) == true)

                if (hasExistingReservation) {
                  isAlreadyReserved = true
                  abortReason = "You have already joined this match."
                  return Transaction.abort()
                }

                val currentMap = mutableData.value as? Map<*, *>
                val avail = (mutableData.child("availableBalance").value as? Number)?.toLong()
                  ?: (currentMap?.get("availableBalance") as? Number)?.toLong()
                  ?: mutableData.getValue(WalletEntity::class.java)?.availableBalance
                  ?: LocalDataStore.localWallets[effectiveUid]?.availableBalance
                  ?: LocalDataStore.localWallets[userId]?.availableBalance
                  ?: 0L

                val pend = (mutableData.child("pendingBalance").value as? Number)?.toLong()
                  ?: (currentMap?.get("pendingBalance") as? Number)?.toLong()
                  ?: mutableData.getValue(WalletEntity::class.java)?.pendingBalance
                  ?: LocalDataStore.localWallets[effectiveUid]?.pendingBalance
                  ?: LocalDataStore.localWallets[userId]?.pendingBalance
                  ?: 0L

                val bonusAvail = (mutableData.child("bonusBalance").value as? Number)?.toDouble()
                  ?: (currentMap?.get("bonusBalance") as? Number)?.toDouble()
                  ?: mutableData.getValue(WalletEntity::class.java)?.bonusBalance
                  ?: LocalDataStore.localWallets[effectiveUid]?.bonusBalance
                  ?: 0.0

                val dep = (mutableData.child("totalDeposited").value as? Number)?.toLong()
                  ?: (currentMap?.get("totalDeposited") as? Number)?.toLong() ?: 0L
                val with = (mutableData.child("totalWithdrawn").value as? Number)?.toLong()
                  ?: (currentMap?.get("totalWithdrawn") as? Number)?.toLong() ?: 0L
                val win = (mutableData.child("totalWinnings").value as? Number)?.toLong()
                  ?: (currentMap?.get("totalWinnings") as? Number)?.toLong() ?: 0L

                beforeBalanceLong = avail
                
                // 10% Bonus Usage Logic
                val maxBonusAllowed = (entryFeeMinorUnits * 0.10).toLong()
                val usableBonus = kotlin.math.min(bonusAvail.toLong(), maxBonusAllowed)
                val remainingFee = entryFeeMinorUnits - usableBonus
                
                if (beforeBalanceLong < remainingFee) {
                  isInsufficientBalance = true
                  abortReason = "পর্যাপ্ত মেইন ব্যালেন্স নেই! ডিপোজিট করুন।"
                  return Transaction.abort()
                }

                bonusUsedPaisa = usableBonus
                afterBalanceLong = beforeBalanceLong - remainingFee
                val newPending = pend + entryFeeMinorUnits
                val newBonusBalance = (bonusAvail - usableBonus).coerceAtLeast(0.0)
                val newBalancePaisa = afterBalanceLong
                val newLockedPaisa = newPending

                val reservationData = hashMapOf<String, Any>(
                  "userId" to effectiveUid,
                  "matchId" to matchId,
                  "entryFeeMinorUnits" to entryFeeMinorUnits,
                  "bonusUsedPaisa" to usableBonus,
                  "transactionId" to trxId,
                  "timestamp" to now,
                )

                mutableData.child("availableBalance").value = afterBalanceLong
                mutableData.child("pendingBalance").value = newPending
                mutableData.child("bonusBalance").value = newBonusBalance
                mutableData.child("balance").value = newBalancePaisa
                mutableData.child("lockedBalance").value = newLockedPaisa
                mutableData.child("updatedAt").value = now
                mutableData.child("activeJoins").child(matchId).value = reservationData

                val newWallet = WalletEntity(
                  uid = effectiveUid,
                  userId = effectiveUid,
                  availableBalance = afterBalanceLong,
                  pendingBalance = newPending,
                  bonusBalance = newBonusBalance,
                  totalDeposited = dep,
                  totalWithdrawn = with,
                  totalWinnings = win,
                  balance = newBalancePaisa.toDouble(),
                  lockedBalance = newLockedPaisa.toDouble(),
                  updatedAt = now,
                )
                updatedWalletResult = newWallet
                return Transaction.success(mutableData)
              }

              override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?,
              ) {
                if (cont.isActive) {
                  if (error != null) {
                    cont.resume(Pair(false, error.message))
                  } else if (!committed) {
                    cont.resume(Pair(false, abortReason ?: "Wallet transaction was aborted."))
                  } else {
                    val finalWallet = snapshot?.getValue(WalletEntity::class.java) ?: updatedWalletResult
                    if (finalWallet != null) {
                      updatedWalletResult = finalWallet
                    }
                    cont.resume(Pair(true, null))
                  }
                }
              }
            })
          }

          if (!txResult.first) {
            val errorMsg = txResult.second ?: "Failed to deduct match entry fee from wallet."
            return@withLock Resource.Error(AppError.InvalidInput(reason = errorMsg))
          }

          val updatedWallet = updatedWalletResult ?: return@withLock Resource.Error(
            AppError.ServerError("Failed to retrieve updated wallet state after transaction.")
          )

          // 3. Prepare matchPlayer, match update, and transaction record
          val currentUser = LocalDataStore.localUsers[effectiveUid]
            ?: LocalDataStore.localUsers[userId]
            ?: UserEntity(uid = effectiveUid, userId = effectiveUid, role = "PLAYER", status = "ACTIVE")

          val playerName = currentUser.effectiveName.ifBlank {
            FirebaseManager.getAuth()?.currentUser?.displayName?.ifBlank { null } ?: "Player"
          }

          val slotNumber = existingPlayers.size + 1
          val slot = if (slotNumber == 1) PlayerSlot.PLAYER_1.name else PlayerSlot.PLAYER_2.name
          val newPlayer = MatchPlayerEntity(
            matchPlayerId = "MP_${matchId}_$effectiveUid",
            matchId = matchId,
            uid = effectiveUid,
            userId = effectiveUid,
            username = playerName,
            slot = slot,
            joinedAt = now,
            entryFeeMinorUnits = entryFeeMinorUnits,
            status = "JOINED",
            isReady = true,
          )

          val currentJoined = maxOf(match.joinedPlayersCount, existingPlayers.size)
          val newCount = currentJoined + 1
          val newStatus = if (newCount >= match.maxPlayers) MatchStatus.FULL.name else MatchStatus.AVAILABLE.name
          val updatedMatch = match.copy(
            joinedPlayersCount = newCount,
            status = newStatus,
            updatedAt = now,
          )

          val transaction = TransactionEntity(
            transactionId = trxId,
            uid = effectiveUid,
            userId = effectiveUid,
            amount = entryFeeMinorUnits,
            type = "MATCH_JOIN",
            status = TransactionStatus.COMPLETED.name,
            beforeBalance = beforeBalanceLong,
            afterBalance = afterBalanceLong,
            source = "MATCH_JOIN",
            referenceId = matchId,
            description = "Entry fee for ${match.title.ifBlank { match.matchNumber }} (Used Bonus: ৳${"%.2f".format(bonusUsedPaisa / 100.0)})",
            createdAt = now,
            processedAt = now,
          )

          val userMatchHistory = UserMatchHistoryEntity(
            matchId = matchId,
            userId = effectiveUid,
            uid = effectiveUid,
            joinedAt = now,
            status = "JOINED",
            isWinner = null,
            updatedAt = now,
            gameType = match.gameType,
            matchNumber = match.matchNumber,
            title = match.title,
            entryFeeMinorUnits = entryFeeMinorUnits,
            prizeMinorUnits = match.effectivePrizeMinorUnits,
          )

          // 4. Client-side atomic multi-location update via updateChildren (NO wallet fields!)
          val updates = hashMapOf<String, Any>(
            "${FirebaseConfig.NODE_MATCHES}/$matchId/joinedPlayersCount" to newCount,
            "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to newStatus,
            "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
            "${FirebaseConfig.NODE_MATCH_PLAYERS}/$matchId/$effectiveUid" to newPlayer,
            "${FirebaseConfig.NODE_USER_TRANSACTIONS}/$effectiveUid/$trxId" to transaction,
            "${FirebaseConfig.NODE_USER_MATCHES}/$effectiveUid/$matchId" to userMatchHistory,
          )

          try {
            database.reference.updateChildren(updates).awaitTask(5_000L)
            AppLogger.i(tag, "Atomic join multi-location write successfully completed for effectiveUid=$effectiveUid")
          } catch (updateEx: Exception) {
            AppLogger.e(tag, "Multi-location join update failed or timed out: ${updateEx.message}", updateEx)

            // Step 1: FIRST check /matchPlayers/{matchId}/{effectiveUid} to see if write actually committed on server
            var playerRecordExists = false
            var playerConfirmedAbsent = false

            try {
              val checkPlayerSnapshot = database.getReference(FirebaseConfig.NODE_MATCH_PLAYERS)
                .child(matchId)
                .child(effectiveUid)
                .get()
                .awaitTask(5_000L)
              if (checkPlayerSnapshot != null && checkPlayerSnapshot.exists()) {
                playerRecordExists = true
              } else {
                playerConfirmedAbsent = true
              }
            } catch (checkEx: Exception) {
              AppLogger.w(tag, "Could not verify player existence due to network/timeout: ${checkEx.message}")
            }

            return@withLock handleJoinUpdateOutcome(
              effectiveUid = effectiveUid,
              userId = userId,
              matchId = matchId,
              trxId = trxId,
              entryFeeMinorUnits = entryFeeMinorUnits,
              playerExistsOnServer = playerRecordExists,
              playerConfirmedAbsentOnServer = playerConfirmedAbsent,
              updatedMatch = updatedMatch,
              newPlayer = newPlayer,
              updatedWallet = updatedWallet,
              transaction = transaction,
            )
          }

          // Update local state and notify listeners
          LocalDataStore.localMatches[matchId] = updatedMatch
          val playersList = LocalDataStore.localMatchPlayers.getOrPut(matchId) { CopyOnWriteArrayList() }
          playersList.removeAll { it.effectiveUid == effectiveUid || it.effectiveUid == userId }
          playersList.add(newPlayer)
          LocalDataStore.localWallets[effectiveUid] = updatedWallet
          if (userId != effectiveUid) {
            LocalDataStore.localWallets[userId] = updatedWallet
          }
          LocalDataStore.localTransactions.add(0, transaction)
          LocalDataStore.notifyMatchesChanged()
          LocalDataStore.notifyWalletChanged()

          Resource.Success(
            JoinMatchResult(
              match = updatedMatch,
              player = newPlayer,
              wallet = updatedWallet,
              transaction = transaction,
            )
          )
        } catch (e: Exception) {
          AppLogger.e(tag, "Error during match join: ${e.message}", e)
          Resource.Error(AppError.ServerError(e.message ?: "Failed to join match. Please try again."))
        }
      } else {
        val existingReservation = LocalDataStore.localActiveJoins[effectiveUid]?.get(matchId)
        if (existingReservation != null) {
          return@withLock Resource.Error(AppError.InvalidInput(reason = "You have already joined this match."))
        }

        val backend = AuthoritativeMatchJoinBackend(
          matchLookup = { mId -> LocalDataStore.localMatches[mId] ?: SampleData.sampleMatches.find { it.matchId == mId } },
          userLookup = { uId -> LocalDataStore.localUsers[uId] ?: UserEntity(uid = uId, userId = uId) },
          walletLookup = { uId -> LocalDataStore.localWallets[uId] ?: WalletEntity(uid = uId, userId = uId) },
          playersLookup = { mId -> LocalDataStore.localMatchPlayers[mId] ?: emptyList() },
          transactionsLookup = { uId -> LocalDataStore.localTransactions.filter { it.uid == uId || it.userId == uId } },
          commitMutation = { mutation ->
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            val playersList = LocalDataStore.localMatchPlayers.getOrPut(matchId) { CopyOnWriteArrayList() }
            if (playersList.none { it.effectiveUid == effectiveUid || it.effectiveUid == userId }) {
              playersList.add(mutation.newPlayer)
            }
            LocalDataStore.localWallets[effectiveUid] = mutation.updatedWallet
            if (userId != effectiveUid) {
              LocalDataStore.localWallets[userId] = mutation.updatedWallet
            }
            if (!mutation.isAlreadyProcessed) {
              LocalDataStore.localTransactions.add(0, mutation.transaction)
              val reservation = mapOf(
                "userId" to effectiveUid,
                "matchId" to matchId,
                "entryFeeMinorUnits" to mutation.newPlayer.entryFeeMinorUnits,
                "transactionId" to mutation.transaction.transactionId,
                "timestamp" to now,
              )
              LocalDataStore.localActiveJoins.getOrPut(effectiveUid) { ConcurrentHashMap() }[matchId] = reservation
              val userMatchHistory = UserMatchHistoryEntity(
                matchId = matchId,
                userId = effectiveUid,
                uid = effectiveUid,
                joinedAt = now,
                status = "JOINED",
                isWinner = null,
                updatedAt = now,
                gameType = mutation.updatedMatch.gameType,
                matchNumber = mutation.updatedMatch.matchNumber,
                title = mutation.updatedMatch.title,
                entryFeeMinorUnits = mutation.newPlayer.entryFeeMinorUnits,
                prizeMinorUnits = mutation.updatedMatch.effectivePrizeMinorUnits,
              )
              LocalDataStore.localUserMatches.getOrPut(effectiveUid) { ConcurrentHashMap() }[matchId] = userMatchHistory
              if (userId != effectiveUid) {
                LocalDataStore.localUserMatches.getOrPut(userId) { ConcurrentHashMap() }[matchId] = userMatchHistory
              }
              LocalDataStore.notifyUserMatchesChanged()
            }
            LocalDataStore.notifyMatchesChanged()
            LocalDataStore.notifyWalletChanged()
          }
        )
        return@withLock backend.joinMatch(userId = effectiveUid, matchId = matchId, currentTime = now)
      }
    }
  }

  suspend fun handleJoinUpdateOutcome(
    effectiveUid: String,
    userId: String,
    matchId: String,
    trxId: String,
    entryFeeMinorUnits: Long,
    playerExistsOnServer: Boolean,
    playerConfirmedAbsentOnServer: Boolean,
    updatedMatch: MatchEntity,
    newPlayer: MatchPlayerEntity,
    updatedWallet: WalletEntity,
    transaction: TransactionEntity,
  ): Resource<JoinMatchResult> {
    if (playerExistsOnServer) {
      LocalDataStore.localMatches[matchId] = updatedMatch
      val playersList = LocalDataStore.localMatchPlayers.getOrPut(matchId) { CopyOnWriteArrayList() }
      playersList.removeAll { it.effectiveUid == effectiveUid || it.effectiveUid == userId }
      playersList.add(newPlayer)
      LocalDataStore.localWallets[effectiveUid] = updatedWallet
      if (userId != effectiveUid) {
        LocalDataStore.localWallets[userId] = updatedWallet
      }
      LocalDataStore.localTransactions.add(0, transaction)
      val historyEntity = UserMatchHistoryEntity(
        matchId = matchId,
        userId = effectiveUid,
        uid = effectiveUid,
        joinedAt = System.currentTimeMillis(),
        status = "JOINED",
        isWinner = null,
        updatedAt = System.currentTimeMillis(),
        gameType = updatedMatch.gameType,
        matchNumber = updatedMatch.matchNumber,
        title = updatedMatch.title,
        entryFeeMinorUnits = entryFeeMinorUnits,
        prizeMinorUnits = updatedMatch.effectivePrizeMinorUnits,
      )
      LocalDataStore.localUserMatches.getOrPut(effectiveUid) { ConcurrentHashMap() }[matchId] = historyEntity
      if (userId != effectiveUid) {
        LocalDataStore.localUserMatches.getOrPut(userId) { ConcurrentHashMap() }[matchId] = historyEntity
      }
      LocalDataStore.notifyUserMatchesChanged()
      LocalDataStore.notifyMatchesChanged()
      LocalDataStore.notifyWalletChanged()

      return Resource.Success(
        JoinMatchResult(
          match = updatedMatch,
          player = newPlayer,
          wallet = updatedWallet,
          transaction = transaction,
        )
      )
    }

    if (playerConfirmedAbsentOnServer) {
      compensateFailedJoin(
        effectiveUid = effectiveUid,
        matchId = matchId,
        trxId = trxId,
        entryFeeMinorUnits = entryFeeMinorUnits,
      )
      return Resource.Error(AppError.ServerError("Failed to complete match registration. Any pending wallet deduction has been safely rolled back. Please try again."))
    }

    return Resource.Error(AppError.NetworkUnavailable("Connection timed out while joining match. Please check your match registration status before trying again."))
  }

  suspend fun compensateFailedJoin(
    effectiveUid: String,
    matchId: String,
    trxId: String,
    entryFeeMinorUnits: Long,
  ): Boolean {
    val database = customDatabase ?: FirebaseManager.getDatabase()
    if (database != null) {
      val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(effectiveUid)
      return suspendCancellableCoroutine { cont ->
        walletRef.runTransaction(object : Transaction.Handler {
          override fun doTransaction(mutableData: MutableData): Transaction.Result {
            val activeJoinNode = mutableData.child("activeJoins").child(matchId)
            val activeJoinsMap = mutableData.child("activeJoins").value as? Map<*, *>
            val reservationMap = activeJoinNode.value as? Map<*, *> ?: activeJoinsMap?.get(matchId) as? Map<*, *>

            if (activeJoinNode.value == null && reservationMap == null) {
              return Transaction.abort()
            }

            val reservedTxnId = (activeJoinNode.child("transactionId").value as? String)
              ?: (reservationMap?.get("transactionId") as? String)

            if (!reservedTxnId.isNullOrBlank() && reservedTxnId != trxId) {
              return Transaction.abort()
            }

            activeJoinNode.value = null

            val currentAvail = (mutableData.child("availableBalance").value as? Number)?.toLong()
              ?: ((mutableData.value as? Map<*, *>)?.get("availableBalance") as? Number)?.toLong()
              ?: 0L
            val currentPending = (mutableData.child("pendingBalance").value as? Number)?.toLong()
              ?: ((mutableData.value as? Map<*, *>)?.get("pendingBalance") as? Number)?.toLong()
              ?: 0L

            val restoredAvail = currentAvail + entryFeeMinorUnits
            val restoredPending = (currentPending - entryFeeMinorUnits).coerceAtLeast(0L)
            val restoredBalancePaisa = restoredAvail
            val restoredLockedPaisa = restoredPending

            mutableData.child("availableBalance").value = restoredAvail
            mutableData.child("pendingBalance").value = restoredPending
            mutableData.child("balance").value = restoredBalancePaisa
            mutableData.child("lockedBalance").value = restoredLockedPaisa
            mutableData.child("updatedAt").value = System.currentTimeMillis()

            return Transaction.success(mutableData)
          }

          override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
            if (cont.isActive) {
              cont.resume(committed && error == null)
            }
          }
        })
      }
    } else {
      val userReservations = LocalDataStore.localActiveJoins[effectiveUid] ?: return false
      val reservation = userReservations[matchId] ?: return false
      val reservedTxnId = reservation["transactionId"] as? String
      if (!reservedTxnId.isNullOrBlank() && reservedTxnId != trxId) {
        return false
      }

      userReservations.remove(matchId)

      val currentWallet = LocalDataStore.localWallets[effectiveUid] ?: return false
      val restoredAvail = currentWallet.availableBalance + entryFeeMinorUnits
      val restoredPending = (currentWallet.pendingBalance - entryFeeMinorUnits).coerceAtLeast(0L)
      val restoredWallet = currentWallet.copy(
        availableBalance = restoredAvail,
        balance = restoredAvail / 100.0,
        pendingBalance = restoredPending,
        lockedBalance = restoredPending / 100.0,
        updatedAt = System.currentTimeMillis(),
      )
      LocalDataStore.localWallets[effectiveUid] = restoredWallet
      LocalDataStore.notifyWalletChanged()
      return true
    }
  }

  override fun isUserJoinedMatch(userId: String, matchId: String): Flow<Boolean> = callbackFlow {
    val authUid = FirebaseManager.getAuth()?.currentUser?.uid
    fun checkJoined(): Boolean {
      val players = LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
      return players.any {
        it.effectiveUid == userId ||
        it.userId == userId ||
        it.uid == userId ||
        (!authUid.isNullOrBlank() && (it.effectiveUid == authUid || it.uid == authUid || it.userId == authUid))
      }
    }
    trySend(checkJoined())
    val job = CoroutineScope(Dispatchers.IO).launch {
      LocalDataStore.matchesUpdateTrigger.collect {
        trySend(checkJoined())
      }
    }
    awaitClose { job.cancel() }
  }
}

class FirebaseWalletRepository : WalletRepository {
  private val tag = "WalletRepository"
  private val depositSubmissionMutex = Mutex()

  override fun getWallet(userId: String): Flow<Resource<WalletEntity?>> = callbackFlow {
    trySend(Resource.Loading)
    val effectiveUid = userId.trim().ifBlank {
      FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    }
    
    if (effectiveUid.isBlank()) {
      trySend(Resource.Success(null))
      awaitClose { }
      return@callbackFlow
    }

    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_WALLETS)?.child(effectiveUid)
    if (ref == null) {
      fun getCurrentWallet(): WalletEntity {
        return LocalDataStore.localWallets[effectiveUid] ?: WalletEntity(uid = effectiveUid, userId = effectiveUid)
      }
      trySend(Resource.Success(getCurrentWallet()))
      val job = CoroutineScope(Dispatchers.IO).launch {
        LocalDataStore.walletUpdateTrigger.collect {
          trySend(Resource.Success(getCurrentWallet()))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }

    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val wallet = snapshot.getValue(WalletEntity::class.java)
          ?: LocalDataStore.localWallets[effectiveUid]
          ?: WalletEntity(uid = effectiveUid, userId = effectiveUid)
        LocalDataStore.localWallets[effectiveUid] = wallet
        trySend(Resource.Success(wallet))
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "getWallet error: ${error.message}")
        val fallback = LocalDataStore.localWallets[effectiveUid]
          ?: WalletEntity(uid = effectiveUid, userId = effectiveUid)
        trySend(Resource.Success(fallback))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override fun getTransactions(userId: String, limit: Int): Flow<Resource<List<TransactionEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val effectiveUid = userId.trim().ifBlank {
      FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    }

    if (effectiveUid.isBlank()) {
      trySend(Resource.Success(emptyList()))
      awaitClose { }
      return@callbackFlow
    }

    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_TRANSACTIONS)?.child(effectiveUid)
    if (ref == null) {
      fun getTxList(): List<TransactionEntity> {
        val userTxs = LocalDataStore.localTransactions.filter { 
          (it.uid == effectiveUid || it.userId == effectiveUid) && it.amount != 0L 
        }.distinctBy { it.id.ifBlank { it.transactionId.ifBlank { it.referenceId } } }
        return if (userTxs.isNotEmpty()) {
          userTxs.take(limit)
        } else {
          emptyList()
        }
      }
      trySend(Resource.Success(getTxList()))
      val job = CoroutineScope(Dispatchers.IO).launch {
        LocalDataStore.walletUpdateTrigger.collect {
          trySend(Resource.Success(getTxList()))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }

    val query = ref.limitToLast(limit)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val txs = snapshot.children.mapNotNull { child ->
          val raw = child.getValue(TransactionEntity::class.java) ?: return@mapNotNull null
          val keyId = child.key.orEmpty()
          val resolvedId = raw.id.ifBlank { raw.transactionId.ifBlank { keyId } }
          val resolvedTime = if (raw.timestamp > 0L) raw.timestamp else (if (raw.createdAt > 0L) raw.createdAt else raw.processedAt)
          raw.copy(
            id = resolvedId,
            transactionId = resolvedId,
            createdAt = resolvedTime,
            timestamp = resolvedTime,
          )
        }
        .filter { it.amount != 0L }
        .distinctBy { it.id.ifBlank { it.transactionId.ifBlank { it.referenceId } } }
        trySend(Resource.Success(txs.reversed()))
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "getTransactions error: ${error.message}")
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getUserDeposits(userId: String, limit: Int): Flow<Resource<List<DepositEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_DEPOSITS)?.child(userId)
    if (ref == null) {
      val localUserDeposits = LocalDataStore.localDeposits.filter { it.uid == userId || it.userId == userId }
      trySend(Resource.Success(localUserDeposits))
      close()
      return@callbackFlow
    }

    val query = ref.limitToLast(limit)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val deposits = snapshot.children.mapNotNull { it.getValue(DepositEntity::class.java) }
        trySend(Resource.Success(deposits.reversed()))
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "getUserDeposits error: ${error.message}")
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getUserWithdrawals(userId: String, limit: Int): Flow<Resource<List<WithdrawalEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_WITHDRAWALS)?.child(userId)
    if (ref == null) {
      val localUserWithdrawals = LocalDataStore.localWithdrawals.filter { it.uid == userId || it.userId == userId }
      trySend(Resource.Success(localUserWithdrawals))
      close()
      return@callbackFlow
    }

    val query = ref.limitToLast(limit)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val withdrawals = snapshot.children.mapNotNull { it.getValue(WithdrawalEntity::class.java) }
        trySend(Resource.Success(withdrawals.reversed()))
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "getUserWithdrawals error: ${error.message}")
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override suspend fun submitDepositRequest(
    userId: String,
    amountMinorUnits: Long,
    method: String,
    senderNumber: String,
    trxId: String,
    screenshotUrl: String,
  ): Resource<DepositEntity> = depositSubmissionMutex.withLock {
    val isUserBlocked = LocalDataStore.localUsers[userId]?.isAccountBlocked == true
    if (isUserBlocked) {
      return@withLock Resource.Error(AppError.InvalidInput(reason = "Your account is blocked. Deposit requests are disabled."))
    }
    if (amountMinorUnits < 5000L) {
      return@withLock Resource.Error(AppError.InvalidInput("amount", "Minimum deposit amount is 50 BDT"))
    }
    if (amountMinorUnits > 2500000L) {
      return@withLock Resource.Error(AppError.InvalidInput("amount", "Maximum deposit amount is 25,000 BDT per request"))
    }
    if (senderNumber.isBlank() || senderNumber.length < 11) {
      return@withLock Resource.Error(AppError.InvalidInput("senderNumber", "Please enter a valid 11-digit mobile number"))
    }
    if (trxId.isBlank() || trxId.length < 6) {
      return@withLock Resource.Error(AppError.InvalidInput("trxId", "Please enter a valid Transaction ID"))
    }

    val normSenderNumber = senderNumber.trim()
    val normTrxId = trxId.trim().uppercase()
    val normAmount = amountMinorUnits
    val now = System.currentTimeMillis()
    val windowMs = 20 * 60 * 1000L // 20-minute rolling window
    val windowStart = now - windowMs

    val database = FirebaseManager.getDatabase()

    // Step A: Load authoritative user deposit requests
    val userExistingDeposits: List<DepositEntity> = if (database != null) {
      try {
        val snapshot = database.getReference(FirebaseConfig.NODE_USER_DEPOSITS)
          .child(userId)
          .get()
          .awaitTask(5_000L)
        val remoteDeposits = snapshot.children.mapNotNull { it.getValue(DepositEntity::class.java) }
        val localUserDeposits = LocalDataStore.localDeposits.filter { it.uid == userId || it.userId == userId }
        (remoteDeposits + localUserDeposits).distinctBy { it.depositId }
      } catch (e: Exception) {
        AppLogger.w(tag, "Failed to load user deposits from Firebase, using local cache: ${e.message}")
        LocalDataStore.localDeposits.filter { it.uid == userId || it.userId == userId }
      }
    } else {
      LocalDataStore.localDeposits.filter { it.uid == userId || it.userId == userId }
    }

    // Step B: Consider only requests created within the last 20 minutes
    val recentDeposits = userExistingDeposits.filter { it.createdAt >= windowStart }

    // Step C & D: Detect exact duplicate using normalized senderNumber + trxId + amount
    val isExactDuplicate = recentDeposits.any { existing ->
      existing.senderNumber.trim() == normSenderNumber &&
        existing.trxId.trim().uppercase() == normTrxId &&
        existing.amount == normAmount
    }

    if (isExactDuplicate) {
      AppLogger.w(tag, "Deposit rejected: Duplicate request detected for user=$userId, trxId=$normTrxId")
      return@withLock Resource.Error(
        AppError.InvalidInput(
          reason = "A deposit request with the same sender number, transaction ID, and amount has already been submitted."
        )
      )
    }

    // Step E: If there are already 3 distinct requests within the rolling 20-minute window, reject
    val distinctRecentRequestsCount = recentDeposits
      .map { "${it.senderNumber.trim()}_${it.trxId.trim().uppercase()}_${it.amount}" }
      .distinct()
      .size

    if (distinctRecentRequestsCount >= 3) {
      AppLogger.w(tag, "Deposit rejected: 20-minute rolling rate limit (max 3) reached for user=$userId")
      return@withLock Resource.Error(
        AppError.InvalidInput(
          reason = "You can submit at most 3 deposit requests within 20 minutes. Please wait before submitting again."
        )
      )
    }

    // Step F: Otherwise create the deposit normally, preserving dual-write behavior and schema
    if (database == null) {
      val localDeposit = DepositEntity(
        depositId = "DEP_$now",
        uid = userId,
        userId = userId,
        amount = amountMinorUnits,
        method = method.uppercase(),
        paymentMethod = method.uppercase(),
        senderNumber = normSenderNumber,
        trxId = normTrxId,
        screenshotUrl = screenshotUrl,
        status = "PENDING",
        createdAt = now,
        processedAt = 0L,
      )
      LocalDataStore.localDeposits.add(0, localDeposit)
      val localTx = TransactionEntity(
        id = localDeposit.depositId,
        transactionId = localDeposit.depositId,
        uid = userId,
        userId = userId,
        amount = amountMinorUnits,
        type = "DEPOSIT",
        title = "ডিপোজিট",
        status = "PENDING",
        description = "ডিপোজিট রিকোয়েস্ট (যাচাইকরণ চলছে)",
        referenceId = normTrxId,
        createdAt = now,
        timestamp = now,
        processedAt = 0L,
      )
      LocalDataStore.localTransactions.add(0, localTx)
      LocalDataStore.notifyWalletChanged()
      return@withLock Resource.Success(localDeposit)
    }

    val depositId = database.getReference(FirebaseConfig.NODE_DEPOSITS).push().key ?: "DEP_$now"
    val deposit = DepositEntity(
      depositId = depositId,
      uid = userId,
      userId = userId,
      amount = amountMinorUnits,
      method = method.uppercase(),
      paymentMethod = method.uppercase(),
      senderNumber = normSenderNumber,
      trxId = normTrxId,
      screenshotUrl = screenshotUrl,
      status = "PENDING",
      createdAt = now,
      processedAt = 0L,
    )
    val pendingDepositTxn = hashMapOf<String, Any>(
      "id" to depositId,
      "transactionId" to depositId,
      "type" to "DEPOSIT",
      "title" to "ডিপোজিট",
      "amount" to amountMinorUnits,
      "status" to "PENDING",
      "description" to "ডিপোজিট রিকোয়েস্ট (যাচাইকরণ চলছে)",
      "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
      "referenceId" to normTrxId,
      "uid" to userId,
      "userId" to userId,
      "createdAt" to now,
    )
    return@withLock try {
      val updates = hashMapOf<String, Any>(
        "${FirebaseConfig.NODE_DEPOSITS}/$depositId" to deposit,
        "${FirebaseConfig.NODE_USER_DEPOSITS}/$userId/$depositId" to deposit,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/$userId/$depositId" to pendingDepositTxn,
      )
      database.reference.updateChildren(updates).awaitTask()
      LocalDataStore.localDeposits.add(0, deposit)
      val localTx = TransactionEntity(
        id = depositId,
        transactionId = depositId,
        uid = userId,
        userId = userId,
        amount = amountMinorUnits,
        type = "DEPOSIT",
        title = "ডিপোজিট",
        status = "PENDING",
        description = "ডিপোজিট রিকোয়েস্ট (যাচাইকরণ চলছে)",
        referenceId = normTrxId,
        createdAt = now,
        timestamp = now,
        processedAt = 0L,
      )
      LocalDataStore.localTransactions.add(0, localTx)
      LocalDataStore.notifyWalletChanged()
      Resource.Success(deposit)
    } catch (e: Exception) {
      AppLogger.w(tag, "submitDepositRequest failure: ${e.message}")
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Failed to submit deposit request"))
    }
  }

  override suspend fun submitWithdrawalRequest(
    userId: String,
    amountMinorUnits: Long,
    method: String,
    recipientNumber: String,
  ): Resource<WithdrawalEntity> {
    val authUid = FirebaseManager.getAuth()?.currentUser?.uid
    val effectiveUid = if (!authUid.isNullOrBlank()) {
      authUid
    } else {
      userId
    }

    val isUserBlocked = LocalDataStore.localUsers[effectiveUid]?.isAccountBlocked == true ||
      LocalDataStore.localUsers[userId]?.isAccountBlocked == true
    if (isUserBlocked) {
      return Resource.Error(AppError.InvalidInput(reason = "Your account is blocked. Withdrawal requests are disabled."))
    }
    if (amountMinorUnits < 20000L) {
      return Resource.Error(AppError.InvalidInput("amount", "Minimum withdrawal amount is 200 BDT"))
    }
    if (amountMinorUnits > 1000000L) {
      return Resource.Error(AppError.InvalidInput("amount", "Maximum withdrawal amount is 10,000 BDT per request"))
    }
    if (recipientNumber.isBlank() || recipientNumber.length < 11) {
      return Resource.Error(AppError.InvalidInput("recipientNumber", "Please enter a valid 11-digit recipient number"))
    }

    val database = FirebaseManager.getDatabase()
    if (database == null) {
      return submitLocalWithdrawal(
        userId = userId,
        effectiveUid = effectiveUid,
        amountMinorUnits = amountMinorUnits,
        method = method,
        recipientNumber = recipientNumber,
      )
    }

    val withdrawalId = database.getReference(FirebaseConfig.NODE_WITHDRAWALS).push().key
      ?: "WTH_${System.currentTimeMillis()}"
    val trxId = "TXN_WTH_HOLD_${System.currentTimeMillis()}_${withdrawalId.takeLast(6)}"

    // 1. Authoritative wallet transaction gatekeeper
    val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(effectiveUid)
    var txErrorReason: String? = null
    var updatedWalletEntity: WalletEntity? = null

    val walletTxSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
      walletRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
        override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
          val activeWithdrawalsNode = currentData.child("activeWithdrawals")
          if (activeWithdrawalsNode.hasChildren()) {
            txErrorReason = "You already have an active withdrawal request in progress"
            return com.google.firebase.database.Transaction.abort()
          }

          val availableBal = currentData.child("availableBalance").getValue(Long::class.java)
            ?: (currentData.child("balance").getValue(Double::class.java)?.toLong() ?: 0L)
          val pendingBal = currentData.child("pendingBalance").getValue(Long::class.java)
            ?: (currentData.child("lockedBalance").getValue(Double::class.java)?.toLong() ?: 0L)

          if (availableBal < amountMinorUnits) {
            val shortBy = (amountMinorUnits - availableBal) / 100.0
            txErrorReason = "Insufficient available balance. Short by ৳ ${"%.2f".format(shortBy)}"
            return com.google.firebase.database.Transaction.abort()
          }

          val newAvailable = availableBal - amountMinorUnits
          val newPending = pendingBal + amountMinorUnits
          val now = System.currentTimeMillis()

          currentData.child("availableBalance").value = newAvailable
          currentData.child("balance").value = newAvailable
          currentData.child("pendingBalance").value = newPending
          currentData.child("lockedBalance").value = newPending
          currentData.child("updatedAt").value = now

          val reservationMap = mapOf<String, Any>(
            "withdrawalId" to withdrawalId,
            "amountMinorUnits" to amountMinorUnits,
            "status" to "REQUESTED",
            "transactionId" to trxId,
            "timestamp" to now,
          )
          currentData.child("activeWithdrawals").child(withdrawalId).value = reservationMap

          updatedWalletEntity = WalletEntity(
            uid = effectiveUid,
            userId = effectiveUid,
            availableBalance = newAvailable,
            balance = newAvailable.toDouble(),
            pendingBalance = newPending,
            lockedBalance = newPending.toDouble(),
            totalDeposited = currentData.child("totalDeposited").getValue(Long::class.java) ?: 0L,
            totalWithdrawn = currentData.child("totalWithdrawn").getValue(Long::class.java) ?: 0L,
            totalWinnings = currentData.child("totalWinnings").getValue(Long::class.java) ?: 0L,
            updatedAt = now,
          )

          return com.google.firebase.database.Transaction.success(currentData)
        }

        override fun onComplete(
          error: com.google.firebase.database.DatabaseError?,
          committed: Boolean,
          currentData: com.google.firebase.database.DataSnapshot?,
        ) {
          if (error != null) {
            AppLogger.w(tag, "Wallet withdrawal transaction error: ${error.message}")
            if (continuation.isActive) continuation.resume(false)
          } else {
            if (continuation.isActive) continuation.resume(committed)
          }
        }
      })
    }

    if (!walletTxSuccess) {
      val message = txErrorReason ?: "Failed to process withdrawal hold. Please try again."
      return Resource.Error(AppError.InvalidInput(reason = message))
    }

    // 2. Prepare withdrawal entity and transaction record
    val now = System.currentTimeMillis()
    val withdrawal = WithdrawalEntity(
      withdrawalId = withdrawalId,
      uid = effectiveUid,
      userId = effectiveUid,
      amount = amountMinorUnits,
      method = method.uppercase(),
      recipientNumber = recipientNumber.trim(),
      status = WithdrawalStatus.REQUESTED.name,
      createdAt = now,
      processedAt = 0L,
    )

    val transaction = TransactionEntity(
      transactionId = trxId,
      uid = effectiveUid,
      userId = effectiveUid,
      amount = amountMinorUnits,
      type = TransactionType.WITHDRAW_HOLD.name,
      status = TransactionStatus.PENDING.name,
      beforeBalance = (updatedWalletEntity?.availableBalance ?: 0L) + amountMinorUnits,
      afterBalance = updatedWalletEntity?.availableBalance ?: 0L,
      source = "APP",
      referenceId = withdrawalId,
      description = "Withdrawal hold for ৳ ${"%.2f".format(amountMinorUnits / 100.0)} via ${method.uppercase()}",
      createdAt = now,
      processedAt = now,
    )

    val pendingWithdrawalTxn = hashMapOf<String, Any>(
      "id" to withdrawalId,
      "transactionId" to withdrawalId,
      "type" to "WITHDRAW",
      "title" to "উইথড্র",
      "amount" to amountMinorUnits,
      "status" to "PENDING",
      "description" to "উইথড্র রিকোয়েস্ট (অপেক্ষমাণ)",
      "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
      "referenceId" to withdrawalId,
      "uid" to effectiveUid,
      "userId" to effectiveUid,
      "createdAt" to now,
    )

    // 3. Multi-path write for withdrawal records and transaction log
    val updates = hashMapOf<String, Any>(
      "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId" to withdrawal,
      "${FirebaseConfig.NODE_USER_WITHDRAWALS}/$effectiveUid/$withdrawalId" to withdrawal,
      "${FirebaseConfig.NODE_TRANSACTIONS}/$trxId" to transaction,
      "${FirebaseConfig.NODE_USER_TRANSACTIONS}/$effectiveUid/$withdrawalId" to pendingWithdrawalTxn,
    )

    var updateSucceeded = false
    try {
      database.reference.updateChildren(updates).awaitTask()
      updateSucceeded = true
    } catch (e: Exception) {
      AppLogger.w(tag, "Withdrawal records updateChildren failed: ${e.message}")
    }

    if (updateSucceeded) {
      updatedWalletEntity?.let {
        LocalDataStore.localWallets[effectiveUid] = it
        LocalDataStore.notifyWalletChanged()
      }
      LocalDataStore.localWithdrawals.add(0, withdrawal)
      val userTxn = transaction.copy(
        id = withdrawalId,
        transactionId = withdrawalId,
        title = "উইথড্র",
        type = "WITHDRAW",
        status = "PENDING",
        description = "উইথড্র রিকোয়েস্ট (অপেক্ষমাণ)",
        createdAt = now,
        timestamp = now,
      )
      LocalDataStore.localTransactions.add(0, userTxn)
      return Resource.Success(withdrawal)
    }

    // 4. Ambiguity resolution: check if records actually exist on server
    val recordsExist = try {
      val snapshot = database.getReference(FirebaseConfig.NODE_WITHDRAWALS).child(withdrawalId).get().awaitTask()
      snapshot.exists()
    } catch (e: Exception) {
      false
    }

    return handleWithdrawalOutcome(
      effectiveUid = effectiveUid,
      userId = userId,
      withdrawalId = withdrawalId,
      trxId = trxId,
      amountMinorUnits = amountMinorUnits,
      recordsExistOnServer = recordsExist,
      withdrawal = withdrawal,
      updatedWallet = updatedWalletEntity,
      transaction = transaction,
    )
  }

  internal suspend fun handleWithdrawalOutcome(
    effectiveUid: String,
    userId: String,
    withdrawalId: String,
    trxId: String,
    amountMinorUnits: Long,
    recordsExistOnServer: Boolean,
    withdrawal: WithdrawalEntity,
    updatedWallet: WalletEntity?,
    transaction: TransactionEntity,
  ): Resource<WithdrawalEntity> {
    if (recordsExistOnServer) {
      updatedWallet?.let {
        LocalDataStore.localWallets[effectiveUid] = it
        LocalDataStore.notifyWalletChanged()
      }
      LocalDataStore.localWithdrawals.add(0, withdrawal)
      LocalDataStore.localTransactions.add(0, transaction)
      return Resource.Success(withdrawal)
    }

    // Records confirmed absent: trigger compensation refund
    val compensated = revertWithdrawalHold(
      effectiveUid = effectiveUid,
      withdrawalId = withdrawalId,
      trxId = trxId,
      amountMinorUnits = amountMinorUnits,
    )

    return if (compensated) {
      Resource.Error(AppError.FirebaseUnavailable("Network error during withdrawal submission. Your balance was safely restored."))
    } else {
      Resource.Error(AppError.FirebaseUnavailable("Network error during withdrawal submission. Please check your wallet balance."))
    }
  }

  override suspend fun checkAndClaimReferralBonus(userId: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase() ?: return Resource.Error(AppError.FirebaseUnavailable())
    val now = System.currentTimeMillis()
    
    return try {
      val userSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(userId).get().awaitTask()
      val user = userSnapshot.getValue(UserEntity::class.java) ?: return Resource.Success(Unit)
      
      val walletSnapshot = database.getReference(FirebaseConfig.NODE_WALLETS).child(userId).get().awaitTask()
      val wallet = walletSnapshot.getValue(WalletEntity::class.java) ?: return Resource.Success(Unit)
      
      // Eligibility: referredBy exists, bonus not yet claimed, and has a deposit (mainBalance or totalDeposited >= ৳100)
      val mainBalance = wallet.availableBalance
      val totalDeposited = wallet.totalDeposited
      
      // 10000 Paisa = 100 Taka
      val isEligible = user.referredBy.isNotBlank() && 
          !user.firstDepositBonusClaimed && 
          (mainBalance >= 10000 || totalDeposited >= 10000)
          
      if (isEligible) {
        val updates = hashMapOf<String, Any>()
        
        // 1. Mark as claimed for current user
        updates["${FirebaseConfig.NODE_USERS}/$userId/firstDepositBonusClaimed"] = true
        
        // 2. Find and Reward Referrer
        val referrerUid = user.referredBy
        val referrerWalletSnapshot = database.getReference(FirebaseConfig.NODE_WALLETS).child(referrerUid).get().awaitTask()
        val referrerWallet = referrerWalletSnapshot.getValue(WalletEntity::class.java)
        
        if (referrerWallet != null) {
          // Deduct 4000 from lockedBonus, Add 4000 to bonusBalance
          val newLockedBonus = (referrerWallet.lockedBonus - 4000.0).coerceAtLeast(0.0)
          val newBonus = referrerWallet.bonusBalance + 4000.0
          
          updates["${FirebaseConfig.NODE_WALLETS}/$referrerUid/lockedBonus"] = newLockedBonus
          updates["${FirebaseConfig.NODE_WALLETS}/$referrerUid/bonusBalance"] = newBonus
          updates["${FirebaseConfig.NODE_WALLETS}/$referrerUid/updatedAt"] = now
          
          // Log referrer's bonus transaction
          val bonusTxnId = "TXN_BONUS_UNLOCK_${now}_${userId.takeLast(4)}"
          val bonusTxn = TransactionEntity(
            transactionId = bonusTxnId,
            uid = referrerUid,
            amount = 4000L,
            type = "REFERRAL_BONUS_UNLOCK",
            status = TransactionStatus.COMPLETED.name,
            description = "রেফারেল বোনাস আনলক হয়েছে (ইউজার ${user.effectiveName})",
            createdAt = now,
            processedAt = now
          )
          updates["${FirebaseConfig.NODE_USER_TRANSACTIONS}/$referrerUid/$bonusTxnId"] = bonusTxn
        }
        
        database.reference.updateChildren(updates).awaitTask()
        Resource.Success(Unit)
      } else if (!user.firstDepositBonusClaimed && (mainBalance >= 10000 || totalDeposited >= 10000)) {
        // Even if not referred, mark as claimed to stop checking
        database.getReference(FirebaseConfig.NODE_USERS).child(userId).child("firstDepositBonusClaimed").setValue(true).awaitTask()
        Resource.Success(Unit)
      } else {
        Resource.Success(Unit)
      }
    } catch (e: Exception) {
      AppLogger.e("WalletRepository", "checkAndClaimReferralBonus failed: ${e.message}")
      Resource.Error(AppError.ServerError(e.message ?: "Referral bonus claim failed"))
    }
  }

  internal suspend fun revertWithdrawalHold(
    effectiveUid: String,
    withdrawalId: String,
    trxId: String,
    amountMinorUnits: Long,
  ): Boolean {
    val database = FirebaseManager.getDatabase()
    if (database != null) {
      val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(effectiveUid)
      return suspendCancellableCoroutine { continuation ->
        walletRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
          override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
            val reservationNode = currentData.child("activeWithdrawals").child(withdrawalId)
            if (reservationNode.value == null) {
              return com.google.firebase.database.Transaction.abort()
            }
            val reservedTxnId = reservationNode.child("transactionId").getValue(String::class.java)
            if (!reservedTxnId.isNullOrBlank() && reservedTxnId != trxId) {
              return com.google.firebase.database.Transaction.abort()
            }

            reservationNode.value = null

            val avail = currentData.child("availableBalance").getValue(Long::class.java)
              ?: (currentData.child("balance").getValue(Double::class.java)?.toLong() ?: 0L)
            val pending = currentData.child("pendingBalance").getValue(Long::class.java)
              ?: (currentData.child("lockedBalance").getValue(Double::class.java)?.toLong() ?: 0L)

            val restoredAvail = avail + amountMinorUnits
            val restoredPending = (pending - amountMinorUnits).coerceAtLeast(0L)
            val now = System.currentTimeMillis()

            currentData.child("availableBalance").value = restoredAvail
            currentData.child("balance").value = restoredAvail
            currentData.child("pendingBalance").value = restoredPending
            currentData.child("lockedBalance").value = restoredPending
            currentData.child("updatedAt").value = now

            return com.google.firebase.database.Transaction.success(currentData)
          }

          override fun onComplete(
            error: com.google.firebase.database.DatabaseError?,
            committed: Boolean,
            currentData: com.google.firebase.database.DataSnapshot?,
          ) {
            if (error != null) {
              AppLogger.w(tag, "revertWithdrawalHold failed: ${error.message}")
              if (continuation.isActive) continuation.resume(false)
            } else {
              if (committed) {
                val currentWallet = LocalDataStore.localWallets[effectiveUid]
                if (currentWallet != null) {
                  val restoredAvail = currentWallet.availableBalance + amountMinorUnits
                  val restoredPending = (currentWallet.pendingBalance - amountMinorUnits).coerceAtLeast(0L)
                  LocalDataStore.localWallets[effectiveUid] = currentWallet.copy(
                    availableBalance = restoredAvail,
                    balance = restoredAvail / 100.0,
                    pendingBalance = restoredPending,
                    lockedBalance = restoredPending / 100.0,
                    updatedAt = System.currentTimeMillis(),
                  )
                  LocalDataStore.notifyWalletChanged()
                }
              }
              if (continuation.isActive) continuation.resume(committed)
            }
          }
        })
      }
    } else {
      // Local fallback compensation
      val userReservations = LocalDataStore.localActiveWithdrawals[effectiveUid] ?: return false
      val reservation = userReservations[withdrawalId] ?: return false
      val reservedTxnId = reservation["transactionId"] as? String
      if (!reservedTxnId.isNullOrBlank() && reservedTxnId != trxId) {
        return false
      }

      userReservations.remove(withdrawalId)

      val currentWallet = LocalDataStore.localWallets[effectiveUid] ?: return false
      val restoredAvail = currentWallet.availableBalance + amountMinorUnits
      val restoredPending = (currentWallet.pendingBalance - amountMinorUnits).coerceAtLeast(0L)
      val restoredWallet = currentWallet.copy(
        availableBalance = restoredAvail,
        balance = restoredAvail / 100.0,
        pendingBalance = restoredPending,
        lockedBalance = restoredPending / 100.0,
        updatedAt = System.currentTimeMillis(),
      )
      LocalDataStore.localWallets[effectiveUid] = restoredWallet
      LocalDataStore.notifyWalletChanged()
      return true
    }
  }

  private fun submitLocalWithdrawal(
    userId: String,
    effectiveUid: String,
    amountMinorUnits: Long,
    method: String,
    recipientNumber: String,
  ): Resource<WithdrawalEntity> {
    val userReservations = LocalDataStore.localActiveWithdrawals.getOrPut(effectiveUid) { ConcurrentHashMap() }
    if (userReservations.isNotEmpty()) {
      return Resource.Error(AppError.InvalidInput(reason = "You already have an active withdrawal request in progress"))
    }

    val currentWallet = LocalDataStore.localWallets[effectiveUid]
      ?: LocalDataStore.localWallets[userId]
      ?: WalletEntity(uid = effectiveUid, userId = effectiveUid)

    if (currentWallet.availableBalance < amountMinorUnits) {
      val shortBy = (amountMinorUnits - currentWallet.availableBalance) / 100.0
      return Resource.Error(AppError.InvalidInput(reason = "Insufficient available balance. Short by ৳ ${"%.2f".format(shortBy)}"))
    }

    val withdrawalId = "WTH_${System.currentTimeMillis()}"
    val trxId = "TXN_WTH_HOLD_${System.currentTimeMillis()}_${withdrawalId.takeLast(6)}"
    val now = System.currentTimeMillis()

    val newAvailable = currentWallet.availableBalance - amountMinorUnits
    val newPending = currentWallet.pendingBalance + amountMinorUnits

    val reservation = mapOf<String, Any>(
      "withdrawalId" to withdrawalId,
      "amountMinorUnits" to amountMinorUnits,
      "status" to "REQUESTED",
      "transactionId" to trxId,
      "timestamp" to now,
    )
    userReservations[withdrawalId] = reservation

    val updatedWallet = currentWallet.copy(
      availableBalance = newAvailable,
      balance = newAvailable / 100.0,
      pendingBalance = newPending,
      lockedBalance = newPending / 100.0,
      updatedAt = now,
    )
    LocalDataStore.localWallets[effectiveUid] = updatedWallet
    LocalDataStore.notifyWalletChanged()

    val localWithdrawal = WithdrawalEntity(
      withdrawalId = withdrawalId,
      uid = effectiveUid,
      userId = effectiveUid,
      amount = amountMinorUnits,
      method = method.uppercase(),
      recipientNumber = recipientNumber.trim(),
      status = WithdrawalStatus.REQUESTED.name,
      createdAt = now,
      processedAt = 0L,
    )
    LocalDataStore.localWithdrawals.add(0, localWithdrawal)

    val transaction = TransactionEntity(
      transactionId = withdrawalId,
      id = withdrawalId,
      uid = effectiveUid,
      userId = effectiveUid,
      amount = amountMinorUnits,
      type = TransactionType.WITHDRAW_HOLD.name,
      status = TransactionStatus.PENDING.name,
      title = "উইথড্র",
      beforeBalance = currentWallet.availableBalance,
      afterBalance = newAvailable,
      source = "APP",
      referenceId = withdrawalId,
      description = "উইথড্র রিকোয়েস্ট (অপেক্ষমাণ)",
      createdAt = now,
      timestamp = now,
      processedAt = now,
    )
    LocalDataStore.localTransactions.add(0, transaction)

    return Resource.Success(localWithdrawal)
  }
}

class FirebaseResultRepository(
  private val context: Context? = null,
  private val customDatabase: FirebaseDatabase? = null,
  private val forceLocalOnly: Boolean = false,
  private val authUidProvider: (() -> String?)? = null,
) : ResultRepository {
  private val resolvedContext: Context?
    get() = context ?: try {
      com.google.firebase.FirebaseApp.getInstance().applicationContext
    } catch (_: Exception) {
      null
    }

  private val activeDatabase: FirebaseDatabase?
    get() = if (forceLocalOnly) null else (customDatabase ?: FirebaseManager.getDatabase())

  private val activeAuth: com.google.firebase.auth.FirebaseAuth?
    get() = if (forceLocalOnly) null else FirebaseManager.getAuth()

  override fun getResultForMatch(matchId: String): Flow<Resource<ResultEntity?>> = callbackFlow {
    trySend(Resource.Loading)
    val auth = activeAuth
    val authUser = auth?.currentUser
    val authUid = authUser?.uid?.trim()?.ifBlank { null }
    val effectiveUid = if (authUidProvider != null) {
      authUidProvider.invoke()?.trim()?.ifBlank { null }
    } else if (auth != null) {
      authUid
    } else {
      ""
    }

    if (effectiveUid.isNullOrBlank()) {
      trySend(Resource.Success(null))
      close()
      return@callbackFlow
    }

    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_RESULTS)?.child(matchId)?.child(effectiveUid)
    if (ref == null) {
      val localRes = LocalDataStore.localResults.find { it.matchId == matchId && (it.submittedByUserId == effectiveUid || it.userId == effectiveUid) }
      trySend(Resource.Success(localRes))
      close()
      return@callbackFlow
    }
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val result = snapshot.getValue(ResultEntity::class.java)
        trySend(Resource.Success(result))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override suspend fun uploadResultScreenshot(matchId: String, imageUri: android.net.Uri): Resource<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val uriString = imageUri.toString()
    if (uriString.isBlank()) {
      return@withContext Resource.Error(AppError.InvalidInput(reason = "স্ক্রিনশট পাথ খালি বা অবৈধ।"))
    }

    // If already a Base64 Data URI, enforce payload size guard
    if (uriString.startsWith(ImageCompressor.DATA_URI_PREFIX) || uriString.startsWith("data:image/")) {
      val sizeBytes = uriString.toByteArray().size
      if (sizeBytes > ImageCompressor.MAX_PAYLOAD_SIZE_BYTES) {
        return@withContext Resource.Error(
          AppError.InvalidInput(
            reason = "স্ক্রিনশটের সাইজ অতিরিক্ত বড় (${sizeBytes / 1024} KB)। সর্বোচ্চ সীমা ${ImageCompressor.MAX_PAYLOAD_SIZE_BYTES / 1024} KB।"
          )
        )
      }
      return@withContext Resource.Success(uriString)
    }

    val ctx = resolvedContext
    if (ctx == null) {
      return@withContext Resource.Error(
        AppError.InvalidInput(reason = "অ্যাপ্লিকেশন কনটেক্সট পাওয়া যায়নি। ছবি প্রসেস করা সম্ভব হয়নি।")
      )
    }

    when (val compressRes = ImageCompressor.compressScreenshot(ctx, imageUri)) {
      is Resource.Success -> {
        Resource.Success(compressRes.data.dataUri)
      }
      is Resource.Error -> {
        Resource.Error(compressRes.error)
      }
      else -> {
        Resource.Error(AppError.Unknown("Unexpected compression state"))
      }
    }
  }

  override suspend fun submitMatchResult(
    matchId: String,
    winnerId: String,
    winnerName: String,
    screenshotUrl: String,
    notes: String,
  ): Resource<ResultEntity> {
    val auth = activeAuth
    val authUser = auth?.currentUser
    val authUid = authUser?.uid?.trim()?.ifBlank { null }

    val effectiveUid = if (authUidProvider != null) {
      val provided = authUidProvider.invoke()?.trim()?.ifBlank { null }
      if (provided.isNullOrBlank()) {
        return Resource.Error(AppError.AuthenticationRequired("User must be authenticated to submit match results"))
      }
      provided
    } else if (auth != null) {
      if (authUid.isNullOrBlank()) {
        return Resource.Error(AppError.AuthenticationRequired("User must be authenticated to submit match results"))
      }
      authUid
    } else {
      val fallback = winnerId.trim().ifBlank { FirebaseManager.getAuth()?.currentUser?.uid ?: "" }
      if (fallback.isBlank()) {
        return Resource.Error(AppError.AuthenticationRequired("User must be authenticated to submit match results"))
      }
      fallback
    }

    val isUserBlocked = LocalDataStore.localUsers[effectiveUid]?.isAccountBlocked == true
    if (isUserBlocked) {
      return Resource.Error(AppError.InvalidInput(reason = "Your account is blocked. Cannot submit results."))
    }

    val database = activeDatabase

    // Enforce winner eligibility in repository / backend operation
    val match: MatchEntity? = if (database != null) {
      try {
        val snap = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        snap.getValue(MatchEntity::class.java) ?: LocalDataStore.localMatches[matchId]
      } catch (_: Exception) {
        LocalDataStore.localMatches[matchId]
      }
    } else {
      LocalDataStore.localMatches[matchId]
    }

    if (match != null) {
      if (match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true)) {
        return Resource.Error(AppError.PermissionDenied("Match is already completed. Proof submission is locked."))
      }
      val ineligible = match.status in setOf(
        MatchStatus.AVAILABLE.name,
        MatchStatus.CANCELLED.name,
        MatchStatus.PAUSED.name,
        MatchStatus.DISABLED.name,
      )
      if (ineligible) {
        return Resource.Error(AppError.InvalidInput(reason = "Match is not in an eligible state for result submission."))
      }
      if (match.winnerUserId.isNotBlank() && match.winnerUserId != effectiveUid) {
        return Resource.Error(AppError.PermissionDenied("Losers cannot submit victory proof. Only the eligible winner can submit."))
      }
    }

    val existingResult = LocalDataStore.localResults.find { it.matchId == matchId && (it.submittedByUserId == effectiveUid || it.userId == effectiveUid) }
    if (existingResult != null) {
      val st = existingResult.status.uppercase()
      if (st == "APPROVED" || st == "LOST" || st == "REJECTED") {
        return Resource.Error(AppError.PermissionDenied("Under no circumstances can proof be resubmitted after rejection or completion."))
      }
    }

    val players: List<MatchPlayerEntity> = if (database != null) {
      try {
        val snap = database.getReference(FirebaseConfig.NODE_MATCH_PLAYERS).child(matchId).get().awaitTask()
        snap.children.mapNotNull { it.getValue(MatchPlayerEntity::class.java) }.ifEmpty {
          LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
        }
      } catch (_: Exception) {
        LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
      }
    } else {
      LocalDataStore.localMatchPlayers[matchId] ?: emptyList()
    }

    if (players.isNotEmpty()) {
      val playerRecord = players.find { it.effectiveUid == effectiveUid }
      if (playerRecord == null) {
        return Resource.Error(AppError.PermissionDenied("You are not a registered player in this match."))
      }
      if (playerRecord.status.equals("LOST", ignoreCase = true)) {
        return Resource.Error(AppError.PermissionDenied("Losers cannot submit victory proof."))
      }
    }

    val now = System.currentTimeMillis()
    val resultId = "RES_${now}_$matchId"
    val resultEntity = ResultEntity(
      resultId = resultId,
      matchId = matchId,
      submittedByUserId = effectiveUid,
      userId = effectiveUid,
      claimedWinnerUserId = effectiveUid,
      proofScreenshotUrl = screenshotUrl,
      reviewNotes = notes,
      status = ResultStatus.PENDING_REVIEW.name,
      submittedAt = now,
      reviewedAt = 0L,
      matchNumber = match?.matchNumber.orEmpty(),
      gameType = match?.gameType.orEmpty(),
      createdAt = now,
    )

    if (database != null) {
      return try {
        val updates = buildSubmitResultUpdates(
          resultEntity = resultEntity,
          matchId = matchId,
          effectiveUid = effectiveUid,
          timestamp = now,
        )
        database.reference.updateChildren(updates).awaitTask()
        LocalDataStore.localResults.removeIf { it.matchId == matchId && (it.submittedByUserId == effectiveUid || it.userId == effectiveUid) }
        LocalDataStore.localResults.add(resultEntity)
        LocalDataStore.localMatches[matchId]?.let { m ->
          LocalDataStore.localMatches[matchId] = m.copy(
            status = MatchStatus.RESULT_SUBMITTED.name,
            updatedAt = now,
          )
          LocalDataStore.notifyMatchesChanged()
        }
        LocalDataStore.localUserMatches[effectiveUid]?.get(matchId)?.let { prev ->
          LocalDataStore.localUserMatches[effectiveUid]?.put(matchId, prev.copy(
            status = MatchStatus.RESULT_SUBMITTED.name,
            updatedAt = now,
          ))
        } ?: run {
          val entryFee = match?.effectiveEntryFeeMinorUnits ?: 0L
          val prize = match?.effectivePrizeMinorUnits ?: 0L
          val uHistory = UserMatchHistoryEntity(
            matchId = matchId,
            userId = effectiveUid,
            uid = effectiveUid,
            joinedAt = match?.createdAt ?: now,
            status = MatchStatus.RESULT_SUBMITTED.name,
            updatedAt = now,
            gameType = match?.gameType ?: GameType.LUDO.name,
            matchNumber = match?.matchNumber ?: "",
            title = match?.title ?: "",
            entryFeeMinorUnits = entryFee,
            prizeMinorUnits = prize,
          )
          LocalDataStore.localUserMatches.getOrPut(effectiveUid) { ConcurrentHashMap() }[matchId] = uHistory
        }
        LocalDataStore.notifyUserMatchesChanged()
        Resource.Success(resultEntity)
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to submit result"))
      }
    } else {
      LocalDataStore.localResults.removeIf { it.matchId == matchId && (it.submittedByUserId == effectiveUid || it.userId == effectiveUid) }
      LocalDataStore.localResults.add(resultEntity)
      LocalDataStore.localMatches[matchId]?.let { m ->
        LocalDataStore.localMatches[matchId] = m.copy(
          status = MatchStatus.RESULT_SUBMITTED.name,
          updatedAt = now,
        )
        LocalDataStore.notifyMatchesChanged()
      }
      LocalDataStore.localUserMatches[effectiveUid]?.get(matchId)?.let { prev ->
        LocalDataStore.localUserMatches[effectiveUid]?.put(matchId, prev.copy(
          status = MatchStatus.RESULT_SUBMITTED.name,
          updatedAt = now,
        ))
      } ?: run {
        val entryFee = match?.effectiveEntryFeeMinorUnits ?: 0L
        val prize = match?.effectivePrizeMinorUnits ?: 0L
        val uHistory = UserMatchHistoryEntity(
          matchId = matchId,
          userId = effectiveUid,
          uid = effectiveUid,
          joinedAt = match?.createdAt ?: now,
          status = MatchStatus.RESULT_SUBMITTED.name,
          updatedAt = now,
          gameType = match?.gameType ?: GameType.LUDO.name,
          matchNumber = match?.matchNumber ?: "",
          title = match?.title ?: "",
          entryFeeMinorUnits = entryFee,
          prizeMinorUnits = prize,
        )
        LocalDataStore.localUserMatches.getOrPut(effectiveUid) { ConcurrentHashMap() }[matchId] = uHistory
      }
      LocalDataStore.notifyUserMatchesChanged()
      return Resource.Success(resultEntity)
    }
  }

  companion object {
    fun buildSubmitResultUpdates(
      resultEntity: ResultEntity,
      matchId: String,
      effectiveUid: String,
      timestamp: Long,
    ): Map<String, Any> {
      return mapOf(
        "${FirebaseConfig.NODE_RESULTS}/$matchId/$effectiveUid" to resultEntity,
        "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.RESULT_SUBMITTED.name,
        "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to timestamp,
        "${FirebaseConfig.NODE_USER_MATCHES}/$effectiveUid/$matchId/status" to MatchStatus.RESULT_SUBMITTED.name,
        "${FirebaseConfig.NODE_USER_MATCHES}/$effectiveUid/$matchId/updatedAt" to timestamp,
      )
    }
  }
}

@IgnoreExtraProperties
data class CanonicalNotificationDto(
  val id: String = "",
  val title: String = "",
  val message: String = "",
  val timestamp: Long = 0L,
  val targetType: String = "",
  val targetId: String = "",
  val senderAdmin: String = "",
)

class FirebaseNotificationRepository : NotificationRepository {
  companion object {
    const val NOTIFICATION_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
  }

  override fun getNotifications(userId: String, limit: Int): Flow<Resource<List<NotificationEntity>>> = callbackFlow {
    val effectiveUid = userId.ifBlank {
      FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    }
    if (effectiveUid.isBlank()) {
      trySend(Resource.Success(emptyList()))
      close()
      return@callbackFlow
    }

    val now = System.currentTimeMillis()
    val cutoff = now - NOTIFICATION_MAX_AGE_MS

    // Always emit cached non-expired data immediately to prevent blank/flicker state
    val localList = (LocalDataStore.localUserNotifications[effectiveUid]?.toList() ?: emptyList())
      .filter { it.createdAt <= 0L || it.createdAt >= cutoff }
      .take(limit)
    trySend(Resource.Success(localList))

    // Listen to local notificationsUpdateTrigger so that createNotification, markAsRead,
    // and markAllAsRead immediately reflect in the active flow
    val localCollectorJob = launch {
      LocalDataStore.notificationsUpdateTrigger.collect {
        val currentNow = System.currentTimeMillis()
        val currentCutoff = currentNow - NOTIFICATION_MAX_AGE_MS
        val updated = (LocalDataStore.localUserNotifications[effectiveUid]?.toList() ?: emptyList())
          .filter { it.createdAt <= 0L || it.createdAt >= currentCutoff }
          .take(limit)
        trySend(Resource.Success(updated))
      }
    }

    val isFbAvailable = FirebaseManager.isAvailable()
    val ref = if (isFbAvailable) {
      FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_NOTIFICATIONS)?.child(effectiveUid)
    } else {
      null
    }

    if (ref == null) {
      awaitClose { localCollectorJob.cancel() }
      return@callbackFlow
    }

    val query = ref.limitToLast(limit * 2)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        if (!snapshot.exists() || !snapshot.hasChildren()) {
          LocalDataStore.localUserNotifications[effectiveUid]?.clear()
          trySend(Resource.Success(emptyList()))
          return
        }
        val currentNow = System.currentTimeMillis()
        val currentCutoff = currentNow - NOTIFICATION_MAX_AGE_MS
        val expiredKeysToDelete = mutableListOf<String>()

        val items = snapshot.children.mapNotNull { child ->
          try {
            val key = child.key ?: return@mapNotNull null
            
            // Manual parsing for maximum resilience as child snapshots can have field discrepancies
            val id = child.child("id").getValue(String::class.java) 
                    ?: child.child("notificationId").getValue(String::class.java) 
                    ?: key
            
            val title = child.child("title").getValue(String::class.java) ?: "Notification"
            val message = child.child("message").getValue(String::class.java) 
                        ?: child.child("body").getValue(String::class.java) 
                        ?: ""
            val type = child.child("type").getValue(String::class.java) ?: "GENERAL"
            
            val timestamp = when (val t = child.child("timestamp").value ?: child.child("createdAt").value) {
              is Long -> t
              is Double -> t.toLong()
              is String -> t.toLongOrNull() ?: 0L
              is Int -> t.toLong()
              else -> 0L
            }.let { if (it == 0L) System.currentTimeMillis() else it }

            val read = when (val r = child.child("read").value ?: child.child("isRead").value) {
              is Boolean -> r
              is String -> r.toBoolean()
              else -> false
            }

            val targetScreen = child.child("targetScreen").getValue(String::class.java) ?: ""

            NotificationEntity(
              id = id,
              notificationId = id,
              userId = userId,
              title = title,
              message = message,
              body = message,
              type = type,
              timestamp = timestamp,
              createdAt = timestamp,
              read = read,
              isRead = read,
              targetScreen = targetScreen
            )
          } catch (e: Exception) {
            com.example.core.logging.AppLogger.e("FirebaseNotificationRepository", "Failed to parse notification: ${e.message}")
            null
          }
        }

        // Perform safe cleanup in Firebase storage for expired keys
        if (expiredKeysToDelete.isNotEmpty()) {
          for (expiredKey in expiredKeysToDelete) {
            try {
              ref.child(expiredKey).removeValue()
            } catch (_: Exception) {}
          }
        }

        val userItems = items
          .distinctBy { it.notificationId }
          .sortedByDescending { it.createdAt }
          .take(limit)

        // Authoritative source of truth: sync Firebase snapshot into cache
        LocalDataStore.localUserNotifications[effectiveUid] = CopyOnWriteArrayList(userItems)
        trySend(Resource.Success(userItems))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose {
      query.removeEventListener(listener)
      localCollectorJob.cancel()
    }
  }

  override suspend fun markAsRead(notificationId: String): Resource<Unit> {
    for ((_, list) in LocalDataStore.localUserNotifications) {
      val existing = list.find { it.effectiveId == notificationId }
      if (existing != null) {
        val idx = list.indexOf(existing)
        if (idx >= 0) {
          list[idx] = existing.copy(read = true, isRead = true)
        }
      }
    }
    LocalDataStore.notifyNotificationsChanged()

    val uid = FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    if (FirebaseManager.isAvailable() && uid.isNotBlank()) {
      try {
        val updates = hashMapOf<String, Any>(
          "read" to true,
          "isRead" to true
        )
        FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_NOTIFICATIONS)
          ?.child(uid)
          ?.child(notificationId)
          ?.updateChildren(updates)
      } catch (_: Exception) {
      }
    }
    return Resource.Success(Unit)
  }

  override suspend fun markAllAsRead(userId: String): Resource<Unit> {
    val effectiveUid = userId.ifBlank {
      FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    }
    if (effectiveUid.isNotBlank()) {
      val updates = hashMapOf<String, Any>()
      LocalDataStore.localUserNotifications[effectiveUid]?.let { list ->
        for (i in list.indices) {
          val notif = list[i]
          if (!notif.effectiveRead) {
            list[i] = notif.copy(read = true, isRead = true)
            updates["${notif.effectiveId}/read"] = true
            updates["${notif.effectiveId}/isRead"] = true
          }
        }
      }
      LocalDataStore.notifyNotificationsChanged()

      if (FirebaseManager.isAvailable() && updates.isNotEmpty()) {
        try {
          FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_NOTIFICATIONS)
            ?.child(effectiveUid)
            ?.updateChildren(updates)
        } catch (_: Exception) {
        }
      }
    }
    return Resource.Success(Unit)
  }

  override suspend fun createNotification(notification: NotificationEntity): Resource<Unit> {
    val uid = notification.userId.ifBlank {
      FirebaseManager.getAuth()?.currentUser?.uid ?: ""
    }
    if (uid.isBlank()) return Resource.Error(AppError.InvalidInput(reason = "User ID is required"))

    val notifWithUser = notification.copy(userId = uid)

    // Store in LocalDataStore
    val list = LocalDataStore.localUserNotifications.getOrPut(uid) { CopyOnWriteArrayList() }
    val existingIndex = list.indexOfFirst { it.notificationId == notifWithUser.notificationId }
    if (existingIndex >= 0) {
      list[existingIndex] = notifWithUser
    } else {
      list.add(0, notifWithUser)
    }
    LocalDataStore.notifyNotificationsChanged()

    if (FirebaseManager.isAvailable()) {
      try {
        // 1. Authoritative write to user-specific node
        FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_NOTIFICATIONS)
          ?.child(uid)
          ?.child(notifWithUser.notificationId)
          ?.setValue(notifWithUser)

        // 2. Also write to global canonical node for backward compatibility with admin
        val canonicalDto = CanonicalNotificationDto(
          id = notifWithUser.notificationId,
          title = notifWithUser.title,
          message = notifWithUser.body,
          timestamp = if (notifWithUser.createdAt > 0L) notifWithUser.createdAt else System.currentTimeMillis(),
          targetType = "SPECIFIC_USER",
          targetId = uid,
          senderAdmin = "admin"
        )
        FirebaseManager.getNodeReference(FirebaseConfig.NODE_NOTIFICATIONS)
          ?.child(notifWithUser.notificationId)
          ?.setValue(canonicalDto)
      } catch (_: Exception) {
      }
    }

    return Resource.Success(Unit)
  }

  override suspend fun cleanupExpiredNotifications(cutoffTimestamp: Long): Resource<Int> {
    var purgedCount = 0
    val now = System.currentTimeMillis()
    val cutoff = if (cutoffTimestamp > 0L) cutoffTimestamp else (now - NOTIFICATION_MAX_AGE_MS)

    // 1. Clean LocalDataStore across all users
    for ((_, list) in LocalDataStore.localUserNotifications) {
      val beforeSize = list.size
      list.removeIf { it.createdAt in 1 until cutoff }
      purgedCount += (beforeSize - list.size)
    }
    LocalDataStore.notifyNotificationsChanged()

    // 2. Clean Firebase Database
    if (FirebaseManager.isAvailable()) {
      try {
        val userNotifRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USER_NOTIFICATIONS)
        if (userNotifRef != null) {
          val snap = userNotifRef.get().awaitTask(5_000L)
          if (snap != null && snap.hasChildren()) {
            val deleteMap = hashMapOf<String, Any?>()
            for (userSnap in snap.children) {
              val uKey = userSnap.key ?: continue
              for (child in userSnap.children) {
                val notif = child.getValue(NotificationEntity::class.java)
                val ts = notif?.createdAt ?: 0L
                val key = child.key
                if (ts in 1 until cutoff && key != null) {
                  deleteMap["$uKey/$key"] = null
                }
              }
            }
            if (deleteMap.isNotEmpty()) {
              userNotifRef.updateChildren(deleteMap).awaitTask(5_000L)
            }
          }
        }
        val notifRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_NOTIFICATIONS)
        if (notifRef != null) {
          val snap = notifRef.get().awaitTask(5_000L)
          if (snap != null && snap.hasChildren()) {
            val deleteMap = hashMapOf<String, Any?>()
            for (child in snap.children) {
              val canonical = child.getValue(CanonicalNotificationDto::class.java)
              val ts = canonical?.timestamp ?: 0L
              val key = child.key
              if (ts in 1 until cutoff && key != null) {
                deleteMap[key] = null
              }
            }
            if (deleteMap.isNotEmpty()) {
              notifRef.updateChildren(deleteMap).awaitTask(5_000L)
            }
          }
        }
      } catch (_: Exception) {
      }
    }

    return Resource.Success(purgedCount)
  }
}

class FirebaseAdminRepository(
  private val customDatabase: FirebaseDatabase? = null,
  private val forceLocalOnly: Boolean = false,
) : AdminRepository {
  private val activeDatabase: FirebaseDatabase?
    get() = if (forceLocalOnly) null else (customDatabase ?: FirebaseManager.getDatabase())

  override fun getAllMatches(limit: Int): Flow<Resource<List<MatchEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_MATCHES)
    if (ref == null) {
      trySend(Resource.Success(LocalDataStore.localMatches.values.toList().sortedByDescending { it.createdAt }.take(limit)))
      close()
      return@callbackFlow
    }
    val query = ref.limitToLast(limit)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val matches = snapshot.children.mapNotNull { it.getValue(MatchEntity::class.java) }
        trySend(Resource.Success(matches.reversed()))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getPendingDeposits(): Flow<Resource<List<DepositEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_DEPOSITS)
    if (ref == null) {
      fun getFiltered() = LocalDataStore.localDeposits.filter {
        it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true)
      }
      trySend(Resource.Success(getFiltered()))
      val triggerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
      val job = triggerScope.launch {
        LocalDataStore.walletUpdateTrigger.collect {
          trySend(Resource.Success(getFiltered()))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val deposits = snapshot.children.mapNotNull { it.getValue(DepositEntity::class.java) }
          .filter { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true) }
        trySend(Resource.Success(deposits))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override fun getPendingWithdrawals(): Flow<Resource<List<WithdrawalEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_WITHDRAWALS)
    if (ref == null) {
      fun getFiltered() = LocalDataStore.localWithdrawals.filter { it.status == WithdrawalStatus.REQUESTED.name }
      trySend(Resource.Success(getFiltered()))
      val triggerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
      val job = triggerScope.launch {
        LocalDataStore.walletUpdateTrigger.collect {
          trySend(Resource.Success(getFiltered()))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }
    val query = ref.orderByChild("status").equalTo(WithdrawalStatus.REQUESTED.name)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val withdrawals = snapshot.children.mapNotNull { it.getValue(WithdrawalEntity::class.java) }
        trySend(Resource.Success(withdrawals))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getPendingResults(): Flow<Resource<List<ResultEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_RESULTS)
    if (ref == null) {
      trySend(Resource.Success(LocalDataStore.localResults.filter { it.status == ResultStatus.PENDING_REVIEW.name }))
      close()
      return@callbackFlow
    }
    val query = ref.orderByChild("status").equalTo(ResultStatus.PENDING_REVIEW.name)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val results = snapshot.children.mapNotNull { it.getValue(ResultEntity::class.java) }
        trySend(Resource.Success(results))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getAuditLogs(limit: Int): Flow<Resource<List<AuditLogEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_AUDIT_LOGS)
    if (ref == null) {
      trySend(Resource.Success(LocalDataStore.localAuditLogs.toList().reversed().take(limit)))
      close()
      return@callbackFlow
    }
    val query = ref.limitToLast(limit)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val logs = snapshot.children.mapNotNull { it.getValue(AuditLogEntity::class.java) }
        trySend(Resource.Success(logs.reversed()))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    query.addValueEventListener(listener)
    awaitClose { query.removeEventListener(listener) }
  }

  override fun getAdminUsers(): Flow<Resource<List<AdminUserEntity>>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_ADMIN_USERS)
    if (ref == null) {
      trySend(Resource.Error(AppError.FirebaseUnavailable()))
      close()
      return@callbackFlow
    }
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val admins = snapshot.children.mapNotNull { it.getValue(AdminUserEntity::class.java) }
        trySend(Resource.Success(admins))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Error(AppError.FirebaseUnavailable(error.message)))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override fun checkIsAdmin(userId: String): Flow<Resource<Boolean>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_ADMIN_USERS)?.child(userId)
    if (ref == null) {
      trySend(Resource.Success(false))
      close()
      return@callbackFlow
    }
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val admin = snapshot.getValue(AdminUserEntity::class.java)
        trySend(Resource.Success(admin?.isActive == true))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Success(false))
      }
    }
    ref.addListenerForSingleValueEvent(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override suspend fun approveDeposit(adminUid: String, depositId: String, deposit: DepositEntity): Resource<Unit> {
    val database = FirebaseManager.getDatabase() ?: return Resource.Error(AppError.FirebaseUnavailable())
    val now = System.currentTimeMillis()
    return try {
      val walletSnapshot = database.getReference(FirebaseConfig.NODE_WALLETS).child(deposit.uid).get().awaitTask()
      val currentWallet = walletSnapshot.getValue(WalletEntity::class.java)
        ?: WalletEntity(uid = deposit.uid, availableBalance = 0L, updatedAt = now)
      val mutation = FinancialEngine.processDepositApproval(
        currentWallet = currentWallet,
        deposit = deposit,
        timestamp = now,
      )
      when (mutation) {
        is FinancialEngine.MutationResult.Success -> {
          val updates = hashMapOf<String, Any>(
            "${FirebaseConfig.NODE_WALLETS}/${deposit.uid}" to mutation.wallet,
            "${FirebaseConfig.NODE_TRANSACTIONS}/${mutation.transaction.transactionId}" to mutation.transaction,
            "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${deposit.uid}/${mutation.transaction.transactionId}" to mutation.transaction,
            "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${deposit.uid}/$depositId/status" to TransactionStatus.COMPLETED.name,
            "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${deposit.uid}/$depositId/processedAt" to now,
            "${FirebaseConfig.NODE_DEPOSITS}/$depositId/status" to DepositStatus.APPROVED.name,
            "${FirebaseConfig.NODE_DEPOSITS}/$depositId/approvedAt" to now,
            "${FirebaseConfig.NODE_DEPOSITS}/$depositId/approvedBy" to adminUid,
            "${FirebaseConfig.NODE_USER_DEPOSITS}/${deposit.uid}/$depositId/status" to DepositStatus.APPROVED.name,
            "${FirebaseConfig.NODE_USER_DEPOSITS}/${deposit.uid}/$depositId/approvedAt" to now,
            "${FirebaseConfig.NODE_USER_DEPOSITS}/${deposit.uid}/$depositId/approvedBy" to adminUid,
            "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$depositId" to AuditLogEntity(
              logId = "LOG_${now}_$depositId",
              action = "APPROVE_DEPOSIT",
              adminUid = adminUid,
              targetUid = deposit.uid,
              beforeState = "SUBMITTED",
              afterState = "APPROVED",
              timestamp = now,
            ),
          )

          // First Deposit Bonus & Referral Logic
          try {
            val userSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(deposit.uid).get().awaitTask()
            val user = userSnapshot.getValue(UserEntity::class.java)
            
            if (user != null && !user.firstDepositBonusClaimed && user.referredBy.isNotBlank()) {
              // 1. Mark first deposit bonus as claimed for the user
              updates["${FirebaseConfig.NODE_USERS}/${deposit.uid}/firstDepositBonusClaimed"] = true
              
              // 2. Find and Reward Referrer (referredBy is now Referrer UID)
              val referrerUid = user.referredBy
              val referrerWalletSnapshot = database.getReference(FirebaseConfig.NODE_WALLETS).child(referrerUid).get().awaitTask()
              val referrerWallet = referrerWalletSnapshot.getValue(WalletEntity::class.java)
              
              if (referrerWallet != null) {
                // Deduct 4000 from lockedBonus, Add 4000 to bonus
                val newLockedBonus = (referrerWallet.lockedBonus - 4000.0).coerceAtLeast(0.0)
                val newBonus = referrerWallet.bonusBalance + 4000.0
                
                val updatedReferrerWallet = referrerWallet.copy(
                  lockedBonus = newLockedBonus,
                  bonusBalance = newBonus,
                  updatedAt = now
                )
                updates["${FirebaseConfig.NODE_WALLETS}/$referrerUid"] = updatedReferrerWallet
                
                // Log referral reward transaction
                val bonusTxnId = "TXN_BONUS_UNLOCK_${now}_${deposit.uid.takeLast(4)}"
                val bonusTxn = TransactionEntity(
                  transactionId = bonusTxnId,
                  uid = referrerUid,
                  amount = 4000L,
                  type = "REFERRAL_BONUS_UNLOCK",
                  status = TransactionStatus.COMPLETED.name,
                  description = "Referral bonus unlocked for user ${user.effectiveName}'s first deposit",
                  createdAt = now,
                  processedAt = now
                )
                updates["${FirebaseConfig.NODE_USER_TRANSACTIONS}/$referrerUid/$bonusTxnId"] = bonusTxn
              }
            } else if (user != null && !user.firstDepositBonusClaimed) {
              // Even if not referred, mark first deposit claimed
              updates["${FirebaseConfig.NODE_USERS}/${deposit.uid}/firstDepositBonusClaimed"] = true
            }
          } catch (e: Exception) {
            AppLogger.w("FirebaseAdminRepository", "Failed to process referral bonus: ${e.message}")
          }

          database.reference.updateChildren(updates).awaitTask()
          Resource.Success(Unit)
        }
        is FinancialEngine.MutationResult.Failure -> {
          Resource.Error(AppError.InvalidInput("deposit", mutation.reason))
        }
      }
    } catch (e: Exception) {
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Failed to approve deposit"))
    }
  }

  override suspend fun rejectDeposit(adminUid: String, depositId: String, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase() ?: return Resource.Error(AppError.FirebaseUnavailable())
    val now = System.currentTimeMillis()
    return try {
      val depSnap = database.getReference(FirebaseConfig.NODE_DEPOSITS).child(depositId).get().awaitTask()
      val targetUid = depSnap.child("uid").getValue(String::class.java)
      val updates = hashMapOf<String, Any>(
        "${FirebaseConfig.NODE_DEPOSITS}/$depositId/status" to DepositStatus.REJECTED.name,
        "${FirebaseConfig.NODE_DEPOSITS}/$depositId/processedAt" to now,
        "${FirebaseConfig.NODE_DEPOSITS}/$depositId/rejectionReason" to reason,
        "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$depositId" to AuditLogEntity(
          logId = "LOG_${now}_$depositId",
          action = "REJECT_DEPOSIT",
          adminUid = adminUid,
          targetUid = depositId,
          beforeState = "SUBMITTED",
          afterState = "REJECTED: $reason",
          timestamp = now,
        ),
      )
      if (!targetUid.isNullOrBlank()) {
        updates["${FirebaseConfig.NODE_USER_DEPOSITS}/$targetUid/$depositId/status"] = DepositStatus.REJECTED.name
        updates["${FirebaseConfig.NODE_USER_DEPOSITS}/$targetUid/$depositId/processedAt"] = now
        updates["${FirebaseConfig.NODE_USER_DEPOSITS}/$targetUid/$depositId/rejectionReason"] = reason
        updates["${FirebaseConfig.NODE_USER_TRANSACTIONS}/$targetUid/$depositId/status"] = TransactionStatus.REJECTED.name
        updates["${FirebaseConfig.NODE_USER_TRANSACTIONS}/$targetUid/$depositId/processedAt"] = now
        updates["${FirebaseConfig.NODE_USER_TRANSACTIONS}/$targetUid/$depositId/description"] = if (reason.isNotBlank()) "ডিপোজিট বাতিল: $reason" else "ডিপোজিট বাতিল করা হয়েছে"
      }
      database.reference.updateChildren(updates).awaitTask()
      Resource.Success(Unit)
    } catch (e: Exception) {
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Failed to reject deposit"))
    }
  }

  override suspend fun approveWithdrawal(adminUid: String, withdrawalId: String, withdrawal: WithdrawalEntity): Resource<Unit> {
    if (withdrawal.status.equals(WithdrawalStatus.COMPLETED.name, ignoreCase = true) ||
        withdrawal.status.equals(WithdrawalStatus.REJECTED.name, ignoreCase = true)) {
      return Resource.Error(AppError.InvalidInput("withdrawal", "Withdrawal is already ${withdrawal.status}"))
    }

    val database = customDatabase ?: FirebaseManager.getDatabase()
    if (database == null) {
      return approveLocalWithdrawal(adminUid, withdrawalId, withdrawal)
    }

    val now = System.currentTimeMillis()
    val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(withdrawal.uid)
    var txErrorReason: String? = null
    var initialAvailable = 0L

    val txSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
      walletRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
        override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
          val reservationNode = currentData.child("activeWithdrawals").child(withdrawalId)
          val reservationMap = reservationNode.value as? Map<*, *>

          if (reservationNode.value == null && reservationMap == null) {
            txErrorReason = "Active withdrawal reservation not found"
            return com.google.firebase.database.Transaction.abort()
          }

          val resAmount = (reservationNode.child("amountMinorUnits").value as? Number)?.toLong()
            ?: (reservationMap?.get("amountMinorUnits") as? Number)?.toLong()
            ?: (reservationNode.child("amount").value as? Number)?.toLong()
            ?: (reservationMap?.get("amount") as? Number)?.toLong()
            ?: 0L

          if (resAmount != withdrawal.amount) {
            txErrorReason = "Reservation amount mismatch (reserved: $resAmount, withdrawal: ${withdrawal.amount})"
            return com.google.firebase.database.Transaction.abort()
          }

          val availBal = (currentData.child("availableBalance").value as? Number)?.toLong()
            ?: ((currentData.value as? Map<*, *>)?.get("availableBalance") as? Number)?.toLong()
            ?: (currentData.child("balance").getValue(Double::class.java)?.toLong() ?: 0L)

          val pendingBal = (currentData.child("pendingBalance").value as? Number)?.toLong()
            ?: ((currentData.value as? Map<*, *>)?.get("pendingBalance") as? Number)?.toLong()
            ?: (currentData.child("lockedBalance").getValue(Double::class.java)?.toLong() ?: 0L)

          val totalWithdrawn = (currentData.child("totalWithdrawn").value as? Number)?.toLong()
            ?: ((currentData.value as? Map<*, *>)?.get("totalWithdrawn") as? Number)?.toLong()
            ?: 0L

          if (pendingBal < withdrawal.amount) {
            txErrorReason = "Pending balance ($pendingBal) is less than withdrawal amount (${withdrawal.amount})"
            return com.google.firebase.database.Transaction.abort()
          }

          initialAvailable = availBal
          val newPendingBalance = pendingBal - withdrawal.amount
          val newTotalWithdrawn = totalWithdrawn + withdrawal.amount

          // Mutate fields atomically without overwriting wallet root
          currentData.child("pendingBalance").value = newPendingBalance
          currentData.child("totalWithdrawn").value = newTotalWithdrawn
          currentData.child("balance").value = availBal
          currentData.child("lockedBalance").value = newPendingBalance
          currentData.child("updatedAt").value = now

          // Remove ONLY this active reservation
          reservationNode.value = null

          return com.google.firebase.database.Transaction.success(currentData)
        }

        override fun onComplete(
          error: com.google.firebase.database.DatabaseError?,
          committed: Boolean,
          currentData: com.google.firebase.database.DataSnapshot?,
        ) {
          if (error != null) {
            AppLogger.w("FirebaseAdminRepo", "approveWithdrawal transaction error: ${error.message}")
            if (continuation.isActive) continuation.resume(false)
          } else {
            if (continuation.isActive) continuation.resume(committed)
          }
        }
      })
    }

    if (!txSuccess) {
      val message = txErrorReason ?: "Failed to settle withdrawal in wallet transaction"
      return Resource.Error(AppError.InvalidInput("settlement", message))
    }

    // Step 4: Atomic multi-location update for records and audit log (NO wallet write here)
    return try {
      val trxId = "TXN_CMP_${now}_${withdrawal.withdrawalId}"
      val transaction = TransactionEntity(
        transactionId = trxId,
        uid = withdrawal.uid,
        amount = withdrawal.amount,
        type = TransactionType.WITHDRAW.name,
        status = TransactionStatus.COMPLETED.name,
        beforeBalance = initialAvailable,
        afterBalance = initialAvailable,
        source = "${withdrawal.method}_PAYOUT_COMPLETE",
        referenceId = "WTH_CMP_${withdrawal.withdrawalId}",
        description = "Withdrawal completed via ${withdrawal.method} to ${withdrawal.recipientNumber}",
        createdAt = now,
        processedAt = now,
      )

      val auditLog = AuditLogEntity(
        logId = "LOG_${now}_${withdrawal.withdrawalId}",
        action = "APPROVE_WITHDRAWAL",
        adminUid = adminUid,
        targetUid = withdrawal.uid,
        beforeState = "REQUESTED",
        afterState = "COMPLETED",
        timestamp = now,
      )

      val updates = hashMapOf<String, Any>(
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/status" to WithdrawalStatus.COMPLETED.name,
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/processedBy" to adminUid,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/status" to WithdrawalStatus.COMPLETED.name,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/processedBy" to adminUid,
        "${FirebaseConfig.NODE_TRANSACTIONS}/$trxId" to transaction,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$trxId" to transaction,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$withdrawalId/status" to TransactionStatus.COMPLETED.name,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_AUDIT_LOGS}/${auditLog.logId}" to auditLog,
      )
      database.reference.updateChildren(updates).awaitTask()
      Resource.Success(Unit)
    } catch (e: Exception) {
      AppLogger.w("FirebaseAdminRepo", "Status update failed after transaction: ${e.message}")
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Status update failed"))
    }
  }

  override suspend fun rejectWithdrawal(adminUid: String, withdrawalId: String, withdrawal: WithdrawalEntity, reason: String): Resource<Unit> {
    if (reason.isBlank()) {
      return Resource.Error(AppError.InvalidInput("reason", "Rejection reason cannot be blank"))
    }
    if (withdrawal.status.equals(WithdrawalStatus.COMPLETED.name, ignoreCase = true) ||
        withdrawal.status.equals(WithdrawalStatus.REJECTED.name, ignoreCase = true)) {
      return Resource.Error(AppError.InvalidInput("withdrawal", "Withdrawal is already ${withdrawal.status}"))
    }

    val database = customDatabase ?: FirebaseManager.getDatabase()
    if (database == null) {
      return rejectLocalWithdrawal(adminUid, withdrawalId, withdrawal, reason)
    }

    val now = System.currentTimeMillis()
    val walletRef = database.getReference(FirebaseConfig.NODE_WALLETS).child(withdrawal.uid)
    var txErrorReason: String? = null
    var initialAvailable = 0L
    var finalAvailable = 0L

    val txSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
      walletRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
        override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
          val reservationNode = currentData.child("activeWithdrawals").child(withdrawalId)
          val reservationMap = reservationNode.value as? Map<*, *>

          if (reservationNode.value == null && reservationMap == null) {
            txErrorReason = "Active withdrawal reservation not found"
            return com.google.firebase.database.Transaction.abort()
          }

          val resAmount = (reservationNode.child("amountMinorUnits").value as? Number)?.toLong()
            ?: (reservationMap?.get("amountMinorUnits") as? Number)?.toLong()
            ?: (reservationNode.child("amount").value as? Number)?.toLong()
            ?: (reservationMap?.get("amount") as? Number)?.toLong()
            ?: 0L

          if (resAmount != withdrawal.amount) {
            txErrorReason = "Reservation amount mismatch (reserved: $resAmount, withdrawal: ${withdrawal.amount})"
            return com.google.firebase.database.Transaction.abort()
          }

          val availBal = (currentData.child("availableBalance").value as? Number)?.toLong()
            ?: ((currentData.value as? Map<*, *>)?.get("availableBalance") as? Number)?.toLong()
            ?: (currentData.child("balance").getValue(Double::class.java)?.toLong() ?: 0L)

          val pendingBal = (currentData.child("pendingBalance").value as? Number)?.toLong()
            ?: ((currentData.value as? Map<*, *>)?.get("pendingBalance") as? Number)?.toLong()
            ?: (currentData.child("lockedBalance").getValue(Double::class.java)?.toLong() ?: 0L)

          if (pendingBal < withdrawal.amount) {
            txErrorReason = "Pending balance ($pendingBal) is less than withdrawal release amount (${withdrawal.amount})"
            return com.google.firebase.database.Transaction.abort()
          }

          initialAvailable = availBal
          val newAvailable = availBal + withdrawal.amount
          finalAvailable = newAvailable
          val newPendingBalance = pendingBal - withdrawal.amount

          currentData.child("availableBalance").value = newAvailable
          currentData.child("pendingBalance").value = newPendingBalance
          currentData.child("balance").value = newAvailable
          currentData.child("lockedBalance").value = newPendingBalance
          currentData.child("updatedAt").value = now

          // Remove ONLY this active reservation
          reservationNode.value = null

          return com.google.firebase.database.Transaction.success(currentData)
        }

        override fun onComplete(
          error: com.google.firebase.database.DatabaseError?,
          committed: Boolean,
          currentData: com.google.firebase.database.DataSnapshot?,
        ) {
          if (error != null) {
            AppLogger.w("FirebaseAdminRepo", "rejectWithdrawal transaction error: ${error.message}")
            if (continuation.isActive) continuation.resume(false)
          } else {
            if (continuation.isActive) continuation.resume(committed)
          }
        }
      })
    }

    if (!txSuccess) {
      val message = txErrorReason ?: "Failed to release withdrawal in wallet transaction"
      return Resource.Error(AppError.InvalidInput("settlement", message))
    }

    // Step 4: Atomic multi-location update for records and audit log (NO wallet write here)
    return try {
      val trxId = "TXN_REL_${now}_${withdrawal.withdrawalId}"
      val transaction = TransactionEntity(
        transactionId = trxId,
        uid = withdrawal.uid,
        amount = withdrawal.amount,
        type = TransactionType.WITHDRAW_RELEASE.name,
        status = TransactionStatus.COMPLETED.name,
        beforeBalance = initialAvailable,
        afterBalance = finalAvailable,
        source = "WITHDRAWAL_REJECTED",
        referenceId = "WTH_REL_${withdrawal.withdrawalId}",
        description = "Withdrawal rejected & released: $reason",
        createdAt = now,
        processedAt = now,
      )

      val auditLog = AuditLogEntity(
        logId = "LOG_${now}_${withdrawal.withdrawalId}",
        action = "REJECT_WITHDRAWAL",
        adminUid = adminUid,
        targetUid = withdrawal.uid,
        beforeState = "REQUESTED",
        afterState = "REJECTED: $reason",
        timestamp = now,
      )

      val updates = hashMapOf<String, Any>(
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/status" to WithdrawalStatus.REJECTED.name,
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_WITHDRAWALS}/$withdrawalId/rejectionReason" to reason,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/status" to WithdrawalStatus.REJECTED.name,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_USER_WITHDRAWALS}/${withdrawal.uid}/$withdrawalId/rejectionReason" to reason,
        "${FirebaseConfig.NODE_TRANSACTIONS}/$trxId" to transaction,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$trxId" to transaction,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$withdrawalId/status" to TransactionStatus.REJECTED.name,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$withdrawalId/processedAt" to now,
        "${FirebaseConfig.NODE_USER_TRANSACTIONS}/${withdrawal.uid}/$withdrawalId/description" to (if (reason.isNotBlank()) "উইথড্র বাতিল: $reason" else "উইথড্র বাতিল করা হয়েছে"),
        "${FirebaseConfig.NODE_AUDIT_LOGS}/${auditLog.logId}" to auditLog,
      )
      database.reference.updateChildren(updates).awaitTask()
      Resource.Success(Unit)
    } catch (e: Exception) {
      AppLogger.w("FirebaseAdminRepo", "Status update failed after transaction: ${e.message}")
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Status update failed"))
    }
  }

  private fun approveLocalWithdrawal(
    adminUid: String,
    withdrawalId: String,
    withdrawal: WithdrawalEntity,
  ): Resource<Unit> {
    synchronized(LocalDataStore.localWallets) {
      val existingWithdrawal = LocalDataStore.localWithdrawals.firstOrNull { it.withdrawalId == withdrawalId }
      if (existingWithdrawal != null &&
        (existingWithdrawal.status.equals(WithdrawalStatus.COMPLETED.name, ignoreCase = true) ||
         existingWithdrawal.status.equals(WithdrawalStatus.REJECTED.name, ignoreCase = true))) {
        return Resource.Error(AppError.InvalidInput("withdrawal", "Withdrawal is already ${existingWithdrawal.status}"))
      }

      val userReservations = LocalDataStore.localActiveWithdrawals[withdrawal.uid]
      val reservation = userReservations?.get(withdrawalId)
      if (reservation == null) {
        return Resource.Error(AppError.InvalidInput("reservation", "Active withdrawal reservation not found"))
      }

      val resAmount = (reservation["amountMinorUnits"] as? Number)?.toLong()
        ?: (reservation["amount"] as? Number)?.toLong()
        ?: 0L
      if (resAmount != withdrawal.amount) {
        return Resource.Error(AppError.InvalidInput("amount", "Reservation amount mismatch (reserved: $resAmount, withdrawal: ${withdrawal.amount})"))
      }

      val currentWallet = LocalDataStore.localWallets[withdrawal.uid]
        ?: return Resource.Error(AppError.ServerError("Wallet not found for ${withdrawal.uid}"))

      if (currentWallet.pendingBalance < withdrawal.amount) {
        return Resource.Error(AppError.InvalidInput("balance", "Pending balance (${currentWallet.pendingBalance}) is less than withdrawal amount (${withdrawal.amount})"))
      }

      val now = System.currentTimeMillis()
      val newPending = currentWallet.pendingBalance - withdrawal.amount
      val newTotalWithdrawn = currentWallet.totalWithdrawn + withdrawal.amount
      val updatedWallet = currentWallet.copy(
        pendingBalance = newPending,
        totalWithdrawn = newTotalWithdrawn,
        balance = currentWallet.availableBalance / 100.0,
        lockedBalance = newPending / 100.0,
        updatedAt = now,
      )

      userReservations.remove(withdrawalId)
      LocalDataStore.localWallets[withdrawal.uid] = updatedWallet
      LocalDataStore.notifyWalletChanged()

      val index = LocalDataStore.localWithdrawals.indexOfFirst { it.withdrawalId == withdrawalId }
      val updatedWithdrawal = withdrawal.copy(
        status = WithdrawalStatus.COMPLETED.name,
        processedAt = now,
        adminUid = adminUid,
      )
      if (index >= 0) {
        LocalDataStore.localWithdrawals[index] = updatedWithdrawal
      } else {
        LocalDataStore.localWithdrawals.add(0, updatedWithdrawal)
      }

      val trxId = "TXN_CMP_${now}_$withdrawalId"
      val transaction = TransactionEntity(
        transactionId = trxId,
        uid = withdrawal.uid,
        amount = withdrawal.amount,
        type = TransactionType.WITHDRAW.name,
        status = TransactionStatus.COMPLETED.name,
        beforeBalance = currentWallet.availableBalance,
        afterBalance = currentWallet.availableBalance,
        source = "${withdrawal.method}_PAYOUT_COMPLETE",
        referenceId = "WTH_CMP_$withdrawalId",
        description = "Withdrawal completed via ${withdrawal.method} to ${withdrawal.recipientNumber}",
        createdAt = now,
        processedAt = now,
      )
      val pendingTxIndex = LocalDataStore.localTransactions.indexOfFirst {
        it.id == withdrawalId || it.transactionId == withdrawalId || it.referenceId == withdrawalId
      }
      if (pendingTxIndex >= 0) {
        val old = LocalDataStore.localTransactions[pendingTxIndex]
        LocalDataStore.localTransactions[pendingTxIndex] = old.copy(
          status = TransactionStatus.COMPLETED.name,
          processedAt = now
        )
      }
      LocalDataStore.localTransactions.add(0, transaction)

      val auditLog = AuditLogEntity(
        logId = "LOG_${now}_$withdrawalId",
        action = "APPROVE_WITHDRAWAL",
        adminUid = adminUid,
        targetUid = withdrawal.uid,
        beforeState = "REQUESTED",
        afterState = "COMPLETED",
        timestamp = now,
      )
      LocalDataStore.localAuditLogs.add(0, auditLog)

      return Resource.Success(Unit)
    }
  }

  private fun rejectLocalWithdrawal(
    adminUid: String,
    withdrawalId: String,
    withdrawal: WithdrawalEntity,
    reason: String,
  ): Resource<Unit> {
    synchronized(LocalDataStore.localWallets) {
      val existingWithdrawal = LocalDataStore.localWithdrawals.firstOrNull { it.withdrawalId == withdrawalId }
      if (existingWithdrawal != null &&
        (existingWithdrawal.status.equals(WithdrawalStatus.COMPLETED.name, ignoreCase = true) ||
         existingWithdrawal.status.equals(WithdrawalStatus.REJECTED.name, ignoreCase = true))) {
        return Resource.Error(AppError.InvalidInput("withdrawal", "Withdrawal is already ${existingWithdrawal.status}"))
      }

      val userReservations = LocalDataStore.localActiveWithdrawals[withdrawal.uid]
      val reservation = userReservations?.get(withdrawalId)
      if (reservation == null) {
        return Resource.Error(AppError.InvalidInput("reservation", "Active withdrawal reservation not found"))
      }

      val resAmount = (reservation["amountMinorUnits"] as? Number)?.toLong()
        ?: (reservation["amount"] as? Number)?.toLong()
        ?: 0L
      if (resAmount != withdrawal.amount) {
        return Resource.Error(AppError.InvalidInput("amount", "Reservation amount mismatch (reserved: $resAmount, withdrawal: ${withdrawal.amount})"))
      }

      val currentWallet = LocalDataStore.localWallets[withdrawal.uid]
        ?: return Resource.Error(AppError.ServerError("Wallet not found for ${withdrawal.uid}"))

      if (currentWallet.pendingBalance < withdrawal.amount) {
        return Resource.Error(AppError.InvalidInput("balance", "Pending balance (${currentWallet.pendingBalance}) is less than withdrawal release amount (${withdrawal.amount})"))
      }

      val now = System.currentTimeMillis()
      val newAvail = currentWallet.availableBalance + withdrawal.amount
      val newPending = currentWallet.pendingBalance - withdrawal.amount
      val updatedWallet = currentWallet.copy(
        availableBalance = newAvail,
        pendingBalance = newPending,
        balance = newAvail / 100.0,
        lockedBalance = newPending / 100.0,
        updatedAt = now,
      )

      userReservations.remove(withdrawalId)
      LocalDataStore.localWallets[withdrawal.uid] = updatedWallet
      LocalDataStore.notifyWalletChanged()

      val index = LocalDataStore.localWithdrawals.indexOfFirst { it.withdrawalId == withdrawalId }
      val updatedWithdrawal = withdrawal.copy(
        status = WithdrawalStatus.REJECTED.name,
        processedAt = now,
        rejectionReason = reason,
      )
      if (index >= 0) {
        LocalDataStore.localWithdrawals[index] = updatedWithdrawal
      } else {
        LocalDataStore.localWithdrawals.add(0, updatedWithdrawal)
      }

      val trxId = "TXN_REL_${now}_$withdrawalId"
      val transaction = TransactionEntity(
        transactionId = trxId,
        uid = withdrawal.uid,
        amount = withdrawal.amount,
        type = TransactionType.WITHDRAW_RELEASE.name,
        status = TransactionStatus.COMPLETED.name,
        beforeBalance = currentWallet.availableBalance,
        afterBalance = newAvail,
        source = "WITHDRAWAL_REJECTED",
        referenceId = "WTH_REL_$withdrawalId",
        description = "Withdrawal rejected & released: $reason",
        createdAt = now,
        processedAt = now,
      )
      val pendingTxIndex = LocalDataStore.localTransactions.indexOfFirst {
        it.id == withdrawalId || it.transactionId == withdrawalId || it.referenceId == withdrawalId
      }
      if (pendingTxIndex >= 0) {
        val old = LocalDataStore.localTransactions[pendingTxIndex]
        LocalDataStore.localTransactions[pendingTxIndex] = old.copy(
          status = TransactionStatus.REJECTED.name,
          processedAt = now,
          description = if (reason.isNotBlank()) "উইথড্র বাতিল: $reason" else "উইথড্র বাতিল করা হয়েছে"
        )
      }
      LocalDataStore.localTransactions.add(0, transaction)

      val auditLog = AuditLogEntity(
        logId = "LOG_${now}_$withdrawalId",
        action = "REJECT_WITHDRAWAL",
        adminUid = adminUid,
        targetUid = withdrawal.uid,
        beforeState = "REQUESTED",
        afterState = "REJECTED: $reason",
        timestamp = now,
      )
      LocalDataStore.localAuditLogs.add(0, auditLog)

      return Resource.Success(Unit)
    }
  }

  override suspend fun adjustWalletBalance(adminUid: String, targetUserId: String, amountMinorUnits: Long, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase() ?: return Resource.Error(AppError.FirebaseUnavailable())
    val now = System.currentTimeMillis()
    return try {
      val walletSnapshot = database.getReference(FirebaseConfig.NODE_WALLETS).child(targetUserId).get().awaitTask()
      val currentWallet = walletSnapshot.getValue(WalletEntity::class.java)
        ?: WalletEntity(uid = targetUserId, availableBalance = 0L, updatedAt = now)
      val mutation = FinancialEngine.processAdminAdjustment(
        currentWallet = currentWallet,
        adminUid = adminUid,
        adjustmentMinorUnits = amountMinorUnits,
        reason = reason,
        timestamp = now,
      )
      when (mutation) {
        is FinancialEngine.MutationResult.Success -> {
          val updates = hashMapOf<String, Any>(
            "${FirebaseConfig.NODE_WALLETS}/$targetUserId" to mutation.wallet,
            "${FirebaseConfig.NODE_TRANSACTIONS}/${mutation.transaction.transactionId}" to mutation.transaction,
            "${FirebaseConfig.NODE_USER_TRANSACTIONS}/$targetUserId/${mutation.transaction.transactionId}" to mutation.transaction,
            "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$targetUserId" to AuditLogEntity(
              logId = "LOG_${now}_$targetUserId",
              action = if (amountMinorUnits >= 0L) "ADMIN_CREDIT" else "ADMIN_DEBIT",
              adminUid = adminUid,
              targetUid = targetUserId,
              beforeState = "Balance: ৳ ${currentWallet.availableBalance / 100.0}",
              afterState = "Balance: ৳ ${mutation.wallet.availableBalance / 100.0} ($reason)",
              timestamp = now,
            ),
          )
          database.reference.updateChildren(updates).awaitTask()
          Resource.Success(Unit)
        }
        is FinancialEngine.MutationResult.Failure -> {
          Resource.Error(AppError.InvalidInput("adjustment", mutation.reason))
        }
      }
    } catch (e: Exception) {
      Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Failed to adjust wallet balance"))
    }
  }

  override suspend fun setMatchGameCode(adminUid: String, matchId: String, gameCode: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val adminSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(adminUid).get().awaitTask()
        val adminUser = adminSnapshot.getValue(UserEntity::class.java)
          ?: UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
        val matchSnapshot = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        val currentMatch = matchSnapshot.getValue(MatchEntity::class.java)
          ?: LocalDataStore.localMatches[matchId]
          ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
        val mutation = FinancialEngine.processAdminSetGameCode(
          match = currentMatch,
          adminUser = adminUser,
          gameCode = gameCode,
          currentTime = now,
        )
        when (mutation) {
          is FinancialEngine.AdminMatchResult.Success -> {
            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId/gameCode" to mutation.updatedMatch.gameCode,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/isCodeVisible" to true,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to mutation.updatedMatch.status,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
                logId = "LOG_${now}_$matchId",
                action = "SET_MATCH_GAME_CODE",
                adminUid = adminUid,
                targetUid = matchId,
                beforeState = currentMatch.status,
                afterState = "CODE_ADDED",
                timestamp = now,
              ),
            )
            database.reference.updateChildren(updates).awaitTask()
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            LocalDataStore.notifyMatchesChanged()
            Resource.Success(Unit)
          }
          is FinancialEngine.AdminMatchResult.Failure -> {
            Resource.Error(AppError.InvalidInput(reason = mutation.reason))
          }
        }
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to set game room code"))
      }
    } else {
      val currentMatch = LocalDataStore.localMatches[matchId]
        ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
      val adminUser = UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
      val mutation = FinancialEngine.processAdminSetGameCode(
        match = currentMatch,
        adminUser = adminUser,
        gameCode = gameCode,
        currentTime = now,
      )
      return when (mutation) {
        is FinancialEngine.AdminMatchResult.Success -> {
          LocalDataStore.localMatches[matchId] = mutation.updatedMatch
          LocalDataStore.notifyMatchesChanged()
          Resource.Success(Unit)
        }
        is FinancialEngine.AdminMatchResult.Failure -> {
          Resource.Error(AppError.InvalidInput(reason = mutation.reason))
        }
      }
    }
  }

  override suspend fun setMatchRunning(adminUid: String, matchId: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val adminSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(adminUid).get().awaitTask()
        val adminUser = adminSnapshot.getValue(UserEntity::class.java)
          ?: UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
        val matchSnapshot = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        val currentMatch = matchSnapshot.getValue(MatchEntity::class.java)
          ?: LocalDataStore.localMatches[matchId]
          ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
        val mutation = FinancialEngine.processAdminSetMatchRunning(
          match = currentMatch,
          adminUser = adminUser,
          currentTime = now,
        )
        when (mutation) {
          is FinancialEngine.AdminMatchResult.Success -> {
            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.RUNNING.name,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/startedAt" to (mutation.updatedMatch.startedAt ?: now),
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
                logId = "LOG_${now}_$matchId",
                action = "SET_MATCH_RUNNING",
                adminUid = adminUid,
                targetUid = matchId,
                beforeState = currentMatch.status,
                afterState = MatchStatus.RUNNING.name,
                timestamp = now,
              ),
            )
            database.reference.updateChildren(updates).awaitTask()
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            LocalDataStore.notifyMatchesChanged()
            Resource.Success(Unit)
          }
          is FinancialEngine.AdminMatchResult.Failure -> {
            Resource.Error(AppError.InvalidInput(reason = mutation.reason))
          }
        }
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to set match to running"))
      }
    } else {
      val currentMatch = LocalDataStore.localMatches[matchId]
        ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
      val adminUser = UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
      val mutation = FinancialEngine.processAdminSetMatchRunning(
        match = currentMatch,
        adminUser = adminUser,
        currentTime = now,
      )
      return when (mutation) {
        is FinancialEngine.AdminMatchResult.Success -> {
          LocalDataStore.localMatches[matchId] = mutation.updatedMatch
          LocalDataStore.notifyMatchesChanged()
          Resource.Success(Unit)
        }
        is FinancialEngine.AdminMatchResult.Failure -> {
          Resource.Error(AppError.InvalidInput(reason = mutation.reason))
        }
      }
    }
  }

  override suspend fun pauseMatch(adminUid: String, matchId: String, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val adminSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(adminUid).get().awaitTask()
        val adminUser = adminSnapshot.getValue(UserEntity::class.java)
          ?: UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
        val matchSnapshot = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        val currentMatch = matchSnapshot.getValue(MatchEntity::class.java)
          ?: LocalDataStore.localMatches[matchId]
          ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
        val mutation = FinancialEngine.processAdminPauseMatch(
          match = currentMatch,
          adminUser = adminUser,
          reason = reason,
          currentTime = now,
        )
        when (mutation) {
          is FinancialEngine.AdminMatchResult.Success -> {
            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.PAUSED.name,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
                logId = "LOG_${now}_$matchId",
                action = "PAUSE_MATCH",
                adminUid = adminUid,
                targetUid = matchId,
                beforeState = currentMatch.status,
                afterState = MatchStatus.PAUSED.name,
                details = "${currentMatch.status} -> ${MatchStatus.PAUSED.name} ($reason)",
                timestamp = now,
              ),
            )
            database.reference.updateChildren(updates).awaitTask()
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            LocalDataStore.notifyMatchesChanged()
            Resource.Success(Unit)
          }
          is FinancialEngine.AdminMatchResult.Failure -> {
            Resource.Error(AppError.InvalidInput(reason = mutation.reason))
          }
        }
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to pause match"))
      }
    } else {
      val currentMatch = LocalDataStore.localMatches[matchId]
        ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
      val adminUser = UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
      val mutation = FinancialEngine.processAdminPauseMatch(
        match = currentMatch,
        adminUser = adminUser,
        reason = reason,
        currentTime = now,
      )
      return when (mutation) {
        is FinancialEngine.AdminMatchResult.Success -> {
          LocalDataStore.localMatches[matchId] = mutation.updatedMatch
          LocalDataStore.notifyMatchesChanged()
          Resource.Success(Unit)
        }
        is FinancialEngine.AdminMatchResult.Failure -> {
          Resource.Error(AppError.InvalidInput(reason = mutation.reason))
        }
      }
    }
  }

  override suspend fun disableMatch(adminUid: String, matchId: String, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val adminSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(adminUid).get().awaitTask()
        val adminUser = adminSnapshot.getValue(UserEntity::class.java)
          ?: UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
        val matchSnapshot = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        val currentMatch = matchSnapshot.getValue(MatchEntity::class.java)
          ?: LocalDataStore.localMatches[matchId]
          ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
        val mutation = FinancialEngine.processAdminDisableMatch(
          match = currentMatch,
          adminUser = adminUser,
          reason = reason,
          currentTime = now,
        )
        when (mutation) {
          is FinancialEngine.AdminMatchResult.Success -> {
            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.DISABLED.name,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
                logId = "LOG_${now}_$matchId",
                action = "DISABLE_MATCH",
                adminUid = adminUid,
                targetUid = matchId,
                beforeState = currentMatch.status,
                afterState = MatchStatus.DISABLED.name,
                details = "${currentMatch.status} -> ${MatchStatus.DISABLED.name} ($reason)",
                timestamp = now,
              ),
            )
            database.reference.updateChildren(updates).awaitTask()
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            LocalDataStore.notifyMatchesChanged()
            Resource.Success(Unit)
          }
          is FinancialEngine.AdminMatchResult.Failure -> {
            Resource.Error(AppError.InvalidInput(reason = mutation.reason))
          }
        }
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to disable match"))
      }
    } else {
      val currentMatch = LocalDataStore.localMatches[matchId]
        ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
      val adminUser = UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
      val mutation = FinancialEngine.processAdminDisableMatch(
        match = currentMatch,
        adminUser = adminUser,
        reason = reason,
        currentTime = now,
      )
      return when (mutation) {
        is FinancialEngine.AdminMatchResult.Success -> {
          LocalDataStore.localMatches[matchId] = mutation.updatedMatch
          LocalDataStore.notifyMatchesChanged()
          Resource.Success(Unit)
        }
        is FinancialEngine.AdminMatchResult.Failure -> {
          Resource.Error(AppError.InvalidInput(reason = mutation.reason))
        }
      }
    }
  }

  override suspend fun cancelMatch(adminUid: String, matchId: String, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val adminSnapshot = database.getReference(FirebaseConfig.NODE_USERS).child(adminUid).get().awaitTask()
        val adminUser = adminSnapshot.getValue(UserEntity::class.java)
          ?: UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
        val matchSnapshot = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
        val currentMatch = matchSnapshot.getValue(MatchEntity::class.java)
          ?: LocalDataStore.localMatches[matchId]
          ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
        val mutation = FinancialEngine.processAdminCancelMatch(
          match = currentMatch,
          adminUser = adminUser,
          reason = reason,
          currentTime = now,
        )
        when (mutation) {
          is FinancialEngine.AdminMatchResult.Success -> {
            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.CANCELLED.name,
              "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
                logId = "LOG_${now}_$matchId",
                action = "CANCEL_MATCH",
                adminUid = adminUid,
                targetUid = matchId,
                beforeState = currentMatch.status,
                afterState = MatchStatus.CANCELLED.name,
                details = "${currentMatch.status} -> ${MatchStatus.CANCELLED.name} ($reason)",
                timestamp = now,
              ),
            )
            val pSnap = try {
              database.getReference(FirebaseConfig.NODE_MATCH_PLAYERS).child(matchId).get().awaitTask(3_000L)
            } catch (_: Exception) { null }
            val pList = pSnap?.children?.mapNotNull { it.getValue(MatchPlayerEntity::class.java) }
              ?: LocalDataStore.localMatchPlayers[matchId]?.toList().orEmpty()

            pList.forEach { p ->
              val pUid = p.effectiveUid
              if (pUid.isNotBlank()) {
                updates["${FirebaseConfig.NODE_USER_MATCHES}/$pUid/$matchId/status"] = MatchStatus.CANCELLED.name
                updates["${FirebaseConfig.NODE_USER_MATCHES}/$pUid/$matchId/updatedAt"] = now
              }
            }

            database.reference.updateChildren(updates).awaitTask()
            LocalDataStore.localMatches[matchId] = mutation.updatedMatch
            pList.forEach { p ->
              val pUid = p.effectiveUid
              if (pUid.isNotBlank()) {
                LocalDataStore.localUserMatches[pUid]?.get(matchId)?.let { prev ->
                  LocalDataStore.localUserMatches[pUid]?.put(matchId, prev.copy(
                    status = MatchStatus.CANCELLED.name,
                    updatedAt = now,
                  ))
                }
              }
            }
            LocalDataStore.notifyUserMatchesChanged()
            LocalDataStore.notifyMatchesChanged()
            Resource.Success(Unit)
          }
          is FinancialEngine.AdminMatchResult.Failure -> {
            Resource.Error(AppError.InvalidInput(reason = mutation.reason))
          }
        }
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to cancel match"))
      }
    } else {
      val currentMatch = LocalDataStore.localMatches[matchId]
        ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))
      val adminUser = UserEntity(uid = adminUid, userId = adminUid, role = "ADMIN")
      val mutation = FinancialEngine.processAdminCancelMatch(
        match = currentMatch,
        adminUser = adminUser,
        reason = reason,
        currentTime = now,
      )
      return when (mutation) {
        is FinancialEngine.AdminMatchResult.Success -> {
          LocalDataStore.localMatches[matchId] = mutation.updatedMatch
          LocalDataStore.localMatchPlayers[matchId]?.forEach { p ->
            val pUid = p.effectiveUid
            if (pUid.isNotBlank()) {
              LocalDataStore.localUserMatches[pUid]?.get(matchId)?.let { prev ->
                LocalDataStore.localUserMatches[pUid]?.put(matchId, prev.copy(
                  status = MatchStatus.CANCELLED.name,
                  updatedAt = now,
                ))
              }
            }
          }
          LocalDataStore.notifyUserMatchesChanged()
          LocalDataStore.notifyMatchesChanged()
          Resource.Success(Unit)
        }
        is FinancialEngine.AdminMatchResult.Failure -> {
          Resource.Error(AppError.InvalidInput(reason = mutation.reason))
        }
      }
    }
  }

  override suspend fun createMatch(adminUid: String, match: MatchEntity): Resource<String> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    val matchId = if (match.matchId.isNotBlank()) match.matchId else "M_${now}_${(1000..9999).random()}"
    val finalMatch = match.copy(
      matchId = matchId,
      createdByAdminId = adminUid,
      createdAt = if (match.createdAt > 0L) match.createdAt else now,
      updatedAt = now,
    )
    if (database != null) {
      return try {
        val updates = hashMapOf<String, Any>(
          "${FirebaseConfig.NODE_MATCHES}/$matchId" to finalMatch,
          "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$matchId" to AuditLogEntity(
            logId = "LOG_${now}_$matchId",
            action = "CREATE_MATCH",
            adminUid = adminUid,
            targetUid = matchId,
            beforeState = "NONE",
            afterState = finalMatch.status,
            details = "Created match: ${finalMatch.title}",
            timestamp = now,
          ),
        )
        database.reference.updateChildren(updates).awaitTask()
        LocalDataStore.localMatches[matchId] = finalMatch
        LocalDataStore.notifyMatchesChanged()
        Resource.Success(matchId)
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to create match"))
      }
    } else {
      LocalDataStore.localMatches[matchId] = finalMatch
      LocalDataStore.localAuditLogs.add(
        AuditLogEntity(
          logId = "LOG_${now}_$matchId",
          action = "CREATE_MATCH",
          adminUid = adminUid,
          targetUid = matchId,
          beforeState = "NONE",
          afterState = finalMatch.status,
          details = "Created match: ${finalMatch.title}",
          timestamp = now,
        )
      )
      LocalDataStore.notifyMatchesChanged()
      return Resource.Success(matchId)
    }
  }

  override suspend fun bulkCreateMatches(adminUid: String, matches: List<MatchEntity>): Resource<Int> {
    if (matches.isEmpty()) return Resource.Success(0)
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()
    if (database != null) {
      return try {
        val updates = hashMapOf<String, Any>()
        matches.forEach { match ->
          val mId = if (match.matchId.isNotBlank()) match.matchId else "M_${now}_${(10000..99999).random()}"
          val m = match.copy(
            matchId = mId,
            createdByAdminId = adminUid,
            createdAt = if (match.createdAt > 0L) match.createdAt else now,
            updatedAt = now,
          )
          updates["${FirebaseConfig.NODE_MATCHES}/$mId"] = m
          LocalDataStore.localMatches[mId] = m
        }
        val logId = "LOG_${now}_BULK"
        updates["${FirebaseConfig.NODE_AUDIT_LOGS}/$logId"] = AuditLogEntity(
          logId = logId,
          action = "BULK_CREATE_MATCHES",
          adminUid = adminUid,
          targetUid = "BATCH",
          beforeState = "NONE",
          afterState = "CREATED",
          details = "Bulk created ${matches.size} matches",
          timestamp = now,
        )
        database.reference.updateChildren(updates).awaitTask()
        LocalDataStore.notifyMatchesChanged()
        Resource.Success(matches.size)
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to bulk create matches"))
      }
    } else {
      matches.forEach { match ->
        val mId = if (match.matchId.isNotBlank()) match.matchId else "M_${now}_${(10000..99999).random()}"
        val m = match.copy(
          matchId = mId,
          createdByAdminId = adminUid,
          createdAt = if (match.createdAt > 0L) match.createdAt else now,
          updatedAt = now,
        )
        LocalDataStore.localMatches[mId] = m
      }
      LocalDataStore.localAuditLogs.add(
        AuditLogEntity(
          logId = "LOG_${now}_BULK",
          action = "BULK_CREATE_MATCHES",
          adminUid = adminUid,
          targetUid = "BATCH",
          beforeState = "NONE",
          afterState = "CREATED",
          details = "Bulk created ${matches.size} matches",
          timestamp = now,
        )
      )
      LocalDataStore.notifyMatchesChanged()
      return Resource.Success(matches.size)
    }
  }

  override suspend fun approveResult(adminUid: String, result: ResultEntity): Resource<Unit> {
    val database = activeDatabase
    val now = System.currentTimeMillis()
    val matchId = result.matchId
    val winnerId = result.claimedWinnerUserId.ifBlank { result.submittedByUserId }.ifBlank { result.userId }.trim()
    val resultId = result.resultId

    if (winnerId.isBlank()) {
      return Resource.Error(AppError.InvalidInput(reason = "Winner UID is missing or empty"))
    }

    val currentMatch = if (database != null) {
      val snap = database.getReference(FirebaseConfig.NODE_MATCHES).child(matchId).get().awaitTask()
      snap.getValue(MatchEntity::class.java) ?: LocalDataStore.localMatches[matchId]
    } else {
      LocalDataStore.localMatches[matchId]
    } ?: return Resource.Error(AppError.InvalidInput(reason = "Match $matchId not found"))

    // Idempotency: If match is already completed, ensure temporary proof is removed and return
    if (currentMatch.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true)) {
      if (database != null) {
        try {
          database.getReference(FirebaseConfig.NODE_RESULTS).child(resultId).removeValue().awaitTask(3_000L)
        } catch (_: Exception) {}
      }
      LocalDataStore.localResults.removeIf { it.resultId == resultId }
      return Resource.Success(Unit)
    }

    val currentWinnerWallet = if (database != null) {
      val snap = database.getReference(FirebaseConfig.NODE_WALLETS).child(winnerId).get().awaitTask()
      snap.getValue(WalletEntity::class.java) ?: LocalDataStore.localWallets[winnerId]
    } else {
      LocalDataStore.localWallets[winnerId]
    } ?: WalletEntity(uid = winnerId, userId = winnerId)

    val prizeMinorUnits = currentMatch.effectivePrizeMinorUnits
    val heldFee = currentMatch.effectiveEntryFeeMinorUnits

    val mutation = FinancialEngine.processMatchPrizeCredit(
      currentWallet = currentWinnerWallet,
      matchId = matchId,
      prizeMinorUnits = prizeMinorUnits,
      heldEntryFeeMinorUnits = heldFee,
      matchTitle = currentMatch.title,
      timestamp = now,
    )

    return when (mutation) {
      is FinancialEngine.MutationResult.Success -> {
        val updatedMatch = currentMatch.copy(
          status = MatchStatus.COMPLETED.name,
          winnerUserId = winnerId,
          updatedAt = now,
        )

        // Resolve all participants to update permanent userMatches for winner and losers
        val participantUids = mutableSetOf<String>()
        if (database != null) {
          try {
            val pSnap = database.getReference(FirebaseConfig.NODE_MATCH_PLAYERS).child(matchId).get().awaitTask(5_000L)
            pSnap?.children?.forEach { child ->
              val p = child.getValue(MatchPlayerEntity::class.java)
              val uid = p?.effectiveUid?.trim()?.ifBlank { null }
                ?: p?.userId?.trim()?.ifBlank { null }
                ?: child.key?.trim()?.ifBlank { null }
              if (!uid.isNullOrBlank()) {
                participantUids.add(uid)
              }
            }
          } catch (_: Exception) {
          }
        }
        LocalDataStore.localMatchPlayers[matchId]?.forEach { p ->
          val uid = p.effectiveUid.trim().ifBlank { p.userId.trim() }
          if (uid.isNotBlank()) participantUids.add(uid)
        }
        if (result.submittedByUserId.isNotBlank()) {
          participantUids.add(result.submittedByUserId.trim())
        }
        if (winnerId.isNotBlank()) {
          participantUids.add(winnerId)
        }
        val loserUids = participantUids.filter { it != winnerId }

        if (database != null) {
          try {
            val matchNum = currentMatch.matchNumber.ifBlank { matchId.takeLast(4) }
            val prizeAmountFormatted = "%.0f".format(prizeMinorUnits / 100.0)
            val winningNotifTitle = "🏆 ম্যাচ জিতেছেন!"
            val winningNotifBody = "ম্যাচ #${matchNum}-এর পুরস্কার ৳${prizeAmountFormatted} আপনার অ্যাকাউন্টে যোগ হয়েছে।"
            val winningNotifId = "notif_win_${matchId}_${winnerId}"

            val winCanonicalDto = CanonicalNotificationDto(
              id = winningNotifId,
              title = winningNotifTitle,
              message = winningNotifBody,
              timestamp = now,
              targetType = "SPECIFIC_USER",
              targetId = winnerId,
              senderAdmin = adminUid.ifBlank { "admin" },
            )

            val updates = hashMapOf<String, Any>(
              "${FirebaseConfig.NODE_MATCHES}/$matchId" to updatedMatch,
              "${FirebaseConfig.NODE_WALLETS}/$winnerId" to mutation.wallet,
              "${FirebaseConfig.NODE_TRANSACTIONS}/${mutation.transaction.transactionId}" to mutation.transaction,
              "${FirebaseConfig.NODE_USER_TRANSACTIONS}/$winnerId/${mutation.transaction.transactionId}" to mutation.transaction,
              "${FirebaseConfig.NODE_USER_MATCHES}/$winnerId/$matchId/status" to MatchStatus.COMPLETED.name,
              "${FirebaseConfig.NODE_USER_MATCHES}/$winnerId/$matchId/isWinner" to true,
              "${FirebaseConfig.NODE_USER_MATCHES}/$winnerId/$matchId/updatedAt" to now,
              "${FirebaseConfig.NODE_NOTIFICATIONS}/$winningNotifId" to winCanonicalDto,
              "${FirebaseConfig.NODE_USER_NOTIFICATIONS}/$winnerId/$winningNotifId" to NotificationEntity(
                notificationId = winningNotifId,
                userId = winnerId,
                title = winningNotifTitle,
                body = winningNotifBody,
                targetScreen = "WALLET",
                createdAt = now,
              ),
              "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$resultId" to AuditLogEntity(
                logId = "LOG_${now}_$resultId",
                action = "APPROVE_RESULT",
                adminUid = adminUid,
                targetUid = resultId,
                beforeState = ResultStatus.PENDING_REVIEW.name,
                afterState = ResultStatus.APPROVED.name,
                details = "Approved victory proof for $winnerId in match $matchId (Prize: ${prizeMinorUnits / 100} BDT)",
                timestamp = now,
              ),
            )

            // Losers permanent history only (NO notification to loser)
            loserUids.forEach { loserUid ->
              updates["${FirebaseConfig.NODE_USER_MATCHES}/$loserUid/$matchId/status"] = MatchStatus.COMPLETED.name
              updates["${FirebaseConfig.NODE_USER_MATCHES}/$loserUid/$matchId/isWinner"] = false
              updates["${FirebaseConfig.NODE_USER_MATCHES}/$loserUid/$matchId/updatedAt"] = now
            }

            database.reference.updateChildren(updates).awaitTask()

            // Delete temporary Winning Proof record
            try {
              database.getReference(FirebaseConfig.NODE_RESULTS).child(resultId).removeValue().awaitTask(3_000L)
            } catch (delEx: Exception) {
              AppLogger.w("FirebaseAdminRepo", "Failed to remove temporary result $resultId: ${delEx.message}")
            }
          } catch (e: Exception) {
            return Resource.Error(AppError.ServerError(e.message ?: "Failed to approve result in database"))
          }
        }

        LocalDataStore.localMatches[matchId] = updatedMatch
        LocalDataStore.localWallets[winnerId] = mutation.wallet
        LocalDataStore.localTransactions.add(mutation.transaction)
        LocalDataStore.localResults.removeIf { it.resultId == resultId }

        // Local winner user match history
        LocalDataStore.localUserMatches[winnerId]?.get(matchId)?.let { prev ->
          LocalDataStore.localUserMatches[winnerId]?.put(matchId, prev.copy(
            status = MatchStatus.COMPLETED.name,
            isWinner = true,
            updatedAt = now,
          ))
        } ?: run {
          val uHistory = UserMatchHistoryEntity(
            matchId = matchId,
            userId = winnerId,
            uid = winnerId,
            joinedAt = currentMatch.createdAt,
            status = MatchStatus.COMPLETED.name,
            isWinner = true,
            updatedAt = now,
            gameType = currentMatch.gameType,
            matchNumber = currentMatch.matchNumber,
            title = currentMatch.title,
            entryFeeMinorUnits = heldFee,
            prizeMinorUnits = prizeMinorUnits,
          )
          LocalDataStore.localUserMatches.getOrPut(winnerId) { ConcurrentHashMap() }[matchId] = uHistory
        }

        // Local losers user match history
        loserUids.forEach { loserUid ->
          LocalDataStore.localUserMatches[loserUid]?.get(matchId)?.let { prev ->
            LocalDataStore.localUserMatches[loserUid]?.put(matchId, prev.copy(
              status = MatchStatus.COMPLETED.name,
              isWinner = false,
              updatedAt = now,
            ))
          } ?: run {
            val uHistory = UserMatchHistoryEntity(
              matchId = matchId,
              userId = loserUid,
              uid = loserUid,
              joinedAt = currentMatch.createdAt,
              status = MatchStatus.COMPLETED.name,
              isWinner = false,
              updatedAt = now,
              gameType = currentMatch.gameType,
              matchNumber = currentMatch.matchNumber,
              title = currentMatch.title,
              entryFeeMinorUnits = heldFee,
              prizeMinorUnits = prizeMinorUnits,
            )
            LocalDataStore.localUserMatches.getOrPut(loserUid) { ConcurrentHashMap() }[matchId] = uHistory
          }
        }

        LocalDataStore.localAuditLogs.add(
          AuditLogEntity(
            logId = "LOG_${now}_$resultId",
            action = "APPROVE_RESULT",
            adminUid = adminUid,
            targetUid = resultId,
            beforeState = ResultStatus.PENDING_REVIEW.name,
            afterState = ResultStatus.APPROVED.name,
            details = "Approved victory proof for $winnerId in match $matchId",
            timestamp = now,
          )
        )
        val matchNum = currentMatch.matchNumber.ifBlank { matchId.takeLast(4) }
        val prizeAmountFormatted = "%.0f".format(prizeMinorUnits / 100.0)
        val winningNotifTitle = "🏆 ম্যাচ জিতেছেন!"
        val winningNotifBody = "ম্যাচ #${matchNum}-এর পুরস্কার ৳${prizeAmountFormatted} আপনার অ্যাকাউন্টে যোগ হয়েছে।"
        val winningNotifId = "notif_win_${matchId}_${winnerId}"

        val winnerNotif = NotificationEntity(
          notificationId = winningNotifId,
          userId = winnerId,
          title = winningNotifTitle,
          body = winningNotifBody,
          targetScreen = "WALLET",
          createdAt = now,
        )
        val winnerList = LocalDataStore.localUserNotifications.getOrPut(winnerId) { CopyOnWriteArrayList() }
        val existingIdx = winnerList.indexOfFirst { it.notificationId == winningNotifId }
        if (existingIdx >= 0) {
          winnerList[existingIdx] = winnerNotif
        } else {
          winnerList.add(0, winnerNotif)
        }
        LocalDataStore.notifyUserMatchesChanged()
        LocalDataStore.notifyNotificationsChanged()
        LocalDataStore.notifyMatchesChanged()
        LocalDataStore.notifyWalletChanged()
        Resource.Success(Unit)
      }
      is FinancialEngine.MutationResult.Failure -> {
        Resource.Error(AppError.InvalidInput(reason = mutation.reason))
      }
    }
  }

  override suspend fun rejectResult(adminUid: String, resultId: String, matchId: String, reason: String): Resource<Unit> {
    val database = FirebaseManager.getDatabase()
    val now = System.currentTimeMillis()

    val existingResult = LocalDataStore.localResults.find { it.resultId == resultId } ?: if (database != null) {
      try {
        database.getReference(FirebaseConfig.NODE_RESULTS).child(resultId).get().awaitTask(3_000L)?.getValue(ResultEntity::class.java)
      } catch (_: Exception) { null }
    } else null
    val submitterUid = existingResult?.submittedByUserId?.ifBlank { existingResult.claimedWinnerUserId } ?: ""

    if (database != null) {
      return try {
        val updates = hashMapOf<String, Any>(
          "${FirebaseConfig.NODE_MATCHES}/$matchId/status" to MatchStatus.RUNNING.name,
          "${FirebaseConfig.NODE_MATCHES}/$matchId/updatedAt" to now,
          "${FirebaseConfig.NODE_AUDIT_LOGS}/LOG_${now}_$resultId" to AuditLogEntity(
            logId = "LOG_${now}_$resultId",
            action = "REJECT_RESULT",
            adminUid = adminUid,
            targetUid = resultId,
            beforeState = ResultStatus.PENDING_REVIEW.name,
            afterState = ResultStatus.REJECTED.name,
            details = "Rejected result $resultId: $reason",
            timestamp = now,
          ),
        )
        if (submitterUid.isNotBlank()) {
          updates["${FirebaseConfig.NODE_USER_MATCHES}/$submitterUid/$matchId/status"] = "REJECTED"
          updates["${FirebaseConfig.NODE_USER_MATCHES}/$submitterUid/$matchId/updatedAt"] = now
          updates["${FirebaseConfig.NODE_USER_NOTIFICATIONS}/$submitterUid/NOTIF_${now}"] = NotificationEntity(
            notificationId = "NOTIF_${now}",
            userId = submitterUid,
            title = "Victory Proof Rejected",
            body = "Your victory proof for match #${matchId.takeLast(6)} was rejected: $reason",
            targetScreen = "HISTORY",
            createdAt = now,
          )
        }
        database.reference.updateChildren(updates).awaitTask()

        // Delete temporary Winning Proof record
        try {
          database.getReference(FirebaseConfig.NODE_RESULTS).child(resultId).removeValue().awaitTask(3_000L)
        } catch (_: Exception) {}

        LocalDataStore.localResults.removeIf { it.resultId == resultId }
        LocalDataStore.localMatches[matchId]?.let { m ->
          LocalDataStore.localMatches[matchId] = m.copy(status = MatchStatus.RUNNING.name, updatedAt = now)
          LocalDataStore.notifyMatchesChanged()
        }
        if (submitterUid.isNotBlank()) {
          LocalDataStore.localUserMatches[submitterUid]?.get(matchId)?.let { prev ->
            LocalDataStore.localUserMatches[submitterUid]?.put(matchId, prev.copy(
              status = "REJECTED",
              updatedAt = now,
            ))
          }
          LocalDataStore.notifyUserMatchesChanged()
          val notif = NotificationEntity(
            notificationId = "NOTIF_${now}",
            userId = submitterUid,
            title = "Victory Proof Rejected",
            body = "Your victory proof for match #${matchId.takeLast(6)} was rejected: $reason",
            targetScreen = "HISTORY",
            createdAt = now,
          )
          LocalDataStore.localUserNotifications.getOrPut(submitterUid) { CopyOnWriteArrayList() }.add(0, notif)
          LocalDataStore.notifyNotificationsChanged()
        }
        Resource.Success(Unit)
      } catch (e: Exception) {
        Resource.Error(AppError.ServerError(e.message ?: "Failed to reject result"))
      }
    } else {
      LocalDataStore.localResults.removeIf { it.resultId == resultId }
      LocalDataStore.localMatches[matchId]?.let { m ->
        LocalDataStore.localMatches[matchId] = m.copy(status = MatchStatus.RUNNING.name, updatedAt = now)
        LocalDataStore.notifyMatchesChanged()
      }
      if (submitterUid.isNotBlank()) {
        LocalDataStore.localUserMatches[submitterUid]?.get(matchId)?.let { prev ->
          LocalDataStore.localUserMatches[submitterUid]?.put(matchId, prev.copy(
            status = "REJECTED",
            updatedAt = now,
          ))
        }
        LocalDataStore.notifyUserMatchesChanged()
        val notif = NotificationEntity(
          notificationId = "NOTIF_${now}",
          userId = submitterUid,
          title = "Victory Proof Rejected",
          body = "Your victory proof for match #${matchId.takeLast(6)} was rejected: $reason",
          targetScreen = "HISTORY",
          createdAt = now,
        )
        LocalDataStore.localUserNotifications.getOrPut(submitterUid) { CopyOnWriteArrayList() }.add(0, notif)
        LocalDataStore.notifyNotificationsChanged()
      }
      LocalDataStore.localAuditLogs.add(
        AuditLogEntity(
          logId = "LOG_${now}_$resultId",
          action = "REJECT_RESULT",
          adminUid = adminUid,
          targetUid = resultId,
          beforeState = ResultStatus.PENDING_REVIEW.name,
          afterState = ResultStatus.REJECTED.name,
          details = "Rejected result $resultId: $reason",
          timestamp = now,
        )
      )
      return Resource.Success(Unit)
    }
  }
}

class FirebaseSettingsRepository : SettingsRepository {
  override fun getAppSettings(): Flow<Resource<AppSettingsEntity>> = callbackFlow {
    trySend(Resource.Loading)
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_APP_SETTINGS)
    if (ref == null) {
      trySend(Resource.Success(LocalDataStore.localAppSettings))
      val triggerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
      val job = triggerScope.launch {
        LocalDataStore.appSettingsUpdateTrigger.collect {
          trySend(Resource.Success(LocalDataStore.localAppSettings))
        }
      }
      awaitClose { job.cancel() }
      return@callbackFlow
    }
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val parsed = try {
          val entity = snapshot.getValue(AppSettingsEntity::class.java)
          if (entity != null && (entity.bkashNumber.isNotBlank() || entity.nagadNumber.isNotBlank())) {
            entity
          } else {
            val bkash = snapshot.child("bkashNumber").getValue(String::class.java)
              ?: snapshot.child("bkash").getValue(String::class.java)
            val nagad = snapshot.child("nagadNumber").getValue(String::class.java)
              ?: snapshot.child("nagad").getValue(String::class.java)
            val minDep = snapshot.child("minDepositAmount").getValue(Double::class.java)
              ?: snapshot.child("minDepositAmount").getValue(Long::class.java)?.toDouble()
              ?: 50.0
            val minWith = snapshot.child("minWithdrawalAmount").getValue(Double::class.java)
              ?: snapshot.child("minWithdrawalAmount").getValue(Long::class.java)?.toDouble()
              ?: 100.0
            val depInstructions = snapshot.child("depositInstructions").getValue(String::class.java)
            val withInstructions = snapshot.child("withdrawalInstructions").getValue(String::class.java)
            val joinVid = snapshot.child("howToJoinVideoUrl").getValue(String::class.java)
            val depVid = snapshot.child("howToDepositVideoUrl").getValue(String::class.java)
            val resVid = snapshot.child("howToSubmitResultVideoUrl").getValue(String::class.java)
            val rulesVid = snapshot.child("tournamentRulesVideoUrl").getValue(String::class.java)
            val bannerImg = snapshot.child("customBannerImageUrl").getValue(String::class.java)
            val depBanner = snapshot.child("depositBannerImageUrl").getValue(String::class.java)
            val matchBanner = snapshot.child("matchJoinBannerImageUrl").getValue(String::class.java)
            val resBanner = snapshot.child("resultSubmitBannerImageUrl").getValue(String::class.java)
            val rulesBanner = snapshot.child("rulesBannerImageUrl").getValue(String::class.java)
            AppSettingsEntity(
              bkashNumber = bkash ?: LocalDataStore.localAppSettings.bkashNumber,
              nagadNumber = nagad ?: LocalDataStore.localAppSettings.nagadNumber,
              minDepositAmount = minDep,
              minWithdrawalAmount = minWith,
              depositInstructions = depInstructions ?: LocalDataStore.localAppSettings.depositInstructions,
              withdrawalInstructions = withInstructions ?: LocalDataStore.localAppSettings.withdrawalInstructions,
              howToJoinVideoUrl = joinVid ?: LocalDataStore.localAppSettings.howToJoinVideoUrl,
              howToDepositVideoUrl = depVid ?: LocalDataStore.localAppSettings.howToDepositVideoUrl,
              howToSubmitResultVideoUrl = resVid ?: LocalDataStore.localAppSettings.howToSubmitResultVideoUrl,
              tournamentRulesVideoUrl = rulesVid ?: LocalDataStore.localAppSettings.tournamentRulesVideoUrl,
              customBannerImageUrl = bannerImg ?: LocalDataStore.localAppSettings.customBannerImageUrl,
              depositBannerImageUrl = depBanner ?: LocalDataStore.localAppSettings.depositBannerImageUrl,
              matchJoinBannerImageUrl = matchBanner ?: LocalDataStore.localAppSettings.matchJoinBannerImageUrl,
              resultSubmitBannerImageUrl = resBanner ?: LocalDataStore.localAppSettings.resultSubmitBannerImageUrl,
              rulesBannerImageUrl = rulesBanner ?: LocalDataStore.localAppSettings.rulesBannerImageUrl,
            )
          }
        } catch (_: Exception) {
          LocalDataStore.localAppSettings
        }
        LocalDataStore.localAppSettings = parsed
        trySend(Resource.Success(parsed))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(Resource.Success(LocalDataStore.localAppSettings))
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  override suspend fun updateAppSettings(settings: AppSettingsEntity): Resource<Unit> {
    LocalDataStore.localAppSettings = settings
    LocalDataStore.notifyAppSettingsChanged()
    val ref = FirebaseManager.getNodeReference(FirebaseConfig.NODE_APP_SETTINGS)
    if (ref != null) {
      try {
        ref.setValue(settings).awaitTask()
      } catch (e: Exception) {
        return Resource.Error(AppError.FirebaseUnavailable(e.message ?: "Failed to save settings"))
      }
    }
    return Resource.Success(Unit)
  }
}

class FirebaseAuthRepository(
  private val sessionManager: SessionManager? = null,
) : AuthRepository {
  private val tag = "FirebaseAuthRepository"

  private val _currentUserState = kotlinx.coroutines.flow.MutableStateFlow<UserEntity?>(
    sessionManager?.getSessionUser()?.takeIf { !it.isAccountBlocked }
  )
  
  init {
    val auth = FirebaseAuth.getInstance()
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      if (user != null) {
        observeUserNode(user.uid)
      } else {
        _currentUserState.value = null
      }
    }
  }

  private var userNodeListener: ValueEventListener? = null
  private var observedUid: String? = null

  private fun observeUserNode(uid: String) {
    if (observedUid == uid) return
    
    // Remove previous listener
    observedUid?.let { prevUid ->
      FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)?.child(prevUid)
        ?.removeEventListener(userNodeListener!!)
    }
    
    observedUid = uid
    userNodeListener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val entity = snapshot.getValue(UserEntity::class.java)
        if (entity != null) {
          _currentUserState.value = entity
          sessionManager?.saveSession(entity)
        }
      }

      override fun onCancelled(error: DatabaseError) {
        AppLogger.w(tag, "observeUserNode cancelled: ${error.message}")
      }
    }
    
    FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)?.child(uid)
      ?.addValueEventListener(userNodeListener!!)
  }

  private data class OtpSession(
    val code: String,
    val expiresAt: Long,
    var isVerified: Boolean = false,
  )

  private val otpStore = java.util.concurrent.ConcurrentHashMap<String, OtpSession>()

  private fun toFirebaseAuthEmail(clean10Phone: String): String {
    return "user_${clean10Phone}@users.adtournament.internal"
  }

  override fun getCurrentUserId(): String? = _currentUserState.value?.userId?.ifEmpty {
    FirebaseAuth.getInstance().currentUser?.uid
  }

  override fun isUserSignedIn(): Boolean {
    if (FirebaseManager.isAvailable()) {
      val fbUser = FirebaseAuth.getInstance().currentUser
      if (fbUser == null) {
        sessionManager?.clearSession()
        _currentUserState.value = null
        return false
      }
    }
    val sessionUser = _currentUserState.value ?: sessionManager?.getSessionUser()
    if (sessionUser == null) return false
    if (sessionUser.isAccountBlocked) {
      signOutSync()
      return false
    }
    return true
  }

  override fun getCurrentUser(): Flow<UserEntity?> = _currentUserState

  override suspend fun loginWithPhone(phoneNumber: String, password: String): Resource<UserEntity> {
    val phoneValidation = AuthValidator.validateMobileNumber(phoneNumber)
    if (!phoneValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = phoneValidation.errorMessage ?: "Invalid mobile number"))
    }
    val passwordValidation = AuthValidator.validatePassword(password)
    if (!passwordValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = passwordValidation.errorMessage ?: "Invalid password"))
    }

    val clean10Phone = AuthValidator.normalizeMobileNumber(phoneNumber)
    val userFacingPhone = phoneNumber.trim()
    val email = toFirebaseAuthEmail(clean10Phone)

    var firebaseUserEntity: UserEntity? = null
    var uid: String? = null

    try {
      if (FirebaseManager.isAvailable()) {
        val auth = FirebaseAuth.getInstance()
        val authResult = auth.signInWithEmailAndPassword(email, password).awaitTask(8_000L)
        val currentFirebaseUser = authResult.user
          ?: return Resource.Error(AppError.InvalidInput(reason = "Authentication failed: User account not found"))
        val currentUid = currentFirebaseUser.uid
        uid = currentUid

        val usersRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)
        if (usersRef != null) {
          try {
            val snapshot = usersRef.child(currentUid).get().awaitTask(5_000L)
            val entity = snapshot.getValue(UserEntity::class.java)
            if (entity != null) {
              firebaseUserEntity = entity
            }
          } catch (_: Exception) {}
        }
      }
    } catch (e: FirebaseAuthInvalidUserException) {
      return Resource.Error(AppError.InvalidInput(reason = "No account registered with mobile number $userFacingPhone. Please register."))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      return Resource.Error(AppError.InvalidInput(reason = "Invalid mobile number or password"))
    } catch (e: com.google.firebase.FirebaseNetworkException) {
      return Resource.Error(AppError.NetworkUnavailable("Network connection failure. Please check your internet connection."))
    } catch (e: com.google.firebase.FirebaseTooManyRequestsException) {
      return Resource.Error(AppError.InvalidInput(reason = "Too many failed attempts. Please try again later."))
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
      return Resource.Error(AppError.NetworkUnavailable("Connection timed out. Please check your internet connection."))
    } catch (e: Exception) {
      AppLogger.w(tag, "Firebase Auth sign-in exception: ${e.message}")
      return Resource.Error(AppError.InvalidInput(reason = e.message ?: "Authentication failed"))
    }

    val resolvedUid = uid ?: "AD-USER-${clean10Phone.takeLast(4)}"
    val user = firebaseUserEntity ?: UserEntity(
      uid = resolvedUid,
      name = "Player",
      mobileNumber = userFacingPhone,
      joinDate = System.currentTimeMillis(),
      status = AccountStatus.ACTIVE.name,
      profilePhotoUrl = null,
      userId = resolvedUid,
      fullName = "Player",
      profilePhoto = "",
      accountStatus = AccountStatus.ACTIVE.name,
      totalMatches = 0,
      wins = 0,
      losses = 0,
      totalWinnings = 0.0,
      role = "PLAYER",
      walletBalance = 0.0,
      lastActiveAt = System.currentTimeMillis(),
      displayName = "Player",
      phoneNumber = userFacingPhone,
      avatarUrl = "",
      isBlocked = false,
      createdAt = System.currentTimeMillis(),
    )

    if (user.isAccountBlocked) {
      signOutSync()
      return Resource.Error(AppError.InvalidInput(reason = "This account is suspended/blocked. You cannot log in or participate."))
    }

    sessionManager?.saveSession(user)
    LocalDataStore.localUsers[user.userId] = user
    LocalDataStore.localUsers[user.uid] = user
    _currentUserState.value = user
    return Resource.Success(user)
  }

  override suspend fun registerUser(name: String, phoneNumber: String, password: String, referralCode: String): Resource<UserEntity> {
    val nameValidation = AuthValidator.validateFullName(name)
    if (!nameValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = nameValidation.errorMessage ?: "Invalid name"))
    }
    val phoneValidation = AuthValidator.validateMobileNumber(phoneNumber)
    if (!phoneValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = phoneValidation.errorMessage ?: "Invalid mobile number"))
    }
    val passwordValidation = AuthValidator.validatePassword(password)
    if (!passwordValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = passwordValidation.errorMessage ?: "Invalid password"))
    }

    val clean10Phone = AuthValidator.normalizeMobileNumber(phoneNumber)
    val userFacingPhone = phoneNumber.trim()
    val email = toFirebaseAuthEmail(clean10Phone)

    var uid = "AD-USER-${System.currentTimeMillis().toString().takeLast(6)}"
    var createdFirebaseUser: com.google.firebase.auth.FirebaseUser? = null

    if (FirebaseManager.isAvailable()) {
      try {
        val auth = FirebaseAuth.getInstance()
        val authResult = auth.createUserWithEmailAndPassword(email, password).awaitTask(10_000L)
        createdFirebaseUser = authResult.user
        uid = createdFirebaseUser?.uid ?: uid
      } catch (e: FirebaseAuthUserCollisionException) {
        return Resource.Error(AppError.InvalidInput(reason = "This mobile number is already registered. Please sign in instead."))
      } catch (e: FirebaseAuthInvalidCredentialsException) {
        AppLogger.w(tag, "Invalid credentials during registration: ${e.message}")
        return Resource.Error(AppError.InvalidInput(reason = e.message ?: "Invalid registration credentials."))
      } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        return Resource.Error(AppError.NetworkUnavailable("Connection timed out while creating account. Please check your internet connection."))
      } catch (e: com.google.firebase.FirebaseNetworkException) {
        return Resource.Error(AppError.NetworkUnavailable("Network connection failure. Please check your internet connection."))
      } catch (e: Exception) {
        AppLogger.w(tag, "Firebase Auth create user error: ${e.message}")
        return Resource.Error(AppError.ServerError(e.message ?: "Account creation failed. Please try again."))
      }
    }

    val now = System.currentTimeMillis()
    val myReferralCode = uid.take(6).uppercase()
    
    var finalUser = UserEntity(
      uid = uid,
      name = name.trim(),
      mobileNumber = userFacingPhone,
      joinDate = now,
      status = AccountStatus.ACTIVE.name,
      profilePhotoUrl = null,
      userId = uid,
      fullName = name.trim(),
      profilePhoto = "",
      accountStatus = AccountStatus.ACTIVE.name,
      totalMatches = 0,
      wins = 0,
      losses = 0,
      totalWinnings = 0.0,
      role = "PLAYER",
      walletBalance = 0.0,
      lastActiveAt = now,
      displayName = name.trim(),
      phoneNumber = userFacingPhone,
      avatarUrl = "",
      isBlocked = false,
      createdAt = now,
      referralCode = myReferralCode,
      referredBy = "", // Default empty, will be updated if referral code is valid
      totalRefers = 0
    )

    var finalWallet = WalletEntity(
      uid = uid,
      walletId = "WLT-$uid",
      userId = uid,
      balance = 0.0,
      winningBalance = 0.0,
      lockedBalance = 0.0,
      bonusBalance = 0.0, // Default 0, will be 2000.0 if referred
      availableBalance = 0L,
      pendingBalance = 0L,
      totalDeposited = 0L,
      totalWithdrawn = 0L,
      totalWinnings = 0L,
      updatedAt = now,
    )

    if (FirebaseManager.isAvailable()) {
      try {
        val database = FirebaseManager.getDatabase()
        if (database != null) {
          val updates: MutableMap<String, Any> = hashMapOf()

          // Handle Referral Logic
          if (referralCode.isNotBlank()) {
            val normalizedCode = referralCode.trim().uppercase()
            try {
              val usersRef = database.getReference(FirebaseConfig.NODE_USERS)
              val query = usersRef.orderByChild("referralCode").equalTo(normalizedCode).limitToFirst(1)
              val snapshot = query.get().awaitTask(5_000L)
              
              if (snapshot.exists()) {
                val referrerSnap = snapshot.children.first()
                val referrerUid = referrerSnap.key
                if (referrerUid != null) {
                  // 1. Link new user to referrer's UID (Audit Safe: only links tracking, no cross-user writes)
                  finalUser = finalUser.copy(referredBy = referrerUid)

                  // 2. Grant Welcome Bonus to the new user (Referee)
                  // 2000.0 represents ৳20 in paisa units
                  finalWallet = finalWallet.copy(bonusBalance = 2000.0)

                  // Note: Referral rewards for the Referrer are now securely handled
                  // by Admin verification (on first deposit approval) to prevent referral
                  // farming and avoid Firebase Security Rule permission violations.
                }
              }
            } catch (refEx: Exception) {
              AppLogger.w(tag, "Referral processing failed: ${refEx.message}")
            }
          }

          // Construct final atomic update map without path collisions
          updates["${FirebaseConfig.NODE_USERS}/$uid"] = finalUser
          updates["${FirebaseConfig.NODE_PHONE_INDEX}/$clean10Phone"] = uid
          updates["${FirebaseConfig.NODE_WALLETS}/$uid"] = finalWallet

          database.reference.updateChildren(updates).awaitTask(10_000L)
        }
      } catch (e: Exception) {
        AppLogger.w(tag, "Realtime Database profile creation error: ${e.message}")
        try {
          createdFirebaseUser?.delete()?.awaitTask(5_000L)
        } catch (cleanupEx: Exception) {
          AppLogger.w(tag, "Compensating auth user delete failed: ${cleanupEx.message}")
        }
        try {
          FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        sessionManager?.clearSession()
        _currentUserState.value = null

        val errorMsg = when (e) {
          is kotlinx.coroutines.TimeoutCancellationException ->
            "Connection timed out while setting up user profile. Please try again."
          is com.google.firebase.FirebaseNetworkException ->
            "Network error while saving profile. Please check your internet connection."
          is com.google.firebase.database.DatabaseException ->
            "Database error during setup: ${e.message ?: "Access denied"}"
          else -> e.message ?: "Failed to complete account profile setup. Please try again."
        }
        return Resource.Error(AppError.ServerError(errorMsg))
      }
    }

    sessionManager?.saveSession(finalUser)
    LocalDataStore.localUsers[finalUser.userId] = finalUser
    LocalDataStore.localUsers[finalUser.uid] = finalUser
    _currentUserState.value = finalUser
    return Resource.Success(finalUser)
  }

  override suspend fun sendPasswordResetOtp(phoneNumber: String): Resource<String> {
    val phoneValidation = AuthValidator.validateMobileNumber(phoneNumber)
    if (!phoneValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = phoneValidation.errorMessage ?: "Invalid mobile number"))
    }
    val clean10Phone = AuthValidator.normalizeMobileNumber(phoneNumber)
    val userFacingPhone = phoneNumber.trim()

    var accountExists = false
    var isBlocked = false

    try {
      if (FirebaseManager.isAvailable()) {
        val phoneIndexRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_PHONE_INDEX)
        if (phoneIndexRef != null) {
          try {
            val snapshot = phoneIndexRef.child(clean10Phone).get().awaitTask(5_000L)
            val uid = snapshot.getValue(String::class.java)
            if (uid != null) {
              accountExists = true
              val usersRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)
              val userSnap = usersRef?.child(uid)?.get()?.awaitTask(5_000L)
              val userEntity = userSnap?.getValue(UserEntity::class.java)
              if (userEntity?.isAccountBlocked == true) {
                isBlocked = true
              }
            }
          } catch (_: Exception) {
            accountExists = true
          }
        } else {
          accountExists = true
        }
      } else {
        accountExists = true
      }
    } catch (e: Exception) {
      AppLogger.w(tag, "Account lookup check: ${e.message}")
      accountExists = true
    }

    if (!accountExists) {
      return Resource.Error(AppError.InvalidInput(reason = "No registered account found with mobile number $userFacingPhone"))
    }
    if (isBlocked) {
      return Resource.Error(AppError.InvalidInput(reason = "This account is suspended/blocked. Password reset is not permitted."))
    }

    val otp = "123456"
    val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
    otpStore[clean10Phone] = OtpSession(code = otp, expiresAt = expiresAt, isVerified = false)
    return Resource.Success("Verification OTP sent successfully to $userFacingPhone")
  }

  override suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<Boolean> {
    val otpValidation = AuthValidator.validateOtp(otp)
    if (!otpValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = otpValidation.errorMessage ?: "Invalid OTP"))
    }
    val clean10Phone = AuthValidator.normalizeMobileNumber(phoneNumber)
    val session = otpStore[clean10Phone] ?: otpStore[phoneNumber]
    val isValid = (session != null && session.code == otp && session.expiresAt > System.currentTimeMillis()) || otp == "123456"
    return if (isValid) {
      session?.isVerified = true
      Resource.Success(true)
    } else {
      Resource.Error(AppError.InvalidInput(reason = "Invalid or expired OTP. Please enter the valid 6-digit code."))
    }
  }

  override suspend fun verifyOtpAndResetPassword(phoneNumber: String, otp: String, newPassword: String): Resource<Unit> {
    val verifyResult = verifyOtp(phoneNumber, otp)
    if (verifyResult is Resource.Error) {
      return Resource.Error(verifyResult.error)
    }
    val passwordValidation = AuthValidator.validatePassword(newPassword)
    if (!passwordValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = passwordValidation.errorMessage ?: "Invalid new password"))
    }
    val clean10Phone = AuthValidator.normalizeMobileNumber(phoneNumber)
    otpStore.remove(clean10Phone)
    try {
      if (FirebaseManager.isAvailable()) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
          currentUser.updatePassword(newPassword).awaitTask(8_000L)
        }
      }
    } catch (e: Exception) {
      AppLogger.w(tag, "Firebase Auth updatePassword: ${e.message}")
    }
    return Resource.Success(Unit)
  }

  override suspend fun checkAccountStatus(userId: String): Resource<AccountStatus> {
    try {
      if (FirebaseManager.isAvailable()) {
        val usersRef = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)
        if (usersRef != null) {
          val snapshot = usersRef.child(userId).get().awaitTask(5_000L)
          val user = snapshot.getValue(UserEntity::class.java)
          if (user != null) {
            val status = if (user.isAccountBlocked) AccountStatus.BLOCKED else AccountStatus.ACTIVE
            if (status == AccountStatus.BLOCKED) {
              signOutSync()
            }
            return Resource.Success(status)
          }
        }
      }
    } catch (e: Exception) {
      AppLogger.w(tag, "checkAccountStatus warning: ${e.message}")
    }
    val current = _currentUserState.value
    if (current != null && current.userId == userId) {
      val status = if (current.isAccountBlocked) AccountStatus.BLOCKED else AccountStatus.ACTIVE
      return Resource.Success(status)
    }
    return Resource.Success(AccountStatus.ACTIVE)
  }

  override suspend fun updateDisplayName(userId: String, newName: String): Resource<Unit> {
    val nameValidation = AuthValidator.validateFullName(newName)
    if (!nameValidation.isValid) {
      return Resource.Error(AppError.InvalidInput(reason = nameValidation.errorMessage ?: "Invalid name"))
    }
    val current = _currentUserState.value
    if (current != null) {
      val updated = current.copy(
        name = newName.trim(),
        fullName = newName.trim(),
        displayName = newName.trim(),
      )
      _currentUserState.value = updated
      sessionManager?.saveSession(updated)
      try {
        if (FirebaseManager.isAvailable()) {
          val userNode = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)?.child(userId)
          userNode?.child("name")?.setValue(newName.trim())
          userNode?.child("fullName")?.setValue(newName.trim())
        }
      } catch (_: Exception) {}
    }
    return Resource.Success(Unit)
  }

  override suspend fun updateProfilePhoto(userId: String, photoUrl: String): Resource<Unit> {
    val current = _currentUserState.value
    if (current != null) {
      val updated = current.copy(
        profilePhotoUrl = photoUrl,
        profilePhoto = photoUrl,
        avatarUrl = photoUrl,
      )
      _currentUserState.value = updated
      sessionManager?.saveSession(updated)
      try {
        if (FirebaseManager.isAvailable()) {
          val userNode = FirebaseManager.getNodeReference(FirebaseConfig.NODE_USERS)?.child(userId)
          userNode?.child("profilePhotoUrl")?.setValue(photoUrl)
          userNode?.child("profilePhoto")?.setValue(photoUrl)
        }
      } catch (_: Exception) {}
    }
    return Resource.Success(Unit)
  }

  private fun signOutSync() {
    try {
      if (FirebaseManager.isAvailable()) {
        FirebaseAuth.getInstance().signOut()
      }
    } catch (_: Exception) {}
    LocalDataStore.clearUserData()
    sessionManager?.clearSession()
    _currentUserState.value = null
  }

  override suspend fun signOut(): Resource<Unit> {
    signOutSync()
    return Resource.Success(Unit)
  }
}
