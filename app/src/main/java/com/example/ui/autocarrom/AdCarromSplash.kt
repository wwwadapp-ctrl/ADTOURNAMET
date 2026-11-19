package com.example.ui.autocarrom

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlinx.coroutines.delay

// AD Carrom loading screen shown before the board. VISUAL ONLY: the bar fills in 3 seconds,
// then onFinished() is called once. It covers the game screen and swallows touches meanwhile.

@Composable
fun AdCarromSplashScreen(onFinished: () -> Unit) {
    var target by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "CarromSplashProgress",
        finishedListener = { if (it >= 1f) onFinished() }
    )
    LaunchedEffect(Unit) { target = 1f }

    val tips = listOf(
        "টিপস: নিজের গুটি পকেটে ফেললে আবার পালা পাবেন",
        "টিপস: কুইন ফেলে পরের শটে নিজের গুটি ফেলে কভার করুন",
        "টিপস: স্ট্রাইকার পকেটে পড়লে ফাউল হয়",
        "টিপস: পাউডার দিলে গুটি বেশি পিছলে যায়"
    )
    val stages = listOf("বোর্ড সাজানো হচ্ছে...", "গুটি সাজানো হচ্ছে...", "পাউডার ছিটানো হচ্ছে...", "খেলা শুরু হচ্ছে!")
    val marks = listOf("বোর্ড" to 'w', "গুটি" to 's', "পাউডার" to 'q', "প্রস্তুত" to 'k')
    var tipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tipIndex = (tipIndex + 1) % tips.size } }

    val inf = rememberInfiniteTransition(label = "CarromSplashInf")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "twinkle")
    val floatY by inf.animateFloat(0f, -6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val bob by inf.animateFloat(0f, -4f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")

    val near = remember { csNearSparks() }
    val far = remember { csFarSparks() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08103A))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }
    ) {
        val k = (maxHeight.value / 820f).coerceIn(0.72f, 1.1f)

        // background: radial navy, corner filigree, sparkles
        Canvas(Modifier.fillMaxSize()) { drawCarromSplashBg(far, tw) }

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = (40 * k).dp, bottom = (22 * k).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // crest with its aura (glow, swooshes, near sparkles)
            Box(Modifier.size((240 * k).dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawCarromAura(near, tw) }
                Canvas(Modifier.fillMaxSize().graphicsLayer { translationY = floatY.dp.toPx() }) { drawCarromCrest() }
            }
            Text(
                text = "AD CARROM",
                style = TextStyle(
                    brush = Brush.verticalGradient(0f to Color(0xFFFFF7D0), 0.3f to Color(0xFFFFD86A), 0.55f to Color(0xFFE0A12A), 0.75f to Color(0xFF8C5A08), 1f to Color(0xFFE8B848)),
                    fontSize = (46 * k).sp, fontWeight = FontWeight.Black, letterSpacing = (4 * k).sp,
                    shadow = Shadow(Color(0xFF3A2400), Offset(0f, 6f), 10f)
                ),
                modifier = Modifier.padding(top = (6 * k).dp)
            )
            Text(
                text = "১v১ ক্যারম ব্যাটল · জিতুন আসল পুরস্কার",
                style = TextStyle(brush = Brush.verticalGradient(listOf(Color(0xFFFFF0B8), Color(0xFFE3AE3C))), fontSize = (15 * k).sp, fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height((26 * k).dp))
            Row(horizontalArrangement = Arrangement.spacedBy((28 * k).dp), verticalAlignment = Alignment.CenterVertically) {
                for (c in listOf('w', 's', 'q', 'k')) {
                    val big = c == 's'
                    Canvas(Modifier.size(((if (big) 58 else 50) * k).dp).graphicsLayer { if (big) translationY = bob.dp.toPx() }) {
                        val r = size.width * 0.42f
                        drawCircle(Brush.radialGradient(listOf(Color(0x59FFC850), Color(0x00FFC850)), center = center, radius = r * 1.5f), r * 1.5f, center)
                        if (big) drawStriker(center.x, center.y, r, 0.5f) else drawCoin(c, center.x, center.y, r)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // tip box with ornate gold corners
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .heightIn(min = (58 * k).dp)
                    .drawWithContent {
                        drawContent()
                        val d = 1.dp.toPx(); val unit = 43f * d / 90f
                        for ((cx, sx) in listOf(0f to 1f, size.width to -1f)) for ((cy, sy) in listOf(0f to 1f, size.height to -1f)) {
                            withTransform({ translate(cx, cy); scale(sx, sy, Offset.Zero) }) {
                                val p = -3.3f * d
                                csScroll(p, p, 0.62f * unit, PI.toFloat() / 4, false, Color(0xFFE8B848), 3f * unit)
                                csScroll(p, p, 0.62f * unit, PI.toFloat() / 4, true, Color(0xFFE8B848), 3f * unit)
                                drawCircle(csGoldBrush(Offset(p - 6 * unit, p - 6 * unit), Offset(p + 6 * unit, p + 6 * unit)), 6f * unit, Offset(p, p))
                            }
                        }
                    }
                    .shadow(10.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xE6142260), Color(0xE60A123C))))
                    .border(2.dp, Color(0xFFD9A93A))
                    .padding(5.dp)
                    .border(1.dp, Color(0x8CFFDC78))
                    .padding(horizontal = (22 * k).dp, vertical = (10 * k).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tips[tipIndex], color = Color(0xFFFFF7E6), fontSize = (16 * k).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = (22 * k).sp)
            }

            val stageIdx = (progress * 4).toInt().coerceIn(0, 3)
            Text(
                stages[stageIdx],
                style = TextStyle(color = Color(0xFFFFF3D6), fontSize = (19 * k).sp, fontWeight = FontWeight.ExtraBold, shadow = Shadow(Color(0x80FFC85A), Offset.Zero, 18f)),
                modifier = Modifier.padding(top = (22 * k).dp)
            )

            // gold-framed glossy blue bar with the percent on the right
            Box(
                modifier = Modifier
                    .padding(top = (14 * k).dp)
                    .fillMaxWidth(0.84f)
                    .height((29 * k).dp)
                    .shadow(10.dp, RoundedCornerShape(50), ambientColor = Color(0xFFFFBE3C), spotColor = Color(0xFFFFBE3C))
                    .background(Brush.verticalGradient(0f to Color(0xFFFFF0B0), 0.45f to Color(0xFFC8901E), 0.55f to Color(0xFF7A5208), 1f to Color(0xFFE8C060)), RoundedCornerShape(50))
                    .padding(3.5.dp)
            ) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(50)).background(Brush.verticalGradient(listOf(Color(0xFF040820), Color(0xFF0D1640))))) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(0f to Color(0xFF9FB6FF), 0.25f to Color(0xFF4D6FE8), 0.6f to Color(0xFF2140B8), 1f to Color(0xFF132A80)))
                            .drawWithContent {
                                drawContent()
                                val h = size.height
                                if (size.width > h) drawRoundRect(Color(0x73FFFFFF), Offset(h * 0.4f, h * 0.12f), Size(size.width - h * 0.8f, h * 0.2f), CornerRadius(h * 0.1f))
                            }
                    )
                }
                Text(
                    bnDigits("${(progress * 100).toInt()}%"),
                    color = Color(0xFFFFF3D6), fontSize = (16 * k).sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = (12 * k).dp)
                )
            }

            // milestone medals
            Row(Modifier.fillMaxWidth(0.92f).padding(top = (18 * k).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                marks.forEachIndexed { i, (label, coin) ->
                    val on = progress >= (i + 1) / 4f - 0.001f
                    Column(Modifier.alpha(if (on) 1f else 0.38f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.size((58 * k).dp)) { drawCarromMedal(coin) }
                        Text("★ $label", color = Color(0xFFFFD45A), fontSize = (14 * k).sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun bnDigits(s: String) = s.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")
