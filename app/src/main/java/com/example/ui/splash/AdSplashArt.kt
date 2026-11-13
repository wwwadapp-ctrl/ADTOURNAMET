package com.example.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.autoludo.AdDice3D
import com.example.ui.autoludo.AdLudoBoard
import com.example.ui.autoludo.AdLudoPawnToken
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// AD Tournament premium splash artwork. VISUAL ONLY: it never decides timing or navigation.
// The board, pawns and dice are the real in-game composables, so the splash always matches the game.

private val SpGoldStops = listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFC98A10), Color(0xFF8A5A08))
private val SpTextGold = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFD45A), Color(0xFFC98A10), Color(0xFF8A5A08)))

@Composable
fun AdSplashContent(
    progress: Float,
    screenWidth: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "adSplash")
    val twinkle by anim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "twinkle"
    )
    val pulse by anim.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse"
    )
    val bob by anim.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val dicePhase by anim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "dicePhase"
    )

    val k = if (compact) 0.82f else 1f
    val titleSize = (screenWidth.value * 0.098f).coerceIn(28f, 40f) * k
    val tagSize = (screenWidth.value * 0.056f).coerceIn(17f, 23f) * k

    Box(modifier = modifier.fillMaxSize()) {
        AdSplashBackground(twinkle = twinkle, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (compact) 10.dp else 22.dp, bottom = if (compact) 14.dp else 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdSplashEmblem(
                modifier = Modifier
                    .width(230.dp * k)
                    .height(208.dp * k)
                    .testTag("splash_ad_logo")
            )

            Text(
                text = "AD TOURNAMENT",
                style = TextStyle(
                    brush = SpTextGold,
                    fontSize = titleSize.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    shadow = Shadow(color = Color(0xFF3A2600), offset = Offset(0f, 6f), blurRadius = 0f)
                ),
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(10.dp * k))
            AdSplashRibbon(textSize = 15f * k)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val byWidth = maxWidth * 0.615f
                val byHeight = (maxHeight - 20.dp) / (1.04f * 1.41421f * 0.56f)
                val bd = minOf(byWidth, byHeight, 250.dp).coerceAtLeast(120.dp)
                AdSplashBoard(boardSize = bd, dicePhase = dicePhase, pulse = pulse)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SplashGoldWord("PLAY", tagSize)
                SplashCrownIcon(Modifier.padding(horizontal = 9.dp).size(width = 25.dp * k, height = 18.dp * k))
                SplashGoldWord("WIN", tagSize)
                SplashCrownIcon(Modifier.padding(horizontal = 9.dp).size(width = 25.dp * k, height = 18.dp * k))
                SplashGoldWord("BE KING", tagSize)
            }

            Spacer(modifier = Modifier.height(14.dp * k))

            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth(0.62f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A1E08), Color(0xFF120C02))))
                    .border(1.5.dp, Color(0xFFC98A10), RoundedCornerShape(50))
                    .padding(3.dp)
                    .testTag("splash_loading_bar")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFE08A10)))
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp * k))
            Text(
                text = "Loading...",
                style = TextStyle(
                    fontSize = (16f * k).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFE8D7A8)
                )
            )
            Spacer(modifier = Modifier.height(8.dp * k))
            SplashCrownIcon(
                modifier = Modifier
                    .offset(y = bob.dp)
                    .size(width = 32.dp * k, height = 22.dp * k),
                glow = true
            )
        }
    }
}

@Composable
private fun SplashGoldWord(text: String, size: Float) {
    Text(
        text = text,
        style = TextStyle(
            brush = SpTextGold,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            shadow = Shadow(color = Color(0xFF3A2600), offset = Offset(0f, 4f), blurRadius = 0f)
        ),
        maxLines = 1,
        softWrap = false
    )
}

// ---------------------------------------------------------------- background

private class SpDot(val x: Float, val y: Float, val r: Float, val a: Float)

@Composable
private fun AdSplashBackground(twinkle: Float, modifier: Modifier) {
    val rnd = remember { Random(7) }
    val bokeh = remember { List(16) { SpDot(rnd.nextFloat(), rnd.nextFloat(), 10f + rnd.nextFloat() * 22f, 0.25f + rnd.nextFloat() * 0.35f) } }
    val stars = remember { List(70) { SpDot(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 1.1f, 0.3f + rnd.nextFloat() * 0.6f) } }
    val confetti = remember {
        List(14) {
            val left = rnd.nextBoolean()
            SpDot(if (left) rnd.nextFloat() * 0.15f else 0.85f + rnd.nextFloat() * 0.14f, rnd.nextFloat(), rnd.nextFloat() * 180f, 0.55f)
        }
    }
    val sparkles = remember {
        listOf(
            floatArrayOf(.067f, .026f, 6f), floatArrayOf(.205f, .073f, 8f), floatArrayOf(.903f, .040f, 5f),
            floatArrayOf(.846f, .133f, 7f), floatArrayOf(.046f, .166f, 4f), floatArrayOf(.954f, .244f, 4f),
            floatArrayOf(.154f, .355f, 5f), floatArrayOf(.897f, .377f, 5f), floatArrayOf(.077f, .818f, 5f),
            floatArrayOf(.928f, .782f, 6f), floatArrayOf(.231f, .948f, 4f), floatArrayOf(.769f, .936f, 5f)
        )
    }
    Canvas(modifier = modifier) {
        val d = density
        drawRect(
            Brush.radialGradient(
                0f to Color(0xFF1B2660), 0.55f to Color(0xFF0C1338), 1f to Color(0xFF050820),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = maxOf(size.width, size.height) * 0.75f
            )
        )
        bokeh.forEach { b ->
            val c = Offset(b.x * size.width, b.y * size.height)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = 0.55f * b.a), Color.Transparent), center = c, radius = b.r * d),
                radius = b.r * d, center = c
            )
        }
        stars.forEach { s -> drawCircle(Color(0xFFFFE7A0).copy(alpha = s.a), s.r * d, Offset(s.x * size.width, s.y * size.height)) }
        confetti.forEach { c ->
            val p = Offset(c.x * size.width, c.y * size.height)
            rotate(c.r, pivot = p) {
                drawRoundRect(Color(0xFFB5545A).copy(alpha = c.a), p, Size(3f * d, 7f * d), CornerRadius(1.2f * d))
            }
        }
        sparkles.forEachIndexed { i, s ->
            val ph = ((twinkle + (i % 4) * 0.25f) % 1f)
            val t = 0.5f - 0.5f * cos(ph * 2f * Math.PI.toFloat())
            val sc = 0.6f + 0.55f * t
            drawSparkle(Offset(s[0] * size.width, s[1] * size.height), s[2] * d * sc, Color(0xFFFFF3C4).copy(alpha = 0.25f + 0.75f * t))
        }
    }
}

private fun DrawScope.drawSparkle(c: Offset, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - s)
        quadraticBezierTo(c.x + s * .18f, c.y - s * .18f, c.x + s, c.y)
        quadraticBezierTo(c.x + s * .18f, c.y + s * .18f, c.x, c.y + s)
        quadraticBezierTo(c.x - s * .18f, c.y + s * .18f, c.x - s, c.y)
        quadraticBezierTo(c.x - s * .18f, c.y - s * .18f, c.x, c.y - s)
        close()
    }
    drawPath(p, color)
}

// ---------------------------------------------------------------- emblem: crown + laurel + the app's real logo

@Composable
private fun AdSplashEmblem(modifier: Modifier) {
    val logo = painterResource(id = R.drawable.ic_launcher_foreground)
    Canvas(modifier = modifier) {
        val k = size.width / 230f
        val ex = 115f * k
        val ey = 116f * k
        // soft glow
        drawOval(
            Brush.radialGradient(
                listOf(Color(0xFFFFF6D0).copy(alpha = 0.33f), Color(0xFFFFD27A).copy(alpha = 0.16f), Color.Transparent),
                center = Offset(ex, ey + 10f * k), radius = 95f * k
            ),
            topLeft = Offset(ex - 95f * k, ey + 10f * k - 85f * k), size = Size(190f * k, 170f * k)
        )
        // laurel
        val leafBrush = Brush.linearGradient(listOf(Color(0xFFFFE7A0), Color(0xFFA8740E)))
        fun leaf(x: Float, y: Float, rx: Float, ry: Float, deg: Float) {
            rotate(deg, pivot = Offset(x, y)) {
                drawOval(leafBrush, Offset(x - rx, y - ry), Size(rx * 2, ry * 2))
                drawOval(Color(0xFF7A5208), Offset(x - rx, y - ry), Size(rx * 2, ry * 2), style = Stroke(width = 0.5f * k))
            }
        }
        for (side in listOf(-1, 1)) {
            for (i in 0..10) {
                val t = i / 10f
                val angDeg = if (side < 0) 115f + t * 120f else 65f - t * 120f
                val ang = Math.toRadians(angDeg.toDouble())
                val rr = 76f * k
                val x = ex + rr * cos(ang).toFloat(); val y = ey + 12f * k + rr * sin(ang).toFloat() * 1.05f
                val rot = angDeg + if (side < 0) 90f else -90f
                val sz = 1f - t * 0.35f
                leaf(x, y, 4.2f * sz * k, 10f * sz * k, rot + if (side < 0) 25f else -25f)
                val r2 = 66f * k
                val x2 = ex + r2 * cos(ang).toFloat(); val y2 = ey + 12f * k + r2 * sin(ang).toFloat() * 1.05f
                leaf(x2, y2, 3.6f * sz * k, 8.5f * sz * k, rot - if (side < 0) 25f else -25f)
            }
        }
        drawPath(
            Path().apply { moveTo(ex - 30f * k, ey + 92f * k); quadraticBezierTo(ex, ey + 104f * k, ex + 30f * k, ey + 92f * k) },
            Brush.horizontalGradient(listOf(Color(0xFF9A6A10), Color(0xFFFFE7A0), Color(0xFF9A6A10)), startX = ex - 30f * k, endX = ex + 30f * k),
            style = Stroke(width = 3f * k)
        )
        // the app's own logo (ic_launcher_foreground), shield about 143 units tall
        val logoSize = 108f * 2.3f * k
        translate(left = ex - 54f * 2.3f * k, top = ey + 8f * k - 53f * 2.3f * k) {
            with(logo) { draw(Size(logoSize, logoSize)) }
        }
        // crown on top
        drawRoyalCrown(Offset(ex, 38f * k), k)
    }
}

private fun DrawScope.drawRoyalCrown(c: Offset, k: Float) {
    fun o(x: Float, y: Float) = Offset(c.x + x * k, c.y + y * k)
    val gold = Brush.verticalGradient(
        0f to SpGoldStops[0], 0.45f to SpGoldStops[1], 0.75f to SpGoldStops[2], 1f to SpGoldStops[3],
        startY = c.y - 38f * k, endY = c.y + 27f * k
    )
    val dark = Color(0xFF7A5208)
    val body = Path().apply {
        val pts = listOf(-52f to 18f, -58f to -22f, -34f to -2f, -18f to -36f, 0f to -8f, 18f to -36f, 34f to -2f, 58f to -22f, 52f to 18f)
        pts.forEachIndexed { i, (x, y) -> val p = o(x, y); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        close()
    }
    drawPath(body, gold)
    drawPath(body, dark, style = Stroke(width = 1.2f * k, join = StrokeJoin.Round))
    val shade = Path().apply {
        moveTo(o(-50f, 8f).x, o(-50f, 8f).y); lineTo(o(50f, 8f).x, o(50f, 8f).y)
        lineTo(o(52f, 18f).x, o(52f, 18f).y); lineTo(o(-52f, 18f).x, o(-52f, 18f).y); close()
    }
    drawPath(shade, Brush.verticalGradient(listOf(Color(0xFFC98A10), Color(0xFF5C3A04)), startY = c.y + 8f * k, endY = c.y + 18f * k), alpha = 0.55f)
    drawRoundRect(gold, o(-54f, 16f), Size(108f * k, 11f * k), CornerRadius(3f * k))
    drawRoundRect(dark, o(-54f, 16f), Size(108f * k, 11f * k), CornerRadius(3f * k), style = Stroke(width = 1f * k))
    listOf(-58f to -24f, -18f to -38f, 18f to -38f, 58f to -24f, 0f to -12f).forEach { (x, y) ->
        drawCircle(gold, 4.3f * k, o(x, y))
        drawCircle(dark, 4.3f * k, o(x, y), style = Stroke(width = 0.8f * k))
    }
    fun jewel(x: Float, y: Float, r: Float, light: Color, deep: Color) {
        drawCircle(Brush.radialGradient(listOf(light, deep), center = o(x, y), radius = r * k), r * k, o(x, y))
    }
    jewel(0f, 21.5f, 3.6f, Color(0xFFFF8A8A), Color(0xFF9E0E0E))
    jewel(-24f, 21.5f, 3f, Color(0xFF9EC1FF), Color(0xFF1035A8))
    jewel(24f, 21.5f, 3f, Color(0xFF9EC1FF), Color(0xFF1035A8))
    val ruby = Path().apply { moveTo(o(-6f, 4f).x, o(-6f, 4f).y); lineTo(o(0f, -6f).x, o(0f, -6f).y); lineTo(o(6f, 4f).x, o(6f, 4f).y); close() }
    drawPath(ruby, Brush.radialGradient(listOf(Color(0xFFFF8A8A), Color(0xFF9E0E0E)), center = o(0f, 0f), radius = 6f * k), alpha = 0.9f)
}

@Composable
private fun SplashCrownIcon(modifier: Modifier, glow: Boolean = false) {
    Canvas(modifier = modifier) {
        val k = size.width / 120f
        val c = Offset(size.width / 2f, size.height / 2f + 6f * k)
        fun o(x: Float, y: Float) = Offset(c.x + x * k, c.y + y * k)
        if (glow) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFC107).copy(alpha = 0.45f), Color.Transparent), center = c, radius = 70f * k),
                70f * k, c
            )
        }
        val body = Path().apply {
            val pts = listOf(-52f to 18f, -58f to -22f, -34f to -2f, -18f to -36f, 0f to -8f, 18f to -36f, 34f to -2f, 58f to -22f, 52f to 18f)
            pts.forEachIndexed { i, (x, y) -> val p = o(x, y); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            close()
        }
        drawPath(body, Color(0xFFFFC94A))
        drawPath(body, if (glow) Color(0xFFC98A10) else Color(0xFF7A5208), style = Stroke(width = 4f * k, join = StrokeJoin.Round))
        drawRoundRect(Color(0xFFE8A820), o(-54f, 14f), Size(108f * k, 14f * k), CornerRadius(4f * k))
        if (!glow) drawCircle(Color(0xFFD61F1F), 6f * k, o(0f, 21f))
    }
}

// ---------------------------------------------------------------- ribbon

@Composable
private fun AdSplashRibbon(textSize: Float) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10))))
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xFFF2554E), Color(0xFFC41E1E), Color(0xFF7E0A0A))))
                .drawBehind {
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f)),
                            startY = 2.dp.toPx(), endY = 12.dp.toPx()
                        ),
                        topLeft = Offset(12.dp.toPx(), 2.dp.toPx()),
                        size = Size(size.width - 24.dp.toPx(), 10.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx())
                    )
                }
                .padding(horizontal = 20.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("★", style = TextStyle(color = Color(0xFFFFD45A), fontSize = textSize.sp))
            Text(
                text = "LUDO & ESPORTS ARENA",
                style = TextStyle(
                    color = Color(0xFFFFF6E0),
                    fontSize = (textSize + 1f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(color = Color(0x99500000), offset = Offset(0f, 4f), blurRadius = 0f)
                ),
                maxLines = 1,
                softWrap = false
            )
            Text("★", style = TextStyle(color = Color(0xFFFFD45A), fontSize = textSize.sp))
        }
    }
}

// ---------------------------------------------------------------- 3D board with the real board, pawns and dice

private class SpPiece(val y: Float, val content: @Composable () -> Unit)

@Composable
private fun AdSplashBoard(boardSize: Dp, dicePhase: Float, pulse: Float) {
    val density = LocalDensity.current
    val bd = boardSize
    val f = bd * 0.04f                       // frame width
    val outer = bd + f * 2f                  // board + frame, flat
    val diag = outer * 1.41421f              // after the 45 degree turn
    val squash = 0.56f
    val thick = 12.dp * (bd / 240.dp)        // side thickness
    val boxH = diag * squash + thick + 6.dp

    val bdPx = with(density) { bd.toPx() }
    val diagPx = with(density) { diag.toPx() }
    val cxPx = diagPx / 2f
    val cyPx = diagPx * squash / 2f
    fun iso(fx: Float, fy: Float): Offset {
        val dx = fx - bdPx / 2f; val dy = fy - bdPx / 2f
        return Offset(cxPx + (dx - dy) * 0.70710678f, cyPx + (dx + dy) * 0.70710678f * squash)
    }
    val u = bdPx / 15f
    val pw = bd * 0.081f
    val ph = pw * 1.35f
    val pwPx = with(density) { pw.toPx() }
    val phPx = with(density) { ph.toPx() }

    Box(modifier = Modifier.width(diag).height(boxH)) {
        // shadow + side thickness, drawn flat on screen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val th = thick.toPx()
            val l = Offset(0f, cyPx); val b = Offset(cxPx, cyPx * 2f); val r = Offset(diagPx, cyPx)
            drawOval(
                Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), center = Offset(cxPx, cyPx + 30f * density.density), radius = diagPx * 0.52f),
                topLeft = Offset(cxPx - diagPx * 0.52f, cyPx + 30f * density.density - diagPx * 0.19f),
                size = Size(diagPx * 1.04f, diagPx * 0.38f)
            )
            drawPath(
                Path().apply { moveTo(l.x, l.y); lineTo(b.x, b.y); lineTo(b.x, b.y + th); lineTo(l.x, l.y + th); close() },
                Brush.verticalGradient(listOf(Color(0xFFC98A10), Color(0xFF5C3A04)), startY = l.y, endY = b.y + th)
            )
            drawPath(
                Path().apply { moveTo(b.x, b.y); lineTo(r.x, r.y); lineTo(r.x, r.y + th); lineTo(b.x, b.y + th); close() },
                Color(0xFF7A5208)
            )
        }

        // the real game board, turned 45 degrees and laid flat
        Box(
            modifier = Modifier
                .offset(y = -(thick + 6.dp) / 2f)
                .requiredSize(diag)
                .graphicsLayer { scaleY = squash },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(outer)
                    .graphicsLayer { rotationZ = 45f }
                    .drawBehind {
                        val fp = f.toPx()
                        drawRoundRect(
                            Brush.verticalGradient(SpGoldStops),
                            cornerRadius = CornerRadius(fp * 0.83f)
                        )
                        drawRoundRect(
                            Color(0xFF0F1A52),
                            topLeft = Offset(fp * 0.58f, fp * 0.58f),
                            size = Size(size.width - fp * 1.16f, size.height - fp * 1.16f),
                            cornerRadius = CornerRadius(fp * 0.42f)
                        )
                    }
                    .padding(f)
            ) {
                AdLudoBoard(boardRotation = 45f, modifier = Modifier.fillMaxSize())
            }
        }

        // gentle glow over the centre logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFFFFF6D0).copy(alpha = 0.5f * pulse), Color(0xFFFFD27A).copy(alpha = 0.22f * pulse), Color.Transparent),
                    center = Offset(cxPx, cyPx), radius = 70f * density.density
                ),
                topLeft = Offset(cxPx - 70f * density.density, cyPx - 40f * density.density),
                size = Size(140f * density.density, 80f * density.density)
            )
        }

        // pawns (real AdLudoPawnToken) and the rolling dice, drawn back to front
        val pieces = ArrayList<SpPiece>()
        fun addPawn(fx: Float, fy: Float, color: Color, scale: Float) {
            val p = iso(fx, fy)
            val w = pwPx * scale; val h = phPx * scale
            pieces.add(SpPiece(p.y) {
                AdLudoPawnToken(
                    pawnColor = color,
                    isEligible = false,
                    modifier = Modifier
                        .offset { IntOffset((p.x - w / 2f).roundToInt(), (p.y - h).roundToInt()) }
                        .size(pw * scale, ph * scale)
                )
            })
        }
        val homes = listOf(
            Triple(0f, 0f, Color(0xFF2EAD4B)),        // top-left  green
            Triple(9f * u, 0f, Color(0xFFFBC02D)),    // top-right yellow
            Triple(0f, 9f * u, Color(0xFFE53935)),    // bottom-left red
            Triple(9f * u, 9f * u, Color(0xFF1E88E5)) // bottom-right blue
        )
        homes.forEach { (hx, hy, col) ->
            listOf(2f to 2f, 4f to 2f, 2f to 4f, 4f to 4f).forEach { (dx, dy) -> addPawn(hx + dx * u, hy + dy * u, col, 1f) }
        }
        listOf(
            Triple(3, 6, Color(0xFF2EAD4B)), Triple(8, 3, Color(0xFFFBC02D)),
            Triple(11, 8, Color(0xFF1E88E5)), Triple(6, 11, Color(0xFFE53935))
        ).forEach { (c, r, col) -> addPawn((c + 0.5f) * u, (r + 0.5f) * u, col, 0.82f) }

        // dice: rolls with a bounce for the first half of each cycle, then rests showing 6
        val rolling = dicePhase < 0.5f
        val q = if (rolling) dicePhase / 0.5f else 0f
        val kb = bd.value / 240f
        val bounce = if (!rolling) 0f else when {
            q < 0.24f -> -18f * (q / 0.24f)
            q < 0.5f -> -18f + 12f * ((q - 0.24f) / 0.26f)
            q < 0.74f -> -6f - 6f * ((q - 0.5f) / 0.24f)
            else -> -12f + 12f * ((q - 0.74f) / 0.26f)
        } * kb
        val ds = bd * 0.25f
        val dsPx = with(density) { ds.toPx() }
        val diceCx = cxPx
        val diceCy = cyPx - 34f * kb * density.density + bounce * density.density
        pieces.add(SpPiece(cyPx) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lift = (-bounce / (18f * kb)).coerceIn(0f, 1f)
                val s = 1f - 0.3f * lift
                drawOval(
                    Color.Black.copy(alpha = 0.45f - 0.2f * lift),
                    topLeft = Offset(cxPx - 24f * kb * s * density.density, cyPx + 2f * density.density - 9f * kb * s * density.density),
                    size = Size(48f * kb * s * density.density, 18f * kb * s * density.density)
                )
            }
            AdDice3D(
                value = 6,
                spinX = 360f * q,
                spinY = 720f * q,
                color = Color(0xFFE53935),
                modifier = Modifier
                    .offset { IntOffset((diceCx - dsPx / 2f).roundToInt(), (diceCy - dsPx / 2f).roundToInt()) }
                    .size(ds)
            )
        })

        pieces.sortedBy { it.y }.forEach { it.content() }
    }
}
