package com.example.ui.autocarrom

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
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

/** Back button, title + prize, then the big match clock (or sudden death) and a red banner when the connection drops. */
@Composable
fun CarromHeader(prizePool: Double, suddenDeath: Boolean, secondsLeft: Long, connected: Boolean, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CarromBackButton(onBack)
            Spacer(Modifier.width(10.dp))
            com.example.ui.components.AdGoldText("অটো ক্যারম", fontSize = 22.sp, modifier = Modifier.weight(1f))
            Text("পুরস্কার ৳" + CarromRules.bn(CarromRules.amt(prizePool)), color = Color(0xFF3A1A00), fontSize = 13.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00))), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp))
        }
        CarromClockPill(
            if (suddenDeath) "সাডেন ডেথ" else CarromRules.bn(String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)),
            low = suddenDeath || secondsLeft < 60
        )
        if (!connected) Text("সংযোগ বিচ্ছিন্ন… আবার যুক্ত হচ্ছে", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp).background(Color(0xFFB3122F), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 3.dp))
    }
}

/** Gold ring back button (same look as the Auto Ludo page). */
@Composable
fun CarromBackButton(onBack: () -> Unit) {
    Box(Modifier.size(42.dp).shadow(6.dp, CircleShape, ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A)).clip(CircleShape)
        .background(Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))))
        .padding(3.5.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3A4AC8), Color(0xFF121A61)))).clickable(onClick = onBack),
        contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFFFFE27A), modifier = Modifier.size(22.dp)) }
}

/** Big premium match clock: gold double frame, navy glass, gold stopwatch badge, gold digits. [low] = red digits + red pulse. */
@Composable
fun CarromClockPill(text: String, low: Boolean) {
    val inf = rememberInfiniteTransition(label = "carromClock")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "carromClockPulse")
    val shape = RoundedCornerShape(50)
    val glow = if (low) Color(0xFFFF5064) else Color(0xFFFFC850)
    Row(
        Modifier
            .shadow(if (low) (6f + 12f * pulse).dp else 10.dp, shape, ambientColor = glow, spotColor = glow)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC8901E), Color(0xFF8A5A08), Color(0xFFFFE7A0))), shape)
            .padding(2.5.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF22339A), Color(0xFF0A1250))), shape)
            .border(1.dp, Color(0x80FFF3C4), shape)
            .padding(start = 4.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp).shadow(3.dp, CircleShape).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC21E), Color(0xFFA06A08)))), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val u = size.width / 24f; val c = Offset(12f * u, 13.5f * u); val ink = Color(0xFF5A3A00)
                drawCircle(ink, 7.5f * u, c, style = Stroke(2.2f * u))
                drawRect(ink, Offset(10f * u, 2f * u), Size(4f * u, 3f * u))
                drawLine(ink, c, Offset(12f * u, 9.5f * u), 2f * u, StrokeCap.Round)
                drawLine(ink, c, Offset(14.8f * u, 15.5f * u), 2f * u, StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = TextStyle(
                brush = Brush.verticalGradient(
                    if (low) listOf(Color(0xFFFFD0D6), Color(0xFFFF5A6A), Color(0xFFB3122F))
                    else listOf(Color(0xFFFFF7D0), Color(0xFFFFD86A), Color(0xFFE0A12A), Color(0xFFFFE7A0))
                ),
                fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                shadow = Shadow(Color(0xFF3A2400), Offset(0f, 3f), 2f)
            )
        )
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
