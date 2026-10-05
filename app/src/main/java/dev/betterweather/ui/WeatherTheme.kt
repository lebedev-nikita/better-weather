package dev.betterweather.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val light = lightColorScheme(
    primary = Color(0xFF235BCD), onPrimary = Color.White,
    primaryContainer = Color(0xFFDAE7FF), onPrimaryContainer = Color(0xFF173C78),
    secondary = Color(0xFF445D79), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDFA), onSecondaryContainer = Color(0xFF243E5D),
    tertiary = Color(0xFF805400), tertiaryContainer = Color(0xFFFFDEA3), onTertiaryContainer = Color(0xFF5D3D00),
    background = Color(0xFFF5F8FE), onBackground = Color(0xFF152A46),
    surface = Color(0xFFF5F8FE), onSurface = Color(0xFF152A46),
    surfaceVariant = Color(0xFFEAF0FA), onSurfaceVariant = Color(0xFF445D79),
    outlineVariant = Color(0xFFCDD9E9),
)
private val dark = darkColorScheme(
    primary = Color(0xFFA9C7FF), onPrimary = Color(0xFF10346D),
    primaryContainer = Color(0xFF294974), onPrimaryContainer = Color(0xFFE0ECFF),
    secondary = Color(0xFFB7CAE1), onSecondary = Color(0xFF243E5D),
    secondaryContainer = Color(0xFF253A52), onSecondaryContainer = Color(0xFFDDEBFB),
    tertiary = Color(0xFFFFCF78), tertiaryContainer = Color(0xFF5D430E), onTertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFF101C2C), onBackground = Color(0xFFE5EFFF),
    surface = Color(0xFF101C2C), onSurface = Color(0xFFE5EFFF),
    surfaceVariant = Color(0xFF1C2D43), onSurfaceVariant = Color(0xFFB7CAE1),
    outlineVariant = Color(0xFF354A64),
)
private val type = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.copy(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp),
        headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark else light, typography = type, content = content)
}
