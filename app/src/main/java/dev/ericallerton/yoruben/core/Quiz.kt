package dev.ericallerton.yoruben.core

import kotlin.random.Random

enum class QuizMode { Mix, Words, Grammar, Weak }

fun quizMode(route: String): QuizMode = when (route) {
    "words" -> QuizMode.Words
    "grammar" -> QuizMode.Grammar
    "weak" -> QuizMode.Weak
    else -> QuizMode.Mix
}

data class AttemptRecord(val itemId: String, val correct: Boolean)

data class QuizItem(
    val id: String,
    val kind: String,
    val lessonDate: String?,
    val weakSpotId: String?,
    val weight: Int,
    val promptRuby: String,
    val promptEn: String,
    val choices: List<QuizChoice>,
    val answerId: String?,
    val accept: List<String>,
    val mistakes: List<MistakeHint>,
    val explainRuby: String,
    val explainEn: String,
)

object QuizFactory {
    fun session(
        curriculum: Curriculum,
        mode: QuizMode,
        count: Int,
        history: List<AttemptRecord>,
        random: Random,
    ): List<QuizItem> {
        val pool = buildPool(curriculum, mode, random)
        if (pool.isEmpty()) return emptyList()
        return pickWeighted(pool, count.coerceAtLeast(1), random) { item ->
            val misses = history.count { it.itemId == item.id && !it.correct }
            item.weight + (misses * 2).coerceAtMost(6)
        }
    }

    fun buildPool(curriculum: Curriculum, mode: QuizMode, random: Random): List<QuizItem> {
        val items = mutableListOf<QuizItem>()
        if (mode == QuizMode.Mix || mode == QuizMode.Words) {
            items += wordItems(curriculum, random)
        }
        if (mode == QuizMode.Mix || mode == QuizMode.Grammar) {
            items += grammarItems(curriculum, random)
        }
        if (mode == QuizMode.Mix || mode == QuizMode.Weak) {
            items += fixedItems(curriculum)
        }
        return items
    }

    private fun wordItems(curriculum: Curriculum, random: Random): List<QuizItem> {
        val study = curriculum.words.filter { it.studying }
        val glosses = curriculum.words.filter { it.glossJa.isNotBlank() }
        return study.flatMap { word ->
            val weight = if (word.id in curriculum.weakWordIds) 5 else 2
            val accept = (listOf(word.kana) + word.accept).distinct()
            listOf(
                multipleChoice(
                    id = "word-mc-${word.id}",
                    kind = "word_mc",
                    lessonDate = word.lessonDate,
                    weakSpotId = curriculum.profile.weakSpots.firstOrNull { word.id in it.wordIds }?.id,
                    weight = weight,
                    promptRuby = word.ruby,
                    promptEn = "どの いみ？",
                    correct = QuizChoice(word.id, word.glossJa, word.glossEn),
                    distractors = glosses
                        .filter { it.id != word.id && it.glossJa != word.glossJa }
                        .shuffled(random)
                        .take(3)
                        .map { QuizChoice(it.id, it.glossJa, it.glossEn) },
                    explainRuby = word.ruby,
                    explainEn = word.glossEn,
                    mistakes = emptyList(),
                    random = random,
                ),
                QuizItem(
                    id = "word-fill-${word.id}",
                    kind = "word_fill",
                    lessonDate = word.lessonDate,
                    weakSpotId = curriculum.profile.weakSpots.firstOrNull { word.id in it.wordIds }?.id,
                    weight = weight,
                    promptRuby = word.glossJa,
                    promptEn = word.glossEn,
                    choices = emptyList(),
                    answerId = null,
                    accept = accept,
                    mistakes = emptyList(),
                    explainRuby = word.ruby,
                    explainEn = word.kana,
                ),
            )
        }
    }

    private fun grammarItems(curriculum: Curriculum, random: Random): List<QuizItem> {
        val all = curriculum.lessons.flatMap { lesson -> lesson.grammar.map { lesson to it } }
        return all.flatMap { (lesson, point) ->
            val weight = if (point.weakSpotId != null) 4 else 1
            val distractors = all
                .filter { it.second.id != point.id }
                .map { it.second }
                .distinctBy { it.patternRuby }
                .shuffled(random)
                .take(3)
                .map { QuizChoice(it.id, it.patternRuby, it.explanationEn) }
            listOf(
                multipleChoice(
                    id = "grammar-mc-${point.id}",
                    kind = "grammar_mc",
                    lessonDate = lesson.date,
                    weakSpotId = point.weakSpotId,
                    weight = weight,
                    promptRuby = point.exampleRuby,
                    promptEn = "どの かたち？  " + point.exampleEn,
                    correct = QuizChoice(point.id, point.patternRuby, point.explanationEn),
                    distractors = distractors,
                    explainRuby = point.patternRuby,
                    explainEn = point.explanationEn,
                    mistakes = emptyList(),
                    random = random,
                ),
                QuizItem(
                    id = "grammar-fill-${point.id}",
                    kind = "grammar_fill",
                    lessonDate = lesson.date,
                    weakSpotId = point.weakSpotId,
                    weight = weight,
                    promptRuby = point.clozeRuby,
                    promptEn = point.clozeHintEn,
                    choices = emptyList(),
                    answerId = null,
                    accept = (point.clozeAccept + point.clozeAnswer).distinct(),
                    mistakes = point.mistakes,
                    explainRuby = point.exampleRuby,
                    explainEn = point.explanationEn,
                ),
            )
        }
    }

    private fun fixedItems(curriculum: Curriculum): List<QuizItem> {
        return curriculum.profile.weakSpots.flatMap { spot ->
            spot.items.map { item ->
                QuizItem(
                    id = item.id,
                    kind = if (item.mode == "fill") "weak_fill" else "weak_mc",
                    lessonDate = item.lessonDate,
                    weakSpotId = spot.id,
                    weight = spot.weight,
                    promptRuby = item.promptRuby,
                    promptEn = item.promptEn,
                    choices = item.choices,
                    answerId = item.answerId.ifBlank { null },
                    accept = item.accept,
                    mistakes = item.mistakes,
                    explainRuby = item.explainRuby,
                    explainEn = item.explainEn,
                )
            }
        }
    }

    private fun multipleChoice(
        id: String,
        kind: String,
        lessonDate: String?,
        weakSpotId: String?,
        weight: Int,
        promptRuby: String,
        promptEn: String,
        correct: QuizChoice,
        distractors: List<QuizChoice>,
        explainRuby: String,
        explainEn: String,
        mistakes: List<MistakeHint>,
        random: Random,
    ): QuizItem {
        return QuizItem(
            id = id,
            kind = kind,
            lessonDate = lessonDate,
            weakSpotId = weakSpotId,
            weight = weight,
            promptRuby = promptRuby,
            promptEn = promptEn,
            choices = (distractors + correct).distinctBy { it.ruby }.shuffled(random),
            answerId = correct.id,
            accept = emptyList(),
            mistakes = mistakes,
            explainRuby = explainRuby,
            explainEn = explainEn,
        )
    }
}

fun <T> pickWeighted(pool: List<T>, count: Int, random: Random, weightOf: (T) -> Int): List<T> {
    val remaining = pool.toMutableList()
    val picked = mutableListOf<T>()
    repeat(count.coerceAtMost(remaining.size)) {
        val total = remaining.sumOf { weightOf(it).coerceAtLeast(1) }
        var ticket = random.nextInt(total)
        val index = remaining.indexOfFirst { item ->
            ticket -= weightOf(item).coerceAtLeast(1)
            ticket < 0
        }.let { if (it < 0) remaining.lastIndex else it }
        picked += remaining.removeAt(index)
    }
    return picked
}
