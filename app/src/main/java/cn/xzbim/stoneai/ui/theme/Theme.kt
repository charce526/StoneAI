package cn.xzbim.stoneai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = PrimaryGreenDark,

    secondary = SecondaryOrange,
    onSecondary = Color.White,
    secondaryContainer = SecondaryOrangeLight,
    onSecondaryContainer = SecondaryOrangeDark,

    tertiary = TertiaryLavender,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryLavenderLight,
    onTertiaryContainer = TertiaryLavenderDark,

    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = SecondaryOrange,
    onSecondary = DarkBackground,
    secondaryContainer = SecondaryOrangeDark,
    onSecondaryContainer = SecondaryOrangeLight,

    tertiary = TertiaryLavender,
    onTertiary = DarkBackground,
    tertiaryContainer = TertiaryLavenderDark,
    onTertiaryContainer = TertiaryLavenderLight,

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOnSurfaceVariant,
    outlineVariant = DarkSurfaceVariant
)

/**
 * AI 识石 - Soft & Friendly 自然系主题
 */
@Composable
fun StoneAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
