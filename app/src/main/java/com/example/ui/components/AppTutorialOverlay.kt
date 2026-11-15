package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.NeonAmber

@Composable
fun AppTutorialOverlay(
    isArabic: Boolean,
    isDarkMode: Boolean,
    seenStepIds: Set<String> = emptySet(),
    steps: List<TutorialFeatureStep> = AppTutorialRegistry.steps,
    onFocusScreen: (TutorialScreenTarget) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    BackHandler(onBack = onClose)

    val initialStepIndex = remember(steps, seenStepIds) {
        if (seenStepIds.isNotEmpty()) {
            val firstUnseen = steps.indexOfFirst { it.id !in seenStepIds }
            if (firstUnseen >= 0) firstUnseen else 0
        } else {
            0
        }
    }

    var currentIndex by remember { mutableIntStateOf(initialStepIndex.coerceIn(0, steps.lastIndex)) }
    val currentStep = steps[currentIndex.coerceIn(0, steps.lastIndex)]
    val isNewStep = seenStepIds.isNotEmpty() && currentStep.id !in seenStepIds

    // Sync background live app screen whenever tutorial step changes
    LaunchedEffect(currentStep.id) {
        onFocusScreen(currentStep.targetScreen)
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val targetRegistry = LocalTutorialTargetRegistry.current

    var overlayOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    val rawTargetRectInRoot = targetRegistry?.resolveFirst(currentStep.targetKeys)
    val localTargetRect: Rect? = remember(rawTargetRectInRoot, overlayOriginInRoot, currentStep.spotlightPaddingDp) {
        rawTargetRectInRoot?.let { r ->
            val padPx = with(density) { currentStep.spotlightPaddingDp.dp.toPx() }
            Rect(
                left = (r.left - overlayOriginInRoot.x - padPx).coerceAtLeast(4f),
                top = (r.top - overlayOriginInRoot.y - padPx).coerceAtLeast(4f),
                right = r.right - overlayOriginInRoot.x + padPx,
                bottom = r.bottom - overlayOriginInRoot.y + padPx
            )
        }
    }

    val animatedAccent by animateColorAsState(
        targetValue = currentStep.accentColor,
        animationSpec = tween(durationMillis = 360),
        label = "coachmark_accent"
    )

    val infiniteHalo = rememberInfiniteTransition(label = "coachmark_halo")
    val haloPulse by infiniteHalo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // Spring-interpolated spotlight cutout rect so spotlight glides seamlessly from widget to widget
    val spotlightSpring = remember {
        spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    }
    val hasSpotlightTarget = localTargetRect != null
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (hasSpotlightTarget) 1f else 0f,
        animationSpec = tween(260),
        label = "spotlight_alpha"
    )
    val fallbackRect = Rect(120f, 300f, 420f, 440f)
    val goalRect = localTargetRect ?: fallbackRect
    val animLeft by animateFloatAsState(goalRect.left, spotlightSpring, label = "sp_left")
    val animTop by animateFloatAsState(goalRect.top, spotlightSpring, label = "sp_top")
    val animRight by animateFloatAsState(goalRect.right, spotlightSpring, label = "sp_right")
    val animBottom by animateFloatAsState(goalRect.bottom, spotlightSpring, label = "sp_bottom")
    val animRadiusPx by animateFloatAsState(
        targetValue = with(density) { currentStep.spotlightCornerRadiusDp.dp.toPx() },
        animationSpec = spotlightSpring,
        label = "sp_radius"
    )
    val animatedRect = if (hasSpotlightTarget) {
        Rect(animLeft, animTop, animRight.coerceAtLeast(animLeft + 8f), animBottom.coerceAtLeast(animTop + 8f))
    } else {
        null
    }

    val scrimColor = if (isDarkMode) Color(0xC6050A14) else Color(0xBD08101E)
    val cardBg = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xF5131F36), Color(0xF50B1324)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xFAF1F5F9)))
    }
    val titleColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val bodyColor = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val mutedColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInRoot = coords.positionInRoot()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("app_tutorial_overlay")
    ) {
        val availableHeight = maxHeight
        val overlayHeightPx = with(density) { availableHeight.toPx() }

        // 1. Live Dimming Scrim with Smooth Rounded Spotlight Cutout (reveals live underlying UI!)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawRect(color = scrimColor)

            if (animatedRect != null && spotlightAlpha > 0.02f) {
                val r = CornerRadius(animRadiusPx, animRadiusPx)
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(animatedRect.left, animatedRect.top),
                    size = Size(animatedRect.width, animatedRect.height),
                    cornerRadius = r,
                    blendMode = BlendMode.Clear
                )

                // Outer pulsing halo glow around spotlight
                val pulseExpand = (3.dp + (haloPulse * 4f).dp).toPx()
                drawRoundRect(
                    color = animatedAccent.copy(alpha = (0.34f - 0.14f * haloPulse) * spotlightAlpha),
                    topLeft = Offset(animatedRect.left - pulseExpand, animatedRect.top - pulseExpand),
                    size = Size(animatedRect.width + pulseExpand * 2f, animatedRect.height + pulseExpand * 2f),
                    cornerRadius = CornerRadius(animRadiusPx + pulseExpand, animRadiusPx + pulseExpand),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Crisp gradient rim around spotlight hole
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(animatedAccent.copy(alpha = spotlightAlpha), EmeraldSavings.copy(alpha = spotlightAlpha))
                    ),
                    topLeft = Offset(animatedRect.left, animatedRect.top),
                    size = Size(animatedRect.width, animatedRect.height),
                    cornerRadius = r,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 2. Interactive Animated Vector Gesture Cues anchored directly to the Spotlight Target
        TutorialSpotlightMotionCue(
            gestureType = currentStep.gestureType,
            targetRect = animatedRect,
            accentColor = animatedAccent,
            isArabic = isArabic,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Smart Floating Coachmark Card positioned so it never covers the highlighted UI element
        val placeCardAbove = when {
            animatedRect == null -> false // Two-finger full-screen zone sits in upper half; place card lower
            animatedRect.center.y > overlayHeightPx * 0.50f -> true
            else -> false
        }

        val clearanceFromSpotlight = if (animatedRect != null) {
            with(density) {
                if (placeCardAbove) {
                    (overlayHeightPx - animatedRect.top).coerceAtLeast(0f).toDp() + 26.dp
                } else {
                    animatedRect.bottom.coerceAtLeast(0f).toDp() + 20.dp
                }
            }
        } else {
            0.dp
        }

        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = if (placeCardAbove || animatedRect == null) {
                    Alignment.BottomCenter
                } else {
                    Alignment.TopCenter
                }
            ) {
                val maxCardHeight = (availableHeight * 0.50f).coerceIn(220.dp, 360.dp)
                val safeCardModifier = when {
                    animatedRect == null -> Modifier.padding(bottom = 28.dp)
                    placeCardAbove -> Modifier.padding(bottom = clearanceFromSpotlight.coerceAtMost(availableHeight * 0.42f))
                    else -> Modifier.padding(top = clearanceFromSpotlight.coerceAtMost(availableHeight * 0.45f))
                }

                Column(
                    modifier = safeCardModifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .heightIn(max = maxCardHeight)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = animatedAccent.copy(alpha = 0.4f),
                            spotColor = animatedAccent.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(cardBg)
                        .border(
                            BorderStroke(
                                1.3.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        animatedAccent.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = if (isDarkMode) 0.22f else 0.85f),
                                        EmeraldSavings.copy(alpha = 0.55f)
                                    )
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Top Row: Step Counter + Badge + Optional "NEW" Pill + Skip Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(animatedAccent.copy(alpha = 0.16f))
                                    .border(BorderStroke(1.dp, animatedAccent.copy(alpha = 0.45f)), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = animatedAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${currentIndex + 1} / ${steps.size}",
                                    color = animatedAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (isNewStep) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(NeonAmber.copy(alpha = 0.18f))
                                        .border(BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)), RoundedCornerShape(50))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                        .testTag("tutorial_new_feature_badge")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "جديد" else "NEW",
                                        color = NeonAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isArabic) currentStep.badgeAr else currentStep.badgeEn,
                                    color = animatedAccent.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = animatedAccent),
                                    onClick = {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        onClose()
                                    }
                                )
                                .padding(horizontal = 8.dp)
                                .testTag("tutorial_skip_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "تخطي" else "Skip",
                                    color = mutedColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "تخطي الدليل" else "Skip tutorial",
                                    tint = mutedColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Animated Content Swap for Headline, Description & Tip
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                        },
                        label = "coachmark_step_text"
                    ) { step ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) step.titleAr else step.titleEn,
                                color = titleColor,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isArabic) step.descriptionAr else step.descriptionEn,
                                color = bodyColor,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "• ${step.tipAr}" else "• ${step.tipEn}",
                                color = animatedAccent.copy(alpha = 0.95f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Morphing Progress Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { index, stepItem ->
                            val isSelected = currentIndex == index
                            val isStepNew = seenStepIds.isNotEmpty() && stepItem.id !in seenStepIds
                            val pillWidth by animateDpAsState(
                                targetValue = if (isSelected) 26.dp else 8.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "dot_w_$index"
                            )
                            val dotColor = when {
                                isSelected -> animatedAccent
                                isStepNew -> NeonAmber.copy(alpha = 0.85f)
                                else -> mutedColor.copy(alpha = 0.36f)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(8.dp)
                                    .width(pillWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .clickable {
                                        currentIndex = index
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row (48dp+ touch targets)
                    val isFirst = currentIndex == 0
                    val isLast = currentIndex == steps.lastIndex

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isFirst) {
                            Box(
                                modifier = Modifier
                                    .weight(0.36f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDarkMode) Color(0x1CFFFFFF) else Color(0xFFE2E8F0))
                                    .border(
                                        BorderStroke(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = animatedAccent),
                                        onClick = {
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) {}
                                            currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                        }
                                    )
                                    .testTag("tutorial_back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = titleColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "السابق" else "Back",
                                        color = titleColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(if (isFirst) 1f else 0.64f)
                                .height(48.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = animatedAccent.copy(alpha = 0.45f),
                                    spotColor = animatedAccent.copy(alpha = 0.55f)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(animatedAccent, EmeraldSavings)
                                    )
                                )
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White),
                                    onClick = {
                                        if (isLast) {
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                            onClose()
                                        } else {
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) {}
                                            currentIndex = (currentIndex + 1).coerceAtMost(steps.lastIndex)
                                        }
                                    }
                                )
                                .testTag(if (isLast) "tutorial_finish_button" else "tutorial_next_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        isLast && isArabic -> "فهمت، لنبدأ!"
                                        isLast -> "Got It!"
                                        isArabic -> "التالي"
                                        else -> "Next"
                                    },
                                    color = Color(0xFF04121D),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isLast) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF04121D),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
