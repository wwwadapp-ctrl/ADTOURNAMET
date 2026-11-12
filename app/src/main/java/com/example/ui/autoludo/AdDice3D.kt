package com.example.ui.autoludo

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// AD Ludo premium 3D dice. VISUAL ONLY: draws the dice showing `value` at a given spin.
// spinX / spinY are extra rotations in degrees (multiples of 360 land exactly on `value`).
@Composable
fun AdDice3D(
    value: Int,
    spinX: Float,
    spinY: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) { drawAdDice(value.coerceIn(1, 6), spinX, spinY, color) }
}

private class AdQuad(val pts: Array<FloatArray>, val n: FloatArray)

private object AdDiceMesh {
    const val R = 0.34f
    const val IN = 1f - R
    private val T = floatArrayOf(-1f, -0.95f, -0.84f, -0.68f, 0f, 0.68f, 0.84f, 0.95f, 1f)
    // axis, sign, value (opposite faces add up to 7)
    val faces = listOf(intArrayOf(0, 1, 3), intArrayOf(0, -1, 4), intArrayOf(1, 1, 2), intArrayOf(1, -1, 5), intArrayOf(2, 1, 1), intArrayOf(2, -1, 6))
    val quads: List<AdQuad> by lazy {
        val out = ArrayList<AdQuad>()
        for (f in faces) {
            val a = f[0]; val s = f[1].toFloat(); val u = (a + 1) % 3; val v = (a + 2) % 3
            for (i in 0 until T.size - 1) for (j in 0 until T.size - 1) {
                val cs = arrayOf(T[i] to T[j], T[i + 1] to T[j], T[i + 1] to T[j + 1], T[i] to T[j + 1]).map { (x, y) ->
                    val p = FloatArray(3); p[a] = s; p[u] = x; p[v] = y; surf(p)
                }
                val n = FloatArray(3); cs.forEach { n[0] += it.second[0]; n[1] += it.second[1]; n[2] += it.second[2] }
                val l = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])
                out.add(AdQuad(cs.map { it.first }.toTypedArray(), floatArrayOf(n[0] / l, n[1] / l, n[2] / l)))
            }
        }
        out
    }
    private fun surf(p: FloatArray): Pair<FloatArray, FloatArray> {
        val q = FloatArray(3) { max(-IN, min(IN, p[it])) }
        val d = FloatArray(3) { p[it] - q[it] }
        val l = sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]).let { if (it == 0f) 1f else it }
        val n = FloatArray(3) { d[it] / l }
        return FloatArray(3) { q[it] + R * n[it] } to n
    }
    val pips: Map<Int, List<Pair<Float, Float>>> = mapOf(
        1 to listOf(0f to 0f),
        2 to listOf(-0.42f to -0.42f, 0.42f to 0.42f),
        3 to listOf(-0.44f to -0.44f, 0f to 0f, 0.44f to 0.44f),
        4 to listOf(-0.42f to -0.42f, 0.42f to -0.42f, -0.42f to 0.42f, 0.42f to 0.42f),
        5 to listOf(-0.44f to -0.44f, 0.44f to -0.44f, 0f to 0f, -0.44f to 0.44f, 0.44f to 0.44f),
        6 to listOf(-0.42f to -0.46f, 0.42f to -0.46f, -0.42f to 0f, 0.42f to 0f, -0.42f to 0.46f, 0.42f to 0.46f)
    )
    // rotation (degrees) that turns each value toward the viewer
    val faceRot: Map<Int, Pair<Float, Float>> = mapOf(1 to (0f to 0f), 6 to (0f to 180f), 3 to (0f to -90f), 4 to (0f to 90f), 2 to (90f to 0f), 5 to (-90f to 0f))
}

private typealias M3 = Array<FloatArray>
private fun mul(a: M3, b: M3): M3 = Array(3) { i -> FloatArray(3) { j -> a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j] } }
private fun rx(deg: Float): M3 { val t = deg * PI.toFloat() / 180f; val c = cos(t); val s = sin(t); return arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, c, -s), floatArrayOf(0f, s, c)) }
private fun ry(deg: Float): M3 { val t = deg * PI.toFloat() / 180f; val c = cos(t); val s = sin(t); return arrayOf(floatArrayOf(c, 0f, s), floatArrayOf(0f, 1f, 0f), floatArrayOf(-s, 0f, c)) }
private fun ap(m: M3, p: FloatArray) = floatArrayOf(
    m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2],
    m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2],
    m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2]
)

private val AdLight = floatArrayOf(-0.5f, 0.75f, 0.75f).let { val l = sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]); floatArrayOf(it[0] / l, it[1] / l, it[2] / l) }
private val AdHalf = floatArrayOf(AdLight[0], AdLight[1], AdLight[2] + 1f).let { val l = sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]); floatArrayOf(it[0] / l, it[1] / l, it[2] / l) }

private fun DrawScope.drawAdDice(value: Int, spinX: Float, spinY: Float, color: Color) {
    val (frx, fry) = AdDiceMesh.faceRot.getValue(value)
    val m = mul(rx(15f), mul(rx(frx + spinX), ry(fry + spinY)))
    val cx = size.width / 2f; val cy = size.height / 2f
    val sc = min(size.width, size.height) * 0.34f
    val dist = 11f
    val dark = color.red > 0.8f && color.green > 0.55f && color.blue < 0.45f // yellow dice -> dark red details
    fun proj(p: FloatArray): Triple<Float, Float, Float> {
        val q = ap(m, p); val f = dist / (dist - q[2])
        return Triple(cx + q[0] * sc * f, cy - q[1] * sc * f, q[2])
    }
    fun shade(n: FloatArray): Color {
        val dif = max(0f, n[0] * AdLight[0] + n[1] * AdLight[1] + n[2] * AdLight[2])
        val sp = max(0f, n[0] * AdHalf[0] + n[1] * AdHalf[1] + n[2] * AdHalf[2]).pow(70)
        val env = 0.5f + 0.5f * n[1]
        val fr = (1f - max(0f, n[2])).pow(2.4f)
        val k = 0.22f + 0.6f * dif + 0.32f * env
        val add = sp * 1.1f + fr * 0.12f
        return Color(min(1f, color.red * k + add), min(1f, color.green * k + add), min(1f, color.blue * k + add))
    }
    // body (painter's order)
    data class Item(val path: Path, val c: Color, val z: Float)
    val items = ArrayList<Item>()
    for (qd in AdDiceMesh.quads) {
        val n = ap(m, qd.n)
        if (n[2] <= 0f) continue
        val pp = qd.pts.map { proj(it) }
        val path = Path().apply {
            moveTo(pp[0].first, pp[0].second); for (k in 1..3) lineTo(pp[k].first, pp[k].second); close()
        }
        items.add(Item(path, shade(n), (pp[0].third + pp[1].third + pp[2].third + pp[3].third) / 4f))
    }
    items.sortBy { it.z }
    for (it in items) { drawPath(it.path, it.c); drawPath(it.path, it.c, style = Stroke(width = 0.8f)) }

    // faces: rim + pips
    for (f in AdDiceMesh.faces) {
        val nrm = FloatArray(3); nrm[f[0]] = f[1].toFloat()
        val n = ap(m, nrm)
        if (n[2] < 0.12f) continue
        val vis = min(1f, (n[2] - 0.12f) / 0.35f)
        val a = f[0]; val u = (a + 1) % 3; val v = (a + 2) % 3
        fun pt(x: Float, y: Float): Offset { val p = FloatArray(3); p[a] = f[1].toFloat(); p[u] = x; p[v] = y; val r = proj(p); return Offset(r.first, r.second) }
        val h = 0.74f; val cr = 0.2f
        val rim = Path()
        var first = true
        listOf(Triple(h - cr, h - cr, 0f), Triple(-(h - cr), h - cr, 90f), Triple(-(h - cr), -(h - cr), 180f), Triple(h - cr, -(h - cr), 270f)).forEach { (ox, oy, a0) ->
            for (k in 0..6) {
                val t = (a0 + 90f * k / 6f) * PI.toFloat() / 180f
                val q = pt(ox + cr * cos(t), oy + cr * sin(t))
                if (first) { rim.moveTo(q.x, q.y); first = false } else rim.lineTo(q.x, q.y)
            }
        }
        rim.close()
        val g0 = pt(-h, h); val g1 = pt(h, -h)
        val rimBrush = if (dark) Brush.linearGradient(listOf(Color(0xFFE0655A), Color(0xFFA8231C), Color(0xFF5A0A08)), g0, g1)
        else Brush.linearGradient(listOf(Color(0xFFFFF4C2), Color(0xFFF2C04A), Color(0xFF9A6400)), g0, g1)
        drawPath(rim, Color(0x8C462300).copy(alpha = 0.55f * vis), style = Stroke(width = sc * 0.06f))
        drawPath(rim, rimBrush, alpha = vis, style = Stroke(width = sc * 0.042f))
        drawPath(rim, (if (dark) Color(0xCCFFD7BE) else Color(0xD9FFFADC)).copy(alpha = 0.85f * vis), style = Stroke(width = sc * 0.012f))

        for ((px, py) in AdDiceMesh.pips.getValue(f[2])) {
            fun circle(r0: Float): Path = Path().apply {
                for (k in 0..22) {
                    val t = k / 22f * 2f * PI.toFloat()
                    val q = pt(px + r0 * cos(t), py + r0 * sin(t))
                    if (k == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y)
                }
                close()
            }
            drawPath(circle(0.165f), Color(0x993C1900).copy(alpha = 0.6f * vis))
            val lo = pt(px + 0.06f, py - 0.06f)
            val pipBrush = if (dark) Brush.radialGradient(0f to Color(0xFFE87A6E), 0.4f to Color(0xFFB3261E), 0.85f to Color(0xFF5E0A07), 1f to Color(0xFF3A0503), center = lo, radius = sc * 0.2f)
            else Brush.radialGradient(0f to Color(0xFFFFF6CF), 0.4f to Color(0xFFF4C24E), 0.85f to Color(0xFFA86E00), 1f to Color(0xFF6E4300), center = lo, radius = sc * 0.2f)
            val c15 = circle(0.15f)
            drawPath(c15, pipBrush, alpha = vis)
            drawPath(c15, Color(0xE6FFECAA).copy(alpha = 0.9f * vis), style = Stroke(width = sc * 0.014f))
            drawCircle(Color(0x8CFFFFF0).copy(alpha = 0.55f * vis), radius = sc * 0.025f, center = pt(px - 0.05f, py + 0.06f))
        }
    }
    // soft glossy sheen
    drawCircle(
        Brush.radialGradient(listOf(Color(0x29FFFFFF), Color(0x00FFFFFF)), center = Offset(cx - sc * 0.55f, cy - sc * 0.75f), radius = sc * 0.9f),
        radius = sc * 0.9f, center = Offset(cx - sc * 0.55f, cy - sc * 0.75f)
    )
}
