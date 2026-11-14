package com.example.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.minigames.mgScallopMedal
import kotlin.math.cos
import kotlin.math.sin

// Home page part 6a artwork: "সকল ম্যাচ" banner card, the game filter buttons and the live matches heading.
// VISUAL ONLY: the caller keeps every text, click action and test tag.

internal val TaOrangeGold = Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC83A), Color(0xFFFF8A1A)))
internal val TaOrangeFill = Brush.verticalGradient(listOf(Color(0xFFFFD27A), Color(0xFFFFA62B), Color(0xFFE07B0A)))
private val TaGoldEdge = Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)))

@Composable
internal fun TaImage(@DrawableRes res: Int, sizeDp: Int, modifier: Modifier = Modifier) {
    Image(painter = painterResource(id = res), contentDescription = null, modifier = modifier.size(sizeDp.dp))
}

/** Gold corner line with a leaf (top-left shape). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.taCorner(x: Float, y: Float, sx: Float, sy: Float, s: Float) {
    val gold = Color(0xFFE0A82A)
    fun o(px: Float, py: Float) = Offset(x + px * s * sx, y + py * s * sy)
    val st = Stroke(1.2f * s, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(o(3f, 24f).x, o(3f, 24f).y); lineTo(o(3f, 3f).x, o(3f, 3f).y); lineTo(o(24f, 3f).x, o(24f, 3f).y) }, gold, style = st)
    drawPath(Path().apply { moveTo(o(7f, 14f).x, o(7f, 14f).y); quadraticBezierTo(o(7f, 7f).x, o(7f, 7f).y, o(14f, 7f).x, o(14f, 7f).y) }, gold, style = st)
    drawCircle(Color(0xFFFFD45A), 1.5f * s, o(11f, 11f))
}

/** Big banner card for "সকল ম্যাচ": trophy medal on turning gold rays, floating game pictures, glowing arrow button. */
@Composable
fun AdAllMatchesCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "allMatches")
    val rays by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "amRays")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "amBob")
    val glow by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "amGlow")
    val sweep by anim.animateFloat(-0.3f, 2.2f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing)), label = "amSweep")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .adPressScale(press, 0.97f)
            .fillMaxWidth()
            .height(122.dp)
            .shadow(14.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFFFFC850))
            .clip(outer)
            .background(TaGoldEdge)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag("home_all_matches_quick_card")
            .padding(1.6.dp)
            .clip(RoundedCornerShape(18.5.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2B3AA8), Color(0xFF1E2780), Color(0xFF151B5E))))
            .drawBehind {
                val w = size.width; val h = size.height
                drawOval(Brush.radialGradient(listOf(Color(0x59FFC85A), Color.Transparent), center = Offset(w * 0.18f, h * 0.55f), radius = w * 0.3f), topLeft = Offset(-w * 0.12f, -h * 0.2f), size = Size(w * 0.6f, h * 1.4f))
                drawOval(Brush.radialGradient(listOf(Color(0x668C6EFF), Color.Transparent), center = Offset(w * 0.82f, h * 0.55f), radius = w * 0.3f), topLeft = Offset(w * 0.52f, -h * 0.2f), size = Size(w * 0.6f, h * 1.4f))
                // gold rays turning behind the trophy
                val c = Offset(52.dp.toPx(), h * 0.5f)
                val len = 95.dp.toPx()
                val rayBrush = Brush.radialGradient(listOf(Color(0x4DFFDC82), Color(0x00FFDC82)), center = c, radius = len)
                var a = rays
                repeat(22) {
                    val r1 = Math.toRadians(a.toDouble()); val r2 = Math.toRadians((a + 6f).toDouble())
                    drawPath(Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + len * cos(r1).toFloat(), c.y + len * sin(r1).toFloat())
                        lineTo(c.x + len * cos(r2).toFloat(), c.y + len * sin(r2).toFloat())
                        close()
                    }, rayBrush)
                    a += 16.4f
                }
                // inner gold line and corners
                drawRoundRect(Color(0x8CE0A82A), topLeft = Offset(7.dp.toPx(), 7.dp.toPx()), size = Size(w - 14.dp.toPx(), h - 14.dp.toPx()), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(1.dp.toPx()))
                val s = 1.dp.toPx(); val e = 4.dp.toPx()
                taCorner(e, e, 1f, 1f, s); taCorner(w - e, e, -1f, 1f, s); taCorner(e, h - e, 1f, -1f, s); taCorner(w - e, h - e, -1f, -1f, s)
            }
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x3DFFFFFF), Color(0x00FFFFFF)), startX = x - 17.dp.toPx(), endX = x + 17.dp.toPx()),
                        topLeft = Offset(x - 17.dp.toPx(), -size.height), size = Size(34.dp.toPx(), size.height * 3f)
                    )
                }
            }
    ) {
        // trophy medal
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .graphicsLayer { translationY = -4.dp.toPx() * bob }
                .size(72.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(Color(0xBFFFC850), Color.Transparent), radius = size.minDimension * 0.75f), radius = size.minDimension * 0.75f) },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                mgScallopMedal(Brush.radialGradient(listOf(Color(0xFF2C3CB8), Color(0xFF0B1040)), center = Offset(center.x, center.y * 0.7f)))
            }
            Canvas(modifier = Modifier.size(40.dp)) {
                val k = size.width / 24f
                fun o(x: Float, y: Float) = Offset(x * k, y * k)
                val gold = Brush.verticalGradient(listOf(Color(0xFFFFF8D8), Color(0xFFFFC94A), Color(0xFFB9780C)))
                val cup = Path().apply {
                    moveTo(o(7f, 3f).x, o(7f, 3f).y); lineTo(o(17f, 3f).x, o(17f, 3f).y); lineTo(o(17f, 9f).x, o(17f, 9f).y)
                    cubicTo(o(17f, 12f).x, o(17f, 12f).y, o(15f, 15f).x, o(15f, 15f).y, o(13f, 15.5f).x, o(13f, 15.5f).y)
                    lineTo(o(13f, 18f).x, o(13f, 18f).y); lineTo(o(16f, 18f).x, o(16f, 18f).y); lineTo(o(16f, 21f).x, o(16f, 21f).y)
                    lineTo(o(8f, 21f).x, o(8f, 21f).y); lineTo(o(8f, 18f).x, o(8f, 18f).y); lineTo(o(11f, 18f).x, o(11f, 18f).y)
                    lineTo(o(11f, 15.5f).x, o(11f, 15.5f).y)
                    cubicTo(o(9f, 15f).x, o(9f, 15f).y, o(7f, 12f).x, o(7f, 12f).y, o(7f, 9f).x, o(7f, 9f).y)
                    close()
                }
                drawPath(cup, gold)
                drawPath(cup, Color(0xFF7A4E00), style = Stroke(0.4f * k))
                drawArc(gold, 90f, 180f, false, topLeft = o(3.5f, 5f), size = Size(5f * k, 6f * k), style = Stroke(1.6f * k))
                drawArc(gold, 270f, 180f, false, topLeft = o(15.5f, 5f), size = Size(5f * k, 6f * k), style = Stroke(1.6f * k))
            }
            Text("✦", style = TextStyle(fontSize = 12.sp, color = Color(0xFFFFF8D8).copy(alpha = glow)), modifier = Modifier.align(Alignment.TopStart))
        }

        // title and subtitle
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 98.dp, end = 120.dp)
        ) {
            Text(title, style = TextStyle(brush = TaOrangeGold, fontSize = 22.sp, fontWeight = FontWeight.Black, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 4f), 0f)), maxLines = 1)
            Text(
                subtitle,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 3f)),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }

        // floating game pictures
        TaImage(R.drawable.mg_carrom, 58, Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 38.dp).graphicsLayer { rotationZ = -14f; translationY = -4.dp.toPx() * bob })
        TaImage(R.drawable.mg_dice, 36, Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 66.dp).graphicsLayer { rotationZ = 12f; translationY = -4.dp.toPx() * (1f - bob) })
        TaImage(R.drawable.mg_coin, 26, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 16.dp).graphicsLayer { translationY = -3.dp.toPx() * bob })

        // glowing arrow button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(30.dp)
                .drawBehind {
                    drawCircle(Color(0xFFFFAA3C).copy(alpha = 0.6f * glow), radius = size.minDimension * 0.85f)
                    drawCircle(Color(0xFF8A4A06), radius = size.minDimension / 2f, center = Offset(center.x, center.y + 3.dp.toPx()))
                }
                .clip(CircleShape)
                .background(TaOrangeFill)
                .border(1.5.dp, Color(0xFFFFE7A0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(13.dp)) {
                val k = size.width / 24f
                drawPath(Path().apply { moveTo(9f * k, 5f * k); lineTo(16f * k, 12f * k); lineTo(9f * k, 19f * k) }, Color(0xFF3A1A00), style = Stroke(3.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
