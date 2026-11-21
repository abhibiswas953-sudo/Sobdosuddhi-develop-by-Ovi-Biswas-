package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LiteraryLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = AntiqueGold,
    onSecondary = Color.White,
    secondaryContainer = AntiqueGoldContainer,
    onSecondaryContainer = OnAntiqueGoldContainer,
    tertiary = BotanicalSage,
    onTertiary = Color.White,
    tertiaryContainer = BotanicalSageContainer,
    onTertiaryContainer = OnBotanicalSageContainer,
    background = IvoryCanvas,
    onBackground = InkEspresso,
    surface = IvoryCard,
    onSurface = InkEspresso,
    surfaceVariant = IvorySurface,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = IvoryCard,
    surfaceContainerLow = IvoryCanvas,
    surfaceContainer = IvorySurface,
    surfaceContainerHigh = IvorySurfaceHigh,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410E0B)
)

private val LiteraryDarkColorScheme = darkColorScheme(
    primary = TerracottaNight,
    onPrimary = OnTerracottaNight,
    primaryContainer = TerracottaContainerNight,
    onPrimaryContainer = OnTerracottaContainerNight,
    secondary = GoldNight,
    onSecondary = Color(0xFF3A2703),
    secondaryContainer = GoldContainerNight,
    onSecondaryContainer = OnGoldContainerNight,
    tertiary = SageNight,
    onTertiary = Color(0xFF0A2E1B),
    tertiaryContainer = SageContainerNight,
    onTertiaryContainer = OnSageContainerNight,
    background = NightCanvas,
    onBackground = ParchmentLightText,
    surface = NightCard,
    onSurface = ParchmentLightText,
    surfaceVariant = NightSurface,
    onSurfaceVariant = ParchmentMutedText,
    surfaceContainerLowest = NightCanvas,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightSurfaceHigh,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

val SobdosuddhiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SobdosuddhiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useSerifFont: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LiteraryDarkColorScheme else LiteraryLightColorScheme
    val typography = buildSobdosuddhiTypography(
        useSerifForBody = useSerifFont,
        scale = fontScale
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = SobdosuddhiShapes,
        content = content
    )
}
