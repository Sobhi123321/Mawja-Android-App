package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MawjaSemanticColors(
    val downloadBlue: Color,
    val uploadTeal: Color,
    val healthyGreen: Color,
    val healthyGreenContainer: Color,
    val warningAmber: Color,
    val warningAmberContainer: Color,
    val blockedRed: Color,
    val blockedRedContainer: Color,
    val cardBorder: Color,
    val heroStart: Color,
    val heroEnd: Color
)

private val LightMawjaColors = MawjaSemanticColors(
    downloadBlue = GoogleBlue,
    uploadTeal = NestTeal,
    healthyGreen = SignalGreenLight,
    healthyGreenContainer = SignalGreenContainerLight,
    warningAmber = AmberWarningLight,
    warningAmberContainer = AmberContainerLight,
    blockedRed = GoogleRed,
    blockedRedContainer = GoogleRedContainer,
    cardBorder = LightOutlineVariant,
    heroStart = Color(0xFFE8F0FE),
    heroEnd = Color(0xFFF3F8FF)
)

private val DarkMawjaColors = MawjaSemanticColors(
    downloadBlue = GoogleBlueDark,
    uploadTeal = NestTealDark,
    healthyGreen = SignalGreenDark,
    healthyGreenContainer = SignalGreenContainerDark,
    warningAmber = AmberWarningDark,
    warningAmberContainer = AmberContainerDark,
    blockedRed = GoogleRedDark,
    blockedRedContainer = GoogleRedContainerDark,
    cardBorder = DarkOutlineVariant,
    heroStart = Color(0xFF192438),
    heroEnd = Color(0xFF151A24)
)

val LocalMawjaColors = staticCompositionLocalOf { LightMawjaColors }

private val MawjaLightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = GoogleBlueOnPrimary,
    primaryContainer = GoogleBlueContainer,
    onPrimaryContainer = GoogleBlueOnContainer,
    secondary = NestTeal,
    onSecondary = NestTealOnSecondary,
    secondaryContainer = NestTealContainer,
    onSecondaryContainer = NestTealOnContainer,
    tertiary = CalmIndigo,
    onTertiary = CalmIndigoOnTertiary,
    tertiaryContainer = CalmIndigoContainer,
    onTertiaryContainer = CalmIndigoOnTertiaryContainer,
    error = GoogleRed,
    onError = GoogleRedOnError,
    errorContainer = GoogleRedContainer,
    onErrorContainer = GoogleRedOnContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val MawjaDarkColorScheme = darkColorScheme(
    primary = GoogleBlueDark,
    onPrimary = GoogleBlueOnPrimaryDark,
    primaryContainer = GoogleBlueContainerDark,
    onPrimaryContainer = GoogleBlueOnContainerDark,
    secondary = NestTealDark,
    onSecondary = NestTealOnSecondaryDark,
    secondaryContainer = NestTealContainerDark,
    onSecondaryContainer = NestTealOnContainerDark,
    tertiary = CalmIndigoDark,
    onTertiary = CalmIndigoOnTertiaryDark,
    tertiaryContainer = CalmIndigoContainerDark,
    onTertiaryContainer = CalmIndigoOnTertiaryContainerDark,
    error = GoogleRedDark,
    onError = GoogleRedOnErrorDark,
    errorContainer = GoogleRedContainerDark,
    onErrorContainer = GoogleRedOnContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val MawjaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MawjaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MawjaDarkColorScheme else MawjaLightColorScheme
    val semanticColors = if (darkTheme) DarkMawjaColors else LightMawjaColors

    CompositionLocalProvider(LocalMawjaColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MawjaTypography,
            shapes = MawjaShapes,
            content = content
        )
    }
}
