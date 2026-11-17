package dev.ericallerton.yoruben.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Persimmon = Color(0xFFC4552A)
private val PersimmonDark = Color(0xFFE7895A)
private val Ink = Color(0xFF241710)
private val Paper = Color(0xFFFFF6EE)
private val PaperDeep = Color(0xFFFFE8D6)
private val Tea = Color(0xFF2F5D50)
private val TeaBright = Color(0xFF8FCBB8)
private val Indigo = Color(0xFF2C3E6B)
private val IndigoBright = Color(0xFFB7C4E6)
private val Night = Color(0xFF1A120E)
private val NightCard = Color(0xFF2A1E18)

private val LightColors = lightColorScheme(
    primary = Persimmon,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD7C4),
    onPrimaryContainer = Color(0xFF3B1406),
    secondary = Tea,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EDE4),
    onSecondaryContainer = Color(0xFF06241C),
    tertiary = Indigo,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E2FA),
    onTertiaryContainer = Color(0xFF10182E),
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFFFBF7),
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = Color(0xFF5C4A40),
    outline = Color(0xFFD9C3B4),
)

private val DarkColors = darkColorScheme(
    primary = PersimmonDark,
    onPrimary = Color(0xFF3B1406),
    primaryContainer = Color(0xFF6E3118),
    onPrimaryContainer = Color(0xFFFFD7C4),
    secondary = TeaBright,
    onSecondary = Color(0xFF06241C),
    secondaryContainer = Color(0xFF1C4036),
    onSecondaryContainer = Color(0xFFD5EDE4),
    tertiary = IndigoBright,
    onTertiary = Color(0xFF10182E),
    tertiaryContainer = Color(0xFF243256),
    onTertiaryContainer = Color(0xFFD9E2FA),
    background = Night,
    onBackground = Color(0xFFF6E7DC),
    surface = NightCard,
    onSurface = Color(0xFFF6E7DC),
    surfaceVariant = Color(0xFF3A2B24),
    onSurfaceVariant = Color(0xFFE4D0C4),
    outline = Color(0xFF6E584C),
)

@Composable
fun YorubenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
