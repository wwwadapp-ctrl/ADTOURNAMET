package com.example.ui.autocarrom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The carrom board with pieces, powder, aim and touch input (bottom base line = the player on this phone).
 * All positions are WORLD positions (player 1 at the bottom). [flip] = true for player 2: the pieces are
 * turned 180 degrees so player 2 also shoots from the bottom; touches are turned back to world positions.
 * Shared by the practice screen and the online game screen. [tick] changes every frame to redraw.
 */
@Composable
fun CarromBoardView(
    tick: Int, now: Long, bodies: List<CBody>, sim: CarromSim?,
    strikerX: Double, strikerY: Double, showStrikerRing: Boolean, aim: DoubleArray?, blocked: Boolean,
    canControl: Boolean, powderMode: Boolean, inputKey: Any, powder: PowderLayer, flip: Boolean = false,
    onSlide: (Double) -> Unit, onAim: (DoubleArray?) -> Unit, onShoot: (DoubleArray) -> Unit, onPowder: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val sxNow by rememberUpdatedState(strikerX)
    val aimNow by rememberUpdatedState(aim)
    val blockedNow by rememberUpdatedState(blocked)
    val slide by rememberUpdatedState(onSlide)
    val aimCb by rememberUpdatedState(onAim)
    val shoot by rememberUpdatedState(onShoot)
    val pw by rememberUpdatedState(onPowder)
    Box(modifier.fillMaxWidth().aspectRatio(1f).shadow(14.dp, RoundedCornerShape(16.dp))) {
        Canvas(Modifier.matchParentSize()) { drawCarromBoard() }
        Canvas(
            Modifier.matchParentSize().testTag("carrom_board")
                .pointerInput(inputKey, canControl, powderMode, flip) {
                    if (!canControl) return@pointerInput
                    val k = size.width / CB_W
                    val S = CarromRules.S
                    fun mm(o: Offset): Pair<Double, Double> { val x = (o.x / k - CB_F).toDouble(); val y = (o.y / k - CB_F).toDouble(); return if (flip) (S - x) to (S - y) else x to y }
                    if (powderMode) { detectTapGestures { o -> val (x, y) = mm(o); pw(x, y) }; return@pointerInput }
                    val by = if (flip) CarromRules.BASE else S - CarromRules.BASE
                    var drag = 0
                    detectDragGestures(
                        onDragStart = { o ->
                            val (x, y) = mm(o); val dx = x - sxNow; val dy = y - by
                            drag = if (sqrt(dx * dx + dy * dy) < CarromRules.RS * 2.6) 1
                            else if (abs(y - by) < 45) { slide(x.coerceIn(CarromRules.XMIN, CarromRules.XMAX)); 2 } else 0
                        },
                        onDragEnd = { val a = aimNow; if (drag == 1 && a != null && a[2] > 0.08 && !blockedNow) shoot(a) else aimCb(null); drag = 0 },
                        onDragCancel = { aimCb(null); drag = 0 }
                    ) { ch, _ ->
                        ch.consume(); val (x, y) = mm(ch.position)
                        powder.wipe(x, y, 11.0, false)
                        if (drag == 2) { val nx = x.coerceIn(CarromRules.XMIN, CarromRules.XMAX); slide(nx); powder.wipe(nx, by, CarromRules.RS * 0.9, true) }
                        else if (drag == 1) {
                            val dx = sxNow - x; val dy = by - y; val l = sqrt(dx * dx + dy * dy)
                            aimCb(if (l > 4) doubleArrayOf(dx / l, dy / l, Math.min(1.0, l / 170)) else null)
                        }
                    }
                }
        ) {
            tick
            val k = size.width / CB_W
            withTransform({ if (flip) rotate(180f, Offset(size.width / 2, size.height / 2)) }) {
            fun px(v: Double) = (CB_F + v.toFloat()) * k
            powder.draw(this, k)
            val list = sim?.coins ?: bodies
            for (b in list) {
                if (!b.inPocket) drawCoin(b.c, px(b.x), px(b.y), CarromRules.RC.toFloat() * k)
                else if (b.fall > 0f) drawCoin(b.c, px(b.px), px(b.py), CarromRules.RC.toFloat() * k * Math.max(0.3f, b.fall), b.fall)
            }
            val tw = 0.5f + 0.5f * kotlin.math.sin(now / 260f)
            val stk = sim?.striker
            val sx = stk?.x ?: strikerX; val sy = stk?.y ?: strikerY
            if (stk == null || !stk.inPocket) drawStriker(px(sx), px(sy), CarromRules.RS.toFloat() * k, tw)
            else if (stk.fall > 0f) drawStriker(px(stk.px), px(stk.py), CarromRules.RS.toFloat() * k * Math.max(0.3f, stk.fall), tw, stk.fall)
            if (showStrikerRing && sim == null) {
                drawCircle(if (blocked) Color(0xE6FF3C50) else Color(0xD9FFE68C), (CarromRules.RS.toFloat() + 6f + 2f * tw) * k, Offset(px(sx), px(sy)), style = Stroke(3f * k))
                aim?.let { drawCarromAim(k, list, sx, sy, it[0], it[1], it[2]) }
            }
            }
        }
    }
}
