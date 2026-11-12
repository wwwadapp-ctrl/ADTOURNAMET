package com.example.ui.autoludo

import android.media.SoundPool
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings

data class AdDiceArgs(
    val value: Int,
    val enabled: Boolean,
    val isPlayer1Turn: Boolean,
    val color: Color,
    val onRoll: (Int) -> Unit,
    val soundPool: SoundPool?,
    val diceSoundId: Int,
    val toneGenerator: ToneGenerator?,
    val forbidSix: Boolean = false
)

@Composable
fun rememberAdDiceContent(): @Composable (AdDiceArgs) -> Unit = remember {
    movableContentOf { a: AdDiceArgs ->
        Luxury3DCubeDice(
            diceValue = a.value,
            isEnabled = a.enabled,
            isPlayer1Turn = a.isPlayer1Turn,
            diceColor = a.color,
            onRoll = a.onRoll,
            soundPool = a.soundPool,
            diceSoundId = a.diceSoundId,
            toneGenerator = a.toneGenerator,
            forbidSix = a.forbidSix
        )
    }
}

private val GoldBrush = Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00)))

@Composable
fun AdPlayerBattleCard(
    player: LudoPlayer,
    isMe: Boolean,
    timerProgress: Float,
    dice: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val active = player.isCurrentTurn
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = modifier, contentAlignment = if (isMe) Alignment.CenterStart else Alignment.CenterEnd) {
        Box(modifier = Modifier.widthIn(max = 340.dp).height(88.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = if (active) 18.dp else 8.dp,
                        shape = shape,
                        ambientColor = if (active) Color(0xFFFFB300) else Color.Black,
                        spotColor = if (active) Color(0xFFFFA000) else Color.Black
                    )
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B3E95), Color(0xFF18266B))), shape)
                    .border(
                        width = 2.dp,
                        brush = if (active) GoldBrush else Brush.linearGradient(listOf(Color(0x8CFFD54F), Color(0x8CFFD54F))),
                        shape = shape
                    )
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isMe) {
                    AdAvatar(player, timerProgress)
                    AdPlayerInfo(player, isMe, Alignment.Start)
                    AdDiceSlot(dice)
                } else {
                    AdDiceSlot(dice)
                    AdPlayerInfo(player, isMe, Alignment.End)
                    AdAvatar(player, timerProgress)
                }
            }
        }
    }
}

@Composable
private fun AdAvatar(player: LudoPlayer, timerProgress: Float) {
    val pc = Color(player.pawnColor)
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.5.dp.toPx()
            val r = size.minDimension / 2f - stroke / 2f
            drawCircle(Color.White.copy(alpha = 0.18f), r, style = Stroke(width = stroke))
            if (player.isCurrentTurn) {
                val sweep = 360f * timerProgress.coerceIn(0f, 1f)
                drawArc(
                    brush = GoldBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(lerp(pc, Color.White, 0.45f), pc, lerp(pc, Color.Black, 0.45f)))),
            contentAlignment = Alignment.Center
        ) {
            val fetchedPhoto = rememberAdUserPhoto(player.userId)
            val photo = player.avatarUrl.ifBlank { fetchedPhoto }
            if (photo.isNotBlank()) {
                com.example.ui.components.EsportsAvatar(
                    photoUrl = photo,
                    name = player.name,
                    size = 44.dp,
                    borderWidth = 0.dp
                )
            } else {
                val words = player.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                val initials = when {
                    words.size >= 2 -> "${words[0].first()}${words[1].first()}"
                    words.size == 1 -> words[0].take(2)
                    else -> "?"
                }.uppercase()
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AdPlayerInfo(player: LudoPlayer, isMe: Boolean, align: Alignment.Horizontal) {
    val bn = LocalAppStrings.current === BengaliStrings
    Column(horizontalAlignment = align) {
        Text(
            text = player.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val label = if (isMe) (if (bn) "আপনার পালা" else "Your turn") else (if (bn) "প্রতিপক্ষের পালা" else "Opponent's turn")
                Text(
                    text = label,
                    color = Color(0xFF1B1200),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .alpha(if (player.isCurrentTurn) 1f else 0f)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300))), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i < player.strikes) Color(0xFFE53935) else Color(0xFF43A047))
                            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdDiceSlot(dice: (@Composable () -> Unit)?) {
    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
        if (dice != null) {
            dice()
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun rememberAdUserPhoto(uid: String): String {
    val state = androidx.compose.runtime.produceState(initialValue = "", uid) {
        if (uid.isBlank() || uid.startsWith("player_")) return@produceState
        try {
            val userRef = com.example.core.firebase.FirebaseManager.getDatabase()?.getReference("users")?.child(uid)
                ?: return@produceState
            val keys = listOf("profilePhotoUrl", "profilePhoto", "avatarUrl")
            fun tryKey(i: Int) {
                if (i >= keys.size) return
                userRef.child(keys[i]).get()
                    .addOnSuccessListener { snap ->
                        val v = snap.getValue(String::class.java)
                        if (!v.isNullOrBlank()) value = v else tryKey(i + 1)
                    }
                    .addOnFailureListener { tryKey(i + 1) }
            }
            tryKey(0)
        } catch (_: Exception) { }
    }
    return state.value
}
