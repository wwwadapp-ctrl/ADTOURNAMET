package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.error.Resource
import com.example.domain.model.NotificationEntity
import com.example.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    userId: String? = null
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<NotificationEntity>>>(Resource.Loading)
    val notifications: StateFlow<Resource<List<NotificationEntity>>> = _notifications.asStateFlow()

    // Dynamic UID Binding as per requirement
    val uid: String = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        if (uid.isBlank()) {
            _notifications.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            notificationRepository.getNotifications(uid, 50).collectLatest { resource ->
                _notifications.value = resource
            }
        }
    }

    fun markAllAsRead() {
        if (uid.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    class Factory(
        private val notificationRepository: NotificationRepository,
        private val userId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationsViewModel(notificationRepository, userId) as T
        }
    }
}
