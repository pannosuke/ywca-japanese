package dev.ericallerton.yoruben.core

import java.text.Normalizer

enum class VerbGroup { GODAN, ICHIDAN, IRREGULAR }

object Conjugator {
    private val rows = listOf(
        "あいうえお",
        "かきくけこ",
        "がぎぐげご",
        "さしすせそ",
        "たちつてと",
        "なにぬねの",
        "はひふへほ",
        "ばびぶべぼ",
        "ぱぴぷぺぽ",
        "まみむめも",
        "やいゆえよ",
        "らりるれろ",
        "わいうえを",
    )

    private const val RAI = "\u6765"

    fun groupOf(name: String): VerbGroup = when (name) {
        "ichidan" -> VerbGroup.ICHIDAN
        "irregular" -> VerbGroup.IRREGULAR
        else -> VerbGroup.GODAN
    }

    fun supports(form: String): Boolean = form in setOf(
        "dict", "masu", "masen", "mashita", "masen_deshita", "te", "ta", "nai", "te_kudasai",
        "i_negative", "i_past", "i_past_negative", "na_negative", "na_past", "na_past_negative",
    )

    fun answer(kana: String, group: VerbGroup, form: String): String = when (form) {
        "dict" -> kana
        "masu" -> polite(kana, group, "ます")
        "masen" -> polite(kana, group, "ません")
        "mashita" -> polite(kana, group, "ました")
        "masen_deshita" -> polite(kana, group, "ませんでした")
        "te" -> te(kana, group)
        "ta" -> ta(kana, group)
        "nai" -> nai(kana, group)
        "te_kudasai" -> te(kana, group) + "ください"
        else -> adjective(kana, form)
    }

    fun accepts(kana: String, group: VerbGroup, form: String, ruby: String = ""): List<String> {
        val primary = answer(kana, group, form)
        val extra = when (form) {
            "i_negative", "i_past", "i_past_negative" -> listOf(primary + "です")
            "na_negative" -> listOf(kana + "じゃないです", kana + "じゃありません")
            "na_past" -> listOf(kana + "でした")
            "na_past_negative" -> listOf(kana + "じゃなかったです", kana + "じゃありませんでした")
            else -> emptyList()
        }
        return (listOf(primary) + extra).flatMap { spoken ->
            listOf(spoken, spell(ruby, kana, spoken))
        }.distinct()
    }

    /** Kanji-plus-okurigana spelling built from the card ruby. Kana stays kana. */
    fun spell(ruby: String, dictKana: String, conjugated: String): String {
        if (ruby.isBlank()) return conjugated
        if (dictKana == "くる") return spellKuru(conjugated)
        val kanji = Ruby.parse(ruby).filter { it.reading.isNotEmpty() }
        if (kanji.isEmpty()) return conjugated
        val reading = kanji.joinToString("") { it.reading }
        val base = kanji.joinToString("") { it.base }
        return if (reading.isNotEmpty() && conjugated.startsWith(reading)) {
            base + conjugated.removePrefix(reading)
        } else {
            conjugated
        }
    }

    /** Same spelling, with a reading on every kanji, for the answer line. */
    fun spellRuby(ruby: String, dictKana: String, conjugated: String): String {
        if (spell(ruby, dictKana, conjugated) == conjugated) return conjugated
        if (dictKana == "くる") {
            return when {
                conjugated == "くる" -> "来[く]る"
                conjugated.startsWith("き") -> "来[き]${conjugated.removePrefix("き")}"
                conjugated.startsWith("こ") -> "来[こ]${conjugated.removePrefix("こ")}"
                else -> conjugated
            }
        }
        val kanji = Ruby.parse(ruby).filter { it.reading.isNotEmpty() }
        val reading = kanji.joinToString("") { it.reading }
        val base = kanji.joinToString("") { it.base }
        return if (reading.isNotEmpty() && conjugated.startsWith(reading)) {
            "$base[$reading]${conjugated.removePrefix(reading)}"
        } else {
            conjugated
        }
    }

    fun promptKana(kana: String, group: VerbGroup, form: String): String =
        if (form == "dict") polite(kana, group, "ます") else kana

    fun rule(kana: String, group: VerbGroup, form: String): Pair<String, String> {
        if (form.startsWith("i_") || form.startsWith("na_")) return adjectiveRule(form)
        return verbRule(kana, group, form)
    }

    private fun spellKuru(conjugated: String): String = when {
        conjugated == "くる" -> RAI + "る"
        conjugated.startsWith("き") -> RAI + conjugated.removePrefix("き")
        conjugated.startsWith("こ") -> RAI + conjugated.removePrefix("こ")
        else -> conjugated
    }

    /** Empty string for する itself. Null when this is not a する verb. */
    private fun suruStem(kana: String, group: VerbGroup): String? {
        if (kana == "する") return ""
        if (group == VerbGroup.IRREGULAR && kana.endsWith("する") && kana.length > 2) {
            return kana.removeSuffix("する")
        }
        return null
    }

    private fun polite(kana: String, group: VerbGroup, ending: String): String {
        suruStem(kana, group)?.let { return "${it}し$ending" }
        if (kana == "くる") return "き$ending"
        if (group == VerbGroup.ICHIDAN) return kana.dropLast(1) + ending
        return iStem(kana) + ending
    }

    private fun iStem(kana: String): String {
        val last = kana.last()
        val row = rows.firstOrNull { last in it } ?: return kana
        return kana.dropLast(1) + row[1]
    }

    fun te(kana: String, group: VerbGroup): String {
        if (kana == "いく") return "いって"
        suruStem(kana, group)?.let { return "${it}して" }
        if (kana == "くる") return "きて"
        if (group == VerbGroup.ICHIDAN) return kana.dropLast(1) + "て"
        val stem = kana.dropLast(1)
        return when (kana.last()) {
            'う', 'つ', 'る' -> stem + "って"
            'む', 'ぶ', 'ぬ' -> stem + "んで"
            'く' -> stem + "いて"
            'ぐ' -> stem + "いで"
            'す' -> stem + "して"
            else -> stem + "て"
        }
    }

    private fun ta(kana: String, group: VerbGroup): String {
        val t = te(kana, group)
        return when {
            t.endsWith("て") -> t.dropLast(1) + "た"
            t.endsWith("で") -> t.dropLast(1) + "だ"
            else -> t
        }
    }

    private fun nai(kana: String, group: VerbGroup): String {
        suruStem(kana, group)?.let { return "${it}しない" }
        if (kana == "くる") return "こない"
        if (group == VerbGroup.ICHIDAN) return kana.dropLast(1) + "ない"
        val last = kana.last()
        val stem = kana.dropLast(1)
        if (last == 'う') return stem + "わない"
        val row = rows.firstOrNull { last in it } ?: return stem + "ない"
        return stem + row[0] + "ない"
    }

    private fun adjective(kana: String, form: String): String {
        val stem = kana.removeSuffix("い")
        return when (form) {
            "i_negative" -> stem + "くない"
            "i_past" -> stem + "かった"
            "i_past_negative" -> stem + "くなかった"
            "na_negative" -> kana + "じゃない"
            "na_past" -> kana + "だった"
            "na_past_negative" -> kana + "じゃなかった"
            else -> error("unknown form $form")
        }
    }

    private fun verbRule(kana: String, group: VerbGroup, form: String): Pair<String, String> {
        val pair = when (form) {
            "dict" -> dictRule(kana, group)
            "masu" -> politeRule(kana, group, "ます", "masu")
            "masen" -> politeRule(kana, group, "ません", "masen")
            "mashita" -> politeRule(kana, group, "ました", "mashita")
            "masen_deshita" -> politeRule(kana, group, "ませんでした", "masen deshita")
            "te" -> teOrTa(kana, group, ta = false)
            "ta" -> teOrTa(kana, group, ta = true)
            "nai" -> naiRule(kana, group)
            else -> teOrTa(kana, group, ta = false)
        }
        if (form != "te_kudasai") return pair
        return (pair.first + " 「ください」を つけます。") to (pair.second + " Then add kudasai.")
    }

    private fun dictRule(kana: String, group: VerbGroup): Pair<String, String> {
        val stem = suruStem(kana, group)
        val godanRu = group == VerbGroup.GODAN && kana.endsWith("る")
        val word = if (stem == null) "" else if (stem.isEmpty()) "する" else "${stem}する"
        val ruby = when {
            stem != null -> "「${stem}します」は「$word」に もどします。"
            kana == "くる" -> "「きます」は「来[く]る」に もどします。"
            group == VerbGroup.ICHIDAN -> "「ます」を とって「る」を つけます。 るどうしです。"
            else -> "「ます」を とります。 いだん を うだん に もどします。 うどうしです。" +
                if (godanRu) " る で おわっても、 るどうし では ありません。" else ""
        }
        val en = when {
            stem != null -> "shimasu goes back to suru."
            kana == "くる" -> "kimasu goes back to kuru."
            group == VerbGroup.ICHIDAN -> "Drop ます and add ru. This is a ru-verb."
            godanRu -> "Drop ます and change the i-row back to the u-row. This is an u-verb. It ends in ru, but it is an u-verb."
            else -> "Drop ます and change the i-row back to the u-row. This is an u-verb."
        }
        return ruby to en
    }

    private fun politeRule(kana: String, group: VerbGroup, ending: String, endingEn: String): Pair<String, String> {
        val stem = suruStem(kana, group)
        val godanRu = group == VerbGroup.GODAN && kana.endsWith("る")
        val ruby = when {
            stem != null -> {
                val word = if (stem.isEmpty()) "する" else "${stem}する"
                "「$word」は「${stem}し$ending」。"
            }
            kana == "くる" -> "「来[く]る」は「来[き]$ending」。"
            group == VerbGroup.ICHIDAN -> "るどうしです。 「る」を とって「$ending」。"
            else -> "うどうしです。 うだん を いだん に して「$ending」。" +
                if (godanRu) " る で おわっても、 るどうし では ありません。" else ""
        }
        val en = when {
            stem != null -> "suru becomes shi + $endingEn."
            kana == "くる" -> "kuru becomes ki + $endingEn."
            group == VerbGroup.ICHIDAN -> "ru-verb. Drop ru and add $endingEn."
            godanRu -> "u-verb. Change the last kana to the i-row, then add $endingEn. It ends in ru, but it is an u-verb."
            else -> "u-verb. Change the last kana to the i-row, then add $endingEn."
        }
        return ruby to en
    }

    private fun teOrTa(kana: String, group: VerbGroup, ta: Boolean): Pair<String, String> {
        val stem = suruStem(kana, group)
        val ruby = when {
            kana == "いく" && !ta -> "「行[い]く」だけ て形[けい]は「って」です。 ほかの「く」は「いて」です。 うどうしです。"
            kana == "いく" && ta -> "「行[い]く」だけ た形[けい]は「った」です。 ほかの「く」は「いた」です。 うどうしです。"
            kana == "くる" && !ta -> "「来[く]る」は「来[き]て」です。"
            kana == "くる" && ta -> "「来[く]る」は「来[き]た」です。"
            stem != null && !ta -> "「${if (stem.isEmpty()) "する" else "${stem}する"}」は「${stem}して」です。"
            stem != null && ta -> "「${if (stem.isEmpty()) "する" else "${stem}する"}」は「${stem}した」です。"
            group == VerbGroup.ICHIDAN && !ta -> "るどうしです。 「る」を とって「て」。"
            group == VerbGroup.ICHIDAN && ta -> "るどうしです。 「る」を とって「た」。"
            group == VerbGroup.GODAN && kana.endsWith("る") && !ta ->
                "うどうしです。 る で おわっても「って」です。 るどうし では ありません。"
            group == VerbGroup.GODAN && kana.endsWith("る") && ta ->
                "うどうしです。 る で おわっても「った」です。 るどうし では ありません。"
            kana.endsWith("く") && !ta -> "うどうしです。 「く」は「いて」。"
            kana.endsWith("く") && ta -> "うどうしです。 「く」は「いた」。"
            kana.endsWith("ぐ") && !ta -> "うどうしです。 「ぐ」は「いで」。"
            kana.endsWith("ぐ") && ta -> "うどうしです。 「ぐ」は「いだ」。"
            kana.endsWith("す") && !ta -> "うどうしです。 「す」は「して」。"
            kana.endsWith("す") && ta -> "うどうしです。 「す」は「した」。"
            kana.last() in "うつ" && !ta -> "うどうしです。 「う・つ」は「って」。"
            kana.last() in "うつ" && ta -> "うどうしです。 「う・つ」は「った」。"
            !ta -> "うどうしです。 「む・ぶ・ぬ」は「んで」。"
            else -> "うどうしです。 「む・ぶ・ぬ」は「んだ」。"
        }
        val en = when {
            kana == "いく" && !ta -> "u-verb. iku is the exception: te-form is itte, not ite."
            kana == "いく" && ta -> "u-verb. iku is the exception: ta-form is itta, not ita."
            kana == "くる" && !ta -> "kuru is irregular: kite."
            kana == "くる" && ta -> "kuru is irregular: kita."
            stem != null && !ta -> "suru becomes shite."
            stem != null && ta -> "suru becomes shita."
            group == VerbGroup.ICHIDAN && !ta -> "ru-verb. Drop ru and add te."
            group == VerbGroup.ICHIDAN && ta -> "ru-verb. Drop ru and add ta."
            group == VerbGroup.GODAN && kana.endsWith("る") && !ta ->
                "u-verb. It ends in ru, but te-form is tte, not te."
            group == VerbGroup.GODAN && kana.endsWith("る") && ta ->
                "u-verb. It ends in ru, but ta-form is tta, not ta."
            kana.endsWith("す") && !ta -> "u-verb. su becomes shite."
            kana.endsWith("す") && ta -> "u-verb. su becomes shita."
            !ta -> "u-verb. This is the te-form."
            else -> "u-verb. This is the ta-form."
        }
        return ruby to en
    }

    private fun naiRule(kana: String, group: VerbGroup): Pair<String, String> {
        val stem = suruStem(kana, group)
        return when {
            stem != null -> {
                val word = if (stem.isEmpty()) "する" else "${stem}する"
                "「$word」は「${stem}しない」です。" to "suru becomes shinai."
            }
            kana == "くる" -> "「来[く]る」は「来[こ]ない」です。" to "kuru becomes konai."
            group == VerbGroup.ICHIDAN -> "るどうしです。 「る」を とって「ない」。" to "ru-verb. Drop ru and add nai."
            group == VerbGroup.GODAN && kana.endsWith("る") ->
                "うどうしです。 る で おわっても「らない」です。 るどうし では ありません。" to
                    "u-verb. It ends in ru, so the negative is ranai, not nai."
            kana.endsWith("う") -> "うどうしです。 「う」は「わない」です。" to "u-verb. A verb ending in u takes wanai."
            else -> "うどうしです。 うだん を あだん に して「ない」。" to "u-verb. Change the last kana to the a-row, then add nai."
        }
    }

    private fun adjectiveRule(form: String): Pair<String, String> = when (form) {
        "i_negative" -> "いけいようしです。 「い」を とって「くない」。" to "i-adjective. Drop i and add kunai."
        "i_past" -> "いけいようしです。 「い」を とって「かった」。" to "i-adjective. Drop i and add katta."
        "i_past_negative" -> "いけいようしです。 「い」を とって「くなかった」。" to "i-adjective. Drop i and add kunakatta."
        "na_negative" -> "なけいようしです。 「じゃない」を つけます。 な は つけません。" to "na-adjective. Add janai. Do not add na."
        "na_past" -> "なけいようしです。 「だった」を つけます。 な は つけません。" to "na-adjective. Add datta. Do not add na."
        else -> "なけいようしです。 「じゃなかった」を つけます。 な は つけません。" to "na-adjective. Add janakatta. Do not add na."
    }

    fun normalize(raw: String): String {
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val hiragana = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            hiragana.append(if (ch in '\u30A1'..'\u30F6') (ch.code - 0x60).toChar() else ch)
        }
        return hiragana.toString().replace(Regex("\\s+"), "").removeSuffix("。")
    }

    fun matches(raw: String, accepted: List<String>): Boolean {
        val n = normalize(raw)
        return accepted.any { normalize(it) == n }
    }
}
