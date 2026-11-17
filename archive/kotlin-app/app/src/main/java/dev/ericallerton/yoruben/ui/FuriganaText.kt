package dev.ericallerton.yoruben.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import dev.ericallerton.yoruben.core.RubyMarkup
import dev.ericallerton.yoruben.core.RubyPiece
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FuriganaText(
    markup: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val resolved = if (color == Color.Unspecified) LocalContentColor.current else color
    val measurer = rememberTextMeasurer()
    val pieces = remember(markup) { RubyMarkup.parse(markup) }
    val spoken = remember(markup) { RubyMarkup.spoken(markup) }
    val readingStyle = style.copy(
        fontSize = readingSize(style.fontSize),
        lineHeight = readingSize(style.fontSize),
        color = resolved,
    )
    val baseStyle = style.copy(color = resolved)
    val holder = remember { arrayOfNulls<RubyLayout>(1) }
    androidx.compose.foundation.layout.Box(
        modifier
            .semantics { contentDescription = spoken }
            .layout { measurable, constraints ->
                val built = buildRubyLayout(
                    pieces = pieces,
                    maxWidth = if (constraints.maxWidth == Constraints.Infinity) 100_000 else constraints.maxWidth,
                    measurer = measurer,
                    baseStyle = baseStyle,
                    readingStyle = readingStyle,
                    gapPx = 1.sp.toPx(),
                    lineGapPx = 2.sp.toPx(),
                )
                holder[0] = built
                val width = built.width.coerceIn(constraints.minWidth, constraints.maxWidth.coerceAtLeast(built.width))
                val height = built.height.coerceAtLeast(constraints.minHeight)
                val placeable = measurable.measure(Constraints.fixed(width.coerceAtLeast(1), height.coerceAtLeast(1)))
                layout(width, height) { placeable.place(0, 0) }
            }
            .drawBehind {
                holder[0]?.let { layout ->
                    for (glyph in layout.glyphs) {
                        glyph.reading?.let { reading ->
                            drawText(reading.layout, topLeft = reading.offset)
                        }
                        drawText(glyph.base.layout, topLeft = glyph.base.offset)
                    }
                }
            },
    )
}

private fun readingSize(base: TextUnit): TextUnit {
    if (!base.isSpecified) return 10.sp
    return max(10f, base.value * 0.45f).sp
}

private data class Measured(val layout: TextLayoutResult, val offsetX: Float, val offsetY: Float) {
    val offset get() = androidx.compose.ui.geometry.Offset(offsetX, offsetY)
}

private data class PlacedGlyph(val base: Measured, val reading: Measured?)

private data class RubyLayout(val width: Int, val height: Int, val glyphs: List<PlacedGlyph>)

private data class Token(val text: String, val reading: String?)

private fun buildRubyLayout(
    pieces: List<RubyPiece>,
    maxWidth: Int,
    measurer: androidx.compose.ui.text.TextMeasurer,
    baseStyle: TextStyle,
    readingStyle: TextStyle,
    gapPx: Float,
    lineGapPx: Float,
): RubyLayout {
    val tokens = pieces.flatMap { piece ->
        when (piece) {
            is RubyPiece.Text -> splitPlain(piece.text).map { Token(it, null) }
            is RubyPiece.Ruby -> listOf(Token(piece.base, piece.reading))
        }
    }.filter { it.text.isNotEmpty() }
    if (tokens.isEmpty()) return RubyLayout(0, 0, emptyList())

    data class MeasuredToken(
        val text: String,
        val reading: String?,
        val baseLayout: TextLayoutResult,
        val readingLayout: TextLayoutResult?,
    )

    val measured = tokens.map { token ->
        MeasuredToken(
            text = token.text,
            reading = token.reading,
            baseLayout = measurer.measure(token.text, baseStyle, overflow = TextOverflow.Clip, softWrap = false, maxLines = 1),
            readingLayout = token.reading?.let {
                measurer.measure(it, readingStyle, overflow = TextOverflow.Clip, softWrap = false, maxLines = 1)
            },
        )
    }

    val glyphs = mutableListOf<PlacedGlyph>()
    var line = mutableListOf<MeasuredToken>()
    var lineWidth = 0f
    var y = 0f
    var widest = 0

    fun flush() {
        if (line.isEmpty()) return
        val readingHeight = line.maxOf { it.readingLayout?.size?.height ?: 0 }
        val baseHeight = line.maxOf { it.baseLayout.size.height }
        val gap = if (readingHeight > 0) gapPx else 0f
        val lineHeight = readingHeight + gap + baseHeight
        var x = 0f
        for (token in line) {
            val baseW = token.baseLayout.size.width.toFloat()
            val readW = token.readingLayout?.size?.width?.toFloat() ?: 0f
            val width = max(baseW, readW)
            val reading = token.readingLayout?.let { layout ->
                Measured(layout, x + (width - readW) / 2f, y)
            }
            val base = Measured(token.baseLayout, x + (width - baseW) / 2f, y + readingHeight + gap)
            glyphs += PlacedGlyph(base, reading)
            x += width
        }
        widest = max(widest, x.roundToInt())
        y += lineHeight + lineGapPx
        line = mutableListOf()
        lineWidth = 0f
    }

    for (token in measured) {
        if (token.text == " " && line.isEmpty()) continue
        val width = max(
            token.baseLayout.size.width.toFloat(),
            token.readingLayout?.size?.width?.toFloat() ?: 0f,
        )
        if (line.isNotEmpty() && lineWidth + width > maxWidth) flush()
        line += token
        lineWidth += width
    }
    flush()
    val height = if (y > 0f) (y - lineGapPx).roundToInt().coerceAtLeast(0) else 0
    return RubyLayout(widest, height, glyphs)
}

private fun splitPlain(text: String): List<String> {
    val out = mutableListOf<String>()
    val buf = StringBuilder()
    fun flush() {
        if (buf.isNotEmpty()) {
            out += buf.toString()
            buf.clear()
        }
    }
    for (ch in text) {
        val latin = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '\'' || ch == '’' || ch == '-' || ch == 'Ō' || ch == 'ō'
        if (latin) {
            buf.append(ch)
        } else {
            flush()
            out += ch.toString()
        }
    }
    flush()
    return out
}
