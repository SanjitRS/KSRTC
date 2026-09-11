package net.kibotu.geofencerelay.features.ai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Authentic Apple iOS System Colors and Design Metrics.
 * Conforms precisely to Apple Human Interface Guidelines (Assistive Access & Springboard).
 */
object IosColors {
    // Apple iOS Dark Mode System Backgrounds
    val SystemGroupedBackground = Color(0xFF0D0E12)
    val SecondarySystemGroupedBackground = Color(0xFF1C1D24)
    val TertiarySystemGroupedBackground = Color(0xFF282932)

    // iOS System Tint Colors
    val SystemBlue = Color(0xFF0A84FF)
    val SystemGreen = Color(0xFF30D158)
    val SystemIndigo = Color(0xFF5E5CE6)
    val SystemOrange = Color(0xFFFF9F0A)
    val SystemPink = Color(0xFFFF375F)
    val SystemPurple = Color(0xFFBF5AF2)
    val SystemRed = Color(0xFFFF453A)
    val SystemTeal = Color(0xFF64D2FF)
    val SystemYellow = Color(0xFFFFD60A)

    // Apple iOS Dark Mode Neutral Grays
    val LabelPrimary = Color(0xFFFFFFFF)
    val LabelSecondary = Color(0xFFA0A1AA)
    val LabelTertiary = Color(0xFF6B6C75)
    val Separator = Color(0x33545458)
    val CardBorder = Color(0x33484852)

    // iOS Icon Gradients & Tints
    val CameraIconBg = Color(0xFF48484A)
    val SafariCompassRing = Color(0xFF0A84FF)
    val SafariNeedleRed = Color(0xFFFF453A)
}

/**
 * Vibrant Google Theme Color Palette for joyful, colorful cognitive care.
 */
object GoogleColors {
    val Blue = Color(0xFF4285F4)
    val Red = Color(0xFFEA4335)
    val Yellow = Color(0xFFFBBC05)
    val Green = Color(0xFF34A853)
}

object IosDimensions {
    val CardCornerRadius = 26.dp
    val IconSquircleCornerRadius = 18.dp
    val PillCornerRadius = 100.dp
    val CardElevation = 2.dp
}
