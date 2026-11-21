package dev.ericallerton.yoruben.core

data class KanjiMnemonic(
    val kanji: String,
    val parts: String,
    val english: String,
    val japanese: String,
)

data class CardNote(
    val prose: String,
    val intro: String,
    val items: List<KanjiMnemonic>,
) {
    val hasKanji: Boolean get() = intro.isNotBlank() || items.isNotEmpty()
}

object CardNotes {
    private val opener = Regex("""<(div|span) class="(km-k|km-parts|km-en|km-ja)">""")

    fun parse(raw: String): CardNote {
        val start = raw.indexOf("<div class=\"kanji-mnemonic\">")
        val prose = clean(if (start < 0) raw else raw.substring(0, start))
        if (start < 0) return CardNote(prose, "", emptyList())
        val html = raw.substring(start)
        val items = mutableListOf<KanjiMnemonic>()
        var intro = ""
        var kanji = ""
        var parts = ""
        var english = ""
        var japanese = ""
        var seen = false
        fun flush() {
            if (kanji.isBlank() && parts.isBlank() && english.isBlank() && japanese.isBlank()) return
            items += KanjiMnemonic(kanji, parts, english, japanese)
            kanji = ""
            parts = ""
            english = ""
            japanese = ""
        }
        for ((kind, body) in pieces(html)) {
            val text = clean(body)
            if (text.isBlank()) continue
            when (kind) {
                "km-k" -> {
                    flush()
                    kanji = text
                    seen = true
                }
                "km-parts" -> parts = text
                "km-en" -> if (!seen && kanji.isBlank()) intro = text else english = text
                "km-ja" -> japanese = text
            }
        }
        flush()
        return CardNote(prose, intro, items)
    }

    private fun pieces(html: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var from = 0
        while (from < html.length) {
            val match = opener.find(html, from) ?: break
            val tag = match.groupValues[1]
            val kind = match.groupValues[2]
            val close = "</$tag>"
            val end = html.indexOf(close, match.range.last + 1)
            if (end < 0) break
            out += kind to html.substring(match.range.last + 1, end)
            from = end + close.length
        }
        return out
    }

    private fun clean(raw: String): String {
        val text = raw
            .replace(Regex("<(script|style)\\b[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), "")
        return unescape(text).trim()
    }

    private fun unescape(raw: String): String = buildString {
        var i = 0
        while (i < raw.length) {
            if (raw[i] != '&') {
                append(raw[i])
                i++
                continue
            }
            val semi = raw.indexOf(';', i + 1)
            val entity = if (semi > i) raw.substring(i + 1, semi) else ""
            val mapped = when (entity) {
                "amp" -> "&"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "apos", "#39" -> "'"
                "nbsp" -> " "
                else -> when {
                    entity.startsWith("#x") -> entity.drop(2).toIntOrNull(16)?.toChar()?.toString()
                    entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.toChar()?.toString()
                    else -> null
                }
            }
            if (mapped == null) {
                append(raw[i])
                i++
            } else {
                append(mapped)
                i = semi + 1
            }
        }
    }
}
