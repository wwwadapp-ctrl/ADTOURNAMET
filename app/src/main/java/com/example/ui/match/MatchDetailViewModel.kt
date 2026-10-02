package com.example.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchPlayerEntity
import com.example.domain.model.ResultEntity
import com.example.domain.repository.MatchRepository
import com.example.domain.repository.ResultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchDetailUiState(
  val isLoading: Boolean = true,
  val match: MatchEntity? = null,
  val players: List<MatchPlayerEntity> = emptyList(),
  val isJoined: Boolean = false,
  val isJoining: Boolean = false,
  val joinSuccess: Boolean = false,
  val submittedResult: ResultEntity? = null,
  val isSubmittingResult: Boolean = false,
  val isAdmin: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

class MatchDetailViewModel(
  private val matchRepository: MatchRepository,
  private val resultRepository: ResultRepository,
  private val matchId: String,
  val currentUserId: String,
  private val isAdminUser: Boolean = false,
) : ViewModel() {

  private val _uiState = MutableStateFlow(MatchDetailUiState(isAdmin = isAdminUser))
  val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

  init {
    loadMatchDetails()
  }

  fun loadMatchDetails() {
    viewModelScope.launch {
      matchRepository.getMatchById(matchId).collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(match = res.data, isLoading = false)
        }
      }
    }
    viewModelScope.launch {
      matchRepository.getMatchPlayers(matchId).collect { res ->
        if (res is Resource.Success) {
          val players = res.data
          val authUid = com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.uid
          val joined = players.any {
            it.effectiveUid == currentUserId ||
            it.userId == currentUserId ||
            it.uid == currentUserId ||
            (!authUid.isNullOrBlank() && (it.effectiveUid == authUid || it.uid == authUid || it.userId == authUid))
          }
          _uiState.value = _uiState.value.copy(players = players, isJoined = joined)
        }
      }
    }
    viewModelScope.launch {
      resultRepository.getResultForMatch(matchId).collect { res ->
        if (res is Resource.Success) {
          _uiState.value = _uiState.value.copy(submittedResult = res.data)
        }
      }
    }
  }

  fun joinMatch() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isJoining = true, errorMessage = null)
      when (val res = matchRepository.joinMatch(currentUserId, matchId)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isJoining = false,
            isJoined = true,
            joinSuccess = true,
            successMessage = "Successfully joined tournament battle!",
          )
          loadMatchDetails()
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isJoining = false,
            errorMessage = res.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun submitVictoryProof(imageUri: android.net.Uri, roomCode: String, matchNumber: String, notes: String) {
    if (_uiState.value.isSubmittingResult) return
    val auth = com.example.core.firebase.FirebaseManager.getAuth()
    val authUser = auth?.currentUser
    val authUid = authUser?.uid?.trim()?.ifBlank { null }
    val effectiveUid = if (auth != null) {
      if (authUid.isNullOrBlank()) {
        _uiState.value = _uiState.value.copy(
          isSubmittingResult = false,
          errorMessage = "User must be authenticated to submit match results. Please log in.",
        )
        return
      }
      authUid
    } else {
      if (currentUserId.isBlank()) {
        _uiState.value = _uiState.value.copy(
          isSubmittingResult = false,
          errorMessage = "User must be authenticated to submit match results. Please log in.",
        )
        return
      }
      currentUserId.trim()
    }

    val currentMatch = _uiState.value.match
    val existingResult = _uiState.value.submittedResult
    if (currentMatch?.status?.equals(com.example.domain.model.MatchStatus.COMPLETED.name, ignoreCase = true) == true) {
      _uiState.value = _uiState.value.copy(
        isSubmittingResult = false,
        errorMessage = "ম্যাচ সমাপ্ত / ফলাফল নিশ্চিত হয়েছে। প্রুফ জমা দেওয়া সম্ভব নয়।",
      )
      return
    }
    if (existingResult != null) {
      val st = existingResult.status.uppercase()
      if (st == "APPROVED" || st == "LOST" || st == "REJECTED") {
        val msg = if (st == "REJECTED") {
          "প্রুফ বাতিল করা হয়েছে। পুনরায় প্রুফ সাবমিট করার সুযোগ নেই।"
        } else {
          "ফলাফল ইতিমধ্যে নিশ্চিত হয়েছে। প্রুফ জমা দেওয়া বন্ধ।"
        }
        _uiState.value = _uiState.value.copy(
          isSubmittingResult = false,
          errorMessage = msg,
        )
        return
      }
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSubmittingResult = true, errorMessage = null)
      when (val uploadRes = resultRepository.uploadResultScreenshot(matchId, imageUri)) {
        is Resource.Success -> {
          val screenshotUrl = uploadRes.data
          val player = _uiState.value.players.find { it.effectiveUid == effectiveUid }
          val winnerName = player?.username?.ifEmpty { effectiveUid } ?: effectiveUid
          val formattedNotes = "রুমকোড: $roomCode | ম্যাচ নাম্বার: $matchNumber${if (notes.isNotBlank()) " | নোট: $notes" else ""}"
          when (val res = resultRepository.submitMatchResult(
            matchId = matchId,
            winnerId = effectiveUid,
            winnerName = winnerName,
            screenshotUrl = screenshotUrl,
            notes = formattedNotes,
          )) {
            is Resource.Success -> {
              _uiState.value = _uiState.value.copy(
                isSubmittingResult = false,
                submittedResult = res.data,
                successMessage = "Victory proof submitted successfully! Pending admin review.",
              )
            }
            is Resource.Error -> {
              _uiState.value = _uiState.value.copy(
                isSubmittingResult = false,
                errorMessage = res.error.message,
              )
            }
            else -> {
              _uiState.value = _uiState.value.copy(isSubmittingResult = false)
            }
          }
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isSubmittingResult = false,
            errorMessage = uploadRes.error.message ?: "Failed to upload screenshot image.",
          )
        }
        else -> {
          _uiState.value = _uiState.value.copy(isSubmittingResult = false)
        }
      }
    }
  }

  class Factory(
    private val matchRepository: MatchRepository,
    private val resultRepository: ResultRepository,
    private val matchId: String,
    private val currentUserId: String,
    private val isAdmin: Boolean = false,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MatchDetailViewModel(matchRepository, resultRepository, matchId, currentUserId, isAdmin) as T
    }
  }
}
