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

// Home page part 6a artwork: game filter buttons and the live matches heading. VISUAL ONLY.

/** One game filter button: raised orange when selected, blue glass with a gold edge otherwise. iconKind: 0 all, 1 ludo, 2 carrom. */
@Composable
fun AdGameFilterChip(
    label: String,
    selected: Boolean,
    iconKind: Int,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "filterChip")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fcBob")
    val sweep by anim.animateFloat(-0.4f, 2.4f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing)), label = "fcSweep")
    val twinkle by anim.animateFloat(0.2f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "fcTw")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(16.dp)
    Box(modifier = modifier.adPressScale(press).height(57.dp)) {
        // raised edge
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).offset(y = 5.dp).clip(outer)
                .background(if (selected) Color(0xFF8A4A06) else Color(0xFF0B1040))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(if (selected) 10.dp else 6.dp, outer, ambientColor = Color.Black, spotColor = if (selected) Color(0xFFFFAA3C) else Color.Black)
                .clip(outer)
                .background(
                    if (selected) Brush.linearGradient(listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A), Color(0xFFC98A10)))
                    else Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF6B4A10), Color(0xFFE0A82A)))
                )
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(1.5.dp)
                .clip(RoundedCornerShape(14.5.dp))
                .background(
                    if (selected) TaOrangeFill
                    else Brush.verticalGradient(listOf(Color(0xFF2E3CB0), Color(0xFF18217A)))
                )
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = if (selected) 0.55f else 0.2f), Color.Transparent), startY = 0f, endY = size.height * 0.45f),
                        size = Size(size.width, size.height * 0.45f)
                    )
                    if (!selected) {
                        drawRoundRect(Color(0x73E0A82A), topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), cornerRadius = CornerRadius(11.5.dp.toPx()), style = Stroke(1.dp.toPx()))
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (selected) {
                        val x = size.width * sweep
                        drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                            drawRect(
                                Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x8CFFFFFF), Color(0x00FFFFFF)), startX = x - 9.dp.toPx(), endX = x + 9.dp.toPx()),
                                topLeft = Offset(x - 9.dp.toPx(), -size.height), size = Size(18.dp.toPx(), size.height * 3f)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (iconKind) {
                    1 -> TaImage(R.drawable.mg_dice, 30, Modifier.graphicsLayer { translationY = -3.dp.toPx() * bob })
                    2 -> TaImage(R.drawable.mg_carrom, 28, Modifier.graphicsLayer { rotationZ = -10f; translationY = -3.dp.toPx() * (1f - bob) })
                    else -> Canvas(modifier = Modifier.size(20.dp)) {
                        val k = size.width / 24f
                        val col = if (selected) Color(0xFF3A1A00) else Color(0xFFFFD45A)
                        listOf(3f to 3f, 13f to 3f, 3f to 13f, 13f to 13f).forEach { (x, y) ->
                            drawRoundRect(col, topLeft = Offset(x * k, y * k), size = Size(8f * k, 8f * k), cornerRadius = CornerRadius(2f * k))
                        }
                    }
                }
                Text(
                    label,
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.ExtraBold,
                        color = if (selected) Color(0xFF3A1A00) else Color(0xFFFFF3C4),
                        shadow = if (selected) null else Shadow(Color(0x99000000), Offset(0f, 2f), 3f)
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Text("✦", style = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = twinkle)), modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 6.dp))
            }
        }
    }
}

/** "চলমান ও উন্মুক্ত ম্যাচ" with a pulsing red live dot, and the "সব দেখুন ›" capsule. */
@Composable
fun AdLiveMatchesHeader(
    title: String,
    viewAllText: String,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "liveHeader")
    val ping by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "lhPing")
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f, fill = false)) {
            Box(
                modifier = Modifier.size(14.dp).drawBehind {
                    drawCircle(Color(0xFFF43F5E).copy(alpha = 0.6f * (1f - ping)), radius = size.minDimension / 2f * (0.6f + 1.2f * ping))
                    drawCircle(Color(0xFFF43F5E).copy(alpha = 0.5f), radius = 7.dp.toPx())
                    drawCircle(Color(0xFFF43F5E), radius = 4.5.dp.toPx())
                }
            )
            Text(
                title,
                style = TextStyle(brush = TaOrangeGold, fontSize = 19.sp, fontWeight = FontWeight.Black, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 3f), 0f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .adPressScale(press)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF25338F), Color(0xFF141C5E))))
                .border(1.5.dp, Color(0xFFE0A82A), RoundedCornerShape(18.dp))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onViewAllClick)
                .testTag("home_see_all_matches")
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("$viewAllText ›", style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3C4)), maxLines = 1)
        }
    }
}
