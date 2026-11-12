package com.example.ui.autoludo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val RdGold = Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8B23A), Color(0xFF9A6400), Color(0xFFF6D27A)))
private val RdGoldText = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10)))

@Composable
fun AdResultDialog(
    isWin: Boolean,
    prizePool: Double,
    winnerName: String,
    opponentName: String,
    isOpponentForfeit: Boolean,
    isOpponentDisqualified: Boolean,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val bn = strings === BengaliStrings
    val inf = rememberInfiniteTransition(label = "resultFx")
    val fall by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "fall")
    val pulse by inf.animateFloat(0.8f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")

    val badge: String; val name: String; val line: String; val love: String; val button: String
    when {
        isWin && isOpponentForfeit -> {
            badge = if (bn) "🏳️ প্রতিপক্ষ হার মেনেছেন" else "🏳️ Opponent gave up"
            name = opponentName
            line = if (bn) "আপনার দাপটের সামনে টিকতে না পেরে খেলা ছেড়ে দিয়েছেন।" else "couldn't stand your power and left the game."
            love = if (bn) "বিজয়ের মুকুট আজ আপনার মাথায় 👑" else "The crown is yours today 👑"
            button = if (bn) "দারুণ! 🎉" else "Awesome! 🎉"
        }
        isWin && isOpponentDisqualified -> {
            badge = if (bn) "⏱️ সময়ের লড়াইয়ে আপনিই সেরা" else "⏱️ You ruled the clock"
            name = opponentName
            line = if (bn) "৩ বার চাল মিস করে খেলা থেকে বাদ পড়েছেন।" else "missed 3 turns and was removed from the game."
            love = if (bn) "ধৈর্য আর মনোযোগে আপনিই এগিয়ে ছিলেন ✨" else "Your focus and patience made the difference ✨"
            button = if (bn) "দারুণ! 🎉" else "Awesome! 🎉"
        }
        isWin -> {
            badge = if (bn) "👑 চার গুটিই ঘরে" else "👑 All four pawns home"
            name = ""
            line = if (bn) "অসাধারণ খেলেছেন! প্রতিটি চালেই ছিল জয়ের ছাপ।" else "Brilliant game! Every move had a winning touch."
            love = if (bn) "এই জয়টা পুরোপুরি আপনার 💛" else "This victory is all yours 💛"
            button = if (bn) "দারুণ! 🎉" else "Awesome! 🎉"
        }
        isOpponentDisqualified -> {
            badge = if (bn) "⏱️ আপনি ৩ বার চাল মিস করেছেন" else "⏱️ You missed 3 turns"
            name = winnerName
            line = if (bn) "এবারের জয়টা নিয়ে গেলেন।" else "took the win this time."
            love = if (bn) "পরের বার সময়মতো চাল দিন, জয় আপনারই হবে 💛" else "Move on time next round, the win will be yours 💛"
            button = if (bn) "আবার চেষ্টা করব" else "I'll try again"
        }
        else -> {
            badge = if (bn) "🎲 ম্যাচ শেষ" else "🎲 Match over"
            name = winnerName
            line = if (bn) "এবারের জয়টা নিয়ে গেলেন।" else "took the win this time."
            love = if (bn) "হার মানেই শেষ নয়, পরের ম্যাচে জয় আপনার অপেক্ষায় 💪" else "A loss is not the end, your next win is waiting 💪"
            button = if (bn) "আবার চেষ্টা করব" else "I'll try again"
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isWin) Canvas(modifier = Modifier.fillMaxSize()) { rdConfetti(fall) }
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f), contentAlignment = Alignment.TopCenter) {
                Canvas(modifier = Modifier.matchParentSize()) { rdSparkles(fall) }
                Box(modifier = Modifier.padding(top = 62.dp).fillMaxWidth()) {
                    val outer = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .shadow(28.dp, outer, ambientColor = Color(0xFFFFC107), spotColor = Color(0xFFFFA000), clip = false)
                            .background(Brush.verticalGradient(listOf(Color(0xFF1A2A72), Color(0xFF0E1A4E), Color(0xFF0A1238))), outer)
                            .border(3.dp, RdGold, outer)
                    )
                    Canvas(modifier = Modifier.matchParentSize()) { rdFiligree() }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 78.dp, bottom = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val glow = Shadow(Color(0xB3FFC107), Offset(0f, 0f), 16f)
                        if (isWin) {
                            Text(if (bn) "অভিনন্দন!" else "Congratulations!", style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, shadow = glow))
                            Text(
                                if (bn) "আপনি বিজয়ী! 🏆" else "You won! 🏆",
                                style = TextStyle(brush = RdGoldText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, shadow = Shadow(Color(0xFF5A3C00), Offset(0f, 4f), 6f))
                            )
                        } else {
                            Text(if (bn) "আরেকটু হলেই হতো!" else "So close!", style = TextStyle(color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, shadow = glow))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            badge,
                            color = Color(0xFFFFE9A8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color(0x1AFFD54F), RoundedCornerShape(50))
                                .border(1.dp, Color(0xB3E8B23A), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        if (name.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                name,
                                style = TextStyle(brush = RdGoldText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, shadow = Shadow(Color(0xFF5A3C00), Offset(0f, 3f), 5f)),
                                maxLines = 1
                            )
                        } else Spacer(Modifier.height(8.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(line, color = Color(0xFFE6ECFF), fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(love, color = Color(0xF2FFE9A8), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                        if (isWin) {
                            Spacer(Modifier.height(18.dp))
                            val amount = strings.taka(prizePool)
                            Text(
                                if (bn) "🎉 +$amount আপনার গেম ওয়ালেটে যোগ হয়েছে" else "🎉 +$amount added to your game wallet",
                                color = Color(0xFFFFD27A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF3A0610), Color(0xFF6A0E1C), Color(0xFF3A0610))), RoundedCornerShape(50))
                                    .border(1.5.dp, RdGold, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                        val btnShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .shadow(12.dp, btnShape, ambientColor = Color(0xFFFFB300), spotColor = Color(0xFFFFA000), clip = false)
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF4C2), Color(0xFFF6C84E), Color(0xFFE0A21E), Color(0xFFB57A08))), btnShape)
                            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFFFFBE6), Color(0xFF9A6400))), btnShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { AdGameSfx.play(AdGameSfx.Sfx.TAP); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x00FFFFFF))), RoundedCornerShape(12.dp))
                        )
                        Text(button, color = Color(0xFF2A1700), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Canvas(modifier = Modifier.size(132.dp)) { rdMedallion(isWin, pulse) }
        }
    }
}
}

private fun DrawScope.rdConfetti(t: Float) {
    for (i in 0 until 34) {
        val sx = ((i * 0.6180339f) % 1f)
        val speed = 0.6f + (i % 5) * 0.18f
        val y = (((t * speed) + i * 0.137f) % 1.1f) - 0.05f
        val rot = (t * 360f * speed * 2f) + i * 37f
        val w = (8f + (i % 4) * 3f) * density / 2.2f
        val x = size.width * sx + sin((t * 6f + i) * PI.toFloat()) * 10f * density / 2.2f
        val c = Offset(x, size.height * y)
        rotate(rot, c) {
            val sq = 0.2f + 0.8f * abs(cos(rot * PI.toFloat() / 180f * 1.3f))
            drawRect(
                Brush.horizontalGradient(listOf(Color(0xFFB57A08), Color(0xFFFFF0B0), Color(0xFFE0A21E)), startX = c.x - w / 2f, endX = c.x + w / 2f),
                topLeft = Offset(c.x - w / 2f, c.y - w * 0.22f * sq), size = Size(w, w * 0.44f * sq)
            )
        }
    }
}

private fun DrawScope.rdSparkles(t: Float) {
    val seeds = listOf(0.05f to 0.20f, 0.95f to 0.25f, 0.02f to 0.55f, 0.98f to 0.60f, 0.08f to 0.85f, 0.93f to 0.88f, 0.30f to 0.04f, 0.70f to 0.05f, 0.15f to 0.40f, 0.86f to 0.45f, 0.5f to 0.98f, 0.04f to 0.72f)
    seeds.forEachIndexed { i, (sx, sy) ->
        val ph = (t * 2.7f + i * 0.137f) % 1f
        val a = (sin(ph * 2 * PI).toFloat() * 0.5f + 0.5f)
        val c = Offset(size.width * sx, size.height * sy)
        val r = (2f + (i % 3) * 1.2f) * density
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE8A0).copy(alpha = a), Color(0x00FFC107)), center = c, radius = r * 3f), radius = r * 3f, center = c)
        drawCircle(Color(0xFFFFF6D0).copy(alpha = a), radius = r * 0.6f, center = c)
    }
}

private fun DrawScope.rdFiligree() {
    val inset = 9.dp.toPx(); val w = size.width; val h = size.height
    drawRoundRect(brush = RdGold, topLeft = Offset(inset, inset), size = Size(w - inset * 2, h - inset * 2), cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(width = 1.2.dp.toPx()))
    val s = 26.dp.toPx(); val sw = 1.6.dp.toPx()
    fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
        val p = Path().apply {
            moveTo(cx + dx * s, cy + dy * 2f)
            cubicTo(cx + dx * s * 0.5f, cy + dy * 2f, cx + dx * 2f, cy + dy * s * 0.5f, cx + dx * 2f, cy + dy * s)
            moveTo(cx + dx * s * 0.62f, cy + dy * 2f)
            cubicTo(cx + dx * s * 0.62f, cy + dy * s * 0.45f, cx + dx * s * 0.25f, cy + dy * s * 0.45f, cx + dx * s * 0.3f, cy + dy * s * 0.3f)
            moveTo(cx + dx * 2f, cy + dy * s * 0.62f)
            cubicTo(cx + dx * s * 0.45f, cy + dy * s * 0.62f, cx + dx * s * 0.45f, cy + dy * s * 0.25f, cx + dx * s * 0.3f, cy + dy * s * 0.3f)
        }
        drawPath(p, RdGold, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFFFFE08A), radius = 2.2.dp.toPx(), center = Offset(cx + dx * s * 0.3f, cy + dy * s * 0.3f))
    }
    val o = inset + 4.dp.toPx()
    corner(o, o, 1f, 1f); corner(w - o, o, -1f, 1f); corner(o, h - o, 1f, -1f); corner(w - o, h - o, -1f, -1f)
}

private fun DrawScope.rdStar(c: Offset, r: Float, color: Color, fill: Boolean, strokeW: Float = 0f) {
    val p = Path()
    for (k in 0 until 10) {
        val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
        val rr = if (k % 2 == 0) r else r * 0.42f
        val x = c.x + rr * cos(a); val y = c.y + rr * sin(a)
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    if (fill) drawPath(p, color) else drawPath(p, color, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

private fun DrawScope.rdMedallion(isWin: Boolean, pulse: Float) {
    val c = center; val r = size.minDimension / 2f; val k = r / 66f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC107).copy(alpha = (if (isWin) 0.65f else 0.4f) * pulse), Color(0x00FFC107)), center = c, radius = r), radius = r, center = c)
    val ring = Path()
    val n = 12; val ro = 58f * k; val ri = 51f * k
    for (i in 0..(n * 8)) {
        val t = i / (n * 8f) * 2f * PI.toFloat()
        val rr = ri + (ro - ri) * (0.5f + 0.5f * cos(t * n))
        val x = c.x + rr * cos(t); val y = c.y + rr * sin(t)
        if (i == 0) ring.moveTo(x, y) else ring.lineTo(x, y)
    }
    ring.close()
    drawPath(ring, Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8B23A), Color(0xFF8A5600)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
    drawPath(ring, Color(0xFF6E4300), style = Stroke(width = 1.dp.toPx()))
    drawCircle(Color(0xFF6E4300), radius = 46f * k, center = c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF2E4BB8), Color(0xFF0B1640)), center = Offset(c.x - 4f * k, c.y - 8f * k), radius = 50f * k), radius = 44f * k, center = c)
    drawCircle(Brush.radialGradient(listOf(Color(if (isWin) 0xF2FFF0AA else 0x99FFF0AA), Color(0x00FFF0AA)), center = Offset(c.x, c.y - 4f * k), radius = 34f * k), radius = 34f * k, center = Offset(c.x, c.y - 4f * k))
    fun p(x: Float, y: Float) = Offset(c.x + (x - 66f) * k, c.y + (y - 66f) * k)
    if (isWin) {
        listOf(-0.35f to -0.62f, 0f to -0.75f, 0.35f to -0.62f, -0.6f to 0.15f, 0.6f to 0.15f, -0.45f to 0.5f, 0.45f to 0.5f).forEachIndexed { i, (sx, sy) ->
            rdStar(Offset(c.x + sx * 40f * k, c.y + sy * 40f * k), (if (i == 1) 5f else 3.6f) * k, Color(0xFFFFD54F), true)
        }
        val cup = Brush.horizontalGradient(listOf(Color(0xFFB97A06), Color(0xFFFFF0A8), Color(0xFFF6C445), Color(0xFFA86A00)), startX = p(47f, 0f).x, endX = p(85f, 0f).x)
        val bowl = Path().apply {
            val a = p(47f, 40f); val b = p(85f, 40f); val d = p(83f, 58f); val q = p(66f, 76f); val e = p(49f, 58f)
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); quadraticBezierTo(q.x, q.y, e.x, e.y); close()
        }
        drawPath(bowl, cup); drawPath(bowl, Color(0xFF7A4E00), style = Stroke(width = 1f * k))
        val lh = Path().apply { val a = p(48f, 44f); val q1 = p(36f, 44f); val m = p(39f, 54f); val q2 = p(41f, 61f); val e = p(50f, 61f); moveTo(a.x, a.y); quadraticBezierTo(q1.x, q1.y, m.x, m.y); quadraticBezierTo(q2.x, q2.y, e.x, e.y) }
        val rh = Path().apply { val a = p(84f, 44f); val q1 = p(96f, 44f); val m = p(93f, 54f); val q2 = p(91f, 61f); val e = p(82f, 61f); moveTo(a.x, a.y); quadraticBezierTo(q1.x, q1.y, m.x, m.y); quadraticBezierTo(q2.x, q2.y, e.x, e.y) }
        drawPath(lh, cup, style = Stroke(width = 4.5f * k, cap = StrokeCap.Round)); drawPath(rh, cup, style = Stroke(width = 4.5f * k, cap = StrokeCap.Round))
        drawRect(cup, topLeft = p(62f, 70f), size = Size(8f * k, 10f * k))
        drawRoundRect(cup, topLeft = p(53f, 80f), size = Size(26f * k, 7f * k), cornerRadius = CornerRadius(2f * k))
        drawRoundRect(Color(0xFF7A1E14), topLeft = p(50f, 87f), size = Size(32f * k, 7f * k), cornerRadius = CornerRadius(2f * k))
        drawOval(Color(0xBFFFFFFF), topLeft = p(53f, 40f), size = Size(8f * k, 14f * k))
    } else {
        rdStar(p(66f, 60f), 22f * k, Color(0xFFFFE08A), false, 3f * k)
        val smile = Path().apply { val a = p(50f, 92f); val q = p(66f, 100f); val e = p(82f, 92f); moveTo(a.x, a.y); quadraticBezierTo(q.x, q.y, e.x, e.y) }
        drawPath(smile, Color(0xFFE8B23A), style = Stroke(width = 3f * k, cap = StrokeCap.Round))
    }
}
