package dev.ericallerton.yoruben.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Leaf = Color(0xFF1F6B3A)
private val LeafDeep = Color(0xFF143D28)
private val Gold = Color(0xFFE2B33A)
private val GoldDeep = Color(0xFF8A6420)
private val Parchment = Color(0xFFF3E6C5)
private val ParchmentCard = Color(0xFFFFF8EA)
private val Ink = Color(0xFF1C2416)
private val Moss = Color(0xFF3E4A42)
private val Aqua = Color(0xFF7ED9C3)
private val Night = Color(0xFF102018)
private val NightCard = Color(0xFF1A2E24)

private val Forest = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBD9),
    onPrimaryContainer = LeafDeep,
    secondary = GoldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E3B0),
    onSecondaryContainer = Color(0xFF3A2A08),
    tertiary = Color(0xFF3E6B8A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5E7F2),
    onTertiaryContainer = Color(0xFF102433),
    background = Parchment,
    onBackground = Ink,
    surface = ParchmentCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7D7B4),
    onSurfaceVariant = Moss,
    outline = Color(0xFFC6B48A),
)

private val NightScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = Night,
    primaryContainer = LeafDeep,
    onPrimaryContainer = Color(0xFFD7EBD9),
    secondary = Gold,
    onSecondary = Color(0xFF3A2A08),
    secondaryContainer = Color(0xFF3A2E18),
    onSecondaryContainer = Color(0xFFF6E3B0),
    tertiary = Color(0xFFB7D4EA),
    onTertiary = Color(0xFF102433),
    tertiaryContainer = Color(0xFF243848),
    onTertiaryContainer = Color(0xFFD5E7F2),
    background = Night,
    onBackground = Color(0xFFF3E6C5),
    surface = NightCard,
    onSurface = Color(0xFFF3E6C5),
    surfaceVariant = Color(0xFF24382C),
    onSurfaceVariant = Color(0xFFE7D7B4),
    outline = Color(0xFF5C6B58),
)

private val GoldScheme = lightColorScheme(
    primary = GoldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E3B0),
    onPrimaryContainer = Color(0xFF3A2A08),
    secondary = Leaf,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7EBD9),
    onSecondaryContainer = LeafDeep,
    tertiary = Color(0xFF8A4E2A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3D2BC),
    onTertiaryContainer = Color(0xFF3A1E0C),
    background = Color(0xFFF8EFD4),
    onBackground = Ink,
    surface = Color(0xFFFFF9EE),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0E0B8),
    onSurfaceVariant = Moss,
    outline = Gold,
)

private val PaperScheme = Forest.copy(
    primary = Color(0xFF6E5430),
    background = Color(0xFFF7F0DE),
    surface = Color(0xFFFFFBF2),
    secondary = Leaf,
)

private val AdventureShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun YorubenTheme(themeId: String = "theme-forest", content: @Composable () -> Unit) {
    val colors = when (themeId) {
        "theme-night" -> NightScheme
        "theme-gold" -> GoldScheme
        "theme-parchment" -> PaperScheme
        else -> Forest
    }
    MaterialTheme(colorScheme = colors, shapes = AdventureShapes, content = content)
}
