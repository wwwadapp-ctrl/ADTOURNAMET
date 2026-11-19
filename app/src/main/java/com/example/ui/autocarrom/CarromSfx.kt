package com.example.ui.autocarrom

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Carrom sounds, made in code once and cached as small WAV files (no sound files in the app). */
object CarromSfx {
    enum class S { HIT, WALL, POCKET, FLICK, SHH }

    private const val SR = 44100
    @Volatile private var pool: SoundPool? = null
    @Volatile private var preparing = false
    private val ids = ConcurrentHashMap<S, Int>()
    private val last = ConcurrentHashMap<S, Long>()

    fun prepare(context: Context) {
        if (pool != null || preparing) return
        preparing = true
        val app = context.applicationContext
        Thread {
            try {
                val p = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                ).build()
                val dir = File(app.cacheDir, "carrom_sfx_v1").apply { mkdirs() }
                for (s in S.values()) {
                    val f = File(dir, s.name.lowercase() + ".wav")
                    if (!f.exists() || f.length() < 500L) writeWav(f, synth(s))
                    ids[s] = p.load(f.absolutePath, 1)
                }
                pool = p
            } catch (_: Throwable) {
            } finally { preparing = false }
        }.start()
    }

    /** [gapMs] = minimum time between two plays of the same sound. */
    fun play(s: S, volume: Float = 1f, gapMs: Long = 0L) {
        val p = pool ?: return
        val id = ids[s] ?: return
        val now = System.currentTimeMillis()
        if (gapMs > 0 && now - (last[s] ?: 0L) < gapMs) return
        last[s] = now
        val v = volume.coerceIn(0f, 1f)
        try { p.play(id, v, v, 1, 0, 1f) } catch (_: Throwable) { }
    }

    private fun n(sec: Double) = (sec * SR).toInt()

    private fun synth(s: S): FloatArray = when (s) {
        // hard wooden "tock": short bright click + small body ring
        S.HIT -> FloatArray(n(0.09)) { i ->
            val t = i.toDouble() / SR
            val f = 1500.0 * exp(-t * 18) + 650.0
            (sin(2 * PI * f * t) * exp(-t / 0.018) + 0.5 * sin(2 * PI * 2900 * t) * exp(-t / 0.006)).toFloat()
        }
        // dull thump on the wooden rail
        S.WALL -> FloatArray(n(0.12)) { i ->
            val t = i.toDouble() / SR
            (sin(2 * PI * (180 + 120 * exp(-t * 30)) * t) * exp(-t / 0.035)).toFloat()
        }
        // coin dropping into the pocket net: falling tone + soft rattle
        S.POCKET -> {
            val r = Random(5)
            FloatArray(n(0.32)) { i ->
                val t = i.toDouble() / SR
                (0.8 * sin(2 * PI * (720 * exp(-t * 6) + 160) * t) * exp(-t / 0.12) +
                    0.25 * r.nextDouble(-1.0, 1.0) * exp(-t / 0.05)).toFloat()
            }
        }
        // finger flick on the striker
        S.FLICK -> FloatArray(n(0.06)) { i ->
            val t = i.toDouble() / SR
            (sin(2 * PI * (2200 * exp(-t * 40) + 700) * t) * exp(-t / 0.012)).toFloat()
        }
        // powder sprinkle: soft high noise
        S.SHH -> {
            val r = Random(11); var prev = 0.0
            FloatArray(n(0.5)) { i ->
                val t = i.toDouble() / SR
                val w = r.nextDouble(-1.0, 1.0); val hp = w - prev; prev = w
                (hp * 0.6 * (1 - t / 0.5) * kotlin.math.min(1.0, t / 0.03)).toFloat()
            }
        }
    }

    private fun writeWav(file: File, data: FloatArray) {
        var peak = 1e-9f
        for (v in data) peak = max(peak, abs(v))
        val gain = 0.85f / peak
        val pcm = ByteArray(data.size * 2)
        for (i in data.indices) {
            val v = (data[i] * gain).coerceIn(-1f, 1f)
            val s = (v * 32767).toInt()
            pcm[2 * i] = (s and 0xFF).toByte(); pcm[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
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
