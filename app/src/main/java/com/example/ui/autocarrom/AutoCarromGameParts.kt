package com.example.ui.autocarrom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Small pieces of the online Auto Carrom screen: profile photo, title row with prize/clock, toast.

@Composable
fun rememberCarromPhoto(uid: String): String {
    val st = produceState(initialValue = "", uid) {
        if (uid.isBlank()) return@produceState
        try {
            val ref = AutoCarromManager.getDb()?.getReference("users")?.child(uid) ?: return@produceState
            ref.get().addOnSuccessListener { s ->
                value = listOf("profilePhotoUrl", "profilePhoto", "avatarUrl").firstNotNullOfOrNull { k -> s.child(k).getValue(String::class.java)?.ifBlank { null } } ?: ""
            }
        } catch (_: Exception) { }
    }
    return st.value
}

/** Title + prize, then the match clock (or sudden death) and a red banner when the connection drops. */
@Composable
fun CarromHeader(prizePool: Double, suddenDeath: Boolean, secondsLeft: Long, connected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            com.example.ui.components.AdGoldText("অটো ক্যারম", fontSize = 22.sp, modifier = Modifier.weight(1f))
            Text("পুরস্কার ৳" + CarromRules.bn(CarromRules.amt(prizePool)), color = Color(0xFF3A1A00), fontSize = 13.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00))), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp))
        }
        Text(if (suddenDeath) "⚡ সাডেন ডেথ" else "⏱ " + CarromRules.bn(String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)),
            color = Color(0xFFFFE7A0), fontSize = 13.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.background(Color(0x8C0A1040), RoundedCornerShape(50)).border(1.dp, Color(0x73FFE7A0), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 3.dp))
        if (!connected) Text("সংযোগ বিচ্ছিন্ন… আবার যুক্ত হচ্ছে", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp).background(Color(0xFFB3122F), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 3.dp))
    }
}

/** Gold (good) or red (bad) message in the middle of the screen. */
@Composable
fun CarromToast(t: Pair<String, Boolean>?, visible: Boolean, modifier: Modifier) {
    if (t == null || !visible) return
    Text(t.first, color = if (t.second) Color.White else Color(0xFF3A1A00), fontSize = 16.sp, fontWeight = FontWeight.Black,
        modifier = modifier.shadow(10.dp, RoundedCornerShape(50))
            .background(Brush.verticalGradient(if (t.second) listOf(Color(0xFFFF6A7A), Color(0xFFB3122F)) else listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 6.dp))
}
