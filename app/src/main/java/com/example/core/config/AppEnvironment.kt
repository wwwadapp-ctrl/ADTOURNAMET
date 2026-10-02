package com.example.core.config

import com.example.BuildConfig

enum class EnvironmentType {
  DEVELOPMENT,
  STAGING,
  PRODUCTION
}

data class EnvironmentConfig(
  val type: EnvironmentType,
  val environmentName: String,
  val isDebugLoggingEnabled: Boolean,
  val requiresAppCheck: Boolean,
  val maxMatchListPageSize: Int = 25,
  val fcmEnabled: Boolean = true,
)

object AppEnvironment {
  private val currentType: EnvironmentType = if (BuildConfig.DEBUG) {
    EnvironmentType.DEVELOPMENT
  } else {
    EnvironmentType.PRODUCTION
  }

  val current: EnvironmentConfig = when (currentType) {
    EnvironmentType.DEVELOPMENT -> EnvironmentConfig(
      type = EnvironmentType.DEVELOPMENT,
      environmentName = "Development",
      isDebugLoggingEnabled = true,
      requiresAppCheck = false,
      maxMatchListPageSize = 20,
      fcmEnabled = false,
    )
    EnvironmentType.STAGING -> EnvironmentConfig(
      type = EnvironmentType.STAGING,
      environmentName = "Staging",
      isDebugLoggingEnabled = true,
      requiresAppCheck = true,
      maxMatchListPageSize = 25,
      fcmEnabled = true,
    )
    EnvironmentType.PRODUCTION -> EnvironmentConfig(
      type = EnvironmentType.PRODUCTION,
      environmentName = "Production",
      isDebugLoggingEnabled = false,
      requiresAppCheck = true,
      maxMatchListPageSize = 30,
      fcmEnabled = true,
    )
  }
}
