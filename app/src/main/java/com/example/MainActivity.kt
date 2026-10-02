package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.appStringsFor
import com.example.core.network.NetworkStatus
import com.example.ui.components.NetworkStatusBar
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AdTournamentTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as TournamentApplication
    val container = app.container

    setContent {
      var currentLanguage by remember {
        mutableStateOf(container.sessionManager.getAppLanguage())
      }
      val strings = remember(currentLanguage) {
        appStringsFor(currentLanguage)
      }

      CompositionLocalProvider(
        LocalAppStrings provides strings
      ) {
        AdTournamentTheme(darkTheme = true) {
          val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
          )

          LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(
                  this@MainActivity,
                  Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
              ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
            }
          }

          val navController = rememberNavController()
          val networkStatus by container.networkMonitor.status.collectAsState(initial = NetworkStatus.Available)

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
              NetworkStatusBar(networkStatus = networkStatus)
            },
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              AppNavigation(
                navController = navController,
                container = container,
                currentLanguage = currentLanguage,
                onLanguageChange = { newLang ->
                  container.sessionManager.setAppLanguage(newLang)
                  currentLanguage = newLang
                },
              )
            }
          }
        }
      }
    }
  }
}
