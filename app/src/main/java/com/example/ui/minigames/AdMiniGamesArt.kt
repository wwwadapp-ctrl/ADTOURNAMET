package com.example.ui.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Home page part 4 artwork (mini games): colours, the gold scalloped medal and the section heading.
// VISUAL ONLY: no data, no navigation, no logic.

internal val MgGoldLight = Color(0xFFFFF3C4)
internal val MgGold = Color(0xFFFFD45A)
internal val MgGoldMid = Color(0xFFE0A82A)
internal val MgGoldDeep = Color(0xFFC98A10)
internal val MgGoldDark = Color(0xFF8A5A08)
internal val MgBrown = Color(0xFF3A2600)

/** A gold medal with a scalloped (wavy) edge, like a seal. */
internal fun DrawScope.mgScallopMedal(inner: Brush) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val rOut = size.minDimension / 2f
    val p = Path()
    for (i in 0 until 40) {
        val a = 2.0 * PI * i / 40.0
        val r = if (i % 2 == 0) rOut else rOut * 0.886f
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF3C4), MgGoldMid, MgGoldDark)))
    drawCircle(inner, radius = rOut * 0.71f, center = c)
    drawCircle(MgGoldDeep, radius = rOut * 0.71f, center = c, style = Stroke(1.dp.toPx()))
}

/** Scalloped gold medal with a short text, e.g. "৫০x". */
@Composable
internal fun MgScallopBadge(text: String, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color(0x66000000), radius = this.size.minDimension / 2f, center = Offset(center.x, center.y + 2.dp.toPx()))
            mgScallopMedal(
                Brush.radialGradient(
                    listOf(Color(0xFF5A3A14), Color(0xFF2A1806)),
                    center = Offset(center.x, center.y * 0.7f)
                )
            )
        }
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp, fontWeight = FontWeight.Black, color = MgGoldLight,
                shadow = Shadow(Color.Black, Offset(0f, 2f), 3f)
            ),
            maxLines = 1
        )
    }
}

/** Section heading: gold medal with a game controller, big gold title, and the "view all" capsule. */
@Composable
fun AdMiniGamesHeader(
    title: String,
    viewAllText: String,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(Color(0x99FFC850), Color.Transparent), radius = size.minDimension * 0.75f),
                            radius = size.minDimension * 0.75f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    mgScallopMedal(
                        Brush.radialGradient(
                            listOf(Color(0xFF2C3CB8), Color(0xFF0B1040)),
                            center = Offset(center.x, center.y * 0.7f)
                        )
                    )
                }
                // small game controller
                Canvas(modifier = Modifier.size(26.dp)) {
                    val s = size.width / 24f
                    fun o(x: Float, y: Float) = Offset(x * s, y * s)
                    drawLine(Color(0xFF94A3B8), o(12f, 9f), o(12f, 5f), 1.2f * s)
                    drawCircle(Color(0xFF475569), 1.6f * s, o(12f, 4.5f))
                    val body = Path().apply {
                        moveTo(6f * s, 9f * s); lineTo(18f * s, 9f * s)
                        cubicTo(20.5f * s, 9f * s, 22f * s, 10.8f * s, 22f * s, 13f * s)
                        lineTo(22f * s, 15.5f * s)
                        cubicTo(22f * s, 18f * s, 18.6f * s, 19f * s, 16.6f * s, 17.3f * s)
                        lineTo(15f * s, 15.5f * s); lineTo(9f * s, 15.5f * s); lineTo(7.4f * s, 17.3f * s)
                        cubicTo(5.4f * s, 19f * s, 2f * s, 18f * s, 2f * s, 15.5f * s)
                        lineTo(2f * s, 13f * s)
                        cubicTo(2f * s, 10.8f * s, 3.5f * s, 9f * s, 6f * s, 9f * s)
                        close()
                    }
                    drawPath(body, Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFF64748B)), startY = 9f * s, endY = 18f * s))
                    drawPath(body, Color(0xFF334155), style = Stroke(0.6f * s))
                    drawLine(Color(0xFF1E293B), o(7.5f, 11f), o(7.5f, 14f), 1.2f * s)
                    drawLine(Color(0xFF1E293B), o(6f, 12.5f), o(9f, 12.5f), 1.2f * s)
                    drawCircle(Color(0xFFEF4444), 1f * s, o(16f, 12f))
                    drawCircle(Color(0xFF22C55E), 1f * s, o(18f, 14f))
                }
            }
            Text(
                text = title,
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBE8), Color(0xFFFFE07A), Color(0xFFF2B21E), MgGoldDeep)),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(MgBrown, Offset(0f, 4f), 0f)
                ),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF25338F), Color(0xFF141C5E))))
                .border(1.5.dp, MgGoldMid, RoundedCornerShape(18.dp))
                .drawBehind {
                    drawRoundRect(
                        Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(6.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 12.dp.toPx(), 1.dp.toPx()),
                        cornerRadius = CornerRadius(1f)
                    )
                }
                .clickable(onClick = onViewAllClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("$viewAllText ›", style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = MgGoldLight), maxLines = 1)
        }
    }
}
