package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import com.example.core.i18n.AppLanguage
import com.example.domain.model.AccountStatus
import com.example.domain.model.UserEntity

class SessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  fun saveSession(user: UserEntity) {
    if (user.isAccountBlocked) {
      clearSession()
      return
    }
    prefs.edit().apply {
      putString(KEY_USER_ID, user.userId.ifEmpty { user.uid })
      putString(KEY_FULL_NAME, user.fullName.ifEmpty { user.name })
      putString(KEY_MOBILE_NUMBER, user.mobileNumber.ifEmpty { user.phoneNumber })
      putString(KEY_PROFILE_PHOTO, user.profilePhoto.ifEmpty { user.profilePhotoUrl ?: "" })
      putString(KEY_ACCOUNT_STATUS, user.accountStatus.ifEmpty { user.status })
      putLong(KEY_JOIN_DATE, if (user.joinDate > 0) user.joinDate else user.createdAt)
      putBoolean(KEY_IS_BLOCKED, user.isAccountBlocked)
      putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
      apply()
    }
  }

  fun getSessionUser(): UserEntity? {
    val userId = prefs.getString(KEY_USER_ID, null) ?: return null
    val isBlocked = prefs.getBoolean(KEY_IS_BLOCKED, false)
    val status = prefs.getString(KEY_ACCOUNT_STATUS, AccountStatus.ACTIVE.name) ?: AccountStatus.ACTIVE.name
    if (isBlocked || status.equals(AccountStatus.BLOCKED.name, ignoreCase = true)) {
      clearSession()
      return null
    }
    val mobileNumber = prefs.getString(KEY_MOBILE_NUMBER, "") ?: ""
    val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
    val profilePhoto = prefs.getString(KEY_PROFILE_PHOTO, "") ?: ""
    val joinDate = prefs.getLong(KEY_JOIN_DATE, System.currentTimeMillis())
    return UserEntity(
      uid = userId,
      name = fullName,
      mobileNumber = mobileNumber,
      joinDate = joinDate,
      status = status,
      profilePhotoUrl = profilePhoto.ifEmpty { null },
      userId = userId,
      fullName = fullName,
      profilePhoto = profilePhoto,
      accountStatus = status,
      totalMatches = 0,
      wins = 0,
      losses = 0,
      totalWinnings = 0.0,
      role = "PLAYER",
      walletBalance = 0.0,
      lastActiveAt = System.currentTimeMillis(),
      displayName = fullName,
      phoneNumber = mobileNumber,
      avatarUrl = profilePhoto,
      isBlocked = false,
      createdAt = joinDate,
    )
  }

  fun isUserLoggedIn(): Boolean {
    val user = getSessionUser() ?: return false
    return !user.isAccountBlocked
  }

  fun getAppLanguage(): AppLanguage {
    val code = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.BN.code)
    return AppLanguage.fromCode(code)
  }

  fun setAppLanguage(language: AppLanguage) {
    prefs.edit().putString(KEY_APP_LANGUAGE, language.code).apply()
  }

  fun clearSession() {
    val savedLang = getAppLanguage()
    prefs.edit().clear().apply()
    setAppLanguage(savedLang)
  }

  companion object {
    private const val PREF_NAME = "ad_tournament_session_prefs"
    private const val KEY_USER_ID = "session_user_id"
    private const val KEY_FULL_NAME = "session_full_name"
    private const val KEY_MOBILE_NUMBER = "session_mobile_number"
    private const val KEY_PROFILE_PHOTO = "session_profile_photo"
    private const val KEY_ACCOUNT_STATUS = "session_account_status"
    private const val KEY_JOIN_DATE = "session_join_date"
    private const val KEY_IS_BLOCKED = "session_is_blocked"
    private const val KEY_LAST_LOGIN = "session_last_login"
    private const val KEY_APP_LANGUAGE = "app_language"
  }
}
