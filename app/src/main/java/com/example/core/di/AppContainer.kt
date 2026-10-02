package com.example.core.di

import android.content.Context
import com.example.core.firebase.FirebaseManager
import com.example.core.network.AndroidNetworkMonitor
import com.example.core.network.NetworkMonitor
import com.example.core.notification.FcmTokenManager
import com.example.data.repository.*
import com.example.domain.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AppContainer {
  val networkMonitor: NetworkMonitor
  val matchRepository: MatchRepository
  val walletRepository: WalletRepository
  val resultRepository: ResultRepository
  val notificationRepository: NotificationRepository
  val adminRepository: AdminRepository
  val settingsRepository: SettingsRepository
  val authRepository: AuthRepository
  val sessionManager: com.example.core.security.SessionManager
}

class DefaultAppContainer(
  private val context: Context,
) : AppContainer {

  init {
    FirebaseManager.initialize(context)
    CoroutineScope(Dispatchers.IO).launch {
      FcmTokenManager.retrieveToken()
    }
  }

  override val networkMonitor: NetworkMonitor by lazy {
    AndroidNetworkMonitor(context)
  }

  override val sessionManager: com.example.core.security.SessionManager by lazy {
    com.example.core.security.SessionManager(context)
  }

  override val matchRepository: MatchRepository by lazy {
    FirebaseMatchRepository()
  }

  override val walletRepository: WalletRepository by lazy {
    FirebaseWalletRepository()
  }

  override val resultRepository: ResultRepository by lazy {
    FirebaseResultRepository(context)
  }

  override val notificationRepository: NotificationRepository by lazy {
    FirebaseNotificationRepository()
  }

  override val adminRepository: AdminRepository by lazy {
    FirebaseAdminRepository()
  }

  override val settingsRepository: SettingsRepository by lazy {
    FirebaseSettingsRepository()
  }

  override val authRepository: AuthRepository by lazy {
    FirebaseAuthRepository(sessionManager)
  }
}
