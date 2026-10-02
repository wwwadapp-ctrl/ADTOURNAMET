package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.error.Resource
import com.example.core.security.AuthValidator
import com.example.domain.model.UserEntity
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
  val isLoading: Boolean = false,
  val user: UserEntity? = null,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val isOtpSent: Boolean = false,
  val isOtpVerified: Boolean = false,
  val isPasswordResetSuccess: Boolean = false,
  val isRegistrationSuccess: Boolean = false,
  val isLoginSuccess: Boolean = false,
)

class AuthViewModel(
  private val authRepository: AuthRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  val currentUser: StateFlow<UserEntity?> = authRepository.getCurrentUser() as StateFlow<UserEntity?>

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }

  fun login(mobileNumber: String, password: String) {
    val phoneValidation = AuthValidator.validateMobileNumber(mobileNumber)
    if (!phoneValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = phoneValidation.errorMessage)
      return
    }
    val passValidation = AuthValidator.validatePassword(password)
    if (!passValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = passValidation.errorMessage)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val result = authRepository.loginWithPhone(mobileNumber.trim(), password)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            user = result.data,
            isLoginSuccess = true,
            errorMessage = null,
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun register(name: String, mobileNumber: String, password: String, referralCode: String = "") {
    val nameValidation = AuthValidator.validateFullName(name)
    if (!nameValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = nameValidation.errorMessage)
      return
    }
    val phoneValidation = AuthValidator.validateMobileNumber(mobileNumber)
    if (!phoneValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = phoneValidation.errorMessage)
      return
    }
    val passValidation = AuthValidator.validatePassword(password)
    if (!passValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = passValidation.errorMessage)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val result = authRepository.registerUser(name.trim(), mobileNumber.trim(), password, referralCode.trim())) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            user = result.data,
            isRegistrationSuccess = true,
            successMessage = "Account created successfully! Welcome to AD Tournament.",
            errorMessage = null,
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun sendResetOtp(mobileNumber: String) {
    val phoneValidation = AuthValidator.validateMobileNumber(mobileNumber)
    if (!phoneValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = phoneValidation.errorMessage)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val result = authRepository.sendPasswordResetOtp(mobileNumber.trim())) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            isOtpSent = true,
            successMessage = result.data,
            errorMessage = null,
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun verifyOtp(mobileNumber: String, otp: String) {
    val otpValidation = AuthValidator.validateOtp(otp)
    if (!otpValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = otpValidation.errorMessage)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val result = authRepository.verifyOtp(mobileNumber.trim(), otp.trim())) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            isOtpVerified = true,
            successMessage = "OTP verified successfully!",
            errorMessage = null,
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun resetPassword(mobileNumber: String, otp: String, newPassword: String) {
    val passValidation = AuthValidator.validatePassword(newPassword)
    if (!passValidation.isValid) {
      _uiState.value = _uiState.value.copy(errorMessage = passValidation.errorMessage)
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      when (val result = authRepository.verifyOtpAndResetPassword(mobileNumber.trim(), otp.trim(), newPassword)) {
        is Resource.Success -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            isPasswordResetSuccess = true,
            successMessage = "Password reset successfully! Please sign in with your new password.",
            errorMessage = null,
          )
        }
        is Resource.Error -> {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = result.error.message,
          )
        }
        else -> Unit
      }
    }
  }

  fun clearSuccessFlags() {
    _uiState.value = _uiState.value.copy(
      isLoginSuccess = false,
      isRegistrationSuccess = false,
      isPasswordResetSuccess = false,
      isOtpVerified = false,
      isOtpSent = false
    )
  }

  fun signOut() {
    viewModelScope.launch {
      authRepository.signOut()
      _uiState.value = AuthUiState()
    }
  }

  class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AuthViewModel(authRepository) as T
    }
  }
}
