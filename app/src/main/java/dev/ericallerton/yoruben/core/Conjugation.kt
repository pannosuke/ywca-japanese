package dev.ericallerton.yoruben.core

enum class VerbGroup { GODAN, ICHIDAN, IRREGULAR }

enum class VerbForm { MASU, MASEN, MASHITA, MASEN_DESHITA, TE, TA }

data class Verb(
    val id: String,
    val dictRuby: String,
    val dictKana: String,
    val group: VerbGroup,
    val glossEn: String,
    val lessonDate: String?,
    val fromCurriculum: Boolean,
)

data class Conjugated(
    val kana: String,
    val ruby: String,
    val ruleId: String,
    val explainRuby: String,
    val explainEn: String,
)

object Conjugator {
    fun conjugate(verb: Verb, form: VerbForm): Conjugated {
        val kana = kana(verb, form)
        val rule = ruleId(verb, form)
        val (explainRuby, explainEn) = explain(verb, form, rule)
        return Conjugated(kana, rubyFor(verb, kana), rule, explainRuby, explainEn)
    }

    fun ruleId(verb: Verb, form: VerbForm): String = when (form) {
        VerbForm.TE, VerbForm.TA -> teRule(verb)
        VerbForm.MASU -> masuRule(verb)
        VerbForm.MASEN -> "neg-polite"
        VerbForm.MASHITA -> "past-polite"
        VerbForm.MASEN_DESHITA -> "past-neg-polite"
    }

    fun groupId(verb: Verb): String = when (verb.group) {
        VerbGroup.GODAN -> "group-1"
        VerbGroup.ICHIDAN -> "group-2"
        VerbGroup.IRREGULAR -> "group-3"
    }

    fun groupExplain(verb: Verb): Pair<String, String> = when (verb.group) {
        VerbGroup.GODAN ->
            "グループ1。うだんの どうし。ます は いだん＋ます。" to
                "Group 1, an u-verb. The ます-form uses the i-row."
        VerbGroup.ICHIDAN ->
            "グループ2。る を とって ます。" to
                "Group 2. Drop る, then add ます."
        VerbGroup.IRREGULAR ->
            "グループ3。する と 来[く]る。" to
                "Group 3. する and 来る, and verbs built on する."
    }

    private fun kana(verb: Verb, form: VerbForm): String = when {
        verb.dictKana == "くる" -> kuru(form)
        verb.dictKana.endsWith("する") -> {
            val head = verb.dictKana.removeSuffix("する")
            head + suruEnding(form)
        }
        verb.group == VerbGroup.ICHIDAN -> verb.dictKana.removeSuffix("る") + ichidanEnding(form)
        else -> godan(verb, form)
    }

    private fun godan(verb: Verb, form: VerbForm): String {
        val dict = verb.dictKana
        val stem = dict.dropLast(1)
        val iRow = I_ROW.getValue(dict.last())
        val polite = stem + iRow
        return when (form) {
            VerbForm.MASU -> polite + "ます"
            VerbForm.MASEN -> polite + "ません"
            VerbForm.MASHITA -> polite + "ました"
            VerbForm.MASEN_DESHITA -> polite + "ませんでした"
            VerbForm.TE -> teGodan(dict)
            VerbForm.TA -> taFromTe(teGodan(dict))
        }
    }

    private fun teGodan(dict: String): String {
        if (dict == "いく") return "いって"
        val stem = dict.dropLast(1)
        return when (dict.last()) {
            'う', 'つ', 'る' -> stem + "って"
            'む', 'ぶ', 'ぬ' -> stem + "んで"
            'く' -> stem + "いて"
            'ぐ' -> stem + "いで"
            'す' -> stem + "して"
            else -> error("not a godan verb: $dict")
        }
    }

    private fun taFromTe(te: String): String = when {
        te.endsWith("って") -> te.removeSuffix("って") + "った"
        te.endsWith("んで") -> te.removeSuffix("んで") + "んだ"
        te.endsWith("いて") -> te.removeSuffix("いて") + "いた"
        te.endsWith("いで") -> te.removeSuffix("いで") + "いだ"
        te.endsWith("して") -> te.removeSuffix("して") + "した"
        te.endsWith("て") -> te.removeSuffix("て") + "た"
        else -> te.dropLast(1) + "た"
    }

    private fun kuru(form: VerbForm): String = when (form) {
        VerbForm.MASU -> "きます"
        VerbForm.MASEN -> "きません"
        VerbForm.MASHITA -> "きました"
        VerbForm.MASEN_DESHITA -> "きませんでした"
        VerbForm.TE -> "きて"
        VerbForm.TA -> "きた"
    }

    private fun suruEnding(form: VerbForm): String = when (form) {
        VerbForm.MASU -> "します"
        VerbForm.MASEN -> "しません"
        VerbForm.MASHITA -> "しました"
        VerbForm.MASEN_DESHITA -> "しませんでした"
        VerbForm.TE -> "して"
        VerbForm.TA -> "した"
    }

    private fun ichidanEnding(form: VerbForm): String = when (form) {
        VerbForm.MASU -> "ます"
        VerbForm.MASEN -> "ません"
        VerbForm.MASHITA -> "ました"
        VerbForm.MASEN_DESHITA -> "ませんでした"
        VerbForm.TE -> "て"
        VerbForm.TA -> "た"
    }

    private fun teRule(verb: Verb): String = when {
        verb.dictKana == "くる" -> "te-kuru"
        verb.dictKana.endsWith("する") -> "te-suru"
        verb.group == VerbGroup.ICHIDAN -> "te-ichidan"
        verb.dictKana == "いく" -> "te-iku"
        verb.dictKana.last() == 'く' -> "te-ite"
        verb.dictKana.last() == 'ぐ' -> "te-ide"
        verb.dictKana.last() == 'す' -> "te-shite"
        verb.dictKana.last() in "うつる" -> "te-tte"
        else -> "te-nde"
    }

    private fun masuRule(verb: Verb): String = when {
        verb.dictKana == "くる" -> "masu-kuru"
        verb.dictKana.endsWith("する") -> "masu-suru"
        verb.group == VerbGroup.ICHIDAN -> "masu-ichidan"
        else -> "masu-godan"
    }

    private fun explain(verb: Verb, form: VerbForm, rule: String): Pair<String, String> {
        val done = rubyFor(verb, kana(verb, form))
        val te = when (rule) {
            "te-iku" -> "「行[い]く」だけ て形[けい]は「って」。ほかの「く」は「いて」。" to
                "行く is the one く-verb whose て-form is って. Other く-verbs use いて."
            "te-ite" -> "グループ1。「く」は「いて」。" to "Group 1. A verb ending in く changes to いて."
            "te-ide" -> "グループ1。「ぐ」は「いで」。" to "Group 1. A verb ending in ぐ changes to いで."
            "te-shite" -> "グループ1。「す」は「して」。" to "Group 1. A verb ending in す changes to して."
            "te-tte" -> "グループ1。「う・つ・る」は「って」。" to "Group 1. Endings う, つ, and る change to って."
            "te-nde" -> "グループ1。「む・ぶ・ぬ」は「んで」。" to "Group 1. Endings む, ぶ, and ぬ change to んで."
            "te-ichidan" -> "グループ2。る を とって「て」。" to "Group 2. Drop る and add て."
            "te-suru" -> "する は「して」。" to "する becomes して."
            "te-kuru" -> "来[く]る は「来[き]て」。" to "来る becomes きて."
            else -> null
        }
        if ((form == VerbForm.TE || form == VerbForm.TA) && te != null) {
            val tail = if (form == VerbForm.TA) " た形[けい]も おなじ おとの へんか。" to " The plain past uses the same sound change." else "" to ""
            return (te.first + tail.first + " → $done") to (te.second + tail.second)
        }
        val polite = when (verb.group) {
            VerbGroup.GODAN -> "グループ1。いだん＋${endingLabel(form)}。" to "Group 1. Use the i-row, then ${form.name.lowercase()}."
            VerbGroup.ICHIDAN -> "グループ2。る を とって${endingLabel(form)}。" to "Group 2. Drop る, then add the ending."
            VerbGroup.IRREGULAR -> "グループ3。する は し〜、来[く]る は き〜。" to "Group 3. する uses し〜 and 来る uses き〜."
        }
        return (polite.first + " → $done") to polite.second
    }

    private fun endingLabel(form: VerbForm): String = when (form) {
        VerbForm.MASU -> "ます"
        VerbForm.MASEN -> "ません"
        VerbForm.MASHITA -> "ました"
        VerbForm.MASEN_DESHITA -> "ませんでした"
        VerbForm.TE -> "て"
        VerbForm.TA -> "た"
    }

    fun rubyFor(verb: Verb, kana: String): String {
        if (verb.dictKana == "くる") {
            val rest = kana.removePrefix("き")
            return "来[き]$rest"
        }
        val mark = verb.dictRuby.indexOf('[')
        val end = verb.dictRuby.indexOf(']')
        if (mark <= 0 || end < 0) return kana
        val kanji = verb.dictRuby.substring(0, mark)
        val reading = verb.dictRuby.substring(mark + 1, end)
        if (!kana.startsWith(reading)) return kana
        return kanji + "[" + reading + "]" + kana.removePrefix(reading)
    }

    fun choices(verb: Verb, form: VerbForm, random: kotlin.random.Random): List<String> {
        val correct = kana(verb, form)
        val wrongs = if (form == VerbForm.TE || form == VerbForm.TA) {
            teWrongs(verb, form, correct)
        } else {
            politeWrongs(verb, form, correct)
        }
        return (wrongs + correct).distinct().shuffled(random).take(4).let { picked ->
            if (correct in picked) picked else (picked.dropLast(1) + correct).shuffled(random)
        }
    }

    private fun teWrongs(verb: Verb, form: VerbForm, correct: String): List<String> {
        val stem = verb.dictKana.dropLast(1)
        val tes = listOf(
            stem + "って",
            stem + "んで",
            stem + "いて",
            stem + "いで",
            stem + "して",
            stem + "て",
            "いって",
            "きて",
            "して",
        ).map { if (form == VerbForm.TA) taFromTe(it) else it }
        return tes.filter { it != correct }
    }

    private fun politeWrongs(verb: Verb, form: VerbForm, correct: String): List<String> {
        val dict = verb.dictKana
        val guesses = listOf(
            dict.dropLast(1) + "ます",
            dict + "ます",
            dict.dropLast(1) + "ります",
            dict.dropLast(1) + "します",
            "します",
            "きます",
            "きます",
        )
        return guesses.map { swapEnding(it, form) }.filter { it != correct }
    }

    private fun swapEnding(masu: String, form: VerbForm): String {
        val stem = masu.removeSuffix("ます")
        return stem + endingLabel(form)
    }

    private val I_ROW = mapOf(
        'う' to 'い',
        'く' to 'き',
        'ぐ' to 'ぎ',
        'す' to 'し',
        'つ' to 'ち',
        'ぬ' to 'に',
        'ぶ' to 'び',
        'む' to 'み',
        'る' to 'り',
    )
}
