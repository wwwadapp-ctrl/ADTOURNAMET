package com.example.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.splash.AdSplashEmblem
import com.example.ui.splash.AdTournamentTitle
import kotlin.math.cos
import kotlin.random.Random

// AD premium sign-in / sign-up look. VISUAL ONLY: these pieces never validate, log in or navigate;
// the screens pass their existing state and callbacks straight through.

// ---------------------------------------------------------------- background (same sky as the splash)

private class AuDot(val x: Float, val y: Float, val r: Float, val a: Float)

@Composable
fun AdAuthBackground(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "adAuthSky")
    val twinkle by anim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "twinkle"
    )
    val rnd = remember { Random(7) }
    val bokeh = remember { List(16) { AuDot(rnd.nextFloat(), rnd.nextFloat(), 10f + rnd.nextFloat() * 22f, 0.25f + rnd.nextFloat() * 0.35f) } }
    val stars = remember { List(70) { AuDot(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 1.1f, 0.3f + rnd.nextFloat() * 0.6f) } }
    val confetti = remember {
        List(12) {
            val left = rnd.nextBoolean()
            AuDot(if (left) rnd.nextFloat() * 0.15f else 0.85f + rnd.nextFloat() * 0.14f, rnd.nextFloat(), rnd.nextFloat() * 180f, 0.55f)
        }
    }
    val sparkles = remember {
        listOf(
            floatArrayOf(.067f, .026f, 6f), floatArrayOf(.205f, .073f, 8f), floatArrayOf(.903f, .040f, 5f),
            floatArrayOf(.846f, .133f, 7f), floatArrayOf(.046f, .166f, 4f), floatArrayOf(.954f, .244f, 4f),
            floatArrayOf(.077f, .818f, 5f), floatArrayOf(.928f, .782f, 6f), floatArrayOf(.231f, .948f, 4f),
            floatArrayOf(.769f, .936f, 5f)
        )
    }
    Canvas(modifier = modifier) {
        val d = density
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF2A3AB6), 0.18f to Color(0xFF1E2D93), 0.45f to Color(0xFF18227B),
                0.72f to Color(0xFF131C69), 1f to Color(0xFF121A61)
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
            auSparkle(Offset(s[0] * size.width, s[1] * size.height), s[2] * d * sc, Color(0xFFFFF3C4).copy(alpha = 0.25f + 0.75f * t))
        }
    }
}

private fun DrawScope.auSparkle(c: Offset, s: Float, color: Color) {
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

// ---------------------------------------------------------------- logo + title

@Composable
fun AdAuthLogo(modifier: Modifier = Modifier) {
    // the logo floats gently and soft gold rays turn behind it
    val logoAnim = rememberInfiniteTransition(label = "auLogo")
    val logoFloat by logoAnim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "auLogoFloat")
    val logoRays by logoAnim.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "auLogoRays")
    Box(modifier = modifier.size(width = 132.dp, height = 140.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val len = size.width * 1.05f
            val rayBrush = Brush.radialGradient(listOf(Color(0xFFFFD54F).copy(alpha = 0.32f), Color.Transparent), center = center, radius = len)
            for (i in 0 until 24) {
                val a1 = Math.toRadians((logoRays + i * 15f).toDouble()); val a2 = Math.toRadians((logoRays + i * 15f + 5f).toDouble())
                drawPath(Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + len * cos(a1).toFloat(), center.y + len * kotlin.math.sin(a1).toFloat())
                    lineTo(center.x + len * cos(a2).toFloat(), center.y + len * kotlin.math.sin(a2).toFloat())
                    close()
                }, rayBrush)
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFC107).copy(alpha = 0.35f), Color.Transparent),
                    center = center, radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f, center = center
            )
        }
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ad_logo_emblem),
            contentDescription = "AD Tournament Logo",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.size(132.dp).graphicsLayer { translationY = -6.dp.toPx() * logoFloat }
        )
    }
}

@Composable
fun AdAuthTitle(tagline: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AdTournamentTitle(fontSize = 36f)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tagline,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE8D7A8))
        )
    }
}

// ---------------------------------------------------------------- gold-framed card with carved corners

@Composable
fun AdAuthCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val outer = RoundedCornerShape(22.dp)
    val inner = RoundedCornerShape(20.dp)
    // glass shine: a soft light streak passes over the card every few seconds
    val shine by rememberInfiniteTransition(label = "auShine").animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "auShineX")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(3.dp, Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10), Color(0xFFFFE7A0))), outer)
            .padding(3.dp)
            .clip(inner)
            .background(Brush.verticalGradient(listOf(Color(0xFF26349F), Color(0xFF1E2B8E), Color(0xFF18227B))))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF3C50AA).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.3f, 0f), radius = size.width * 0.9f
                    )
                )
            }
            .border(3.dp, Color(0xFF8A6414), inner)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(8.dp)
                .border(1.5.dp, Color(0x8CF0BE50), RoundedCornerShape(14.dp))
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val x = size.width * shine
            rotate(22f, pivot = Offset(x, size.height / 2f)) {
                drawRect(
                    Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x24FFFFFF), Color(0x00FFFFFF)), startX = x - 40.dp.toPx(), endX = x + 40.dp.toPx()),
                    topLeft = Offset(x - 40.dp.toPx(), -size.height), size = Size(80.dp.toPx(), size.height * 3f)
                )
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val y1 = 12.dp.toPx(); val y2 = size.height - 12.dp.toPx(); val x0 = 54.dp.toPx(); val x1 = size.width - 54.dp.toPx()
            val line = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFE8B23A), Color.Transparent), startX = x0, endX = x1)
            drawLine(line, Offset(x0, y1), Offset(x1, y1), strokeWidth = 1.5.dp.toPx())
            drawLine(line, Offset(x0, y2), Offset(x1, y2), strokeWidth = 1.5.dp.toPx())
        }
        AdAuthCorner(Modifier.align(Alignment.TopStart).padding(6.dp), flipX = false, flipY = false)
        AdAuthCorner(Modifier.align(Alignment.TopEnd).padding(6.dp), flipX = true, flipY = false)
        AdAuthCorner(Modifier.align(Alignment.BottomStart).padding(6.dp), flipX = false, flipY = true)
        AdAuthCorner(Modifier.align(Alignment.BottomEnd).padding(6.dp), flipX = true, flipY = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun AdAuthCorner(modifier: Modifier, flipX: Boolean, flipY: Boolean) {
    Canvas(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = if (flipX) -1f else 1f
                scaleY = if (flipY) -1f else 1f
            }
    ) {
        val s = size.width / 44f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val path = Path().apply {
            moveTo(p(40f, 4f).x, p(40f, 4f).y); cubicTo(20f * s, 4f * s, 4f * s, 20f * s, 4f * s, 40f * s)
            moveTo(28f * s, 4f * s); cubicTo(28f * s, 18f * s, 14f * s, 18f * s, 15f * s, 13f * s)
            cubicTo(16f * s, 9f * s, 21f * s, 10f * s, 20f * s, 14f * s)
            moveTo(4f * s, 28f * s); cubicTo(18f * s, 28f * s, 18f * s, 14f * s, 13f * s, 15f * s)
            cubicTo(9f * s, 16f * s, 10f * s, 21f * s, 14f * s, 20f * s)
            moveTo(40f * s, 9f * s); cubicTo(26f * s, 10f * s, 18f * s, 15f * s, 14f * s, 22f * s)
            moveTo(9f * s, 40f * s); cubicTo(10f * s, 26f * s, 15f * s, 18f * s, 22f * s, 14f * s)
        }
        drawPath(path, Color(0xFFE8B23A), style = Stroke(width = 1.5f * s, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFFE08A), 2.4f * s, p(15f, 15f))
        drawCircle(Color(0xFFFFE08A), 1.6f * s, p(40f, 4f))
        drawCircle(Color(0xFFFFE08A), 1.6f * s, p(4f, 40f))
    }
}

@Composable
fun AdAuthCardHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AdAuth3DTitle(title, fontSize = 30.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFE9A8),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                letterSpacing = 0.2.sp,
                shadow = Shadow(color = Color(0xFF0A1040), offset = Offset(0f, 2f), blurRadius = 0f)
            )
        )
    }
}

// ---------------------------------------------------------------- crisp 3D gold title with flowing gold (no blur anywhere)

@Composable
fun AdAuth3DTitle(text: String, fontSize: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified) {
    val flow by rememberInfiniteTransition(label = "auTitle").animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "auTitleFlow")
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { fontSize.toPx() }
    val base = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = lineHeight)
    val outline = Stroke(width = px * 0.09f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    val depth = listOf(Color(0xFFB07A00), Color(0xFF9A6400), Color(0xFF7A4E00), Color(0xFF5A3700), Color(0xFF3A2200))
    val band = px * 6f
    Box(contentAlignment = Alignment.Center) {
        // 1. solid 3D depth: 5 sharp layers, deepest first
        for (i in 5 downTo 1) {
            val m = Modifier.offset { androidx.compose.ui.unit.IntOffset(0, Math.round(px * 0.032f * i)) }
            Text(text, style = base.copy(color = depth[i - 1], drawStyle = outline), modifier = m)
            Text(text, style = base.copy(color = depth[i - 1]), modifier = m)
        }
        // 2. thin dark brown outline
        Text(text, style = base.copy(color = Color(0xFF4A2200), drawStyle = outline))
        // 3. flowing gold fill
        Text(text, style = base.copy(brush = Brush.linearGradient(
            listOf(Color(0xFFE87A00), Color(0xFFFFD02A), Color(0xFFFFFDE8), Color(0xFFFFD02A), Color(0xFFE87A00)),
            start = Offset(-flow * band, 0f), end = Offset(-flow * band + band, 0f), tileMode = androidx.compose.ui.graphics.TileMode.Repeated
        )))
        // 4. shine on the upper half of the letters
        Text(text, style = base.copy(brush = Brush.verticalGradient(0f to Color(0xB3FFFFFF), 0.45f to Color(0x00FFFFFF))))
    }
}

// ---------------------------------------------------------------- input field

@Composable
fun AdAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                if (focused) {
                    val g = 3.dp.toPx()
                    drawRoundRect(
                        Color(0x4DFFC107),
                        topLeft = Offset(-g, -g),
                        size = Size(size.width + 2 * g, size.height + 2 * g),
                        cornerRadius = CornerRadius(17.dp.toPx())
                    )
                }
            }
            .testTag(testTag)
            .semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(Color(0xFFFFC107)),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        decorationBox = { innerField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0E1656), Color(0xFF16206F), Color(0xFF1C2884))))
                    .drawBehind { drawRect(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent), startY = 0f, endY = size.height * 0.45f)) }
                    .border(3.dp, if (focused) Color(0xFFFFC107) else Color(0xFFD9AE52), shape)
                    .padding(start = 14.dp, end = if (trailing != null) 4.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(leadingIcon, contentDescription = null, tint = Color(0xFFFFC94A), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(color = Color(0xFFAEB6D8), fontSize = 17.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                    }
                    innerField()
                }
                if (trailing != null) trailing()
            }
        }
    )
}

// ---------------------------------------------------------------- error box

@Composable
fun AdAuthErrorBox(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
            .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = text,
            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFCF6B), lineHeight = 19.sp)
        )
    }
}

// ---------------------------------------------------------------- glossy gold button

@Composable
fun AdAuthGoldButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFF9A6400))))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE29A), Color(0xFFF6B73C), Color(0xFFD98A10))))
                .clickable(enabled = !isLoading, role = Role.Button) { onClick() }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 10.dp, end = 10.dp, top = 3.dp)
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0f))))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF3A2300),
                        trackColor = Color(0x403A2300),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (isLoading) loadingText else text,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2300)),
                    maxLines = 1
                )
            }
        }
    }
}
