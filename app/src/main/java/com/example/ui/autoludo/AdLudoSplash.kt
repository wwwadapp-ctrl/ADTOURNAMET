package com.example.ui.autoludo

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// AD Ludo branded loading screen (replaces the old image that contained another brand's logo).
// VISUAL ONLY: same behaviour as before — the bar fills in 3 seconds, then onFinished() is called once.

private val SplashGold = Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00)))

@Composable
fun AdLudoSplashScreen(onFinished: () -> Unit) {
    val bn = LocalAppStrings.current === BengaliStrings
    fun digits(s: String) = if (!bn) s else s.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")

    var target by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "AdSplashProgress",
        finishedListener = { if (it >= 1f) onFinished() }
    )
    LaunchedEffect(Unit) { target = 1f }

    val tips = if (bn) listOf(
        "💡 টিপস: ৬ উঠলে ঘর থেকে গুটি বের হয়",
        "💡 টিপস: তারা-চিহ্নের ঘরে গুটি কাটা যায় না",
        "💡 টিপস: প্রতিপক্ষের গুটি কাটলে বোনাস চাল",
        "💡 টিপস: সময়মতো চাল না দিলে স্ট্রাইক হয়"
    ) else listOf(
        "💡 Tip: roll a 6 to bring a pawn out",
        "💡 Tip: pawns on star cells cannot be captured",
        "💡 Tip: capturing a pawn gives a bonus roll",
        "💡 Tip: missing your turn gives a strike"
    )
    val stages = if (bn) listOf("বোর্ড সাজানো হচ্ছে...", "গুটি সাজানো হচ্ছে...", "ছক্কা গরম হচ্ছে...", "খেলা শুরু হচ্ছে!")
    else listOf("Setting up the board...", "Placing the pawns...", "Warming up the dice...", "Starting the game!")
    val marks = if (bn) listOf("বোর্ড", "গুটি", "ছক্কা", "প্রস্তুত") else listOf("Board", "Pawns", "Dice", "Ready")

    var tipIndex by remember { mutableStateOf((0 until 4).random()) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tipIndex = (tipIndex + 1) % tips.size } }

    val inf = rememberInfiniteTransition(label = "AdSplashInf")
    val rayAngle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "rays")
    val floatY by inf.animateFloat(0f, -6f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val hop by inf.animateFloat(0f, -5f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hop")
    val stripe by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing)), label = "stripe")
    val shine by inf.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shine")
    val pulse by inf.animateFloat(1f, 1.25f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2A4BB0), Color(0xFF13205A), Color(0xFF0B1640))))
    ) {
        // faint grid + rotating gold rays
        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(-12f) {
                val step = 34.dp.toPx()
                var x = -size.width; while (x < size.width * 2) { drawLine(Color.White.copy(alpha = 0.07f), Offset(x, -size.height), Offset(x, size.height * 2), 1f); x += step }
                var y = -size.height; while (y < size.height * 2) { drawLine(Color.White.copy(alpha = 0.07f), Offset(-size.width, y), Offset(size.width * 2, y), 1f); y += step }
            }
            val c = Offset(size.width / 2f, size.height * 0.34f)
            val r = size.maxDimension * 0.62f
            rotate(rayAngle, c) {
                for (i in 0 until 20) {
                    val a0 = Math.toRadians(i * 18.0); val a1 = Math.toRadians(i * 18.0 + 6.0)
                    val p = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + r * cos(a0).toFloat(), c.y + r * sin(a0).toFloat())
                        lineTo(c.x + r * cos(a1).toFloat(), c.y + r * sin(a1).toFloat())
                        close()
                    }
                    drawPath(p, Brush.radialGradient(listOf(Color(0x40FFD54F), Color(0x00FFD54F)), center = c, radius = r * 0.6f))
                }
            }
        }

        // logo + title
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer { translationY = floatY.dp.toPx() }
                    .shadow(24.dp, androidx.compose.foundation.shape.CircleShape, ambientColor = Color(0xFFFFC107), spotColor = Color(0xFFFFA000), clip = false)
            ) { drawAdCrest() }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "AD LUDO",
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC107), Color(0xFFB07A00))),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    shadow = Shadow(Color(0xFF5A3C00), Offset(0f, 6f), 8f)
                )
            )
            Text(
                text = if (bn) "১v১ লুডু ব্যাটল · জিতুন আসল পুরস্কার" else "1v1 Ludo Battle · Win real prizes",
                color = Color(0xFFFFE082),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.Bottom) {
                AdLudoPawnToken(pawnColor = Color(0xFFE53935), isEligible = false, modifier = Modifier.size(width = 36.dp, height = 50.dp))
                Canvas(modifier = Modifier.size(54.dp)) { drawSplashDice() }
                AdLudoPawnToken(pawnColor = Color(0xFFFBC02D), isEligible = false, modifier = Modifier.size(width = 36.dp, height = 50.dp))
            }
        }

        // bottom: tip, stage, bar with riding pawn, milestones
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tips[tipIndex],
                color = Color(0xFFDCE3FF),
                fontSize = 13.sp,
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0x1AFFFFFF), Color(0x0AFFFFFF))), RoundedCornerShape(50))
                    .border(1.dp, Color(0x59FFD54F), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            )
            Spacer(Modifier.height(14.dp))
            val stageIdx = (progress * 4).toInt().coerceIn(0, 3)
            Text(
                text = stages[stageIdx],
                color = Color(0xFFFFDF7A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(0.8f).height(70.dp)) {
                val barW = maxWidth
                val riderX = barW * progress
                // riding pawn + percent bubble
                Column(
                    modifier = Modifier.offset(x = riderX - 30.dp, y = hop.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = digits("${(progress * 100).toInt()}") + "%",
                        color = Color(0xFF2B1B00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .size(width = 60.dp, height = 20.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107))), RoundedCornerShape(50))
                            .padding(top = 1.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    AdLudoPawnToken(pawnColor = Color(0xFFE53935), isEligible = false, modifier = Modifier.size(width = 18.dp, height = 25.dp))
                }
                // the bar
                Canvas(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(24.dp)) {
                    val h = size.height; val w = size.width; val rad = CornerRadius(h / 2f, h / 2f)
                    drawRoundRect(Color(0xFFFFE082), topLeft = Offset(-3.5f, -3.5f), size = Size(w + 7f, h + 7f), cornerRadius = CornerRadius(h / 2f + 3.5f))
                    drawRoundRect(Color(0xFFB07A00), topLeft = Offset(-2f, -2f), size = Size(w + 4f, h + 4f), cornerRadius = CornerRadius(h / 2f + 2f))
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF071030), Color(0xFF16245F))), size = size, cornerRadius = rad)
                    for (i in 1 until 15) { val x = w * i / 15f; drawLine(Color.White.copy(alpha = 0.07f), Offset(x, 0f), Offset(x, h), 1f) }
                    val fw = (w - 4f) * progress
                    if (fw > 1f) {
                        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(2f, 2f, 2f + fw, h - 2f, CornerRadius((h - 4f) / 2f))) }
                        clipPath(clip) {
                            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF7CC), Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFC98500))))
                            val sp = 16.dp.toPx(); var x = -h + stripe * sp
                            while (x < w + h) { drawLine(Color.White.copy(alpha = 0.28f), Offset(x, h), Offset(x + h, 0f), 8.dp.toPx() / 2f); x += sp }
                            val sx = w * shine
                            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xD9FFFFFF), Color.Transparent), startX = sx - w * 0.15f, endX = sx + w * 0.15f))
                        }
                        drawCircle(
                            Brush.radialGradient(listOf(Color.White, Color(0xFFFFF3B0), Color(0x99FFC107), Color.Transparent), center = Offset(2f + fw, h / 2f), radius = 13.dp.toPx() * pulse),
                            radius = 13.dp.toPx() * pulse, center = Offset(2f + fw, h / 2f)
                        )
                    }
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0x73FFFFFF), Color.Transparent)), topLeft = Offset(8f, 3f), size = Size(w - 16f, 6.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.SpaceBetween) {
                marks.forEachIndexed { i, m ->
                    val on = progress >= (i + 1) / 4f - 0.001f
                    Text(
                        text = "★ $m",
                        color = if (on) Color(0xFFFFD54F) else Color(0xFF7F8CC9),
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdCrest() {
    val r = size.minDimension / 2f; val c = center; val k = r / 31f
    fun o(x: Float, y: Float) = Offset(c.x + (x - 32f) * k, c.y + (y - 32f) * k)
    val gold = Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00)), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r))
    drawCircle(gold, r, c)
    drawCircle(Color(0xFF0B1640), 28.6f * k, c)
    drawCircle(gold, 20.6f * k, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF2E4BB8), Color(0xFF0B1640)), center = Offset(c.x - 4f * k, c.y - 5f * k), radius = 22f * k), 19.2f * k, c)
    val ar = 26f * k
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true; color = android.graphics.Color.parseColor("#FFD54F"); textSize = 6.4f * k
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.LEFT; letterSpacing = 0.05f
    }
    val arc = android.graphics.Path().apply { addArc(android.graphics.RectF(c.x - ar, c.y - ar, c.x + ar, c.y + ar), 155f, 230f) }
    val len = android.graphics.PathMeasure(arc, false).length
    drawIntoCanvas {
        val label = "AD TOURNAMENT"
        it.nativeCanvas.drawTextOnPath(label, arc, ((len - paint.measureText(label)) / 2f).coerceAtLeast(0f), paint.textSize * 0.35f, paint)
    }
    listOf(o(18.6f, 54.2f), o(32f, 58.4f), o(45.4f, 54.2f)).forEach { drawCircle(Color(0xFFFFD54F), 1.3f * k, it) }
    val cup = Path().apply {
        moveTo(o(24f, 21f).x, o(24f, 21f).y); lineTo(o(40f, 21f).x, o(40f, 21f).y); lineTo(o(39f, 28f).x, o(39f, 28f).y)
        quadraticBezierTo(o(32f, 35f).x, o(32f, 35f).y, o(25f, 28f).x, o(25f, 28f).y); close()
    }
    drawPath(cup, gold); drawPath(cup, Color(0xFF7A5200), style = Stroke(0.5f * k))
    val lh = Path().apply { moveTo(o(24f, 22.5f).x, o(24f, 22.5f).y); quadraticBezierTo(o(19.5f, 23f).x, o(19.5f, 23f).y, o(21.3f, 27f).x, o(21.3f, 27f).y); quadraticBezierTo(o(23f, 29f).x, o(23f, 29f).y, o(25f, 28f).x, o(25f, 28f).y) }
    val rh = Path().apply { moveTo(o(40f, 22.5f).x, o(40f, 22.5f).y); quadraticBezierTo(o(44.5f, 23f).x, o(44.5f, 23f).y, o(42.7f, 27f).x, o(42.7f, 27f).y); quadraticBezierTo(o(41f, 29f).x, o(41f, 29f).y, o(39f, 28f).x, o(39f, 28f).y) }
    drawPath(lh, gold, style = Stroke(1.5f * k, cap = StrokeCap.Round)); drawPath(rh, gold, style = Stroke(1.5f * k, cap = StrokeCap.Round))
    drawRect(gold, o(30.4f, 31.5f), Size(3.2f * k, 3.6f * k))
    drawRoundRect(gold, o(26.2f, 34.8f), Size(11.6f * k, 3f * k), CornerRadius(1.2f * k))
    drawOval(Color.White.copy(alpha = 0.9f), o(26.6f, 22.7f), Size(3.6f * k, 2.2f * k))
    val ludo = android.graphics.Paint().apply {
        isAntiAlias = true; color = android.graphics.Color.WHITE; textSize = 6.8f * k
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER; letterSpacing = 0.12f
    }
    drawIntoCanvas { it.nativeCanvas.drawText("LUDO", c.x, o(32f, 45.5f).y, ludo) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSplashDice() {
    val w = size.width; val k = w / 56f
    drawRoundRect(Color(0xFF9A1010), topLeft = Offset(6f * k, 10f * k), size = Size(44f * k, 44f * k), cornerRadius = CornerRadius(11f * k))
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFF6B66), Color(0xFFE53935), Color(0xFFA31515))), topLeft = Offset(6f * k, 6f * k), size = Size(44f * k, 44f * k), cornerRadius = CornerRadius(11f * k))
    drawRoundRect(Color.White, topLeft = Offset(6f * k, 6f * k), size = Size(44f * k, 44f * k), cornerRadius = CornerRadius(11f * k), style = Stroke(1.5f * k))
    listOf(18f to 18f, 38f to 18f, 28f to 28f, 18f to 38f, 38f to 38f).forEach { (x, y) -> drawCircle(Color.White, 4f * k, Offset(x * k, y * k)) }
}
