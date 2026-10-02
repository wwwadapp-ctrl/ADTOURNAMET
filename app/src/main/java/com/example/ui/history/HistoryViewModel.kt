package com.example.ui.history

import androidx.lifecycle.ViewModel
import com.example.domain.model.MatchEntity
import com.example.domain.model.MatchStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryUiState(
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Int = 0
)

class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val currentUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun updateStats(matches: List<MatchEntity>) {
        val activeUid = currentUid
        if (activeUid.isBlank()) {
            _uiState.value = HistoryUiState()
            return
        }

        val historicalMatches = matches.filter { isHistorical(it) }
        
        var total = 0
        var wins = 0
        var losses = 0

        historicalMatches.forEach { match ->
            val result = getMatchResult(match, activeUid)
            total++
            when (result) {
                HistoryMatchResult.WON -> wins++
                HistoryMatchResult.LOST -> losses++
                else -> { /* Pending or Cancelled don't count for Win/Loss stats usually, but total matches? */ }
            }
        }

        val rate = if (total > 0) ((wins.toDouble() / total) * 100).toInt() else 0
        
        _uiState.value = HistoryUiState(
            totalMatches = total,
            wins = wins,
            losses = losses,
            winRate = rate
        )
    }

    private fun isHistorical(match: MatchEntity): Boolean {
        val status = match.status.uppercase()
        return status == MatchStatus.COMPLETED.name ||
                status == MatchStatus.CANCELLED.name ||
                status == MatchStatus.RESULT_SUBMITTED.name ||
                status == "UNDER_REVIEW" ||
                status == "REJECTED" ||
                status == "WON" ||
                status == "APPROVED" ||
                status == "LOST"
    }

    fun getMatchResult(match: MatchEntity, userId: String): HistoryMatchResult {
        val status = match.status.uppercase()
        val winnerId = match.winnerUserId.ifBlank { "" } // Use winnerUserId as primary
        
        val isWon = winnerId == userId || status == "WON" || status == "APPROVED"
        
        if (isWon) return HistoryMatchResult.WON
        
        if (status == MatchStatus.CANCELLED.name || status == "REFUNDED") {
            return HistoryMatchResult.CANCELLED
        }
        
        if (status == MatchStatus.RESULT_SUBMITTED.name || status == "UNDER_REVIEW") {
            return HistoryMatchResult.PENDING
        }
        
        if (status == MatchStatus.COMPLETED.name || status == "LOST" || status == "REJECTED") {
            return HistoryMatchResult.LOST
        }
        
        return HistoryMatchResult.PENDING
    }
}

enum class HistoryMatchResult {
    WON,
    LOST,
    PENDING,
    CANCELLED
}
