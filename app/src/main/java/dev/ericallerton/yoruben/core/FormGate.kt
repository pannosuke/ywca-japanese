package dev.ericallerton.yoruben.core

object FormGate {
    fun covered(lessons: List<Lesson>, today: String): Set<VerbForm> {
        val past = lessons.filter { it.date <= today }
        if (past.isEmpty()) return emptySet()
        val forms = mutableSetOf(VerbForm.MASU)
        for (lesson in past) {
            val blob = buildString {
                append(lesson.topicRuby)
                append(lesson.topicEn)
                lesson.grammar.forEach {
                    append(it.patternRuby)
                    append(it.explanationJa)
                    append(it.exampleRuby)
                }
            }
            if ("ました" in blob || "past tense" in lesson.topicEn.lowercase()) forms += VerbForm.MASHITA
            if ("ませんでした" in blob) forms += VerbForm.MASEN_DESHITA
            if ("ません" in blob) forms += VerbForm.MASEN
            if ("て形" in blob || "てください" in blob || "te-form" in lesson.topicEn.lowercase()) {
                forms += VerbForm.TE
            }
        }
        return forms
    }

    fun forRun(covered: Set<VerbForm>, preview: Boolean): Set<VerbForm> {
        return if (preview) covered + VerbForm.TA else covered
    }
}
