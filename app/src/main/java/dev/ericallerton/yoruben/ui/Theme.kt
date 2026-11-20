package dev.ericallerton.yoruben.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.ericallerton.yoruben.R

val Jp = FontFamily(Font(R.font.noto_sans_jp_regular))

private val Light = lightColorScheme(
    primary = Color(0xFF2C4A3E),
    onPrimary = Color(0xFFF7F4EE),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1A1916),
    surface = Color(0xFFF4F1EA),
    onSurface = Color(0xFF1A1916),
    onSurfaceVariant = Color(0xFF6E6962),
    outline = Color(0xFFD9D3C6),
    error = Color(0xFF7A3E36),
    onError = Color(0xFFF7F4EE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB7CBBF),
    onPrimary = Color(0xFF13211B),
    background = Color(0xFF11110F),
    onBackground = Color(0xFFF3EFE6),
    surface = Color(0xFF11110F),
    onSurface = Color(0xFFF3EFE6),
    onSurfaceVariant = Color(0xFFA8A298),
    outline = Color(0xFF2E2C28),
    error = Color(0xFFE0B2A8),
    onError = Color(0xFF2A1412),
)

private val Type = Typography(
    headlineLarge = TextStyle(fontFamily = Jp, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Jp, fontSize = 26.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = Jp, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Jp, fontSize = 18.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = Jp, fontSize = 18.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontFamily = Jp, fontSize = 16.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontFamily = Jp, fontSize = 16.sp, lineHeight = 22.sp),
)

@Composable
fun YorubenTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Type,
        content = content,
    )
}
