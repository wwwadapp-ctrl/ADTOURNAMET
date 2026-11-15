package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.adPressScale

// Bottom menu bar artwork: royal glass bar with a gold top edge, gold button for the current page,
// and a raised round gold button for the middle item. VISUAL ONLY: the caller keeps routes, clicks and test tags.

private val BnGoldRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
private val BnGoldFill = Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC83A), Color(0xFFE08A10)))

/** The bar itself. Put the items inside; it adds the navigation bar padding. */
@Composable
fun AdBottomBarFrame(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val anim = rememberInfiniteTransition(label = "bnBar")
    val sweep by anim.animateFloat(-0.2f, 2.4f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing)), label = "bnSweep")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF25339A), Color(0xFF151D6A), Color(0xFF0B1040))))
            .drawBehind {
                val w = size.width; val h = size.height
                // soft gold light in the middle, glass light on top
                drawOval(Brush.radialGradient(listOf(Color(0x38FFC85A), Color.Transparent), center = Offset(w / 2f, 0f), radius = w * 0.3f), topLeft = Offset(w * 0.2f, -h * 0.9f), size = Size(w * 0.6f, h * 1.8f))
                drawRect(Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color.Transparent), endY = h * 0.4f), size = Size(w, h * 0.4f))
                // moving shine
                val x = w * sweep
                drawScopeRotate(22f, pivot = Offset(x, h / 2f)) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x24FFFFFF), Color(0x00FFFFFF)), startX = x - 15.dp.toPx(), endX = x + 15.dp.toPx()), topLeft = Offset(x - 15.dp.toPx(), -h), size = Size(30.dp.toPx(), h * 3f))
                }
                // gold top edge with glow
                drawRect(Brush.verticalGradient(listOf(Color(0x55FFC850), Color.Transparent), endY = 8.dp.toPx()), size = Size(w, 8.dp.toPx()))
                drawRect(Brush.horizontalGradient(listOf(Color(0x33FFE7A0), Color(0xFFFFE7A0), Color(0xFFE0A82A), Color(0xFFFFE7A0), Color(0x33FFE7A0))), size = Size(w, 1.6.dp.toPx()))
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
            content = content
        )
    }
}

/** One menu item. `raised` = the big round middle button. */
@Composable
fun AdBottomNavItem(icon: ImageVector, label: String, selected: Boolean, raised: Boolean, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "bnItem")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bnBob")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "bnSpin")
    val glow by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "bnGlow")
    val on by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "bnOn")
    val press = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .adPressScale(press, 0.88f)
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            .testTag(tag)
    ) {
        if (raised) {
            // big round gold button floating above the bar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(32.dp)
                    .requiredSize(60.dp)
                    .offset(y = (-14).dp)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC850).copy(alpha = 0.55f * glow), Color.Transparent), radius = r * 1.45f), r * 1.45f)
                        drawScopeRotate(spin) {
                            drawArc(Color(0xFFFFF3C4), 0f, 80f, false, topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()), size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(Color(0xFFFFD45A), 180f, 60f, false, topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()), size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    .shadow(8.dp, CircleShape, spotColor = Color.Black)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(BnGoldRing))
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Brush.radialGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC83A), Color(0xFFE08A10)))
                        else Brush.radialGradient(listOf(Color(0xFF3A4AC8), Color(0xFF1E2780), Color(0xFF10165A)))
                    )
                    .drawWithContent {
                        drawContent()
                        drawScopeRotate(-25f, pivot = Offset(size.width * 0.36f, size.height * 0.2f)) {
                            drawOval(Color(0x59FFFFFF), topLeft = Offset(size.width * 0.16f, size.height * 0.1f), size = Size(size.width * 0.4f, size.height * 0.18f))
                        }
                    }
            ) {
                Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF3A1A00) else Color(0xFFFFE7A0), modifier = Modifier.size(28.dp))
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 46.dp, height = 32.dp)
                    .graphicsLayer { translationY = -3.dp.toPx() * bob * on }
                    .drawBehind {
                        if (on > 0.01f) {
                            // light falling from the top edge
                            drawOval(Brush.radialGradient(listOf(Color(0xFFFFD45A).copy(alpha = 0.45f * on), Color.Transparent), center = Offset(size.width / 2f, -8.dp.toPx()), radius = size.width * 0.75f), topLeft = Offset(-size.width * 0.25f, -8.dp.toPx() - size.height), size = Size(size.width * 1.5f, size.height * 2.2f))
                            val rr = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                            drawRoundRect(Color(0xFF8A4A06).copy(alpha = on), topLeft = Offset(0f, 3.dp.toPx()), size = size, cornerRadius = rr)
                            drawRoundRect(BnGoldFill, size = size, cornerRadius = rr, alpha = on)
                            drawRoundRect(Color(0xB3FFFFFF), topLeft = Offset(4.dp.toPx(), 1.dp.toPx()), size = Size(size.width - 8.dp.toPx(), 1.2.dp.toPx()), cornerRadius = rr, alpha = on)
                        }
                    }
            ) {
                Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF3A1A00) else Color(0xD1E6EBFF), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontSize = if (selected || raised) 11.5.sp else 11.sp,
                fontWeight = if (selected || raised) FontWeight.Black else FontWeight.Bold,
                color = if (selected) Color(0xFFFFD45A) else if (raised) Color(0xFFFFE7A0) else Color(0xBFE6EBFF),
                shadow = if (selected) Shadow(Color(0xB3FFC850), blurRadius = 8f) else null
            )
        )
    }
}
