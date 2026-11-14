package com.example.ui.home

import android.os.Build
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.absoluteValue

// Home page part 3 artwork: the guide banner slider. VISUAL ONLY:
// the caller keeps the pictures (from Admin), the auto slide, the click actions and the test tags.

/** Gold frame with a light running around it, glass shine, floating glow, a gold badge ribbon and the press animation. */
@Composable
fun AdBannerFrame(
    badgeText: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val anim = rememberInfiniteTransition(label = "bannerFrame")
    val spin by anim.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "bannerSpin"
    )
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(19.dp)
    Box(modifier = modifier.adPressScale(press, 0.97f).padding(top = 12.dp)) {
        // floating gold glow under the card
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color(0x61FFC85A), Color(0x00FFC85A)),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.width * 0.42f
                        ),
                        topLeft = Offset(size.width * 0.12f, size.height - 14.dp.toPx()),
                        size = Size(size.width * 0.76f, 28.dp.toPx())
                    )
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp)
                .shadow(14.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
                .clip(outer)
                .drawBehind {
                    // gold border with one bright light running around it
                    drawRect(Brush.linearGradient(listOf(Color(0xFFFFE07A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFF8A5A08))))
                    rotate(spin) {
                        drawCircle(
                            Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.08f to Color(0xFFFFD45A),
                                0.13f to Color.White,
                                0.18f to Color(0xFFFFD45A),
                                0.28f to Color.Transparent,
                                1f to Color.Transparent,
                                center = center
                            ),
                            radius = size.maxDimension
                        )
                    }
                }
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(2.5.dp)
                .clip(RoundedCornerShape(16.5.dp))
                .background(Color(0xFF2A1A02))
                .padding(1.5.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF0B1040))
                .drawWithContent {
                    drawContent()
                    // glass shine on top
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0f)),
                            startY = 0f, endY = size.height * 0.38f
                        ),
                        size = Size(size.width, size.height * 0.38f)
                    )
                    // diagonal light streak
                    val x = size.width * 0.48f
                    rotate(24f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color(0x00FFFFFF), Color(0x3DFFFFFF), Color(0x00FFFFFF)),
                                startX = x - 20.dp.toPx(), endX = x + 20.dp.toPx()
                            ),
                            topLeft = Offset(x - 20.dp.toPx(), -size.height),
                            size = Size(40.dp.toPx(), size.height * 3f)
                        )
                    }
                    // soft inner shadow at the edges
                    drawRoundRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Transparent, Color(0x59000000)),
                            center = center,
                            radius = size.maxDimension * 0.62f
                        ),
                        cornerRadius = CornerRadius(15.dp.toPx())
                    )
                }
        ) {
            content()
        }
        // gold badge ribbon on the top-left edge
        if (badgeText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (-11).dp)
                    .shadow(6.dp, RoundedCornerShape(7.dp), ambientColor = Color.Black, spotColor = Color(0xFFFFC850))
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE98A), Color(0xFFE09A18))))
                    .border(1.dp, Color(0xFFFFF8D8), RoundedCornerShape(7.dp))
                    .padding(start = 7.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .drawBehind {
                            drawPath(
                                Path().apply {
                                    moveTo(size.width * 0.18f, size.height * 0.08f)
                                    lineTo(size.width * 0.95f, size.height * 0.5f)
                                    lineTo(size.width * 0.18f, size.height * 0.92f)
                                    close()
                                },
                                Color(0xFF3A2200)
                            )
                        }
                )
                Text(
                    text = badgeText,
                    style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A2200)),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The Admin's banner picture shown WHOLE (never cut). Any empty space around it is filled
 * with a blurred, darker copy of the same picture (plain darker copy on Android 11 and older).
 */
@Composable
fun AdBannerPicture(
    bitmap: ImageBitmap?,
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier = modifier.fillMaxSize()) {
        val backdrop = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f; alpha = 0.55f }
            .then(if (canBlur) Modifier.blur(16.dp) else Modifier)
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = backdrop)
            Image(bitmap = bitmap, contentDescription = contentDescription, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = backdrop)
            AsyncImage(model = url, contentDescription = contentDescription, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Side banners turn away in 3D, get a little smaller and dimmer. [pageOffset] = distance from the centre page. */
fun Modifier.adBannerPageTransform(pageOffset: Float): Modifier = graphicsLayer {
    val d = pageOffset.coerceIn(-1f, 1f)
    val a = d.absoluteValue
    rotationY = d * 26f
    val s = 1f - 0.1f * a
    scaleX = s
    scaleY = s
    alpha = 1f - 0.4f * a
    cameraDistance = 14f * density
}

/** Dots: the current one is a gold bar that fills up like a timer; the others are small glass dots with a gold rim. */
@Composable
fun AdBannerDots(
    count: Int,
    current: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            if (index == current) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.5.dp)
                        .size(width = 34.dp, height = 7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x40FFD45A))
                        .border(1.dp, Color(0xB3FFD45A), RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0.08f, 1f))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFD45A), Color(0xFFE09A18))))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.5.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color(0x8CFFD45A), CircleShape)
                )
            }
        }
    }
}
