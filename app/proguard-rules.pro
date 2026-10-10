# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 1. Firebase Realtime Database & Auth Model Protections
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.Exclude <fields>;
}

# Preserve all domain entities, database models, and enums
-keep class com.example.domain.model.** { *; }
-keep interface com.example.domain.model.** { *; }
-keep enum com.example.domain.model.** { *; }

# Preserve mini-game logic, engines, and result data models (Firebase Serialization)
-keep class com.example.ui.minigames.** { *; }

# Preserve all core data models, repositories, and config
-keep class com.example.core.config.** { *; }
-keep class com.example.core.finance.** { *; }
-keep class com.example.core.error.** { *; }
-keep class com.example.data.repository.LocalDataStore { *; }
-keep class com.example.data.repository.CanonicalNotificationDto { *; }

# 2. Coroutines and Jetpack Compose Stability
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 3. Third-party Libraries (Coil, Retrofit, Moshi)
-keep class coil.** { *; }
-dontwarn coil.**
-keep class retrofit2.** { *; }
-keep class com.squareup.moshi.** { *; }

# Auto Ludo engine and UI protection
-keep class com.example.ui.autoludo.** { *; }
-keepclassmembers class com.example.ui.autoludo.** { *; }
