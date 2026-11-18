package com.example.ui.minigames

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.example.R
import kotlin.random.Random

// Money Wheel artwork, part 3: sounds, result card, confetti. VISUAL / SOUND ONLY.

/** Short game sounds from res/raw. */
object WhSfx {
    enum class S { TICK, CLICK, SPIN, WIN, LOSE }
    private var pool: SoundPool? = null
    private val ids = HashMap<S, Int>()
    fun prepare(context: Context) {
        if (pool != null) return
        try {
            val p = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build()
            val c = context.applicationContext
            ids[S.TICK] = p.load(c, R.raw.wh_tick, 1)
            ids[S.CLICK] = p.load(c, R.raw.wh_click, 1)
            ids[S.SPIN] = p.load(c, R.raw.wh_spin, 1)
            ids[S.WIN] = p.load(c, R.raw.wh_win, 1)
            ids[S.LOSE] = p.load(c, R.raw.wh_lose, 1)
            pool = p
        } catch (_: Throwable) { }
    }
    fun play(s: S, volume: Float = 1f) {
        val p = pool ?: return
        val id = ids[s] ?: return
        try { p.play(id, volume, volume, 1, 0, 1f) } catch (_: Throwable) { }
    }
}

private fun Dp.sp(): TextUnit = TextUnit(this.value, TextUnitType.Sp)

/** Win / lose card over the wheel. Tap anywhere on it to close. */
@Composable
fun WhResultCard(u: Dp, win: Boolean, bigWin: Boolean, title: String, amount: String, sub: String, onClose: () -> Unit) {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(0.45f, 300f)) }
    val inf = rememberInfiniteTransition(label = "whRes")
    val rays by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "whRays")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "whBob")
    val edge = if (win) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
               else listOf(Color(0xFFE8ECFF), Color(0xFF8A96C8), Color(0xFF3A4270), Color(0xFF8A96C8), Color(0xFFE8ECFF))
    val shape = RoundedCornerShape(u * 7)
    Box(
        Modifier.padding(horizontal = u * 10).fillMaxWidth()
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.3f) / 0.7f).coerceIn(0f, 1f) }
            .shadow(u * 4, shape, ambientColor = edge[1], spotColor = edge[1]).clip(shape).background(Brush.linearGradient(edge)).padding(u * 0.7f)
            .clip(RoundedCornerShape(u * 6.3f))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A36A4), Color(0xFF141B6A), Color(0xFF0B1040))))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose).testTag("spin_result_card")
    ) {
        Column(Modifier.fillMaxWidth().padding(start = u * 4, end = u * 4, top = u * 3, bottom = u * 5), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(u * 30, u * 26), contentAlignment = Alignment.Center) {
                Canvas(Modifier.requiredSize(u * 62)) {
                    val c = if (win) Color(0xFFFFD678) else Color(0xFFAABEFF)
                    rotate(rays) {
                        for (k in 0 until 18) drawArc(Brush.radialGradient(listOf(c.copy(alpha = 0.42f), Color.Transparent), radius = size.minDimension / 2), k * 20f, 8f, true)
                    }
                }
                if (win) Image(painterResource(if (bigWin) R.drawable.wh_bag else R.drawable.wh_trophy), null, Modifier.size(u * 24).offset(y = -(u * 1.5f) * bob))
                else Text("😢", fontSize = (u * 18).sp(), modifier = Modifier.offset(y = -(u * 1.5f) * bob))
            }
            Text(title, textAlign = TextAlign.Center, style = TextStyle(
                fontSize = (u * 8.5f).sp(), fontWeight = FontWeight.Black,
                brush = Brush.verticalGradient(if (win) listOf(Color(0xFFFFFDE8), Color(0xFFFFE66A), Color(0xFFFFC21E), Color(0xFFF08A00)) else listOf(Color.White, Color(0xFFDDE3FF), Color(0xFF9AA6D8))),
                shadow = Shadow(if (win) Color(0xFF5A3200) else Color(0xFF141B5A), Offset(0f, 5f), 2f)
            ))
            if (win) {
                Spacer(Modifier.height(u * 2))
                Text(amount, fontSize = (u * 7.5f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFE8FFF0),
                    style = TextStyle(shadow = Shadow(Color(0xFF04210F), Offset(0f, 4f), 1f)),
                    modifier = Modifier.clip(RoundedCornerShape(u * 6)).background(Brush.verticalGradient(listOf(Color(0x5922C55E), Color(0x990B5A2A))))
                        .border(u * 0.5f, Color(0xFF4ADE80), RoundedCornerShape(u * 6)).padding(horizontal = u * 6, vertical = u * 1.2f))
            }
            Spacer(Modifier.height(u * 2))
            Text(sub, fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (win) Color(0xFFFFE7A0) else Color(0xFFC8CEEA))
        }
    }
}

/** Gold, cream, red and blue paper falling once over the whole screen. */
@Composable
fun WhConfetti() {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val bits = remember { List(30) { floatArrayOf(Random.nextFloat(), Random.nextFloat() * 0.25f, Random.nextFloat() * 360f, (it % 4).toFloat()) } }
    val cols = listOf(Color(0xFFFFD45A), Color(0xFFFFF3C4), Color(0xFFFF4D62), Color(0xFF3A5BE8))
    Canvas(Modifier.fillMaxSize()) {
        bits.forEach { b ->
            val t = ((p.value - b[1]) / 0.75f).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach
            val x = b[0] * size.width; val y = -20f + t * (size.height + 40f)
            rotate(b[2] + t * 720f, Offset(x, y)) {
                drawRect(cols[b[3].toInt()], Offset(x - size.width * 0.01f, y - size.width * 0.006f), Size(size.width * 0.02f, size.width * 0.012f))
            }
        }
    }
}
