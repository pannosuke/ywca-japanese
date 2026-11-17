package dev.ericallerton.yoruben.core

/**
 * Anki-style furigana: 夕日[ゆうひ], 厳[きび]しい, 万里[ばんり]の 長城[ちょうじょう].
 * A reading in brackets belongs to the kanji run immediately before it.
 * If the character before '[' is not kanji (A[エー]), the preceding token is the base.
 */
sealed interface RubyPiece {
    data class Text(val text: String) : RubyPiece
    data class Ruby(val base: String, val reading: String) : RubyPiece
}

object RubyMarkup {
    fun parse(input: String): List<RubyPiece> {
        if (input.isEmpty()) return emptyList()
        val out = mutableListOf<RubyPiece>()
        var i = 0
        while (i < input.length) {
            val bracket = input.indexOf('[', i)
            if (bracket < 0) {
                out += RubyPiece.Text(input.substring(i))
                break
            }
            val end = input.indexOf(']', bracket + 1)
            if (end < 0) {
                out += RubyPiece.Text(input.substring(i))
                break
            }
            val reading = input.substring(bracket + 1, end)
            var baseStart = bracket
            if (baseStart > i && input[baseStart - 1].isHan()) {
                while (baseStart > i && input[baseStart - 1].isHan()) baseStart--
            } else if (baseStart > i) {
                while (baseStart > i && !input[baseStart - 1].isWhitespace() && input[baseStart - 1] != ']') {
                    baseStart--
                }
            }
            if (baseStart > i) out += RubyPiece.Text(input.substring(i, baseStart))
            val base = input.substring(baseStart, bracket)
            if (base.isNotEmpty() && reading.isNotEmpty()) {
                out += RubyPiece.Ruby(base, reading)
            } else if (base.isEmpty() && reading.isNotEmpty()) {
                // A slot such as [のりもの], not a reading for the word before it.
                out += RubyPiece.Text(reading)
            } else {
                out += RubyPiece.Text(input.substring(baseStart, end + 1))
            }
            i = end + 1
        }
        return out.filter { piece ->
            when (piece) {
                is RubyPiece.Text -> piece.text.isNotEmpty()
                is RubyPiece.Ruby -> true
            }
        }
    }

    /** Reading for ruby, plain text otherwise. 厳[きび]しい → きびしい. */
    fun spoken(input: String): String = buildString {
        for (piece in parse(input)) {
            when (piece) {
                is RubyPiece.Text -> append(piece.text)
                is RubyPiece.Ruby -> append(piece.reading)
            }
        }
    }.replace(Regex(" +"), " ").trim()

    fun hasBareKanji(input: String): Boolean {
        for (piece in parse(input)) {
            when (piece) {
                is RubyPiece.Text -> if (piece.text.any { it.isHan() }) return true
                is RubyPiece.Ruby -> if (piece.reading.any { it.isHan() } || piece.base.isEmpty()) return true
            }
        }
        return false
    }

    fun containsHan(input: String): Boolean = input.any { it.isHan() }
}

fun Char.isHan(): Boolean {
    return this in '\u4E00'..'\u9FFF' || this in '\u3400'..'\u4DBF' || this == '々' || this == '〆'
}
