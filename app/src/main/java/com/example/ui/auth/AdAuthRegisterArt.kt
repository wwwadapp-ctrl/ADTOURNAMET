package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.example.core.i18n.AppStrings
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AD premium sign-up extras. VISUAL ONLY: never validates, registers or navigates.

@Composable
fun AdAuthRegisterHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFF9A6400))))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF3A4270), Color(0xFF141838)),
                            center = Offset(30f * 2.5f, 23f * 2.5f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0f))))
                )
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFFFFC94A),
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                shadow = Shadow(color = Color(0xFF8A6414), offset = Offset(0f, 5f), blurRadius = 0f)
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD27A),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        )
    }
}

@Composable
fun AdAuthHelperText(text: String) {
    Text(
        text = text,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF2A93B), lineHeight = 19.sp),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun AdAuthTermsCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    highlight: String,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onHighlightClick: (() -> Unit)? = null
) {
    var opened by rememberSaveable { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .zIndex(1f)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
                    .border(1.8.dp, if (checked) Color(0xFFE8B23A) else Color(0xFF8C7A55), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFFFC94A), modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        val start = if (highlight.isNotBlank()) text.indexOf(highlight) else -1
        val highlightStyle = SpanStyle(color = Color(0xFFF2A93B), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    if (start >= 0) {
                        append(text.substring(0, start))
                        if (onHighlightClick != null) {
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = "fair_play_rules",
                                    styles = TextLinkStyles(style = highlightStyle.copy(textDecoration = TextDecoration.Underline))
                                ) {
                                    opened = true
                                    onHighlightClick()
                                }
                            ) {
                                append(highlight)
                            }
                        } else {
                            withStyle(highlightStyle) { append(highlight) }
                        }
                        append(text.substring(start + highlight.length))
                    } else {
                        append(text)
                    }
                },
                style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE6ECFF), lineHeight = 21.sp),
                onTextLayout = { layout = it }
            )

            // little golden hand tapping toward the rules link, until the player has opened it once
            val l = layout
            if (onHighlightClick != null && start >= 0 && !opened && l != null) {
                val mid = (start + highlight.length / 2).coerceIn(0, l.layoutInput.text.length - 1)
                val box = l.getBoundingBox(mid)
                val anim = rememberInfiniteTransition(label = "rulesHand")
                val tap by anim.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "rulesHandTap"
                )
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (box.center.x - 15.dp.toPx()).roundToInt(),
                                (box.bottom - 4.dp.toPx() + tap * 6.dp.toPx()).roundToInt()
                            )
                        }
                        .size(30.dp)
                        .graphicsLayer {
                            val s = 1f - 0.08f * (1f - tap)
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFFFFC107).copy(alpha = 0.45f * (1f - tap) + 0.1f), Color.Transparent)),
                                CircleShape
                            )
                    )
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFFFFD45A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/** Shows sign-up errors in the player's language. Display only: the checks themselves are unchanged. */
fun adRegisterErrorText(raw: String, strings: AppStrings): String = when {
    raw.contains("Full name cannot be empty", ignoreCase = true) -> strings.nameRequiredError
    raw.contains("Full name must be at least 2", ignoreCase = true) -> strings.authNameTooShortError
    raw.contains("Full name cannot exceed", ignoreCase = true) -> strings.authNameTooLongError
    raw.contains("letters, spaces", ignoreCase = true) || raw.equals("Invalid name", ignoreCase = true) -> strings.authNameInvalidError
    raw.contains("Mobile number cannot be empty", ignoreCase = true) -> strings.phoneEmptyError
    raw.contains("valid 11-digit", ignoreCase = true) || raw.contains("Invalid mobile number", ignoreCase = true) -> strings.invalidPhoneError
    raw.contains("Password cannot be empty", ignoreCase = true) -> strings.passwordEmptyError
    raw.contains("at least 6 characters", ignoreCase = true) || raw.equals("Invalid password", ignoreCase = true) -> strings.passwordMinLengthError
    raw.contains("must not exceed 64", ignoreCase = true) -> strings.authPasswordTooLongError
    raw.contains("only of whitespace", ignoreCase = true) -> strings.authPasswordWhitespaceError
    raw.contains("already registered", ignoreCase = true) -> strings.authPhoneAlreadyRegisteredError
    raw.contains("Network", ignoreCase = true) || raw.contains("timed out", ignoreCase = true) -> strings.offlineMode
    raw.contains("Account creation failed", ignoreCase = true) ||
        raw.contains("Failed to complete account profile", ignoreCase = true) ||
        raw.contains("Invalid registration credentials", ignoreCase = true) ||
        raw.contains("Invalid credentials during registration", ignoreCase = true) -> strings.authRegisterFailedError
    else -> raw
}
