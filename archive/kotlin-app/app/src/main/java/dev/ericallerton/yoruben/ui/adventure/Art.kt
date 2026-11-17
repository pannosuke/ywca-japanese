package dev.ericallerton.yoruben.ui.adventure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.ericallerton.yoruben.core.CosmeticSlot
import dev.ericallerton.yoruben.core.Cosmetics
import dev.ericallerton.yoruben.core.Ledger

@Composable
fun HeartRow(filled: Int, max: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { index ->
            Canvas(Modifier.size(18.dp)) {
                val path = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(w / 2f, h * 0.9f)
                    cubicTo(w * -0.1f, h * 0.45f, w * 0.2f, h * 0.05f, w / 2f, h * 0.38f)
                    cubicTo(w * 0.8f, h * 0.05f, w * 1.1f, h * 0.45f, w / 2f, h * 0.9f)
                    close()
                }
                drawPath(path, if (index < filled) Color(0xFFD64545) else Color(0xFFC8B89A))
            }
            if (index != max - 1) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
fun GemMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun GroveBanner(modifier: Modifier = Modifier) {
    val leaf = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val sky = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier) {
        drawRect(sky)
        val ground = size.height * 0.72f
        drawRect(leaf.copy(alpha = 0.85f), topLeft = Offset(0f, ground), size = Size(size.width, size.height - ground))
        drawCircle(gold, radius = size.height * 0.16f, center = Offset(size.width * 0.78f, size.height * 0.28f))
        listOf(0.18f, 0.42f, 0.62f).forEach { x ->
            val path = Path().apply {
                moveTo(size.width * x, ground)
                lineTo(size.width * x + 28f, ground - 70f)
                lineTo(size.width * x + 56f, ground)
                close()
            }
            drawPath(path, leaf)
        }
    }
}

@Composable
fun HeroPreview(ledger: Ledger, modifier: Modifier = Modifier) {
    val tunic = Color(Cosmetics.byId(ledger.equipped(CosmeticSlot.TUNIC))?.swatch ?: 0xFF1F6B3A)
    val shield = Color(Cosmetics.byId(ledger.equipped(CosmeticSlot.SHIELD))?.swatch ?: 0xFF8A5A32)
    val sword = Color(Cosmetics.byId(ledger.equipped(CosmeticSlot.SWORD))?.swatch ?: 0xFFD9D3C5)
    val light = Color(Cosmetics.byId(ledger.equipped(CosmeticSlot.COMPANION))?.swatch ?: 0xFFE2B33A)
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0xFFF0D7A4), radius = size.minDimension * 0.12f, center = Offset(size.width * 0.48f, size.height * 0.28f))
            drawRoundRect(
                tunic,
                topLeft = Offset(size.width * 0.34f, size.height * 0.4f),
                size = Size(size.width * 0.28f, size.height * 0.42f),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawCircle(shield, radius = size.minDimension * 0.1f, center = Offset(size.width * 0.28f, size.height * 0.58f))
            drawLine(sword, start = Offset(size.width * 0.66f, size.height * 0.42f), end = Offset(size.width * 0.78f, size.height * 0.78f), strokeWidth = 8f)
            drawCircle(light, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.72f, size.height * 0.24f))
            drawCircle(light.copy(alpha = 0.35f), radius = size.minDimension * 0.12f, center = Offset(size.width * 0.72f, size.height * 0.24f), style = Stroke(4f))
        }
    }
}
