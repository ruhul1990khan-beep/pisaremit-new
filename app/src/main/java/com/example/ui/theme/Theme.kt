package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bengali Remittance Brand Palette
val MagentaPrimary = Color(0xFFEC1690)
val PurpleSecondary = Color(0xFF6B21A8)
val AmberAccent = Color(0xFFF4B400)
val SoftPinkBackground = Color(0xFFFFF5FA)
val PureWhite = Color(0xFFFFFFFF)
val DarkText = Color(0xFF171717)
val GrayText = Color(0xFF6B7280)
val LightGrayBorder = Color(0xFFE5E7EB)
val LightPurpleCard = Color(0xFFFBF5FD)

// Feedback Colors
val SuccessGreen = Color(0xFF16A34A)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFDC2626)
val InfoBlue = Color(0xFF2563EB)

val LightColorScheme = lightColorScheme(
    primary = MagentaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E8),
    onPrimaryContainer = Color(0xFF3E0021),
    secondary = PurpleSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2DAFF),
    onSecondaryContainer = Color(0xFF2B0053),
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFEFA7),
    onTertiaryContainer = Color(0xFF241A00),
    background = SoftPinkBackground,
    onBackground = DarkText,
    surface = PureWhite,
    onSurface = DarkText,
    surfaceVariant = Color(0xFFF5EEF8),
    onSurfaceVariant = GrayText,
    outline = Color(0xFFE2D6E8),
    error = DangerRed,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF77BE),
    onPrimary = Color(0xFF5D0034),
    primaryContainer = Color(0xFF83004C),
    onPrimaryContainer = Color(0xFFFFD8E8),
    secondary = Color(0xFFD7BAFF),
    onSecondary = Color(0xFF45007A),
    secondaryContainer = Color(0xFF5E00A3),
    onSecondaryContainer = Color(0xFFF2DAFF),
    tertiary = Color(0xFFFFDF70),
    onTertiary = Color(0xFF3F3000),
    background = Color(0xFF181216),
    onBackground = Color(0xFFF0DFE6),
    surface = Color(0xFF211A1F),
    onSurface = Color(0xFFF0DFE6),
    surfaceVariant = Color(0xFF322730),
    onSurfaceVariant = Color(0xFFD4C1CE),
    outline = Color(0xFF5C4757),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun PaisaRemitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
