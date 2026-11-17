package dev.ericallerton.yoruben.core

import java.text.Normalizer

object AnswerCheck {
    fun normalize(raw: String): String {
        val nfkc = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
        val hiragana = buildString {
            for (ch in nfkc) {
                append(if (ch in 'ァ'..'ン') (ch.code - 0x60).toChar() else ch)
            }
        }
        return hiragana
            .replace(" ", "")
            .replace("　", "")
            .replace("・", "")
            .replace("〜", "")
            .replace("～", "")
            .replace("…", "")
            .replace(".", "")
            .replace("。", "")
            .replace("?", "")
            .replace("？", "")
            .lowercase()
    }

    fun matches(accept: List<String>, raw: String): Boolean {
        val given = normalize(raw)
        if (given.isEmpty()) return false
        val targets = accept.map { normalize(it) }.filter { it.isNotEmpty() }.toSet()
        if (given in targets) return true
        val withoutDesu = given.removeSuffix("です")
        return withoutDesu != given && withoutDesu in targets
    }

    fun mistake(mistakes: List<MistakeHint>, raw: String): MistakeHint? {
        val given = normalize(raw)
        if (given.isEmpty()) return null
        return mistakes.firstOrNull { hint ->
            val target = normalize(hint.answer)
            target == given || target == given.removeSuffix("です")
        }
    }
}
