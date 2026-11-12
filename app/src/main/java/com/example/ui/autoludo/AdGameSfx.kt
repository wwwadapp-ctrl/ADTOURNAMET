package com.example.ui.autoludo

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object AdGameSfx {
    enum class Sfx { CAPTURE, GOAL, WIN, LOSS }

    @Volatile var muted: Boolean = false

    private const val SR = 44100
    private const val CACHE_DIR = "ad_sfx_v1"
    @Volatile private var pool: SoundPool? = null
    @Volatile private var preparing = false
    private val ids = ConcurrentHashMap<Sfx, Int>()

    fun prepare(context: Context) {
        if (pool != null || preparing) return
        preparing = true
        val appContext = context.applicationContext
        Thread {
            try {
                val p = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    ).build()
                val dir = File(appContext.cacheDir, CACHE_DIR).apply { mkdirs() }
                for (s in Sfx.values()) {
                    val f = File(dir, s.name.lowercase() + ".wav")
                    if (!f.exists() || f.length() < 1000L) writeWav(f, synth(s))
                    ids[s] = p.load(f.absolutePath, 1)
                }
                pool = p
            } catch (_: Throwable) {
            } finally {
                preparing = false
            }
        }.start()
    }

    fun play(s: Sfx, volume: Float = 1f) {
        if (muted) return
        val p = pool ?: return
        val id = ids[s] ?: return
        try { p.play(id, volume, volume, 1, 0, 1f) } catch (_: Throwable) { }
    }

    private fun synth(s: Sfx): FloatArray = when (s) {
        Sfx.CAPTURE -> capture()
        Sfx.GOAL -> goal()
        Sfx.WIN -> win()
        Sfx.LOSS -> loss()
    }

    private fun n(sec: Double) = (sec * SR).toInt()

    private fun lp(x: FloatArray, fc: Double): FloatArray {
        val a = exp(-2 * PI * fc / SR)
        val y = FloatArray(x.size); var s = 0.0
        for (i in x.indices) { s = (1 - a) * x[i] + a * s; y[i] = s.toFloat() }
        return y
    }

    private fun reverb(x: FloatArray, mix: Double, size: Double, tailSec: Double): FloatArray {
        val xp = FloatArray(x.size + n(tailSec)); System.arraycopy(x, 0, xp, 0, x.size)
        val acc = DoubleArray(xp.size)
        val combs = listOf(0.0297 to 0.82, 0.0371 to 0.80, 0.0411 to 0.78, 0.0437 to 0.76)
        for ((d0, g) in combs) {
            val d = (SR * d0 * size).toInt()
            val y = DoubleArray(xp.size)
            for (i in d until xp.size) { y[i] = xp[i - d] + g * y[i - d]; acc[i] += y[i] }
        }
        for (i in acc.indices) acc[i] /= combs.size
        var cur = acc
        for ((d, g) in listOf((SR * 0.005).toInt() to 0.7, (SR * 0.0017).toInt() to 0.7)) {
            val y = DoubleArray(cur.size)
            for (i in cur.indices) {
                val xd = if (i >= d) cur[i - d] else 0.0
                val yd = if (i >= d) y[i - d] else 0.0
                y[i] = -g * cur[i] + xd + g * yd
            }
            cur = y
        }
        val wet = lp(FloatArray(cur.size) { cur[it].toFloat() }, 5500.0)
        return FloatArray(xp.size) { (xp[it] * (1 - mix) + wet[it] * mix * 2.2).toFloat() }
    }

    private fun fmBell(f: Double, dur: Double, idx: Double, ratio: Double, dec: Double): FloatArray {
        val len = n(dur)
        return FloatArray(len) { i ->
            val t = i.toDouble() / SR
            val im = idx * exp(-t / (dec * 0.4))
            (sin(2 * PI * f * t + im * sin(2 * PI * f * ratio * t)) * min(1.0, t / 0.002) * exp(-t / dec)).toFloat()
        }
    }

    private fun add(x: FloatArray, startSec: Double, y: FloatArray, g: Double) {
        val s = n(startSec)
        for (i in y.indices) { val j = s + i; if (j in x.indices) x[j] += (y[i] * g).toFloat() }
    }

    private fun capture(): FloatArray {
        val rng = Random(7)
        val x = FloatArray(n(0.75))
        val m = n(0.16); var s = 0.0
        for (i in 0 until m) {
            val cut = 5500.0 + (900.0 - 5500.0) * i / m
            val a = exp(-2 * PI * cut / SR)
            s = (1 - a) * rng.nextGaussian() + a * s
            val w = sin(PI * i / m).pow(1.5)
            x[i] += (s * w * 1.6).toFloat()
        }
        val k = 0.15
        val snLen = n(0.012); val sn = FloatArray(snLen) { rng.nextGaussian().toFloat() }
        val snLp = lp(sn, 2500.0)
        add(x, k, FloatArray(snLen) { ((sn[it] - snLp[it]) * exp(-it / (0.003 * SR))).toFloat() }, 1.2)
        val pl = n(0.35); var ph = 0.0
        add(x, k, FloatArray(pl) { i ->
            val t = i.toDouble() / SR
            ph += 2 * PI * (55 + 170 * exp(-t * 22)) / SR
            (sin(ph) * min(1.0, t / 0.002) * exp(-t / 0.11)).toFloat()
        }, 1.5)
        val tl = n(0.5)
        add(x, k, FloatArray(tl) { i ->
            val t = i.toDouble() / SR
            ((sin(2 * PI * 1870 * t) + 0.5 * sin(2 * PI * 2810 * t) + 0.3 * sin(2 * PI * 4120 * t)) *
                min(1.0, t / 0.001) * exp(-t / 0.14)).toFloat()
        }, 0.35)
        return reverb(x, 0.22, 0.8, 1.2)
    }

    private fun goal(): FloatArray {
        val rng = Random(7)
        val x = FloatArray(n(1.6))
        val notes = doubleArrayOf(1046.5, 1318.5, 1568.0, 1975.5, 2093.0)
        for (i in notes.indices) {
            val last = i == notes.size - 1
            add(x, i * 0.075, fmBell(notes[i], if (last) 1.1 else 0.55, 1.6, if (last) 3.5 else 2.0, if (last) 0.6 else 0.25), if (last) 1.0 else 0.7)
        }
        val padStart = 0.28; val pl = x.size - n(padStart)
        val padF = doubleArrayOf(523.25, 523.25 * 1.004, 659.25, 783.99 * 0.997)
        add(x, padStart, FloatArray(pl) { i ->
            val t = i.toDouble() / SR
            (padF.sumOf { sin(2 * PI * it * t) } * min(1.0, t / 0.12) * exp(-t / 0.55)).toFloat()
        }, 0.12)
        for (j in 0 until 9) {
            val st = 0.3 + 0.07 * j + rng.nextDouble() * 0.03
            add(x, st, fmBell(3500 + rng.nextDouble() * 3000, 0.25, 0.6, 1.5, 0.08), 0.12)
        }
        return reverb(x, 0.30, 1.1, 1.2)
    }

    private fun brass(f: Double, dur: Double, att: Double, rel: Double): FloatArray {
        val len = n(dur); var ph = 0.0
        val raw = FloatArray(len) { i ->
            val t = i.toDouble() / SR
            val vib = 1 + 0.004 * sin(2 * PI * 5.5 * t) * min(1.0, t / 0.25)
            ph += 2 * PI * f * vib / SR
            var sig = 0.0
            for (h in 1..11) sig += 0.9.pow(h) / h * sin(h * ph)
            val env = min(1.0, t / att) * (if (t < dur - rel) 1.0 else max(0.0, (dur - t) / rel))
            (sig * env).toFloat()
        }
        val f2 = lp(raw, 1800.0)
        return FloatArray(len) { i -> val t = i.toDouble() / SR; (f2[i] * (0.6 + 0.4 * min(1.0, t / (att * 2)))).toFloat() }
    }

    private fun win(): FloatArray {
        val rng = Random(11)
        val x = FloatArray(n(2.6))
        val G4 = 392.0; val C5 = 523.25; val E5 = 659.25; val G5 = 783.99
        val C4 = 261.63; val E4 = 329.63
        listOf(Triple(G4, 0.0, 0.16), Triple(C5, 0.16, 0.16), Triple(E5, 0.32, 0.16), Triple(G5, 0.48, 0.30))
            .forEach { (f, st, d) -> add(x, st, brass(f, d, 0.015, 0.06), 0.55) }
        for (f in doubleArrayOf(C4, E4, G4, C5, E5, G5)) add(x, 0.80, brass(f, 1.5, 0.04, 0.7), 0.28)
        for (f in doubleArrayOf(1046.5, 1318.5, 1568.0, 2093.0)) add(x, 0.80, fmBell(f, 1.2, 1.2, 2.0, 0.45), 0.10)
        val cl = n(1.7); val cym = FloatArray(cl) { rng.nextGaussian().toFloat() }; val cymLp = lp(cym, 4000.0)
        add(x, 0.78, FloatArray(cl) { i -> val t = i.toDouble() / SR; ((cym[i] - cymLp[i]) * min(1.0, t / 0.05) * exp(-t / 0.6)).toFloat() }, 0.18)
        for (j in 0 until 14) add(x, 0.85 + 0.06 * j + rng.nextDouble() * 0.03, fmBell(3000 + rng.nextDouble() * 3000, 0.3, 0.6, 1.5, 0.08), 0.08)
        val kl = n(0.25)
        add(x, 0.80, FloatArray(kl) { i -> val t = i.toDouble() / SR; (sin(2 * PI * (50 + 90 * exp(-t * 25)) * t) * exp(-t / 0.12)).toFloat() }, 0.6)
        return reverb(x, 0.25, 1.2, 1.5)
    }

    private fun loss(): FloatArray {
        val x = FloatArray(n(2.2))
        fun soft(f: Double, dur: Double, dec: Double) = FloatArray(n(dur)) { i ->
            val t = i.toDouble() / SR
            ((sin(2 * PI * f * t) + 0.25 * sin(4 * PI * f * t) + 0.08 * sin(6 * PI * f * t)) * min(1.0, t / 0.03) * exp(-t / dec)).toFloat()
        }
        listOf(659.25 to 0.0, 587.33 to 0.28, 523.25 to 0.56, 493.88 to 0.84).forEach { (f, st) -> add(x, st, soft(f, 0.9, 0.35), 0.45) }
        for (f in doubleArrayOf(220.0, 261.63, 329.63, 220.0 * 1.003)) {
            add(x, 0.84, FloatArray(n(1.4)) { i -> val t = i.toDouble() / SR; (sin(2 * PI * f * t) * min(1.0, t / 0.25) * exp(-t / 0.7)).toFloat() }, 0.16)
        }
        return reverb(x, 0.32, 1.3, 1.5)
    }

    private fun writeWav(file: File, data: FloatArray) {
        var peak = 1e-9f
        for (v in data) peak = max(peak, abs(v))
        val gain = 0.85f / peak
        val pcm = ByteArray(data.size * 2)
        for (i in data.indices) {
            val v = (data[i] * gain).coerceIn(-1f, 1f)
            val s = (v * 32767).toInt()
            pcm[2 * i] = (s and 0xFF).toByte()
            pcm[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        FileOutputStream(file).use { out ->
            fun i32(v: Int) = byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())
            fun i16(v: Int) = byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())
            out.write("RIFF".toByteArray()); out.write(i32(36 + pcm.size)); out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); out.write(i32(16)); out.write(i16(1)); out.write(i16(1))
            out.write(i32(SR)); out.write(i32(SR * 2)); out.write(i16(2)); out.write(i16(16))
            out.write("data".toByteArray()); out.write(i32(pcm.size)); out.write(pcm)
        }
    }
}
