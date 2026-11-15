package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.NeonAmber
import kotlin.math.PI
import kotlin.math.sin

internal val TutorialViolet = Color(0xFFA855F7)

/**
 * Renders live, 60fps vector gesture cues & neon motion halos anchored directly
 * onto real spotlighted UI components inside the app overlay.
 */
@Composable
fun TutorialSpotlightMotionCue(
    gestureType: TutorialGestureType,
    targetRect: Rect?,
    accentColor: Color,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "spotlight_cue_${gestureType.name}")
    val cycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cue_cycle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cue_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (gestureType) {
            TutorialGestureType.HORIZONTAL_DOCK_SWIPE -> {
                val rect = targetRect ?: Rect(
                    left = size.width * 0.08f,
                    top = size.height * 0.86f,
                    right = size.width * 0.92f,
                    bottom = size.height * 0.95f
                )
                drawHorizontalDockSwipeCue(rect, cycle, pulse, accentColor)
            }

            TutorialGestureType.SINGLE_FINGER_PULL_UP -> {
                val rect = targetRect ?: Rect(
                    left = size.width * 0.12f,
                    top = size.height * 0.45f,
                    right = size.width * 0.88f,
                    bottom = size.height * 0.72f
                )
                drawSingleFingerPullUpCue(rect, cycle, pulse, accentColor)
            }

            TutorialGestureType.BUDGET_PULSE -> {
                val rect = targetRect ?: Rect(
                    left = size.width * 0.08f,
                    top = size.height * 0.16f,
                    right = size.width * 0.92f,
                    bottom = size.height * 0.34f
                )
                drawBudgetPulseCue(rect, cycle, pulse, accentColor)
            }

            TutorialGestureType.TWO_FINGER_VERTICAL_SWIPE -> {
                drawTwoFingerFullScreenCue(cycle, pulse, accentColor)
            }

            TutorialGestureType.SETTINGS_POINTER -> {
                val defaultLeft = if (isArabic) size.width * 0.06f else size.width * 0.68f
                val rect = targetRect ?: Rect(
                    left = defaultLeft,
                    top = size.height * 0.86f,
                    right = defaultLeft + size.width * 0.26f,
                    bottom = size.height * 0.95f
                )
                drawSettingsPointerCue(rect, cycle, pulse, accentColor)
            }

            TutorialGestureType.FEATURE_SPOTLIGHT_PULSE -> {
                val rect = targetRect ?: Rect(
                    left = size.width * 0.15f,
                    top = size.height * 0.35f,
                    right = size.width * 0.85f,
                    bottom = size.height * 0.52f
                )
                drawGenericFeaturePulse(rect, pulse, accentColor)
            }
        }
    }
}

private fun DrawScope.drawHorizontalDockSwipeCue(
    rect: Rect,
    cycle: Float,
    pulse: Float,
    accentColor: Color
) {
    // Oscillating horizontal finger travel across dock bar
    val travelFrac = ((sin(cycle * 2f * PI - PI / 2.0) + 1.0) / 2.0).toFloat()
    val marginX = minOf(rect.width * 0.18f, 52.dp.toPx())
    val startX = rect.left + marginX
    val endX = rect.right - marginX
    val fingerX = startX + (endX - startX) * travelFrac
    val trackY = (rect.top - 22.dp.toPx()).coerceAtLeast(36.dp.toPx())

    // Glowing horizontal swipe rail above spotlighted dock
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, accentColor.copy(alpha = 0.85f), EmeraldSavings.copy(alpha = 0.85f), Color.Transparent),
            startX = startX,
            endX = endX
        ),
        start = Offset(startX, trackY),
        end = Offset(endX, trackY),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Left and right directional chevrons along rail
    val chevronSize = 7.dp.toPx()
    drawPath(
        path = Path().apply {
            moveTo(startX + chevronSize, trackY - chevronSize * 0.7f)
            lineTo(startX, trackY)
            lineTo(startX + chevronSize, trackY + chevronSize * 0.7f)
        },
        color = accentColor.copy(alpha = 0.75f + 0.25f * pulse),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = Path().apply {
            moveTo(endX - chevronSize, trackY - chevronSize * 0.7f)
            lineTo(endX, trackY)
            lineTo(endX - chevronSize, trackY + chevronSize * 0.7f)
        },
        color = EmeraldSavings.copy(alpha = 0.75f + 0.25f * pulse),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Glowing touch orb
    drawTouchOrb(center = Offset(fingerX, trackY), color = accentColor, pulse = pulse)
}

private fun DrawScope.drawSingleFingerPullUpCue(
    rect: Rect,
    cycle: Float,
    pulse: Float,
    accentColor: Color
) {
    val cx = rect.center.x
    // Swipe path sits directly inside/over the lower-to-upper span of the Empty card (or just below Add button)
    val isTallCard = rect.height > 120.dp.toPx()
    val startY = if (isTallCard) rect.bottom - 26.dp.toPx() else rect.bottom + 78.dp.toPx()
    val endY = if (isTallCard) rect.top + rect.height * 0.24f else rect.bottom + 14.dp.toPx()

    val moveT = (cycle / 0.65f).coerceIn(0f, 1f)
    val fadeT = if (cycle > 0.72f) (1f - ((cycle - 0.72f) / 0.28f)).coerceIn(0f, 1f) else 1f
    val currentY = startY + (endY - startY) * moveT

    // Vertical luminous swipe trail
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = 0.92f * fadeT), Color.Transparent),
            startY = currentY,
            endY = startY
        ),
        start = Offset(cx, currentY),
        end = Offset(cx, startY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Triple cascading upward chevron arrows showing straight-up direction
    for (i in 0 until 3) {
        val chevronY = endY + i * 16.dp.toPx() - pulse * 4.dp.toPx()
        val cAlpha = ((1f - i * 0.25f) * (0.65f + 0.35f * pulse)).coerceIn(0f, 1f)
        val arm = 8.dp.toPx()
        drawPath(
            path = Path().apply {
                moveTo(cx - arm, chevronY + arm * 0.65f)
                lineTo(cx, chevronY)
                lineTo(cx + arm, chevronY + arm * 0.65f)
            },
            color = accentColor.copy(alpha = cAlpha),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    if (fadeT > 0.05f) {
        drawTouchOrb(
            center = Offset(cx, currentY),
            color = EmeraldSavings.copy(alpha = fadeT),
            pulse = pulse
        )
    }
}

private fun DrawScope.drawBudgetPulseCue(
    rect: Rect,
    cycle: Float,
    pulse: Float,
    accentColor: Color
) {
    // Expanding sonar glow around the live Supermarket Budget HUD
    val inflate = (cycle * 14.dp.toPx())
    val ringAlpha = ((1f - cycle) * 0.65f).coerceIn(0f, 1f)
    drawRoundRect(
        color = accentColor.copy(alpha = ringAlpha),
        topLeft = Offset(rect.left - inflate, rect.top - inflate),
        size = Size(rect.width + inflate * 2f, rect.height + inflate * 2f),
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(width = 2.4.dp.toPx())
    )

    // Crisp corner brackets highlighting live numbers
    val bracketLen = 18.dp.toPx()
    val strokeW = 3.dp.toPx()
    val pad = 4.dp.toPx()
    val cColor = Color.LerpCompat(accentColor, EmeraldSavings, pulse)
    val l = rect.left - pad
    val t = rect.top - pad
    val r = rect.right + pad
    val b = rect.bottom + pad

    val brackets = Path().apply {
        // Top-left
        moveTo(l, t + bracketLen); lineTo(l, t); lineTo(l + bracketLen, t)
        // Top-right
        moveTo(r - bracketLen, t); lineTo(r, t); lineTo(r, t + bracketLen)
        // Bottom-left
        moveTo(l, b - bracketLen); lineTo(l, b); lineTo(l + bracketLen, b)
        // Bottom-right
        moveTo(r - bracketLen, b); lineTo(r, b); lineTo(r, b - bracketLen)
    }
    drawPath(
        path = brackets,
        color = cColor,
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawTwoFingerFullScreenCue(
    cycle: Float,
    pulse: Float,
    accentColor: Color
) {
    val cx = size.width / 2f
    val zoneTop = size.height * 0.16f
    val zoneBottom = size.height * 0.38f

    // 0.0..0.42: two fingers swipe UP (Full screen ON)
    // 0.50..0.92: two fingers swipe DOWN (Full screen OFF)
    val isSwipingUp = cycle < 0.48f
    val frac = if (isSwipingUp) {
        (cycle / 0.42f).coerceIn(0f, 1f)
    } else {
        ((cycle - 0.50f) / 0.42f).coerceIn(0f, 1f)
    }
    val fingerY = if (isSwipingUp) {
        zoneBottom + (zoneTop - zoneBottom) * frac
    } else {
        zoneTop + (zoneBottom - zoneTop) * frac
    }
    val trailStartY = if (isSwipingUp) zoneBottom else zoneTop
    val gestureColor = if (isSwipingUp) EmeraldSavings else NeonAmber
    val spacing = 28.dp.toPx()

    // Top status-bar ghost indicator showing edge expansion
    val barAlpha = if (isSwipingUp) (1f - frac * 1.4f).coerceIn(0f, 1f) else (frac * 1.4f).coerceIn(0f, 1f)
    drawRoundRect(
        color = ElectricCyan.copy(alpha = 0.72f * barAlpha),
        topLeft = Offset(cx - 44.dp.toPx(), 18.dp.toPx()),
        size = Size(88.dp.toPx(), 5.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Synchronized two-finger touch trails and orbs
    listOf(cx - spacing, cx + spacing).forEach { fx ->
        drawLine(
            brush = Brush.verticalGradient(
                colors = if (isSwipingUp) {
                    listOf(gestureColor.copy(alpha = 0.88f), Color.Transparent)
                } else {
                    listOf(Color.Transparent, gestureColor.copy(alpha = 0.88f))
                },
                startY = minOf(fingerY, trailStartY),
                endY = maxOf(fingerY, trailStartY)
            ),
            start = Offset(fx, fingerY),
            end = Offset(fx, trailStartY),
            strokeWidth = 4.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawTouchOrb(center = Offset(fx, fingerY), color = gestureColor, pulse = pulse)
    }

    // Center directional chevron indicating UP or DOWN
    val midX = cx
    val chevY = if (isSwipingUp) zoneTop - 16.dp.toPx() else zoneBottom + 16.dp.toPx()
    val dir = if (isSwipingUp) 1f else -1f
    val arm = 8.dp.toPx()
    drawPath(
        path = Path().apply {
            moveTo(midX - arm, chevY + dir * arm * 0.7f)
            lineTo(midX, chevY)
            lineTo(midX + arm, chevY + dir * arm * 0.7f)
        },
        color = accentColor.copy(alpha = 0.85f),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSettingsPointerCue(
    rect: Rect,
    cycle: Float,
    pulse: Float,
    accentColor: Color
) {
    val targetCenter = Offset(rect.center.x, rect.top - 10.dp.toPx())
    val bounceOffset = (-12.dp.toPx() * (1f - pulse))
    val pointerPos = Offset(targetCenter.x, (targetCenter.y - 16.dp.toPx() + bounceOffset).coerceAtLeast(32.dp.toPx()))

    // Expanding ripple halo on the Settings tab itself
    val rippleR = 14.dp.toPx() + cycle * 24.dp.toPx()
    drawCircle(
        color = accentColor.copy(alpha = ((1f - cycle) * 0.55f).coerceIn(0f, 1f)),
        radius = rippleR,
        center = rect.center,
        style = Stroke(width = 2.dp.toPx())
    )

    // Vertical downward pointer arrow aimed straight at Settings tab
    drawLine(
        brush = Brush.verticalGradient(listOf(ElectricCyan, accentColor)),
        start = Offset(pointerPos.x, pointerPos.y - 26.dp.toPx()),
        end = pointerPos,
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    val arm = 8.dp.toPx()
    drawPath(
        path = Path().apply {
            moveTo(pointerPos.x - arm, pointerPos.y - arm * 0.8f)
            lineTo(pointerPos.x, pointerPos.y)
            lineTo(pointerPos.x + arm, pointerPos.y - arm * 0.8f)
        },
        color = accentColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawGenericFeaturePulse(
    rect: Rect,
    pulse: Float,
    accentColor: Color
) {
    val pad = (6.dp + (pulse * 8f).dp).toPx()
    drawRoundRect(
        color = accentColor.copy(alpha = 0.65f - 0.35f * pulse),
        topLeft = Offset(rect.left - pad, rect.top - pad),
        size = Size(rect.width + pad * 2f, rect.height + pad * 2f),
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), pulse * 40f)
        )
    )
}

private fun DrawScope.drawTouchOrb(
    center: Offset,
    color: Color,
    pulse: Float
) {
    drawCircle(
        color = color.copy(alpha = 0.24f + 0.14f * pulse),
        radius = 18.dp.toPx(),
        center = center
    )
    drawCircle(
        color = color.copy(alpha = 0.62f),
        radius = 11.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 6.dp.toPx(),
        center = center
    )
}

private fun Color.Companion.LerpCompat(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}
