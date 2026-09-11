package net.kibotu.geofencerelay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VibrantNerColorScheme = lightColorScheme(
    primary = NerColors.Primary,
    onPrimary = Color.White,
    primaryContainer = NerColors.PrimaryTint,
    onPrimaryContainer = NerColors.PrimaryDark,
    secondary = NerColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = NerColors.SecondaryTint,
    onSecondaryContainer = NerColors.SecondaryDark,
    tertiary = NerColors.Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = NerColors.TertiaryTint,
    onTertiaryContainer = NerColors.TertiaryDark,
    background = NerColors.CanvasWarm,
    onBackground = NerColors.Charcoal,
    surface = NerColors.SurfaceWhite,
    onSurface = NerColors.Charcoal,
    error = NerColors.Crimson,
    onError = Color.White
)

@Composable
fun GeofenceRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibrantNerColorScheme,
        typography = Typography,
        content = content
    )
}