package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalVioletPrimary,
    onPrimary = RoyalVioletOnPrimary,
    primaryContainer = RoyalVioletContainer,
    onPrimaryContainer = RoyalVioletOnContainer,
    secondary = ElectricBlueSecondary,
    onSecondary = ElectricBlueOnSecondary,
    secondaryContainer = ElectricBlueContainer,
    onSecondaryContainer = ElectricBlueOnContainer,
    tertiary = MintEmeraldTertiary,
    onTertiary = MintEmeraldOnTertiary,
    tertiaryContainer = MintEmeraldContainer,
    onTertiaryContainer = MintEmeraldOnContainer,
    error = CrimsonError,
    onError = CrimsonOnError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = LavenderMistBackground,
    onBackground = InkOnSurface,
    surface = CrispGlassSurface,
    onSurface = InkOnSurface,
    surfaceVariant = SoftLavenderSurfaceVariant,
    onSurfaceVariant = MutedSlateOnSurfaceVariant,
    outline = SoftBorderOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = VioletDarkPrimary,
    onPrimary = VioletDarkOnPrimary,
    primaryContainer = VioletDarkContainer,
    onPrimaryContainer = VioletDarkOnContainer,
    secondary = BlueDarkSecondary,
    onSecondary = BlueDarkOnSecondary,
    secondaryContainer = BlueDarkContainer,
    onSecondaryContainer = BlueDarkOnContainer,
    tertiary = EmeraldDarkTertiary,
    onTertiary = EmeraldDarkOnTertiary,
    tertiaryContainer = EmeraldDarkContainer,
    onTertiaryContainer = EmeraldDarkOnContainer,
    error = CrimsonError,
    onError = CrimsonOnError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = SlateDarkBackground,
    onBackground = SlateDarkOnSurface,
    surface = SlateDarkSurface,
    onSurface = SlateDarkOnSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = SlateDarkOnSurfaceVariant,
    outline = SlateDarkOutline
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun BiharPrepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
