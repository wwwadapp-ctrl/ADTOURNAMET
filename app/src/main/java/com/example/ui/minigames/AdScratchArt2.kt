package com.example.ui.minigames

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlin.random.Random

// Scratch card game artwork, part 2 (curtains, buttons, legend, host). VISUAL ONLY.

private fun Dp.sp(): TextUnit = TextUnit(this.value, TextUnitType.Sp)

/** Picture for a multiplier: 5 = crown, 2 = diamond, 1 = coin. */
internal fun symbolRes(m: Int): Int = when (m) { 5 -> R.drawable.sc_crown; 2 -> R.drawable.sc_diamond; else -> R.drawable.mg_coin }

/** Two purple velvet curtains with gold valance, fringe and tie balls. [open] 0 = closed, 1 = gathered at the sides. */
@Composable
internal fun ScCurtains(u: Dp, open: Float, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(u * 2))) {
        val w = size.width; val h = size.height
        val pleat = 6.4f * u.toPx()
        val velvet = Brush.horizontalGradient(
            0f to Color(0xFF3A0A6A), 0.34f to Color(0xFF3A0A6A), 0.56f to Color(0xFF7A2AC8), 0.78f to Color(0xFF4A1290), 1f to Color(0xFF2A0650),
            startX = 0f, endX = pleat, tileMode = TileMode.Repeated)
        val cw = w * 0.51f * (1f - 0.83f * open)
        for (side in 0..1) {
            val left = if (side == 0) 0f else w - cw
            clipRect(left, 0f, left + cw, h) {
                // squeeze the pleats when gathered
                withTransform({ scale(1f - 0.83f * open, 1f, Offset(if (side == 0) 0f else w, 0f)) }) {
                    val fl = if (side == 0) 0f else w - w * 0.51f
                    drawRect(velvet, Offset(fl, 0f), Size(w * 0.51f, h))
                }
                drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x73000000)), Offset(left, 0f), Size(cw, h))
                // gold valance with scallops
                val vh = h * 0.12f
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE7A0), Color(0xFFC98A10), Color(0xFF7A4A06)), 0f, vh), Offset(left, 0f), Size(cw, vh))
                val sr = w * 0.02f
                var x = left + sr
                while (x < left + cw + sr) { drawCircle(Color(0xFFC98A10), sr, Offset(x, vh)); x += sr * 2 }
                drawLine(Color(0xFFFFD45A), Offset(left, vh), Offset(left + cw, vh), strokeWidth = u.toPx() * 0.5f)
                // fringe
                val fh = h * 0.06f
                drawRect(Brush.horizontalGradient(0f to Color(0xFFFFD45A), 0.5f to Color(0xFFFFD45A), 0.5f to Color(0xFFB07A10), 1f to Color(0xFFB07A10),
                    startX = 0f, endX = u.toPx() * 1.2f, tileMode = TileMode.Repeated), Offset(left, h - fh), Size(cw, fh))
            }
            // tie ball on the inner edge
            val tx = if (side == 0) cw else w - cw
            val tr = u.toPx() * 1.8f
            drawCircle(Color(0xFFFFD45A).copy(alpha = 0.35f), tr * 1.6f, Offset(tx, h * 0.47f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF7A4A06)), Offset(tx - tr * 0.3f, h * 0.47f - tr * 0.3f), tr * 1.2f), tr, Offset(tx, h * 0.47f))
        }
    }
}

/** Purple "reveal all" button under the ticket. */
@Composable
fun ScRevealButton(geo: WhGeo, u: Dp, text: String, onClick: () -> Unit) {
    Box(Modifier.offset(y = geo.y(876f)).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(text, fontSize = (u * 3.6f).sp(), fontWeight = FontWeight.Black, color = Color.White, maxLines = 1,
            modifier = Modifier.shadow(u * 2, RoundedCornerShape(u * 4), ambientColor = Color(0xFF9B4DFF), spotColor = Color(0xFF9B4DFF))
                .clip(RoundedCornerShape(u * 4)).background(Brush.verticalGradient(listOf(Color(0xFF9B4DFF), Color(0xFF5A1AB0))))
                .border(u * 0.4f, Color(0xFFFFD45A), RoundedCornerShape(u * 4))
                .clickable(onClick = onClick).padding(horizontal = u * 4, vertical = u * 1).testTag("scratch_reveal_all"))
    }
}

/** Prize legend: 3 crowns = 5x, 3 diamonds = 2x, 3 coins = 1x. */
@Composable
fun ScLegend(geo: WhGeo, u: Dp, label: (Int) -> String) {
    val shape = RoundedCornerShape(u * 4)
    Row(
        Modifier.offset(y = geo.y(951.5f)).padding(horizontal = u * 8).fillMaxWidth()
            .shadow(u * 1.5f, shape, ambientColor = Color(0xFF9B4DFF), spotColor = Color(0xFF9B4DFF)).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xD92A145A), Color(0xE6100830)))).border(u * 0.4f, Color(0xB3FFD45A), shape)
            .padding(horizontal = u * 2, vertical = u * 1),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(5, 2, 1).forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(u * 10.4f, u * 4.4f)) {
                    repeat(3) { Image(painterResource(symbolRes(m)), null, Modifier.offset(x = u * 3f * it).size(u * 4.4f)) }
                }
                Spacer(Modifier.width(u * 0.8f))
                Text("= " + label(m), fontSize = (u * 3f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Gold tag above the ticket with the result (e.g. "৫X উইন"). */
@Composable
fun ScResultTag(geo: WhGeo, u: Dp, text: String) {
    Box(Modifier.offset(y = geo.y(541.8f)).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(text, fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.shadow(u * 2, RoundedCornerShape(u * 4), ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A))
                .clip(RoundedCornerShape(u * 4)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))))
                .padding(horizontal = u * 3.4f, vertical = u * 0.6f).testTag("scratch_result_tag"))
    }
}

/** Host in the purple dress (Android 9+ animated, older phones show the first frame). */
@Composable
fun ScHost(geo: WhGeo, clap: Boolean) {
    val w = geo.k * 384f
    Box(Modifier.offset(x = geo.x(514f), y = geo.y(553f)).size(w, w * (737f / 400f))) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { v ->
                val res = if (clap) R.raw.sc_host_clap else R.raw.sc_host_idle
                if (v.tag != res) {
                    v.tag = res
                    if (Build.VERSION.SDK_INT >= 28) {
                        try {
                            val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(v.resources, res))
                            if (d is AnimatedImageDrawable) {
                                d.repeatCount = if (clap) 0 else AnimatedImageDrawable.REPEAT_INFINITE
                                v.setImageDrawable(d); d.start()
                            } else v.setImageDrawable(d)
                        } catch (_: Throwable) { }
                    } else {
                        try { v.setImageBitmap(BitmapFactory.decodeStream(v.resources.openRawResource(res))) } catch (_: Throwable) { }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
