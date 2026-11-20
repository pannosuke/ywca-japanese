@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.ericallerton.yoruben.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ericallerton.yoruben.core.Ruby

@Composable
fun MaybeRuby(text: String, style: TextStyle, known: Set<Char>, modifier: Modifier = Modifier) {
    if ('[' in text || Ruby.bareKanji(text, known).isNotEmpty()) {
        RubyText(text, style, known, modifier)
    } else {
        Text(text, style = style, modifier = modifier)
    }
}

@Composable
fun RubyText(
    markup: String,
    style: TextStyle,
    known: Set<Char>,
    modifier: Modifier = Modifier,
) {
    val pieces = Ruby.parse(markup, known)
    val reading = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        pieces.forEach { piece ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (piece.reading.isNotEmpty()) {
                    Text(
                        piece.reading,
                        fontFamily = Jp,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        color = reading,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.height(13.dp))
                }
                Text(piece.base, style = style, textAlign = TextAlign.Center)
            }
        }
    }
}
