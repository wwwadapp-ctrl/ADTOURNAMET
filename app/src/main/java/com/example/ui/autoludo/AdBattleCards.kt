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
import androidx.compose.foundation.layout.offset
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
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier, contentAlignment = if (isMe) Alignment.CenterStart else Alignment.CenterEnd) {
        Box(modifier = Modifier.widthIn(max = 340.dp).height(88.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = if (active) 20.dp else 8.dp,
                        shape = shape,
                        ambientColor = if (active) Color(0xFFFFB300) else Color.Black,
                        spotColor = if (active) Color(0xFFFFA000) else Color.Black
                    )
                    .background(Brush.verticalGradient(listOf(Color(0xFF22348C), Color(0xFF14205E), Color(0xFF0E1748))), shape)
                    .border(
                        width = 2.5.dp,
                        brush = if (active) GoldBrush else Brush.linearGradient(listOf(Color(0xFFE8C46A), Color(0xFFB9861C), Color(0xFF8A5E10))),
                        shape = shape
                    )
            )
            Canvas(modifier = Modifier.matchParentSize()) { drawAdCardOrnaments(active) }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
    Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
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
            val gr = r - stroke * 1.15f
            drawCircle(GoldBrush, gr, style = Stroke(width = 2.6.dp.toPx()))
            drawCircle(Color(0xFFFFE7A0), gr - 1.9.dp.toPx(), style = Stroke(width = 0.8.dp.toPx()))
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
        if (player.isCurrentTurn) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-15).dp)
                    .size(width = 28.dp, height = 20.dp)
            ) { drawAdCrown() }
        }
    }
}

@Composable
private fun AdPlayerInfo(player: LudoPlayer, isMe: Boolean, align: Alignment.Horizontal) {
    val bn = LocalAppStrings.current === BengaliStrings
    Column(horizontalAlignment = align) {
        Text(
            text = player.name,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))),
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF4A3000), Offset(0f, 3f), 4f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp)
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
        Canvas(modifier = Modifier.size(62.dp)) {
            val c = center; val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1D2B78), Color(0xFF0C1446)), center = c, radius = r), r - 1.dp.toPx(), c)
            drawCircle(GoldBrush, r - 1.2.dp.toPx(), c, style = Stroke(width = 2.2.dp.toPx()))
            for (i in 0 until 24) {
                val a = i / 24f * 2f * Math.PI.toFloat()
                val pr = r - 4.6.dp.toPx()
                drawCircle(Color(0xFFFFE08A), 1.4.dp.toPx(), Offset(c.x + pr * kotlin.math.cos(a), c.y + pr * kotlin.math.sin(a)))
            }
            drawCircle(Color(0xFFB9861C), r - 8.dp.toPx(), c, style = Stroke(width = 0.9.dp.toPx()))
        }
        if (dice != null) dice()
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdCardOrnaments(active: Boolean) {
    val inset = 5.dp.toPx()
    drawRoundRect(
        color = Color(0xFFF0BE50).copy(alpha = 0.75f),
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = if (active) 0.16f else 0.10f), Color.Transparent), startY = inset, endY = inset + 22.dp.toPx()),
        topLeft = Offset(12.dp.toPx(), inset),
        size = androidx.compose.ui.geometry.Size(size.width - 24.dp.toPx(), 22.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
    )
    val s = 16.dp.toPx(); val o = 7.dp.toPx(); val sw = 1.3.dp.toPx()
    fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx + dx * s, cy)
            cubicTo(cx + dx * s * 0.5f, cy, cx, cy + dy * s * 0.5f, cx, cy + dy * s)
            moveTo(cx + dx * s * 0.62f, cy)
            cubicTo(cx + dx * s * 0.62f, cy + dy * s * 0.45f, cx + dx * s * 0.2f, cy + dy * s * 0.45f, cx + dx * s * 0.25f, cy + dy * s * 0.25f)
            moveTo(cx, cy + dy * s * 0.62f)
            cubicTo(cx + dx * s * 0.45f, cy + dy * s * 0.62f, cx + dx * s * 0.45f, cy + dy * s * 0.2f, cx + dx * s * 0.25f, cy + dy * s * 0.25f)
        }
        drawPath(p, Color(0xFFE8B23A), style = Stroke(width = sw, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFFE08A), 1.5.dp.toPx(), Offset(cx + dx * s * 0.25f, cy + dy * s * 0.25f))
    }
    corner(o, o, 1f, 1f); corner(size.width - o, o, -1f, 1f); corner(o, size.height - o, 1f, -1f); corner(size.width - o, size.height - o, -1f, -1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdCrown() {
    val w = size.width; val h = size.height
    fun p(x: Float, y: Float) = Offset(w * x / 28f, h * y / 20f)
    val crown = androidx.compose.ui.graphics.Path().apply {
        moveTo(p(2f, 17f).x, p(2f, 17f).y); lineTo(p(4f, 6f).x, p(4f, 6f).y); lineTo(p(9.5f, 11f).x, p(9.5f, 11f).y)
        lineTo(p(14f, 3f).x, p(14f, 3f).y); lineTo(p(18.5f, 11f).x, p(18.5f, 11f).y); lineTo(p(24f, 6f).x, p(24f, 6f).y)
        lineTo(p(26f, 17f).x, p(26f, 17f).y); close()
    }
    drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFD99A16))))
    drawPath(crown, Color(0xFF8A5600), style = Stroke(width = 1.dp.toPx()))
    drawCircle(Color(0xFFFFE08A), w * 1.7f / 28f, p(4f, 6f))
    drawCircle(Color(0xFFFF6B78), w * 1.9f / 28f, p(14f, 3f))
    drawCircle(Color(0xFFFFE08A), w * 1.7f / 28f, p(24f, 6f))
    drawRoundRect(Color(0xFFC98A10), topLeft = p(3f, 16f), size = androidx.compose.ui.geometry.Size(w * 22f / 28f, h * 3f / 20f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))
}
