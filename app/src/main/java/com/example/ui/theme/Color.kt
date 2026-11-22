package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Liquid Glass Dark Mode Palette (M5 Style)
val BackgroundDark = Color(0xFF0B0E14)
val BackgroundSurfaceDark = Color(0xFF121722)
val BackgroundSurfaceElevated = Color(0xFF161E2E)

// Translucent Glass Tints (Dark)
val GlassSurfaceDark = Color(0x331E2A40)
val GlassSurfaceLightDark = Color(0x4D2A3A54)
val GlassBorderDark = Color(0x26FFFFFF)

// StrikePulse Aurora Light Mode Palette (Matches screenshots)
val BackgroundLight = Color(0xFFF2F6F9)
val StrikePulseTopCyan = Color(0xFFDCF3F8)
val StrikePulseWarmPeach = Color(0xFFFFF2E6)
val StrikePulseLavender = Color(0xFFF1ECF9)

val BackgroundSurfaceLightMode = Color(0xFFE8F0F7)
val BackgroundSurfaceElevatedLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

val LightCardSurface = Color(0xF7FFFFFF)
val LightCardBorder = Color(0xFFE2EAF2)
val GlassBorderLight = Color(0xFFE2EAF2)

// StrikePulse Specific Tinted Containers
val CyanTintBg = Color(0xFFE0F7FD)
val CyanTintBorder = Color(0xFF22D3EE)
val CyanTintText = Color(0xFF0284C7)

val MintTintBg = Color(0xFFD6FBE6)
val MintTintBorder = Color(0xFF34D399)
val MintTintText = Color(0xFF059669)

val PeachTintBg = Color(0xFFFFF2DC)
val PeachTintBorder = Color(0xFFFBBF24)
val PeachTintText = Color(0xFFD97706)

// Glowing Neon Accents
val EmeraldSavings = Color(0xFF00E676)
val EmeraldDark = Color(0xFF059669)

val ElectricCyan = Color(0xFF00E5FF)
val ElectricCyanDark = Color(0xFF0284C7)

val NeonAmber = Color(0xFFFFB300)
val NeonRed = Color(0xFFFF5252)

// Text & Neutral Colors (Dark Mode constants)
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Dynamic theme-adaptive tokens
val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) TextPrimaryLight else TextPrimaryDark

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) TextSecondaryLight else TextSecondaryDark

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) TextMutedLight else TextMutedDark

val GlassSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) LightCardSurface else GlassSurfaceDark

val GlassSurfaceLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) Color(0xFFEEF4F8) else GlassSurfaceLightDark

val GlassBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == BackgroundLight) LightCardBorder else GlassBorderDark

// Supermarket Mode High Contrast
val CheckedGreen = Color(0xFF10B981)

val VioletAccent = Color(0xFFA78BFA)
val VioletDark = Color(0xFF7C3AED)

fun categoryAccentColor(category: String, isLight: Boolean): Color {
    return when (category.lowercase()) {
        "produce" -> if (isLight) EmeraldDark else EmeraldSavings
        "dairy" -> if (isLight) CyanTintText else ElectricCyan
        "bakery" -> if (isLight) PeachTintText else NeonAmber
        "butchery" -> if (isLight) Color(0xFFDC2626) else Color(0xFFFF6B6B)
        "beverages" -> if (isLight) Color(0xFF2563EB) else Color(0xFF60A5FA)
        "household" -> if (isLight) VioletDark else VioletAccent
        "herbaldrinks" -> if (isLight) Color(0xFF0D9488) else Color(0xFF2DD4BF)
        "sweetsnuts" -> if (isLight) Color(0xFFDB2777) else Color(0xFFF472B6)
        else -> if (isLight) CyanTintText else ElectricCyan
    }
}

