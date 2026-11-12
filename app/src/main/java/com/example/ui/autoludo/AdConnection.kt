package com.example.ui.autoludo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.core.config.FirebaseConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// READ-ONLY: true while this phone is connected to the Firebase game server.
@Composable
fun rememberServerConnected(): State<Boolean> {
    val connected = remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val ref = try {
            FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference(".info/connected")
        } catch (_: Exception) { null }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                connected.value = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) { }
        }
        ref?.addValueEventListener(listener)
        onDispose { ref?.removeEventListener(listener) }
    }
    return connected
}
