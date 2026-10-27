package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta Oficial AssinaJÁ — Vinho Luxuoso, Ouro Champagne e Marfim Puro
val WinePrimary = Color(0xFF6B1724)        // Vinho Nobre Imperial
val WineDark = Color(0xFF4A0E17)           // Borgonha Profundo (Cabeçalhos / Contrastes)
val WineLight = Color(0xFFFBF0F2)          // Rosé Suave para containers e chips
val WineBorder = Color(0xFFE8C5CC)         // Borda delicada em tom vinho claro

val GoldChampagne = Color(0xFFC5A059)      // Ouro Champagne Nobre (Acentos, Selos)
val GoldLight = Color(0xFFF9F5EC)          // Fundo de destaque dourado
val GoldBorder = Color(0xFFE5D2A8)         // Borda dourada refinada

val CharcoalText = Color(0xFF1E2024)       // Grafite Escuro para títulos e textos
val MutedText = Color(0xFF5E6573)          // Cinza Médio balanceado
val IvoryBackground = Color(0xFFFBF9F7)    // Fundo Marfim Nobre (Clean e Sério)
val CardWhite = Color(0xFFFFFFFF)          // Branco Puro para cartões
val CardBorder = Color(0xFFE9E4DF)         // Borda de cartão neutra sofisticada

val SuccessGreen = Color(0xFF1B8755)       // Verde Sucesso Institucional
val WarningAmber = Color(0xFFC07000)       // Âmbar para pendências
val ErrorRed = Color(0xFFB3261E)           // Vermelho alerta
val InfoBlue = Color(0xFF1868B7)           // Azul para links e informativos

// Compatibilidade com tokens existentes
val NavyPrimary = WinePrimary
val NavySecondary = WineDark
val TealAccent = GoldChampagne
val GoldLegal = GoldChampagne
val IcpBlue = WinePrimary
val SurfaceBackground = IvoryBackground

private val LightColors = lightColorScheme(
    primary = WinePrimary,
    onPrimary = Color.White,
    primaryContainer = WineLight,
    onPrimaryContainer = WineDark,
    secondary = GoldChampagne,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Color(0xFF5A4418),
    tertiary = GoldChampagne,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = CharcoalText,
    surface = CardWhite,
    onSurface = CharcoalText,
    surfaceVariant = Color(0xFFF4EFEA),
    onSurfaceVariant = MutedText,
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
