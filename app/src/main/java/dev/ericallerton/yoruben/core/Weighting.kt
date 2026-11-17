package dev.ericallerton.yoruben.core

data class AnswerRecord(
    val verbId: String,
    val ruleId: String,
    val correct: Boolean,
    val atEpochMs: Long,
)

object Weighting {
    fun weight(verb: Verb, ruleId: String, history: List<AnswerRecord>, now: Long): Double {
        var score = if (verb.fromCurriculum) 2.0 else 1.0
        for (row in history) {
            val sameVerb = row.verbId == verb.id
            val sameRule = row.ruleId == ruleId
            if (!sameVerb && !sameRule) continue
            val days = ((now - row.atEpochMs).coerceAtLeast(0L)) / 86_400_000.0
            val spaced = when {
                days < 0.5 -> 1.5
                days < 3.0 -> 2.3
                days < 14.0 -> 1.3
                else -> 0.5
            }
            val scale = if (sameVerb) 1.0 else 0.25
            if (row.correct) score -= 0.35 * scale else score += spaced * scale
        }
        return score.coerceAtLeast(0.25)
    }

    fun <T> pick(items: List<T>, weights: List<Double>, random: kotlin.random.Random): T {
        val total = weights.sum().coerceAtLeast(0.01)
        var roll = random.nextDouble() * total
        for (i in items.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return items[i]
        }
        return items.last()
    }
}
