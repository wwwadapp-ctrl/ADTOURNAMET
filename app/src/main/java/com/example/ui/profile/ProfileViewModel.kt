package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.UserEntity
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val isLoggedOut: Boolean = false,
)

class ProfileViewModel(
  private val authRepository: AuthRepository,
  private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

  val currentUser: StateFlow<UserEntity?> = authRepository.getCurrentUser()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  val appSettings: StateFlow<AppSettingsEntity?> = (settingsRepository?.getAppSettings()
    ?.map { if (it is Resource.Success) it.data else null } ?: flowOf(null))
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  fun updateName(userId: String, newName: String) {
    if (newName.isBlank() || userId.isBlank()) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = authRepository.updateDisplayName(userId, newName.trim())) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            successMessage = "Profile updated successfully",
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = res.error.userMessage,
          )
        }
        else -> {
          _uiState.value = _uiState.value.copy(isLoading = false)
        }
      }
    }
  }

  fun updatePhoto(userId: String, photoUrl: String) {
    if (userId.isBlank() || photoUrl.isBlank()) return
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val res = authRepository.updateProfilePhoto(userId, photoUrl)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            successMessage = "Profile photo updated successfully",
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = res.error.userMessage,
          )
        }
        else -> {
          _uiState.value = _uiState.value.copy(isLoading = false)
        }
      }
    }
  }

  fun clearFeedback() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }

  fun signOut(onSignedOut: () -> Unit) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      authRepository.signOut()
      _uiState.value = _uiState.value.copy(isLoading = false, isLoggedOut = true)
      onSignedOut()
    }
  }

  class Factory(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ProfileViewModel(authRepository, settingsRepository) as T
    }
  }
}
