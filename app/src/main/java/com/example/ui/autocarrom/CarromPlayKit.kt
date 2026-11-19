package com.example.ui.autocarrom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sqrt
import kotlin.random.Random

// Auto Carrom helpers shared by the practice screen and the online screen:
// powder look (specks + trails), aim drawing, the simple computer opponent, Bengali messages.

class PowderUi(val key: String, val x: Double, val y: Double, var s: Double) {
    val n = 1500
    val xs = FloatArray(n); val ys = FloatArray(n); val alive = BooleanArray(n) { true }
    init {
        val r = Random(key.hashCode())
        for (i in 0 until n) {
            val a = r.nextDouble() * 6.2832
            val d = sqrt(r.nextDouble()) * CarromSim.POWDER_R * (0.55 + 0.45 * r.nextDouble())
            xs[i] = (x + Math.cos(a) * d).toFloat(); ys[i] = (y + Math.sin(a) * d).toFloat()
            for (p in CarromSim.POCKETS) { val dx = xs[i] - p.first.toFloat(); val dy = ys[i] - p.second.toFloat(); if (dx * dx + dy * dy < ((CarromRules.RP + 6) * (CarromRules.RP + 6)).toFloat()) alive[i] = false }
        }
    }
}

class PowderLayer {
    val patches = mutableListOf<PowderUi>()
    val falling = mutableListOf<FloatArray>() // x, y, height, t

    /** Syncs with CarromGameState.powder; returns true when a new patch appeared (play the sound). */
    fun sync(powder: String): Boolean {
        var added = false
        val keep = mutableSetOf<String>()
        if (powder.isNotBlank()) for (p in powder.split(";")) {
            val f = p.split(","); if (f.size != 3) continue
            val key = f[0] + "," + f[1]; keep.add(key)
            val s = (f[2].toDoubleOrNull() ?: 0.0) / 100.0
            val ex = patches.firstOrNull { it.key == key }
            if (ex != null) ex.s = s else {
                val x = (f[0].toDoubleOrNull() ?: 0.0) / 10.0; val y = (f[1].toDoubleOrNull() ?: 0.0) / 10.0
                patches.add(PowderUi(key, x, y, s)); added = true
                val r = Random(key.hashCode() + 1)
                repeat(70) { val a = r.nextDouble() * 6.2832; val d = sqrt(r.nextDouble()) * 85
                    falling.add(floatArrayOf((x + Math.cos(a) * d).toFloat(), (y + Math.sin(a) * d).toFloat(), (60 + r.nextDouble() * 90).toFloat(), 0f)) }
            }
        }
        patches.removeAll { it.key !in keep }
        return added
    }

    /** A moving piece pushes powder aside (push = true); a finger wipes it away (push = false). */
    fun wipe(x: Double, y: Double, rad: Double, push: Boolean) {
        val xf = x.toFloat(); val yf = y.toFloat(); val rf = rad.toFloat()
        for (p in patches) {
            if (Math.abs(x - p.x) > CarromSim.POWDER_R + rad || Math.abs(y - p.y) > CarromSim.POWDER_R + rad) continue
            for (i in 0 until p.n) {
                if (!p.alive[i]) continue
                val dx = p.xs[i] - xf; val dy = p.ys[i] - yf
                val d = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d < rf) {
                    if (push && d > 0.01f) { val k = (rf + 0.5f + (i % 5) * 0.6f) / d; p.xs[i] = xf + dx * k; p.ys[i] = yf + dy * k } else p.alive[i] = false
                }
            }
        }
    }

    private val paint = android.graphics.Paint().apply { strokeCap = android.graphics.Paint.Cap.SQUARE; isAntiAlias = false }

    fun draw(ds: DrawScope, k: Float) = with(ds) {
        val f = CB_F * k; val s = CarromRules.S.toFloat() * k
        clipRect(f, f, f + s, f + s) {
            for (p in patches) {
                val c = Offset((CB_F + p.x.toFloat()) * k, (CB_F + p.y.toFloat()) * k); val rr = CarromSim.POWDER_R.toFloat() * k
                drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = (0.22 * p.s).toFloat()), 0.7f to Color.White.copy(alpha = (0.12 * p.s).toFloat()), 1f to Color.Transparent, center = c, radius = rr), rr, c)
                val pts = FloatArray(p.n * 2); var m = 0
                for (i in 0 until p.n) if (p.alive[i]) { pts[m++] = (CB_F + p.xs[i]) * k; pts[m++] = (CB_F + p.ys[i]) * k }
                val a1 = Math.min(1.0, p.s * 1.4)
                paint.strokeWidth = 2.4f * k
                paint.color = android.graphics.Color.argb((76 * a1).toInt(), 140, 115, 80)
                drawContext.canvas.nativeCanvas.save(); drawContext.canvas.nativeCanvas.translate(0.7f * k, 0.7f * k)
                drawContext.canvas.nativeCanvas.drawPoints(pts, 0, m, paint); drawContext.canvas.nativeCanvas.restore()
                paint.color = android.graphics.Color.argb((255 * a1).toInt(), 255, 255, 255)
                drawContext.canvas.nativeCanvas.drawPoints(pts, 0, m, paint)
            }
            val it = falling.iterator()
            while (it.hasNext()) {
                val q = it.next(); q[3] += 0.04f
                if (q[3] >= 1f) { it.remove(); continue }
                val yy = q[1] - q[2] * (1 - q[3] * q[3])
                drawCircle(Color.White.copy(alpha = 0.9f * (1 - q[3] * 0.5f)), (1.6f + 1.2f * (1 - q[3])) * k, Offset((CB_F + q[0]) * k, (CB_F + yy) * k))
            }
            if (patches.isNotEmpty()) for ((px, py) in CarromSim.POCKETS) drawPocketHole(Offset((CB_F + px.toFloat()) * k, (CB_F + py.toFloat()) * k), (CarromRules.RP.toFloat() + 1f) * k)
        }
    }
}

/** Dashed aim line with arrow head, ghost striker at the first coin, power arc and pull line. */
fun DrawScope.drawCarromAim(k: Float, coins: List<CBody>, sx: Double, sy: Double, dx: Double, dy: Double, power: Double) {
    val (hitT, hitO) = CarromSim.rayHit(coins, sx, sy, dx, dy)
    val L = Math.min(hitT, 90 + power * 140)
    fun pt(t: Double) = Offset((CB_F + (sx + dx * t).toFloat()) * k, (CB_F + (sy + dy * t).toFloat()) * k)
    drawLine(Color.White.copy(alpha = 0.95f), pt(CarromRules.RS + 4), pt(L - 14), 3.4f * k, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(10f * k, 8f * k)))
    val e = pt(L); val px = (-dy).toFloat(); val py = dx.toFloat(); val ux = dx.toFloat(); val uy = dy.toFloat()
    val arrow = Path().apply {
        moveTo(e.x, e.y); lineTo(e.x - ux * 22 * k + px * 12 * k, e.y - uy * 22 * k + py * 12 * k)
        lineTo(e.x - ux * 16 * k, e.y - uy * 16 * k); lineTo(e.x - ux * 22 * k - px * 12 * k, e.y - uy * 22 * k - py * 12 * k); close()
    }
    drawPath(arrow, Color.White); drawPath(arrow, Color(0xFFC98A10), style = Stroke(1.5f * k))
    if (hitO != null && hitT <= L + 0.1) drawCircle(Color.White.copy(alpha = 0.7f), CarromRules.RS.toFloat() * k, pt(hitT), style = Stroke(2f * k))
    val c = pt(0.0); val rr = (CarromRules.RS.toFloat() + 14f) * k
    val col = if (power > 0.75) Color(0xFFFF5A6A) else if (power > 0.4) Color(0xFFFFC21E) else Color(0xFF4ADE80)
    drawArc(col, -90f, (360 * power).toFloat(), false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(6f * k, cap = StrokeCap.Round))
    drawLine(Color(0x8CFFD45A), c, pt(-power * 120), 5f * k)
}

object CarromBot {
    /** Picks a shot for [color]'s coins from base line y = [baseY]. Returns strikerX, dirX, dirY, power. */
    fun choose(coins: List<CBody>, color: Char, baseY: Double): DoubleArray {
        val dirSign = if (baseY < CarromRules.S / 2) 1.0 else -1.0
        var best: DoubleArray? = null; var bestSc = -1e9
        for (o in coins) {
            if (o.inPocket || (o.c != color && o.c != 'q')) continue
            for ((px, py) in CarromSim.POCKETS) {
                val tx = px - o.x; val ty = py - o.y; val tl = sqrt(tx * tx + ty * ty)
                val gx = o.x - tx / tl * (CarromRules.RC + CarromRules.RS); val gy = o.y - ty / tl * (CarromRules.RC + CarromRules.RS)
                if ((gy - baseY) * dirSign <= 5) continue
                for (k in 0..8) {
                    val sx = CarromRules.XMIN + k * (CarromRules.XMAX - CarromRules.XMIN) / 8
                    val ax = gx - sx; val ay = gy - baseY; val al = sqrt(ax * ax + ay * ay)
                    val cut = (ax * tx + ay * ty) / (al * tl); if (cut < 0.35) continue
                    val (t, h) = CarromSim.rayHit(coins, sx, baseY, ax / al, ay / al)
                    if (h !== o && t < al - 5) continue
                    val sc = cut * 2 - al / 700 - tl / 900
                    if (sc > bestSc) { bestSc = sc; best = doubleArrayOf(sx, ax / al, ay / al, Math.min(1.0, 0.38 + (al + tl) / 1300)) }
                }
            }
        }
        if (best == null) { val dx = Random.nextDouble(-0.3, 0.3); val l = sqrt(dx * dx + 1); best = doubleArrayOf(CarromRules.S / 2, dx / l, dirSign / l, 0.55) }
        val err = Random.nextDouble(-0.03, 0.03); val c = Math.cos(err); val s = Math.sin(err)
        return doubleArrayOf(best[0], best[1] * c - best[2] * s, best[1] * s + best[2] * c, best[3])
    }
}

/** Bengali toast for an engine log line; second = bad news (red). */
fun carromLogText(log: String): Pair<String, Boolean>? = when {
    log.startsWith("Foul") -> "ফাউল! স্ট্রাইকার পকেটে" to true
    log.startsWith("Queen covered") -> "কুইন কভার! 👑" to false
    log.startsWith("Queen pocketed") -> "কুইন! এবার কভার করুন" to false
    log.startsWith("Queen returned") -> "কুইন ফেরত গেল" to true
    log.startsWith("Pocketed") -> CarromRules.bn(log.filter { it.isDigit() }) + "টা গুটি পকেটে!" to false
    log.startsWith("Time over! Sudden") -> "সময় শেষ! সাডেন ডেথ ⚡" to true
    log.startsWith("Time up") -> "সময় শেষ! পালা গেল" to true
    else -> null
}
