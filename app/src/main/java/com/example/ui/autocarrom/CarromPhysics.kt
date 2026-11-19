package com.example.ui.autocarrom

import kotlin.math.sqrt

// Auto Carrom physics. Fixed time step + plain sqrt, so the same shot gives the same result on both phones.

class CBody(
    val c: Char, var x: Double, var y: Double, val r: Double, val m: Double,
    var vx: Double = 0.0, var vy: Double = 0.0,
    var inPocket: Boolean = false, var px: Double = 0.0, var py: Double = 0.0, var fall: Float = 0f
)

class CPatch(val x: Double, val y: Double, val s: Double) // s = 0..1

class CarromSim(board: String, powder: String, strikerX: Double, strikerY: Double) {
    val coins: List<CBody> = CarromRules.decode(board).map { CBody(it.c, it.x, it.y, CarromRules.RC, 1.0) }
    val striker = CBody('s', strikerX, strikerY, CarromRules.RS, 2.7)
    val all: List<CBody> = listOf(striker) + coins
    val patches: List<CPatch> = parsePowder(powder)
    val pocketed = StringBuilder()
    var strikerIn = false
        private set

    /** kind: 0 = hit (v = strength 0..1), 1 = wall, 2 = pocket */
    var onEvent: (Int, Double) -> Unit = { _, _ -> }

    fun shoot(dx: Double, dy: Double, power: Double) {
        val v = 500.0 + power.coerceIn(0.0, 1.0) * 3300.0
        striker.vx = dx * v; striker.vy = dy * v
    }

    fun moving(): Boolean = all.any { !it.inPocket && (Math.abs(it.vx) > 1.0 || Math.abs(it.vy) > 1.0) }

    fun powderAt(x: Double, y: Double): Double {
        var v = 0.0
        for (p in patches) {
            val dx = x - p.x; val dy = y - p.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < POWDER_R) { val f = p.s * (1 - (d / POWDER_R) * (d / POWDER_R)); if (f > v) v = f }
        }
        return v
    }

    /** One fixed physics step of [DT] seconds. */
    fun step() {
        val S = CarromRules.S
        for (o in all) {
            if (o.inPocket) continue
            val sp = sqrt(o.vx * o.vx + o.vy * o.vy)
            if (sp > 0) {
                val dec = ((if (o === striker) 560.0 else 520.0) + 0.35 * sp) * (1 - 0.5 * powderAt(o.x, o.y)) * DT
                val k = Math.max(0.0, sp - dec) / sp
                o.vx *= k; o.vy *= k
            }
            o.x += o.vx * DT; o.y += o.vy * DT
            if (o.x < o.r) { o.x = o.r; o.vx = -o.vx * 0.75; onEvent(1, 0.0) }
            if (o.x > S - o.r) { o.x = S - o.r; o.vx = -o.vx * 0.75; onEvent(1, 0.0) }
            if (o.y < o.r) { o.y = o.r; o.vy = -o.vy * 0.75; onEvent(1, 0.0) }
            if (o.y > S - o.r) { o.y = S - o.r; o.vy = -o.vy * 0.75; onEvent(1, 0.0) }
            for (p in POCKETS) {
                val dx = o.x - p.first; val dy = o.y - p.second
                if (sqrt(dx * dx + dy * dy) < CarromRules.RP) {
                    o.inPocket = true; o.vx = 0.0; o.vy = 0.0; o.px = p.first; o.py = p.second; o.fall = 1f
                    if (o === striker) strikerIn = true else pocketed.append(o.c)
                    onEvent(2, 0.0)
                    break
                }
            }
        }
        for (i in all.indices) for (j in i + 1 until all.size) {
            val a = all[i]; val b = all[j]
            if (a.inPocket || b.inPocket) continue
            val dx = b.x - a.x; val dy = b.y - a.y
            val d = sqrt(dx * dx + dy * dy); val mn = a.r + b.r
            if (d < mn && d > 0) {
                val nx = dx / d; val ny = dy / d; val ov = mn - d; val tm = a.m + b.m
                a.x -= nx * ov * b.m / tm; a.y -= ny * ov * b.m / tm
                b.x += nx * ov * a.m / tm; b.y += ny * ov * a.m / tm
                val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                if (rv < 0) {
                    val jj = -(1 + 0.92) * rv / (1 / a.m + 1 / b.m)
                    a.vx -= jj * nx / a.m; a.vy -= jj * ny / a.m
                    b.vx += jj * nx / b.m; b.vy += jj * ny / b.m
                    if (-rv > 12) onEvent(0, Math.min(1.0, 0.25 + (-rv) / 1400.0))
                }
            }
        }
    }

    /** Runs the whole shot at once (no drawing). */
    fun runToEnd(maxSteps: Int = 20000) { var n = 0; while (moving() && n < maxSteps) { step(); n++ } }

    fun finalBoard(): String = CarromRules.encode(coins.filter { !it.inPocket }.map { CPiece(it.c, it.x, it.y) })

    companion object {
        const val DT = 1.0 / 480.0
        const val POWDER_R = 95.0
        val POCKETS = listOf(
            CarromRules.RP to CarromRules.RP, CarromRules.S - CarromRules.RP to CarromRules.RP,
            CarromRules.RP to CarromRules.S - CarromRules.RP, CarromRules.S - CarromRules.RP to CarromRules.S - CarromRules.RP
        )

        fun parsePowder(s: String): List<CPatch> {
            if (s.isBlank()) return emptyList()
            return s.split(";").mapNotNull { p ->
                val f = p.split(",")
                if (f.size != 3) null else {
                    val x = f[0].toDoubleOrNull(); val y = f[1].toDoubleOrNull(); val st = f[2].toDoubleOrNull()
                    if (x == null || y == null || st == null) null else CPatch(x / 10.0, y / 10.0, st / 100.0)
                }
            }
        }

        /** Distance along (dx,dy) from (sx,sy) until the striker touches the first coin (or Infinity). */
        fun rayHit(coins: List<CBody>, sx: Double, sy: Double, dx: Double, dy: Double): Pair<Double, CBody?> {
            var best = Double.POSITIVE_INFINITY; var hit: CBody? = null
            for (o in coins) {
                if (o.inPocket) continue
                val fx = o.x - sx; val fy = o.y - sy; val t = fx * dx + fy * dy
                if (t <= 0) continue
                val d2 = fx * fx + fy * fy - t * t; val r = o.r + CarromRules.RS
                if (d2 > r * r) continue
                val tt = t - sqrt(r * r - d2)
                if (tt < best) { best = tt; hit = o }
            }
            return best to hit
        }
    }
}
