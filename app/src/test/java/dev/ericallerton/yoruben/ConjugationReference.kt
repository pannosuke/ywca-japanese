package dev.ericallerton.yoruben

/**
 * A second conjugation of the class verbs. It does not call [dev.ericallerton.yoruben.core.Conjugator].
 * The audit test fails when the two disagree.
 */
object ConjugationReference {
    private val iRow = mapOf(
        'う' to 'い', 'く' to 'き', 'ぐ' to 'ぎ', 'す' to 'し', 'つ' to 'ち',
        'ぬ' to 'に', 'ぶ' to 'び', 'む' to 'み', 'る' to 'り',
    )
    private val aRow = mapOf(
        'う' to 'わ', 'く' to 'か', 'ぐ' to 'が', 'す' to 'さ', 'つ' to 'た',
        'ぬ' to 'な', 'ぶ' to 'ば', 'む' to 'ま', 'る' to 'ら',
    )

    fun answer(kana: String, group: String, form: String): String = when (form) {
        "dict" -> kana
        "masu" -> polite(kana, group, "ます")
        "masen" -> polite(kana, group, "ません")
        "mashita" -> polite(kana, group, "ました")
        "masen_deshita" -> polite(kana, group, "ませんでした")
        "te" -> te(kana, group)
        "ta" -> voiced(te(kana, group))
        "nai" -> nai(kana, group)
        "te_kudasai" -> te(kana, group) + "ください"
        "i_negative" -> kana.removeSuffix("い") + "くない"
        "i_past" -> kana.removeSuffix("い") + "かった"
        "i_past_negative" -> kana.removeSuffix("い") + "くなかった"
        "na_negative" -> kana + "じゃない"
        "na_past" -> kana + "だった"
        "na_past_negative" -> kana + "じゃなかった"
        else -> error(form)
    }

    fun kanji(ruby: String, dictKana: String, conjugated: String): String {
        if (dictKana == "くる") {
            return when {
                conjugated == "くる" -> "来る"
                conjugated.startsWith("き") -> "来" + conjugated.removePrefix("き")
                conjugated.startsWith("こ") -> "来" + conjugated.removePrefix("こ")
                else -> conjugated
            }
        }
        val reading = Regex("\\[([^\\]]+)]").findAll(ruby).joinToString("") { it.groupValues[1] }
        if (reading.isEmpty() || !conjugated.startsWith(reading)) return conjugated
        val base = Regex("([\\u4E00-\\u9FFF\\u3005]+)\\[").findAll(ruby).joinToString("") { it.groupValues[1] }
        return base + conjugated.removePrefix(reading)
    }

    fun te(kana: String, group: String): String {
        if (kana == "いく") return "いって"
        suruStem(kana, group)?.let { return "${it}して" }
        if (kana == "くる") return "きて"
        if (group == "ichidan") return kana.dropLast(1) + "て"
        val stem = kana.dropLast(1)
        return when (kana.last()) {
            'う', 'つ', 'る' -> stem + "って"
            'く' -> stem + "いて"
            'ぐ' -> stem + "いで"
            'す' -> stem + "して"
            'ぬ', 'ぶ', 'む' -> stem + "んで"
            else -> error(kana)
        }
    }

    private fun polite(kana: String, group: String, ending: String): String {
        suruStem(kana, group)?.let { return "${it}し$ending" }
        if (kana == "くる") return "き$ending"
        if (group == "ichidan") return kana.dropLast(1) + ending
        val last = kana.last()
        return kana.dropLast(1) + (iRow[last] ?: error(kana)) + ending
    }

    private fun nai(kana: String, group: String): String {
        suruStem(kana, group)?.let { return "${it}しない" }
        if (kana == "くる") return "こない"
        if (group == "ichidan") return kana.dropLast(1) + "ない"
        val last = kana.last()
        return kana.dropLast(1) + (aRow[last] ?: error(kana)) + "ない"
    }

    private fun voiced(te: String): String = when {
        te.endsWith("て") -> te.dropLast(1) + "た"
        te.endsWith("で") -> te.dropLast(1) + "だ"
        else -> te
    }

    private fun suruStem(kana: String, group: String): String? {
        if (kana == "する") return ""
        if (group == "irregular" && kana.endsWith("する") && kana.length > 2) return kana.removeSuffix("する")
        return null
    }
}
