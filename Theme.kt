package com.aetherion.noc.presentation.common.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Material 3 Enterprise Dark Theme
// Developer: Mohammad Abdalftah Ibrahime
// Enterprise minimalist — dark-first, severity-coded colors.
// ═══════════════════════════════════════════════════════════════════════════

// ─── Brand Colors ────────────────────────────────────────────────────────────

object AetherionColors {
    // Primary brand
    val AetherBlue      = Color(0xFF00B4D8)
    val AetherBlueDark  = Color(0xFF0077B6)
    val AetherBlueLight = Color(0xFF90E0EF)

    // Surface / Background
    val SurfaceDark     = Color(0xFF0A0F1E)
    val SurfaceCard     = Color(0xFF111827)
    val SurfaceElevated = Color(0xFF1A2332)
    val SurfaceBorder   = Color(0xFF1F2D40)

    // Text
    val TextPrimary     = Color(0xFFE2E8F0)
    val TextSecondary   = Color(0xFF94A3B8)
    val TextMuted       = Color(0xFF475569)

    // Severity Colors — aligned with Aetherion backend AlarmSeverity
    val Critical        = Color(0xFFFF2D2D)
    val CriticalDim     = Color(0xFF7F1D1D)
    val Major           = Color(0xFFFF7A00)
    val MajorDim        = Color(0xFF7C2D12)
    val Minor           = Color(0xFFFFD600)
    val MinorDim        = Color(0xFF713F12)
    val Warning         = Color(0xFF00E5FF)
    val WarningDim      = Color(0xFF0E4A5C)
    val Info            = Color(0xFF64748B)

    // Status
    val Online          = Color(0xFF00E676)
    val Offline         = Color(0xFFFF1744)
    val Degraded        = Color(0xFFFFAB00)
    val Maintenance     = Color(0xFF40C4FF)

    // Health score gradient
    val HealthGood      = Color(0xFF00E676)
    val HealthWarning   = Color(0xFFFFD600)
    val HealthCritical  = Color(0xFFFF2D2D)

    // Chart colors
    val ChartCpu        = Color(0xFF00B4D8)
    val ChartMemory     = Color(0xFFB388FF)
    val ChartTrafficIn  = Color(0xFF00E676)
    val ChartTrafficOut = Color(0xFFFF7A00)
}

// ─── Color Scheme ────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = AetherionColors.AetherBlue,
    onPrimary        = Color(0xFF003547),
    primaryContainer = AetherionColors.AetherBlueDark,
    onPrimaryContainer = AetherionColors.AetherBlueLight,

    secondary        = Color(0xFF90CDF4),
    onSecondary      = Color(0xFF003549),
    secondaryContainer = Color(0xFF004D63),
    onSecondaryContainer = Color(0xFFB3E5FC),

    tertiary         = Color(0xFF82B1FF),
    onTertiary       = Color(0xFF001E8C),

    error            = AetherionColors.Critical,
    onError          = Color(0xFF690005),
    errorContainer   = AetherionColors.CriticalDim,
    onErrorContainer = Color(0xFFFFDAD6),

    background       = AetherionColors.SurfaceDark,
    onBackground     = AetherionColors.TextPrimary,

    surface          = AetherionColors.SurfaceCard,
    onSurface        = AetherionColors.TextPrimary,
    surfaceVariant   = AetherionColors.SurfaceElevated,
    onSurfaceVariant = AetherionColors.TextSecondary,

    outline          = AetherionColors.SurfaceBorder,
    outlineVariant   = Color(0xFF263248),

    scrim            = Color(0xFF000000),
    inverseSurface   = AetherionColors.TextPrimary,
    inverseOnSurface = AetherionColors.SurfaceDark,
    inversePrimary   = AetherionColors.AetherBlueDark,
)

// ─── Typography ───────────────────────────────────────────────────────────────

val AetherionTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun AetherionNOCTheme(
    content: @Composable () -> Unit
) {
    // Always dark — enterprise standard
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AetherionTypography,
        content = content
    )
}

// ─── Severity Color Utilities ─────────────────────────────────────────────────

import com.aetherion.noc.domain.model.AlertSeverity
import com.aetherion.noc.domain.model.DeviceStatus

fun AlertSeverity.toColor(): Color = when (this) {
    AlertSeverity.CRITICAL -> AetherionColors.Critical
    AlertSeverity.MAJOR    -> AetherionColors.Major
    AlertSeverity.MINOR    -> AetherionColors.Minor
    AlertSeverity.WARNING  -> AetherionColors.Warning
    AlertSeverity.INFO     -> AetherionColors.Info
}

fun AlertSeverity.toDimColor(): Color = when (this) {
    AlertSeverity.CRITICAL -> AetherionColors.CriticalDim
    AlertSeverity.MAJOR    -> AetherionColors.MajorDim
    AlertSeverity.MINOR    -> AetherionColors.MinorDim
    AlertSeverity.WARNING  -> AetherionColors.WarningDim
    AlertSeverity.INFO     -> Color(0xFF1E293B)
}

fun DeviceStatus.toColor(): Color = when (this) {
    DeviceStatus.UP          -> AetherionColors.Online
    DeviceStatus.DOWN        -> AetherionColors.Offline
    DeviceStatus.DEGRADED    -> AetherionColors.Degraded
    DeviceStatus.MAINTENANCE -> AetherionColors.Maintenance
    DeviceStatus.UNKNOWN     -> AetherionColors.Info
}

fun Float.toHealthColor(): Color = when {
    this >= 90f -> AetherionColors.HealthGood
    this >= 70f -> AetherionColors.HealthWarning
    else        -> AetherionColors.HealthCritical
}
