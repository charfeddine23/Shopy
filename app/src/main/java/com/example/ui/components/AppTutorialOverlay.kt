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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.NeonAmber
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AppTutorialOverlay(
    isArabic: Boolean,
    isDarkMode: Boolean,
    isReplayMode: Boolean = false,
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

    var completedStepIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCelebratingSuccess by remember { mutableStateOf(false) }
    var nudgeOutsideTapCount by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val targetRegistry = LocalTutorialTargetRegistry.current
    val interactionBus = LocalTutorialInteractionBus.current
    val isCurrentStepCompleted = currentStep.id in completedStepIds

    // Lock hardware back button during mandatory tutorial; step back inside tutorial if past step 0, or exit only on replay
    BackHandler(enabled = true) {
        if (currentIndex > 0) {
            currentIndex -= 1
        } else if (isReplayMode) {
            onClose()
        } else {
            nudgeOutsideTapCount += 1
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    // Sync target live UI screen & active mandatory gesture expectation
    LaunchedEffect(currentStep.id, isCelebratingSuccess) {
        onFocusScreen(currentStep.targetScreen)
        if (!isCelebratingSuccess) {
            interactionBus?.activeRequiredAction = currentStep.requiredAction
        } else {
            interactionBus?.activeRequiredAction = null
        }
    }

    DisposableEffect(interactionBus) {
        onDispose {
            interactionBus?.clear()
        }
    }

    // React immediately when the user genuinely executes the required gesture/action on live UI
    val completedSeq = interactionBus?.completedActionSequence ?: 0
    LaunchedEffect(completedSeq) {
        if (completedSeq > 0 && interactionBus?.lastCompletedAction == currentStep.requiredAction && !isCelebratingSuccess) {
            isCelebratingSuccess = true
            completedStepIds = completedStepIds + currentStep.id
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            delay(540L)
            isCelebratingSuccess = false
            if (currentIndex >= steps.lastIndex) {
                onClose()
            } else {
                currentIndex += 1
            }
        }
    }

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

    val targetAccent = if (isCelebratingSuccess) EmeraldSavings else currentStep.accentColor
    val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 320),
        label = "coachmark_accent"
    )

    val infiniteHalo = rememberInfiniteTransition(label = "coachmark_halo")
    val haloPulse by infiniteHalo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val spotlightSpring = remember {
        spring<Float>(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
    }
    val hasSpotlightTarget = localTargetRect != null
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (hasSpotlightTarget) 1f else 0f,
        animationSpec = tween(240),
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

    val isLight = !isDarkMode
    val scrimColor = if (isDarkMode) Color(0xC8050A14) else Color(0xBA091221)
    val windowSurface = liquidWindowContainerColor(isLight)
    val titleColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val bodyColor = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val mutedColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInRoot = coords.positionInRoot()
            }
            .testTag("app_tutorial_overlay")
    ) {
        val availableHeight = maxHeight
        val availableWidth = maxWidth
        val overlayWidthPx = with(density) { availableWidth.toPx() }
        val overlayHeightPx = with(density) { availableHeight.toPx() }

        // 1. Touch-blocking scrim strips surrounding the spotlight hole (touches INSIDE the hole pass straight through to live UI!)
        val passThroughRect: Rect = when {
            currentStep.requiredAction == TutorialRequiredAction.TWO_FINGER_FULLSCREEN_SWIPE -> {
                Rect(0f, 0f, overlayWidthPx, overlayHeightPx)
            }
            animatedRect != null -> {
                val extraPx = with(density) { 4.dp.toPx() }
                Rect(
                    left = (animatedRect.left - extraPx).coerceIn(0f, overlayWidthPx),
                    top = (animatedRect.top - extraPx).coerceIn(0f, overlayHeightPx),
                    right = (animatedRect.right + extraPx).coerceIn(0f, overlayWidthPx),
                    bottom = (animatedRect.bottom + extraPx).coerceIn(0f, overlayHeightPx)
                )
            }
            else -> Rect(0f, 0f, overlayWidthPx, overlayHeightPx)
        }

        ScrimBlockerRegions(
            totalWidthPx = overlayWidthPx,
            totalHeightPx = overlayHeightPx,
            hole = passThroughRect,
            onBlockedTap = {
                nudgeOutsideTapCount += 1
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}
            }
        )

        // 2. Live Dimming Scrim with Smooth Rounded Spotlight Cutout (reveals live interactive widget!)
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

                val extraPulse = if (nudgeOutsideTapCount > 0) 3f else 0f
                val pulseExpand = (3.dp + (haloPulse * 4f + extraPulse).dp).toPx()
                drawRoundRect(
                    color = animatedAccent.copy(alpha = (0.38f - 0.14f * haloPulse) * spotlightAlpha),
                    topLeft = Offset(animatedRect.left - pulseExpand, animatedRect.top - pulseExpand),
                    size = Size(animatedRect.width + pulseExpand * 2f, animatedRect.height + pulseExpand * 2f),
                    cornerRadius = CornerRadius(animRadiusPx + pulseExpand, animRadiusPx + pulseExpand),
                    style = Stroke(width = 3.dp.toPx())
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(animatedAccent.copy(alpha = spotlightAlpha), EmeraldSavings.copy(alpha = spotlightAlpha))
                    ),
                    topLeft = Offset(animatedRect.left, animatedRect.top),
                    size = Size(animatedRect.width, animatedRect.height),
                    cornerRadius = r,
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
        }

        // 3. Interactive Vector Gesture Cues anchored to the live UI widget
        if (!isCelebratingSuccess) {
            TutorialSpotlightMotionCue(
                gestureType = currentStep.gestureType,
                targetRect = animatedRect,
                accentColor = animatedAccent,
                isArabic = isArabic,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. Smart Balanced Liquid Glass Coachmark Card
        val placeCardAbove = when {
            animatedRect == null -> false
            animatedRect.center.y > overlayHeightPx * 0.50f -> true
            else -> false
        }

        val clearanceFromSpotlight = if (animatedRect != null) {
            with(density) {
                if (placeCardAbove) {
                    (overlayHeightPx - animatedRect.top).coerceAtLeast(0f).toDp() + 34.dp
                } else {
                    animatedRect.bottom.coerceAtLeast(0f).toDp() + 26.dp
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = if (placeCardAbove || animatedRect == null) {
                    Alignment.BottomCenter
                } else {
                    Alignment.TopCenter
                }
            ) {
                val maxCardHeight = (availableHeight * 0.48f).coerceIn(220.dp, 340.dp)
                val safeCardModifier = when {
                    animatedRect == null -> Modifier.padding(bottom = 26.dp)
                    placeCardAbove -> Modifier.padding(bottom = clearanceFromSpotlight.coerceAtMost(availableHeight * 0.44f))
                    else -> Modifier.padding(top = clearanceFromSpotlight.coerceAtMost(availableHeight * 0.46f))
                }

                Box(
                    modifier = safeCardModifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                        .heightIn(max = maxCardHeight)
                        .shadow(
                            elevation = if (isDarkMode) 26.dp else 16.dp,
                            shape = LiquidWindowShape,
                            ambientColor = animatedAccent.copy(alpha = 0.35f),
                            spotColor = animatedAccent.copy(alpha = 0.42f)
                        )
                        .clip(LiquidWindowShape)
                        .background(windowSurface)
                        .border(liquidWindowBorder(isLight, animatedAccent), LiquidWindowShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Card absorbs its own taps cleanly
                        )
                ) {
                    // Consistent top specular glass hairline matching all LiquidGlass cards & dialogs
                    if (isDarkMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.45f),
                                            animatedAccent.copy(alpha = 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Top Row: Step Counter + Feature / New Tag + Optional Replay Close
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
                                        color = animatedAccent.copy(alpha = 0.92f),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isReplayMode) {
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = animatedAccent),
                                            onClick = onClose
                                        )
                                        .padding(horizontal = 6.dp)
                                        .testTag("tutorial_close_replay_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "إغلاق الدليل" else "Close tutorial",
                                        tint = mutedColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // Mandatory badge indicator (no Skip allowed on first-time mandatory tour)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isCelebratingSuccess) EmeraldSavings.copy(alpha = 0.16f)
                                            else animatedAccent.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isCelebratingSuccess) EmeraldSavings.copy(alpha = 0.5f)
                                                else animatedAccent.copy(alpha = 0.35f)
                                            ),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isCelebratingSuccess && isArabic -> "تم التنفيذ ✓"
                                            isCelebratingSuccess -> "Done ✓"
                                            isArabic -> "تفاعل إجباري"
                                            else -> "Required"
                                        },
                                        color = if (isCelebratingSuccess) EmeraldSavings else animatedAccent,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Headline & Description
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(130)) },
                            label = "coachmark_step_text"
                        ) { step ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isArabic) step.titleAr else step.titleEn,
                                    color = titleColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = if (isArabic) step.descriptionAr else step.descriptionEn,
                                    color = bodyColor,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Execution Callout Banner (turns Emerald when completed!)
                        val actionBannerBg = if (isCelebratingSuccess || isCurrentStepCompleted) {
                            EmeraldSavings.copy(alpha = 0.16f)
                        } else if (nudgeOutsideTapCount > 0) {
                            NeonAmber.copy(alpha = 0.16f)
                        } else {
                            animatedAccent.copy(alpha = 0.14f)
                        }
                        val actionBannerBorder = if (isCelebratingSuccess || isCurrentStepCompleted) {
                            EmeraldSavings.copy(alpha = 0.55f)
                        } else if (nudgeOutsideTapCount > 0) {
                            NeonAmber.copy(alpha = 0.65f)
                        } else {
                            animatedAccent.copy(alpha = 0.45f)
                        }
                        val actionBannerTint = if (isCelebratingSuccess || isCurrentStepCompleted) {
                            EmeraldSavings
                        } else if (nudgeOutsideTapCount > 0) {
                            NeonAmber
                        } else {
                            animatedAccent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(actionBannerBg)
                                .border(BorderStroke(1.dp, actionBannerBorder), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("tutorial_required_action_banner"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCelebratingSuccess || isCurrentStepCompleted) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.TouchApp
                                },
                                contentDescription = null,
                                tint = actionBannerTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isCelebratingSuccess && isArabic -> "أحسنت! جاري الانتقال للخطوة التالية..."
                                    isCelebratingSuccess -> "Great job! Advancing..."
                                    nudgeOutsideTapCount > 0 && isArabic -> "يرجى تنفيذ الحركة داخل الإطار المضيء للمتابعة"
                                    nudgeOutsideTapCount > 0 -> "Please perform the gesture inside the spotlight"
                                    isArabic -> currentStep.instructionAr
                                    else -> currentStep.instructionEn
                                },
                                color = actionBannerTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Capsules (allows stepping only to completed/current steps; future steps stay locked!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            steps.forEachIndexed { index, stepItem ->
                                val isSelected = currentIndex == index
                                val isDone = stepItem.id in completedStepIds
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
                                    isDone -> EmeraldSavings
                                    else -> mutedColor.copy(alpha = 0.32f)
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(8.dp)
                                        .width(pillWidth)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .clickable(enabled = index <= currentIndex || isDone) {
                                            nudgeOutsideTapCount = 0
                                            currentIndex = index
                                        }
                                )
                            }
                        }

                        // Optional Navigation Row ONLY for stepping back or advancing after already completing a step / generic step
                        val canAdvance = isCurrentStepCompleted || currentStep.requiredAction == TutorialRequiredAction.TAP_TO_CONTINUE
                        if (currentIndex > 0 || canAdvance) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isLast = currentIndex == steps.lastIndex
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentIndex > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(if (canAdvance) 0.40f else 1f)
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
                                                    nudgeOutsideTapCount = 0
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
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isArabic) "السابق" else "Back",
                                                color = titleColor,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (canAdvance) {
                                    Box(
                                        modifier = Modifier
                                            .weight(if (currentIndex > 0) 0.60f else 1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Brush.horizontalGradient(listOf(animatedAccent, EmeraldSavings)))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, color = Color.White),
                                                onClick = {
                                                    if (isLast) onClose() else currentIndex += 1
                                                }
                                            )
                                            .testTag(if (isLast) "tutorial_finish_button" else "tutorial_next_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when {
                                                    isLast && isArabic -> "إنهاء"
                                                    isLast -> "Finish"
                                                    isArabic -> "التالي"
                                                    else -> "Next"
                                                },
                                                color = Color(0xFF04121D),
                                                fontSize = 14.sp,
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
        }
    }
}

/**
 * Places 4 invisible pointer-absorbing boxes around [hole] so touches outside the
 * spotlight are safely blocked while touches INSIDE [hole] pass natively through to the live UI.
 */
@Composable
private fun ScrimBlockerRegions(
    totalWidthPx: Float,
    totalHeightPx: Float,
    hole: Rect,
    onBlockedTap: () -> Unit
) {
    val density = LocalDensity.current
    val l = hole.left.coerceIn(0f, totalWidthPx)
    val t = hole.top.coerceIn(0f, totalHeightPx)
    val r = hole.right.coerceIn(l, totalWidthPx)
    val b = hole.bottom.coerceIn(t, totalHeightPx)

    // Top band
    if (t > 1f) {
        BlockerBox(xPx = 0f, yPx = 0f, wPx = totalWidthPx, hPx = t, onBlockedTap = onBlockedTap)
    }
    // Bottom band
    if (totalHeightPx - b > 1f) {
        BlockerBox(xPx = 0f, yPx = b, wPx = totalWidthPx, hPx = totalHeightPx - b, onBlockedTap = onBlockedTap)
    }
    // Left band
    if (l > 1f && b - t > 1f) {
        BlockerBox(xPx = 0f, yPx = t, wPx = l, hPx = b - t, onBlockedTap = onBlockedTap)
    }
    // Right band
    if (totalWidthPx - r > 1f && b - t > 1f) {
        BlockerBox(xPx = r, yPx = t, wPx = totalWidthPx - r, hPx = b - t, onBlockedTap = onBlockedTap)
    }
}

@Composable
private fun BlockerBox(
    xPx: Float,
    yPx: Float,
    wPx: Float,
    hPx: Float,
    onBlockedTap: () -> Unit
) {
    val density = LocalDensity.current
    val wDp = with(density) { wPx.coerceAtLeast(0f).toDp() }
    val hDp = with(density) { hPx.coerceAtLeast(0f).toDp() }
    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(width = wDp, height = hDp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBlockedTap
            )
    )
}
