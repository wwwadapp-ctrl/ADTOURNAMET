package com.example.core.firebase

import android.content.Context
import com.example.core.config.FirebaseConfig
import com.example.core.logging.AppLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseManager {
  private const val TAG = "FirebaseManager"
  private var isInitialized = false

  fun initialize(context: Context) {
    if (isInitialized) return
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        val app = FirebaseApp.initializeApp(context)
        AppLogger.i(TAG, "Firebase initialized: ${app != null}")
      } else {
        AppLogger.i(TAG, "Firebase already initialized by content provider")
      }
      try {
        getDatabase()
      } catch (_: Exception) {
      }
      isInitialized = true
    } catch (e: Exception) {
      AppLogger.w(TAG, "Firebase initialization handled safely: ${e.message}")
    }
  }

  fun isAvailable(): Boolean {
    return try {
      FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
    } catch (_: Exception) {
      false
    }
  }

  fun getAuth(): FirebaseAuth? {
    return try {
      FirebaseAuth.getInstance()
    } catch (e: Exception) {
      AppLogger.w(TAG, "Firebase Auth unavailable: ${e.message}")
      null
    }
  }

  fun getDatabase(): FirebaseDatabase? {
    return try {
      FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).apply {
        try {
          setPersistenceEnabled(true)
        } catch (_: Exception) {
        }
      }
    } catch (e: Exception) {
      AppLogger.w(TAG, "Realtime Database unavailable: ${e.message}")
      null
    }
  }

  fun getNodeReference(nodeName: String): DatabaseReference? {
    return getDatabase()?.getReference(nodeName)
  }

  fun getStorage(): FirebaseStorage? {
    return try {
      FirebaseStorage.getInstance()
    } catch (e: Exception) {
      AppLogger.w(TAG, "Cloud Storage unavailable: ${e.message}")
      null
    }
  }

  fun getStorageReference(path: String): StorageReference? {
    return getStorage()?.getReference(path)
  }
}
