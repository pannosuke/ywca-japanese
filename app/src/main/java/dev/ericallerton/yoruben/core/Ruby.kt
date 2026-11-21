package dev.ericallerton.yoruben.core

data class RubyPiece(val base: String, val reading: String)

object Ruby {
    fun isKanji(ch: Char): Boolean {
        if (ch == '\u3005') return true
        val block = Character.UnicodeBlock.of(ch)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    }

    fun parse(markup: String, known: Set<Char> = emptySet()): List<RubyPiece> {
        val out = mutableListOf<RubyPiece>()
        var i = 0
        val s = markup
        while (i < s.length) {
            if (isKanji(s[i])) {
                val start = i
                while (i < s.length && isKanji(s[i])) i++
                val base = s.substring(start, i)
                val reading = if (i < s.length && s[i] == '[') {
                    val end = s.indexOf(']', i + 1)
                    if (end < 0) "" else {
                        val r = s.substring(i + 1, end)
                        i = end + 1
                        r
                    }
                } else ""
                val hide = reading.isNotEmpty() && base.all { it in known }
                out += RubyPiece(base, if (hide) "" else reading)
            } else {
                val start = i
                while (i < s.length && !isKanji(s[i])) i++
                out += RubyPiece(s.substring(start, i), "")
            }
        }
        return out
    }

    /**
     * Chunks that must stay on one line. A kanji and the kana after it stay
     * together, and a space ends a chunk so the line can wrap there.
     */
    fun words(markup: String, known: Set<Char> = emptySet()): List<List<RubyPiece>> {
        val out = mutableListOf<MutableList<RubyPiece>>()
        var current = mutableListOf<RubyPiece>()
        fun flush() {
            if (current.isEmpty()) return
            out += current
            current = mutableListOf()
        }
        for (piece in parse(markup, known)) {
            if (piece.reading.isNotEmpty()) {
                current += piece
                continue
            }
            for (token in Regex("\\s+|\\S+").findAll(piece.base).map { it.value }) {
                if (token.any { it.isWhitespace() }) {
                    if (current.isNotEmpty()) current += RubyPiece(" ", "")
                    flush()
                } else {
                    current += RubyPiece(token, "")
                }
            }
        }
        flush()
        return out
    }

    /** Kanji runs that are shown without a reading and are not entirely known. */
    fun bareKanji(text: String, known: Set<Char> = emptySet()): List<String> {
        val found = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            if (!isKanji(text[i])) {
                i++
                continue
            }
            val start = i
            while (i < text.length && isKanji(text[i])) i++
            val run = text.substring(start, i)
            val hasReading = i < text.length && text[i] == '['
            if (!hasReading && run.any { it !in known }) found += run
        }
        return found
    }

    fun plain(markup: String): String = parse(markup).joinToString("") { it.base }
}
