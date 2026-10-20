package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NavyPrimary = Color(0xFF0F2043)
val NavySecondary = Color(0xFF1E3A6D)
val TealAccent = Color(0xFF00796B)
val GoldLegal = Color(0xFFB45309)
val IcpBlue = Color(0xFF1565C0)
val SurfaceBackground = Color(0xFFF5F7FB)
val CardWhite = Color(0xFFFFFFFF)
val SuccessGreen = Color(0xFF1B8755)
val WarningAmber = Color(0xFFD97706)
val ErrorRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FA),
    onPrimaryContainer = NavyPrimary,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5EE),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = GoldLegal,
    onTertiary = Color.White,
    background = SurfaceBackground,
    onBackground = Color(0xFF1A2130),
    surface = CardWhite,
    onSurface = Color(0xFF1A2130),
    surfaceVariant = Color(0xFFEEF2F9),
    onSurfaceVariant = Color(0xFF4A5568),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun LetsSignTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
