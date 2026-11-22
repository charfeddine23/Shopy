package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.ElectricCyan

/**
 * Provides the stabilized status-bar / display-cutout top inset so screens remain pixel-stationary
 * when toggling immersive full-screen mode while allowing scrollable lists to flow edge-to-edge
 * behind the Apple Liquid Glass top bar.
 */
val LocalPreservedTopInset = staticCompositionLocalOf { 0.dp }

/**
 * AGSL fragment shader implementing Apple Liquid Glass progressive top-bar fade-in blur:
 * - Samples the hardware-blurred backdrop content.
 * - Applies subtle convex meniscus refraction and RGB chromatic edge dispersion near the glass rim.
 * - Diffuses Apple-style frosted tint (cool ice white in Light Mode, deep translucent slate in Dark Mode).
 * - Applies a smooth S-curve vertical feather mask across the bottom tail so scrolled items melt seamlessly.
 */
private const val APPLE_LIQUID_GLASS_TOP_BAR_SHADER = """
    uniform shader content;
    uniform float2 uResolution;
    uniform float uHeaderBottom;
    uniform float uFadeTail;
    uniform float uProgress;
    uniform float uIsDark;

    half4 main(float2 fragCoord) {
        float2 maxCoord = max(uResolution - float2(1.0), float2(1.0));
        float safeTail = max(uFadeTail, 1.0);

        float fadeStart = max(uHeaderBottom - safeTail * 0.20, 0.0);
        float fadeEnd = min(uHeaderBottom + safeTail, uResolution.y);
        float tFade = clamp((fragCoord.y - fadeStart) / max(fadeEnd - fadeStart, 1.0), 0.0, 1.0);
        float alphaMask = (1.0 - smoothstep(0.0, 1.0, tFade)) * uProgress;

        if (alphaMask <= 0.002) {
            return half4(0.0);
        }

        // Apple Liquid Glass curved meniscus optical bend near bottom transition & outer edges
        float edgeBand = exp(-pow((fragCoord.y - uHeaderBottom) / max(safeTail * 0.62, 1.0), 2.0));
        float xNorm = (fragCoord.x / max(uResolution.x, 1.0)) * 2.0 - 1.0;
        float sideLens = pow(abs(xNorm), 2.2) * 0.55;
        float2 refractOffset = float2(
            -xNorm * sideLens * 6.0 * uProgress,
            -(edgeBand * 7.5) * uProgress
        );

        float2 sampleCoord = clamp(fragCoord + refractOffset, float2(0.0), maxCoord);
        float2 chromaShift = float2(1.5 * uProgress * (0.35 + edgeBand), 0.85 * edgeBand * uProgress);

        half4 centerSample = content.eval(sampleCoord);
        half rSample = content.eval(clamp(sampleCoord + chromaShift, float2(0.0), maxCoord)).r;
        half bSample = content.eval(clamp(sampleCoord - chromaShift, float2(0.0), maxCoord)).b;
        half3 blurredRgb = half3(rSample, centerSample.g, bSample);

        // Frosted Apple Liquid Glass tint diffusion
        half3 frostTint = uIsDark > 0.5
            ? half3(0.055, 0.09, 0.165)
            : half3(0.965, 0.985, 1.0);
        float tintMix = uIsDark > 0.5 ? (0.40 * uProgress) : (0.48 * uProgress);
        half3 tinted = mix(blurredRgb, frostTint, half(tintMix));

        // Subtle luminous specular sheen along the optical meniscus rim
        half specular = half(edgeBand * 0.15 * uProgress * (1.0 - abs(xNorm) * 0.40));
        half3 specularColor = uIsDark > 0.5 ? half3(0.50, 0.90, 1.0) : half3(1.0, 1.0, 1.0);
        half3 finalRgb = clamp(tinted + specularColor * specular, half3(0.0), half3(1.0));

        // Subtle dither to prevent gradient banding across the soft fade tail
        float dither = (fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.005;
        float finalAlpha = clamp(alphaMask + dither * alphaMask, 0.0, 1.0);

        // Premultiplied alpha for Compose RenderEffect compositing
        return half4(finalRgb * half(finalAlpha), half(finalAlpha));
    }
"""

/**
 * Container layout that pins a crisp floating Top Bar above scrollable content and applies
 * an Apple Liquid Glass fade-in progressive backdrop blur as content scrolls underneath.
 */
@Composable
fun AppleLiquidGlassTopBarLayout(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    defaultHeaderHeight: Dp = 68.dp,
    fadeTailHeight: Dp = 26.dp,
    scrollThreshold: Dp = 44.dp,
    topBar: @Composable (blurProgress: Float) -> Unit,
    content: @Composable BoxScope.(topContentPadding: Dp) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val isDarkMode = !isLight
    val density = LocalDensity.current
    val topInset = LocalPreservedTopInset.current

    val topInsetPx = with(density) { topInset.toPx() }
    val defaultHeaderHeightPx = with(density) { defaultHeaderHeight.roundToPx() }
    val fadeTailPx = with(density) { fadeTailHeight.toPx() }
    val scrollThresholdPx = with(density) { scrollThreshold.toPx().coerceAtLeast(1f) }

    var measuredHeaderContentPx by remember { mutableIntStateOf(defaultHeaderHeightPx) }
    val headerBottomPx = topInsetPx + measuredHeaderContentPx.toFloat()
    val topContentPaddingDp = with(density) { headerBottomPx.toDp() }
    val overlayHeightDp = with(density) { (headerBottomPx + fadeTailPx).toDp() }

    // Initial entrance fade-in for top chrome
    var entered by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        entered = 1f
    }
    val enterAlpha by animateFloatAsState(
        targetValue = entered,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "apple_top_bar_enter_fade"
    )

    // Scroll-driven fade-in blur progress (0f at rest -> 1f when scrolled)
    val rawScrollBlur by remember(listState, scrollThresholdPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / scrollThresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    val blurProgress by animateFloatAsState(
        targetValue = rawScrollBlur,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_liquid_glass_fade_in_blur"
    )

    val backdropLayer = rememberGraphicsLayer()
    var overlayWidthPx by remember { mutableFloatStateOf(0f) }
    var overlayHeightPx by remember { mutableFloatStateOf(0f) }

    val glassShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(APPLE_LIQUID_GLASS_TOP_BAR_SHADER) }.getOrNull()
        } else {
            null
        }
    }

    val blurRadiusPx = with(density) { (22.dp * blurProgress).toPx() }

    val backdropRenderEffect = remember(
        blurProgress,
        blurRadiusPx,
        overlayWidthPx,
        overlayHeightPx,
        headerBottomPx,
        fadeTailPx,
        isDarkMode
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            blurProgress <= 0.015f ||
            blurRadiusPx < 1f ||
            overlayWidthPx <= 1f ||
            overlayHeightPx <= 1f
        ) {
            null
        } else {
            val safeBlur = blurRadiusPx.coerceAtLeast(1f)
            val blurEffect = RenderEffect.createBlurEffect(
                safeBlur,
                safeBlur,
                Shader.TileMode.CLAMP
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassShader != null) {
                glassShader.setFloatUniform("uResolution", overlayWidthPx, overlayHeightPx)
                glassShader.setFloatUniform("uHeaderBottom", headerBottomPx)
                glassShader.setFloatUniform("uFadeTail", fadeTailPx)
                glassShader.setFloatUniform("uProgress", blurProgress)
                glassShader.setFloatUniform("uIsDark", if (isDarkMode) 1f else 0f)
                val shaderEffect = RenderEffect.createRuntimeShaderEffect(glassShader, "content")
                RenderEffect.createChainEffect(shaderEffect, blurEffect).asComposeRenderEffect()
            } else {
                blurEffect.asComposeRenderEffect()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Scrollable content recorded into backdropLayer for live real-time blur capture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdropLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(backdropLayer)
                }
        ) {
            content(topContentPaddingDp)
        }

        // 2. Apple Liquid Glass Fade-In Blur Backdrop Overlay (strictly behind topBar chrome)
        if (blurProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(overlayHeightDp)
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
                    .clipToBounds()
                    .onSizeChanged {
                        overlayWidthPx = it.width.toFloat()
                        overlayHeightPx = it.height.toFloat()
                    }
            ) {
                // Hardware-blurred + AGSL-refracted backdrop slice
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = backdropRenderEffect
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                alpha = blurProgress
                            }
                        }
                        .drawBehind {
                            if (backdropLayer.size.width > 0 && backdropLayer.size.height > 0) {
                                drawLayer(backdropLayer)
                            }
                        }
                )

                // Progressive Frosted Tint Scrim & Fallback Gradient (enhances legibility & smooth API < 33 fallback)
                val topScrimColor = if (isDarkMode) {
                    Color(0xFF0A101E).copy(alpha = (0.58f * blurProgress).coerceIn(0f, 0.72f))
                } else {
                    Color(0xFFF8FAFF).copy(alpha = (0.64f * blurProgress).coerceIn(0f, 0.76f))
                }
                val midScrimColor = if (isDarkMode) {
                    Color(0xFF0E1729).copy(alpha = (0.36f * blurProgress).coerceIn(0f, 0.50f))
                } else {
                    Color(0xFFFFFFFF).copy(alpha = (0.44f * blurProgress).coerceIn(0f, 0.56f))
                }
                val headerFraction = (headerBottomPx / overlayHeightPx.coerceAtLeast(1f)).coerceIn(0.2f, 0.92f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to topScrimColor,
                                headerFraction * 0.85f to midScrimColor,
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Subtle Apple Liquid Glass Fresnel Specular Rim near header bottom
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineY = (headerBottomPx - 1.dp.toPx()).coerceIn(0f, size.height)
                    val rimAlpha = 0.42f * blurProgress
                    val rimColors = if (isDarkMode) {
                        listOf(
                            Color.Transparent,
                            ElectricCyan.copy(alpha = rimAlpha * 0.65f),
                            Color.White.copy(alpha = rimAlpha * 0.45f),
                            ElectricCyan.copy(alpha = rimAlpha * 0.55f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = rimAlpha * 0.95f),
                            Color(0xFF38BDF8).copy(alpha = rimAlpha * 0.35f),
                            Color.White.copy(alpha = rimAlpha * 0.95f),
                            Color.Transparent
                        )
                    }
                    drawLine(
                        brush = Brush.horizontalGradient(rimColors),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // 3. Crisp Floating Top Bar Chrome (100% sharp text & buttons, fades in softly on enter)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(3f)
                .graphicsLayer { alpha = enterAlpha }
                .padding(top = topInset)
                .onSizeChanged { measuredHeaderContentPx = it.height }
        ) {
            topBar(blurProgress)
        }
    }
}
