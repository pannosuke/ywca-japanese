package dev.ericallerton.yoruben.core

enum class VerbGroup { GODAN, ICHIDAN, IRREGULAR }

object Conjugator {
    private val rows = listOf(
        "あいうえお",
        "かきくけこ",
        "さしすせそ",
        "たちつてと",
        "なにぬねの",
        "はひふへほ",
        "まみむめも",
        "やいゆえよ",
        "らりるれろ",
        "わいうえを",
    )

    fun groupOf(name: String): VerbGroup = when (name) {
        "ichidan" -> VerbGroup.ICHIDAN
        "irregular" -> VerbGroup.IRREGULAR
        else -> VerbGroup.GODAN
    }

    fun supports(form: String): Boolean = form in setOf(
        "dict", "masu", "masen", "mashita", "masen_deshita", "te", "ta", "te_kudasai",
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
        "te_kudasai" -> te(kana, group) + "ください"
        else -> adjective(kana, form)
    }

    fun accepts(kana: String, group: VerbGroup, form: String): List<String> {
        val primary = answer(kana, group, form)
        val extra = when (form) {
            "i_negative", "i_past", "i_past_negative" -> listOf(primary + "です")
            "na_negative" -> listOf(kana + "じゃないです", kana + "じゃありません")
            "na_past" -> listOf(kana + "でした")
            "na_past_negative" -> listOf(kana + "じゃなかったです", kana + "じゃありませんでした")
            else -> emptyList()
        }
        return (listOf(primary) + extra).distinct()
    }

    fun promptKana(kana: String, group: VerbGroup, form: String): String =
        if (form == "dict") polite(kana, group, "ます") else kana

    fun rule(kana: String, group: VerbGroup, form: String): Pair<String, String> {
        if (form.startsWith("i_") || form.startsWith("na_")) return adjectiveRule(form)
        val (ruby, en) = verbRule(kana, group, form)
        return ruby to en
    }

    private fun polite(kana: String, group: VerbGroup, ending: String): String = when {
        kana == "する" -> "し$ending"
        kana == "くる" -> "き$ending"
        group == VerbGroup.ICHIDAN -> kana.dropLast(1) + ending
        else -> iStem(kana) + ending
    }

    private fun iStem(kana: String): String {
        val last = kana.last()
        val row = rows.firstOrNull { last in it } ?: return kana
        return kana.dropLast(1) + row[1]
    }

    fun te(kana: String, group: VerbGroup): String {
        if (kana == "いく") return "いって"
        if (kana == "する") return "して"
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
        if (form == "te" || form == "te_kudasai" || form == "ta") {
            val ruby = when {
                kana == "いく" -> "「行[い]く」だけ て形[けい]は「って」。ほかの「く」は「いて」。"
                kana == "くる" -> "来[く]る は「来[き]て」。"
                kana == "する" -> "する は「して」。"
                group == VerbGroup.ICHIDAN -> "るどうし。る を とって「て」。"
                kana.endsWith("く") -> "うどうし。「く」は「いて」。"
                kana.endsWith("ぐ") -> "うどうし。「ぐ」は「いで」。"
                kana.endsWith("す") -> "うどうし。「す」は「して」。"
                kana.last() in "うつる" -> "うどうし。「う・つ・る」は「って」。"
                else -> "うどうし。「む・ぶ・ぬ」は「んで」。"
            }
            val en = when {
                kana == "いく" -> "iku is the exception: te-form is itte, not ite."
                kana == "くる" -> "kuru is irregular: kite."
                kana == "する" -> "suru is irregular: shite."
                group == VerbGroup.ICHIDAN -> "ru-verb. Drop ru, add te."
                kana.endsWith("す") -> "u-verb. su becomes shite."
                else -> "u-verb te-form."
            }
            return ruby to en
        }
        val ruby = when (group) {
            VerbGroup.IRREGULAR -> if (kana == "くる") "来[く]る は「来[き]ます」。" else "する は「します」。"
            VerbGroup.ICHIDAN -> "るどうし。る を ます に かえる。"
            VerbGroup.GODAN -> "うどうし。うだん を いだん に して ます。"
        }
        val en = when (group) {
            VerbGroup.IRREGULAR -> "Irregular."
            VerbGroup.ICHIDAN -> "ru-verb. Drop ru, add the masu ending."
            VerbGroup.GODAN -> "u-verb. Change the last kana to the i-row, then add masu."
        }
        return ruby to en
    }

    private fun adjectiveRule(form: String): Pair<String, String> = when {
        form.startsWith("i_") -> "いけいようし。い を とる。" to "i-adjective. Drop i, then add the ending."
        else -> "なけいようし。な は つけない。" to "na-adjective. Do not add na."
    }

    fun normalize(raw: String): String = raw.trim().replace(" ", "").replace("　", "").removeSuffix("。")

    fun matches(raw: String, accepted: List<String>): Boolean {
        val n = normalize(raw)
        return accepted.any { normalize(it) == n }
    }
}
