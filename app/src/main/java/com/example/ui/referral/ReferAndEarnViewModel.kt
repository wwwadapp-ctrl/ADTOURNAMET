package com.example.ui.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReferAndEarnUiState(
    val user: UserEntity? = null,
    val wallet: WalletEntity? = null,
    val isLoading: Boolean = true
)

class ReferAndEarnViewModel(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferAndEarnUiState())
    val uiState: StateFlow<ReferAndEarnUiState> = combine(
        authRepository.getCurrentUser(),
        walletRepository.getWallet(authRepository.getCurrentUserId() ?: "")
    ) { user, walletResource ->
        ReferAndEarnUiState(
            user = user,
            wallet = if (walletResource is Resource.Success) walletResource.data else null,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ReferAndEarnUiState()
    )

    class Factory(
        private val authRepository: AuthRepository,
        private val walletRepository: WalletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReferAndEarnViewModel(authRepository, walletRepository) as T
        }
    }
}
