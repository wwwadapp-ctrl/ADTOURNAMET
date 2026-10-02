package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
  Available,
  Unavailable,
  Losing,
  Lost
}

interface NetworkMonitor {
  val status: Flow<NetworkStatus>
  fun isCurrentlyConnected(): Boolean
}

class AndroidNetworkMonitor(
  private val context: Context,
) : NetworkMonitor {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  override val status: Flow<NetworkStatus> = callbackFlow {
    if (connectivityManager == null) {
      trySend(NetworkStatus.Unavailable)
      close()
      return@callbackFlow
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        super.onAvailable(network)
        trySend(NetworkStatus.Available)
      }

      override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
        trySend(NetworkStatus.Losing)
      }

      override fun onLost(network: Network) {
        super.onLost(network)
        trySend(NetworkStatus.Lost)
      }

      override fun onUnavailable() {
        super.onUnavailable()
        trySend(NetworkStatus.Unavailable)
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    connectivityManager.registerNetworkCallback(request, callback)

    if (isCurrentlyConnected()) {
      trySend(NetworkStatus.Available)
    } else {
      trySend(NetworkStatus.Unavailable)
    }

    awaitClose {
      try {
        connectivityManager.unregisterNetworkCallback(callback)
      } catch (_: Exception) {
      }
    }
  }.distinctUntilChanged()

  override fun isCurrentlyConnected(): Boolean {
    val activeNetwork = connectivityManager?.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
}
