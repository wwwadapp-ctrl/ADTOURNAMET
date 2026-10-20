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
-keep class com.google.firebase.** { *; }
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.database.Exclude <fields>;
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# Preserve all domain entities, database models, and enums
-keep class com.example.model.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.domain.model.** { *; }
-keep interface com.example.domain.model.** { *; }
-keep enum com.example.domain.model.** { *; }

# --- Firebase Realtime Database Data Models Preservation ---
-keep class com.example.ui.autoludo.AutoLudoMatchEntity {
    <init>(...);
    public <methods>;
    public <fields>;
}
-keepclassmembers class com.example.ui.autoludo.AutoLudoMatchEntity { *; }

-keep class com.example.ui.autoludo.LudoGameState {
    <init>(...);
    public <methods>;
    public <fields>;
}
-keepclassmembers class com.example.ui.autoludo.LudoGameState { *; }

# Keep any nested or helper state data classes in AutoLudo / MiniGames
-keep class com.example.ui.autoludo.**State { *; }
-keep class com.example.ui.autoludo.**Model { *; }
-keep class com.example.ui.minigames.**State { *; }
-keep class com.example.ui.minigames.**Model { *; }
-keep class com.example.ui.minigames.**Entity { *; }

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

# Ktor & Google AI SDK rules
-dontwarn io.ktor.**
-dontwarn com.google.ai.client.generativeai.**
