package com.example.core.error

sealed interface Resource<out T> {
  data class Success<T>(val data: T) : Resource<T>
  data class Error(val error: AppError) : Resource<Nothing>
  data object Loading : Resource<Nothing>
  data object Idle : Resource<Nothing>
}
