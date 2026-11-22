package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CyanTintBg
import com.example.ui.theme.CyanTintBorder
import com.example.ui.theme.CyanTintText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.LightCardSurface
import com.example.ui.theme.MintTintBg
import com.example.ui.theme.MintTintBorder
import com.example.ui.theme.MintTintText
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PeachTintBg
import com.example.ui.theme.PeachTintBorder
import com.example.ui.theme.PeachTintText
import com.example.ui.theme.StrikePulseLavender
import com.example.ui.theme.StrikePulseTopCyan
import com.example.ui.theme.StrikePulseWarmPeach

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    if (isDarkMode) {
        // Deep Dark Liquid Glass with Neon Glow Orbs
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDark,
                            Color(0xFF0F1522),
                            Color(0xFF161E2E)
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x2200E5FF),
                                Color(0x0A00E5FF),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.12f),
                            radius = size.width * 0.65f * pulse
                        ),
                        radius = size.width * 0.65f * pulse,
                        center = Offset(size.width * 0.85f, size.height * 0.12f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x2200E676),
                                Color(0x0800E676),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.8f),
                            radius = size.width * 0.7f * pulse
                        ),
                        radius = size.width * 0.7f * pulse,
                        center = Offset(size.width * 0.15f, size.height * 0.8f)
                    )
                }
        ) {
            content()
        }
    } else {
        // StrikePulse Aurora Daylight Canvas (Soft Cyan, Warm Apricot, Pale Lavender)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .drawBehind {
                    // Top Cyan Sky Orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StrikePulseTopCyan.copy(alpha = 0.95f),
                                StrikePulseTopCyan.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.2f, size.height * 0.08f),
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.2f, size.height * 0.08f)
                    )

                    // Right Center Warm Peach/Apricot Orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StrikePulseWarmPeach.copy(alpha = 0.95f),
                                StrikePulseWarmPeach.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.95f, size.height * 0.42f),
                            radius = size.width * 0.8f * pulse
                        ),
                        radius = size.width * 0.8f * pulse,
                        center = Offset(size.width * 0.95f, size.height * 0.42f)
                    )

                    // Lower Left Soft Lavender Orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StrikePulseLavender.copy(alpha = 0.9f),
                                StrikePulseLavender.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.08f, size.height * 0.78f),
                            radius = size.width * 0.75f * pulse
                        ),
                        radius = size.width * 0.75f * pulse,
                        center = Offset(size.width * 0.08f, size.height * 0.78f)
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val defaultBg = if (isLight) Color(0xEBFFFFFF) else Color(0xBF11192B)
    val resolvedBg = backgroundColor ?: defaultBg

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.976f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "card_press_scale"
    )

    val borderStroke = if (isLight) {
        if (glowColor != null) {
            BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        glowColor.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.9f),
                        glowColor.copy(alpha = 0.25f)
                    )
                )
            )
        } else {
            BorderStroke(1.dp, borderColor ?: LightCardBorder)
        }
    } else {
        if (glowColor != null) {
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(glowColor.copy(alpha = 0.58f), Color.White.copy(alpha = 0.16f), glowColor.copy(alpha = 0.14f))
                )
            )
        } else {
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(Color(0x36FFFFFF), Color(0x14FFFFFF), Color(0x06FFFFFF))
                )
            )
        }
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = glowColor ?: ElectricCyan),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .scale(pressScale)
            .then(
                if (isLight) {
                    Modifier.shadow(
                        elevation = if (glowColor != null) 6.dp else 2.dp,
                        shape = shape,
                        ambientColor = Color(0x140F172A),
                        spotColor = (glowColor ?: Color(0xFF0F172A)).copy(alpha = 0.12f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(resolvedBg)
            .then(
                if (glowColor != null) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = if (isLight) 0.08f else 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.12f, 0f),
                                radius = size.width * 0.7f
                            ),
                            radius = size.width * 0.7f,
                            center = Offset(size.width * 0.12f, 0f)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(borderStroke, shape)
            .then(clickableModifier)
    ) {
        // Specular highlight line along top edge for Apple Liquid Glass depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        if (isLight) {
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color(0x48FFFFFF), Color(0x22FFFFFF), Color.Transparent)
                        }
                    )
                )
        )
        content()
    }
}

@Composable
fun SouqSectionHeader(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = if (isLight) 0.14f else 0.18f))
                    .border(
                        BorderStroke(1.dp, accentColor.copy(alpha = if (isLight) 0.35f else 0.4f)),
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = if (isLight) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun NeonBadge(
    text: String,
    accentColor: Color = EmeraldSavings,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    val (bg, border, contentColor) = if (isLight) {
        when (accentColor) {
            EmeraldSavings, Color(0xFF10B981) -> Triple(MintTintBg, MintTintBorder, MintTintText)
            NeonAmber, Color(0xFFFFB300) -> Triple(PeachTintBg, PeachTintBorder, PeachTintText)
            else -> Triple(CyanTintBg, CyanTintBorder, CyanTintText)
        }
    } else {
        Triple(accentColor.copy(alpha = 0.16f), accentColor.copy(alpha = 0.45f), accentColor)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(30.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlowingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = EmeraldSavings,
    enabled: Boolean = true
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    val (btnBrush, btnBorder, contentColor) = if (isLight) {
        if (enabled) {
            Triple(
                Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.85f))),
                BorderStroke(1.dp, color),
                Color.White
            )
        } else {
            Triple(
                Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))),
                BorderStroke(1.dp, Color(0xFFCBD5E1)),
                Color(0xFF94A3B8)
            )
        }
    } else {
        if (enabled) {
            Triple(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.7f))),
                BorderStroke(1.dp, color),
                Color(0xFF06141D)
            )
        } else {
            Triple(
                Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.DarkGray.copy(alpha = 0.3f))),
                BorderStroke(1.dp, Color.Gray),
                Color.Gray
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.968f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "btn_press_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(btnBrush)
            .border(btnBorder, RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

val LiquidWindowShape: RoundedCornerShape = RoundedCornerShape(24.dp)
val LiquidSheetShape: RoundedCornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

fun liquidWindowContainerColor(isLight: Boolean): Color {
    return if (isLight) Color(0xFFFFFFFF) else Color(0xFF111A2E)
}

fun liquidWindowBorder(isLight: Boolean, accentColor: Color = ElectricCyan): BorderStroke {
    return if (isLight) {
        BorderStroke(1.dp, LightCardBorder)
    } else {
        BorderStroke(
            1.15.dp,
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.58f),
                    Color.White.copy(alpha = 0.16f),
                    EmeraldSavings.copy(alpha = 0.34f)
                )
            )
        )
    }
}

@Composable
fun ShopyDialog(
    onDismissRequest: () -> Unit,
    accentColor: Color = ElectricCyan,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = LiquidWindowShape,
        containerColor = liquidWindowContainerColor(isLight),
        tonalElevation = 0.dp,
        modifier = modifier
            .widthIn(max = 400.dp)
            .border(liquidWindowBorder(isLight, accentColor), LiquidWindowShape)
    )
}

