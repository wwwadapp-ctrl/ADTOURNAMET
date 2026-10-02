package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer

class TournamentApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = DefaultAppContainer(this)
  }
}

typealias AdTournamentApplication = TournamentApplication
