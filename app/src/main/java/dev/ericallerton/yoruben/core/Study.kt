package dev.ericallerton.yoruben.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@Serializable
data class ReviewEvent(
    val day: String,
    @SerialName("card_id") val cardId: String,
    val rating: Int,
    val source: String,
    @SerialName("lesson_id") val lessonId: String = "",
)

@Serializable
data class DrillEvent(
    val day: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("form_id") val formId: String,
    val group: String,
    val correct: Boolean,
    @SerialName("lesson_id") val lessonId: String = "",
)

@Serializable
data class QuizEvent(
    val day: String,
    @SerialName("quiz_id") val quizId: String,
    val source: String,
    @SerialName("lesson_id") val lessonId: String = "",
    val correct: Int,
    val total: Int,
    val misses: List<String> = emptyList(),
)

@Serializable
data class HomeworkEvent(
    val day: String,
    @SerialName("worksheet_id") val worksheetId: String,
    val correct: Int,
    val total: Int,
    val misses: List<String> = emptyList(),
)

@Serializable
data class ReaderEvent(
    val day: String,
    @SerialName("reader_id") val readerId: String,
    val correct: Int,
    val total: Int,
)

@Serializable
data class ProgressFile(
    @SerialName("profile_id") val profileId: String = "eric",
    val reviews: List<ReviewEvent> = emptyList(),
    val drills: List<DrillEvent> = emptyList(),
    val quizzes: List<QuizEvent> = emptyList(),
    val homework: List<HomeworkEvent> = emptyList(),
    val readers: List<ReaderEvent> = emptyList(),
    val fsrs: Map<String, FsrsCard> = emptyMap(),
    val weights: Map<String, Int> = emptyMap(),
) {
    fun encode(): String = StudyJson.encodeToString(this)

    companion object {
        fun decode(text: String): ProgressFile = StudyJson.decodeFromString(text)
    }
}

data class StudyCard(
    val id: String,
    val lessonId: String,
    val word: String,
    val definition: String,
    val meaning: String,
    val sentence: String,
    val sentenceEn: String,
    val noteId: Long = -1,
    val ord: Int = 0,
    val fromAnki: Boolean = false,
)

data class Question(
    val id: String,
    val kind: String,
    val prompt: String,
    val promptEn: String = "",
    val answer: String,
    val accept: List<String> = emptyList(),
    val choices: List<String> = emptyList(),
    val source: String,
    val lessonId: String = "",
    val reask: Boolean = false,
)

data class QuizRequest(
    val sources: Set<String> = setOf("deck", "worksheet", "homework", "class"),
    val lessonId: String? = null,
    val weakOnly: Boolean = false,
    val count: Int = 8,
    val seed: Long = 1,
)

class QuizSession(seed: List<Question>) {
    private val items = seed.toMutableList()
    private val reasked = mutableSetOf<String>()
    private var index = 0
    var correct: Int = 0
        private set
    val misses = mutableListOf<String>()
    val answered = mutableListOf<Pair<Question, Boolean>>()

    val totalPlanned: Int get() = items.size
    val position: Int get() = index
    fun current(): Question? = items.getOrNull(index)
    fun done(): Boolean = index >= items.size

    fun submit(raw: String): Boolean {
        val q = current() ?: return false
        val ok = Conjugator.matches(raw, listOf(q.answer) + q.accept)
        answered += q to ok
        if (ok) correct++ else {
            misses += q.id
            if (!q.reask && q.id !in reasked) {
                reasked += q.id
                items += q.copy(reask = true)
            }
        }
        index++
        return ok
    }
}

data class DrillPrompt(
    val itemId: String,
    val word: String,
    val kana: String,
    val formId: String,
    val formLabel: String,
    val prompt: String,
    val answer: String,
    val answerRuby: String,
    val accept: List<String>,
    val ruleRuby: String,
    val ruleEn: String,
    val group: String,
    val lessonId: String,
)

data class GradedLine(val id: String, val expected: String, val given: String, val correct: Boolean)

data class Accuracy(val label: String, val correct: Int, val total: Int) {
    val rate: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
}

data class WeakItem(val label: String, val detail: String, val misses: Int)

data class ScorePoint(val label: String, val correct: Int, val total: Int, val day: String)

data class StatsSnapshot(
    val streak: Int,
    val reviewsToday: Int,
    val retention: Double?,
    val reviewCount: Int,
    val drillAccuracy: Double?,
    val byLesson: List<Accuracy>,
    val bySource: List<Accuracy>,
    val byForm: List<Accuracy>,
    val byGroup: List<Accuracy>,
    val weak: List<WeakItem>,
    val homework: List<ScorePoint>,
    val classQuizzes: List<ScorePoint>,
)

class Study(val content: StudyContent, progress: ProgressFile) {
    var progress: ProgressFile = progress
        private set

    fun day(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().toString()

    fun dueLocal(nowMs: Long, limit: Int = 20): List<StudyCard> =
        content.deck.cards.map { it.toCard() }.filter { card ->
            val state = progress.fsrs[card.id]
            state == null || state.dueMs <= nowMs
        }.take(limit)

    fun gradeLocal(card: StudyCard, rating: Int, nowMs: Long) {
        val next = Fsrs.review(progress.fsrs[card.id] ?: FsrsCard(), rating, nowMs)
        val event = ReviewEvent(day(nowMs), card.id, rating, "local", card.lessonId)
        progress = progress.copy(
            fsrs = progress.fsrs + (card.id to next),
            reviews = progress.reviews + event,
            weights = bump(card.id, rating == 1),
        )
    }

    fun recordAnki(card: StudyCard, rating: Int, nowMs: Long) {
        progress = progress.copy(
            reviews = progress.reviews + ReviewEvent(day(nowMs), card.id, rating, "anki", card.lessonId),
            weights = bump(card.id, rating == 1),
        )
    }

    fun buildQuiz(request: QuizRequest): QuizSession {
        val pool = mutableListOf<Question>()
        if ("deck" in request.sources) pool += deckQuestions()
        if ("worksheet" in request.sources) pool += worksheetQuestions()
        if ("homework" in request.sources) pool += homeworkQuestions()
        if ("class" in request.sources) pool += classQuestions()
        var filtered = pool.filter { request.lessonId == null || it.lessonId == request.lessonId }
        if (request.weakOnly) {
            filtered = filtered.filter { weightOf(it.id) > 1 || it.id in homeworkMissIds() }
        }
        val rng = Random(request.seed)
        val picked = weightedSample(filtered, request.count.coerceIn(5, 10), rng)
        return QuizSession(picked)
    }

    fun finishQuiz(session: QuizSession, nowMs: Long, lessonId: String) {
        val misses = session.misses.distinct()
        var weights = progress.weights
        misses.forEach { id -> weights = weights + (id to (weights[id] ?: 0) + 2) }
        progress = progress.copy(
            quizzes = progress.quizzes + QuizEvent(
                day(nowMs), "session", "mixed", lessonId, session.correct, session.answered.size, misses,
            ),
            weights = weights,
        )
    }

    fun nextDrill(seed: Long): DrillPrompt? {
        val forms = content.forms.filter { it.enabled && Conjugator.supports(it.id) }
        val verbForms = forms.filter { it.appliesTo == "verb" }
        val iForms = forms.filter { it.appliesTo == "i-adjective" }
        val naForms = forms.filter { it.appliesTo == "na-adjective" }
        data class Slot(val weight: Int, val prompt: DrillPrompt)
        val slots = mutableListOf<Slot>()
        content.verbs.forEach { verb ->
            verbForms.forEach { form ->
                slots += Slot(drillWeight(verb.id, form.id, verb.group), verbPrompt(verb, form))
            }
        }
        content.adjectives.forEach { adj ->
            val usable = if (adj.kind == "na") naForms else iForms
            usable.forEach { form ->
                slots += Slot(drillWeight(adj.id, form.id, adj.kind), adjectivePrompt(adj, form))
            }
        }
        if (slots.isEmpty()) return null
        val total = slots.sumOf { it.weight }
        var roll = Random(seed).nextInt(total)
        for (slot in slots) {
            roll -= slot.weight
            if (roll < 0) return slot.prompt
        }
        return slots.last().prompt
    }

    fun gradeDrill(prompt: DrillPrompt, raw: String, nowMs: Long): Boolean {
        val ok = Conjugator.matches(raw, prompt.accept)
        var weights = progress.weights
        if (!ok) {
            val key = "${prompt.itemId}:${prompt.formId}"
            weights = weights + (key to (weights[key] ?: 0) + 3)
            weights = weights + ("group:${prompt.group}:${prompt.formId}" to ((weights["group:${prompt.group}:${prompt.formId}"] ?: 0) + 1))
        }
        progress = progress.copy(
            drills = progress.drills + DrillEvent(
                day(nowMs), prompt.itemId, prompt.formId, prompt.group, ok, prompt.lessonId,
            ),
            weights = weights,
        )
        return ok
    }

    fun gradeWorksheet(sheet: Worksheet, answers: Map<String, String>, nowMs: Long): List<GradedLine> {
        val lines = sheet.items.map { item ->
            val given = answers[item.id].orEmpty()
            val ok = Conjugator.matches(given, listOf(item.expected) + item.accept)
            GradedLine(item.id, item.expected, given, ok)
        }
        val misses = lines.filter { !it.correct }.map { it.id }
        var weights = progress.weights
        misses.forEach { id -> weights = weights + (id to (weights[id] ?: 0) + 3) }
        progress = progress.copy(
            homework = progress.homework + HomeworkEvent(
                day(nowMs), sheet.id, lines.count { it.correct }, lines.size, misses,
            ),
            weights = weights,
        )
        return lines
    }

    fun gradeReader(reader: Reader, picks: Map<String, Int>, nowMs: Long): Int {
        val correct = reader.questions.count { q ->
            val i = picks[q.id] ?: -1
            q.choices.getOrNull(i)?.correct == true
        }
        progress = progress.copy(
            readers = progress.readers + ReaderEvent(day(nowMs), reader.id, correct, reader.questions.size),
        )
        return correct
    }

    fun stats(nowMs: Long): StatsSnapshot {
        val today = day(nowMs)
        val reviews = progress.reviews
        val retention = if (reviews.isEmpty()) null else reviews.count { it.rating >= 2 }.toDouble() / reviews.size
        val drills = progress.drills
        val drillAcc = if (drills.isEmpty()) null else drills.count { it.correct }.toDouble() / drills.size
        return StatsSnapshot(
            streak = streak(today),
            reviewsToday = reviews.count { it.day == today } + drills.count { it.day == today },
            retention = retention,
            reviewCount = reviews.size,
            drillAccuracy = drillAcc,
            byLesson = byLesson(),
            bySource = bySource(),
            byForm = byKey(drills) { it.formId },
            byGroup = byKey(drills) { it.group },
            weak = weakItems(),
            homework = homeworkScores(),
            classQuizzes = classScores(),
        )
    }

    fun exportJson(nowMs: Long): String {
        val snap = stats(nowMs)
        val root = buildJsonObject {
            put("profile_id", progress.profileId)
            put("exported_at", Instant.ofEpochMilli(nowMs).toString())
            put("summary", buildJsonObject {
                put("streak", snap.streak)
                put("reviews_today", snap.reviewsToday)
                put("review_count", snap.reviewCount)
                if (snap.retention == null) put("retention", JsonNull) else put("retention", snap.retention)
                if (snap.drillAccuracy == null) put("drill_accuracy", JsonNull) else put("drill_accuracy", snap.drillAccuracy)
                put("weak", buildJsonArray {
                    snap.weak.forEach { item ->
                        add(buildJsonObject {
                            put("label", item.label)
                            put("misses", item.misses)
                        })
                    }
                })
                put("homework", buildJsonArray {
                    snap.homework.forEach { point ->
                        add(buildJsonObject {
                            put("label", point.label)
                            put("correct", point.correct)
                            put("total", point.total)
                            put("day", point.day)
                        })
                    }
                })
            })
            put("progress", StudyJson.encodeToJsonElement(ProgressFile.serializer(), progress))
        }
        return root.toString()
    }

    private fun deckQuestions(): List<Question> {
        val words = content.vocab.filter { it.kana.isNotBlank() && it.glossEn.isNotBlank() }
        val glosses = words.map { it.glossEn }.distinct()
        return words.map { word ->
            val choices = (listOf(word.glossEn) + glosses.filter { it != word.glossEn }.shuffled(Random(word.id.hashCode())).take(3))
                .distinct().shuffled(Random(word.id.hashCode() + 1))
            Question(
                id = "vocab:${word.id}:meaning",
                kind = "word_to_meaning",
                prompt = word.ruby,
                answer = word.glossEn,
                choices = choices,
                source = "deck",
                lessonId = word.lessonId,
            )
        } + words.map { word ->
            Question(
                id = "vocab:${word.id}:word",
                kind = "meaning_to_word",
                prompt = word.glossEn,
                promptEn = word.glossJa,
                answer = word.kana,
                accept = listOf(word.kana, Ruby.plain(word.ruby)),
                source = "deck",
                lessonId = word.lessonId,
            )
        }
    }

    private fun worksheetQuestions(): List<Question> = content.worksheets.flatMap { sheet ->
        sheet.items.map { item ->
            Question(
                id = "ws:${item.id}",
                kind = item.kind.ifBlank { "fill_blank" },
                prompt = item.promptRuby,
                answer = item.expected,
                accept = item.accept,
                source = "worksheet",
                lessonId = sheet.lessonId,
            )
        }
    }

    private fun homeworkQuestions(): List<Question> = content.homework.flatMap { result ->
        result.items.filter { !it.correct }.map { item ->
            Question(
                id = "hw:${item.id}",
                kind = "fill_blank",
                prompt = item.promptRuby,
                answer = item.expected,
                source = "homework",
                lessonId = content.worksheets.find { it.id == result.worksheetId }?.lessonId ?: "",
            )
        }
    }

    private fun classQuestions(): List<Question> = content.quizzes.flatMap { quiz ->
        quiz.items.map { item ->
            val kind = if (item.kind == "fill_blank" && item.answer.length == 1) "fill_particle" else item.kind
            Question(
                id = "class:${quiz.id}:${item.id}",
                kind = kind,
                prompt = item.promptRuby,
                answer = item.answer,
                choices = item.choices,
                source = "class",
                lessonId = quiz.lessonId,
            )
        }
    } + content.grammar.mapNotNull { point ->
        val particle = point.answer.length <= 2 && point.answer.all { it in "はがをにでとへも" }
        if (!particle && point.answer.isBlank()) return@mapNotNull null
        Question(
            id = "g:${point.id}",
            kind = if (particle) "fill_particle" else "fill_blank",
            prompt = point.clozeRuby.ifBlank { point.patternRuby },
            promptEn = point.glossEn,
            answer = point.answer,
            accept = point.accept,
            source = "class",
            lessonId = point.lessonId,
        )
    }

    private fun verbPrompt(verb: Verb, form: ConjForm): DrillPrompt {
        val group = Conjugator.groupOf(verb.group)
        val answer = Conjugator.answer(verb.kana, group, form.id)
        val (rule, en) = Conjugator.rule(verb.kana, group, form.id)
        val shown = if (form.id == "dict") rubyMasu(verb) else verb.ruby
        val answerRuby = Conjugator.spellRuby(verb.ruby, verb.kana, answer)
        return DrillPrompt(
            verb.id, shown, verb.kana, form.id, form.labelRuby, form.promptRuby,
            answer, answerRuby, Conjugator.accepts(verb.kana, group, form.id, verb.ruby),
            rule, en, verb.group, verb.lessonId,
        )
    }

    private fun adjectivePrompt(adj: Adjective, form: ConjForm): DrillPrompt {
        val answer = Conjugator.answer(adj.kana, VerbGroup.GODAN, form.id)
        val (rule, en) = Conjugator.rule(adj.kana, VerbGroup.GODAN, form.id)
        val answerRuby = Conjugator.spellRuby(adj.ruby, adj.kana, answer)
        return DrillPrompt(
            adj.id, adj.ruby, adj.kana, form.id, form.labelRuby, form.promptRuby,
            answer, answerRuby, Conjugator.accepts(adj.kana, VerbGroup.GODAN, form.id, adj.ruby),
            rule, en, adj.kind, adj.lessonId,
        )
    }

    private fun rubyMasu(verb: Verb): String {
        val masu = Conjugator.answer(verb.kana, Conjugator.groupOf(verb.group), "masu")
        if (verb.kana == "くる") return "来[き]ます"
        val pieces = Ruby.parse(verb.ruby)
        val kanji = pieces.filter { it.reading.isNotEmpty() }
        if (kanji.isEmpty()) return masu
        val reading = kanji.joinToString("") { it.reading }
        val base = kanji.joinToString("") { it.base }
        return if (masu.startsWith(reading)) "$base[$reading]${masu.removePrefix(reading)}" else masu
    }

    private fun drillWeight(itemId: String, formId: String, group: String): Int {
        var w = 1
        w += progress.weights["$itemId:$formId"] ?: 0
        w += progress.weights["group:$group:$formId"] ?: 0
        if (formId == "te" || formId == "te_kudasai") w += 2
        if (group == "godan" && (formId == "te" || formId == "te_kudasai")) w += 2
        val recent = progress.drills.filter { it.itemId == itemId && it.formId == formId }.takeLast(8)
        w += recent.count { !it.correct } * 3
        if (itemId in homeworkMissVerbs() && (formId == "te" || formId == "te_kudasai")) w += 6
        return w
    }

    private fun homeworkMissVerbs(): Set<String> =
        content.homework.flatMap { it.items.filter { item -> !item.correct }.map { item -> item.verbId } }.toSet()

    private fun homeworkMissIds(): Set<String> =
        content.homework.flatMap { it.items.filter { item -> !item.correct }.map { item -> "hw:${item.id}" } }.toSet()

    private fun weightOf(id: String): Int = (progress.weights[id] ?: 0) + if (id in homeworkMissIds()) 4 else 0

    private fun <T> weightedSample(items: List<T>, count: Int, rng: Random): List<T> {
        if (items.isEmpty()) return emptyList()
        val pool = items.toMutableList()
        val out = mutableListOf<T>()
        repeat(minOf(count, pool.size)) {
            val weights = pool.map { item ->
                val id = (item as? Question)?.id ?: ""
                1 + weightOf(id)
            }
            val total = weights.sum()
            var roll = rng.nextInt(total)
            var pick = 0
            for (i in weights.indices) {
                roll -= weights[i]
                if (roll < 0) { pick = i; break }
            }
            out += pool.removeAt(pick)
        }
        return out
    }

    private fun bump(id: String, miss: Boolean): Map<String, Int> {
        if (!miss) return progress.weights
        return progress.weights + (id to (progress.weights[id] ?: 0) + 1)
    }

    private fun streak(today: String): Int {
        val days = buildSet {
            progress.reviews.forEach { add(it.day) }
            progress.drills.forEach { add(it.day) }
            progress.quizzes.forEach { add(it.day) }
            progress.readers.forEach { add(it.day) }
            progress.homework.forEach { add(it.day) }
        }
        var n = 0
        var cursor = java.time.LocalDate.parse(today)
        if (today !in days) cursor = cursor.minusDays(1)
        while (cursor.toString() in days) {
            n++
            cursor = cursor.minusDays(1)
        }
        return n
    }

    private fun byLesson(): List<Accuracy> {
        val bucket = linkedMapOf<String, Pair<Int, Int>>()
        fun add(lesson: String, ok: Boolean) {
            if (lesson.isBlank()) return
            val (c, t) = bucket[lesson] ?: (0 to 0)
            bucket[lesson] = (c + if (ok) 1 else 0) to (t + 1)
        }
        progress.drills.forEach { add(it.lessonId, it.correct) }
        progress.reviews.forEach { add(it.lessonId, it.rating >= 2) }
        return bucket.map { (id, ct) ->
            Accuracy(content.lessonName[id] ?: id, ct.first, ct.second)
        }
    }

    private fun bySource(): List<Accuracy> {
        val bucket = linkedMapOf<String, Pair<Int, Int>>()
        progress.reviews.forEach {
            val (c, t) = bucket[it.source] ?: (0 to 0)
            bucket[it.source] = (c + if (it.rating >= 2) 1 else 0) to (t + 1)
        }
        progress.quizzes.forEach {
            val (c, t) = bucket[it.source] ?: (0 to 0)
            bucket[it.source] = (c + it.correct) to (t + it.total)
        }
        return bucket.map { (k, ct) -> Accuracy(k, ct.first, ct.second) }
    }

    private fun byKey(drills: List<DrillEvent>, key: (DrillEvent) -> String): List<Accuracy> {
        val bucket = linkedMapOf<String, Pair<Int, Int>>()
        drills.forEach {
            val k = key(it)
            val (c, t) = bucket[k] ?: (0 to 0)
            bucket[k] = (c + if (it.correct) 1 else 0) to (t + 1)
        }
        return bucket.map { (k, ct) -> Accuracy(formLabel(k), ct.first, ct.second) }
    }

    private fun wordOf(id: String): String =
        content.verbs.find { it.id == id }?.ruby
            ?: content.adjectives.find { it.id == id }?.ruby
            ?: content.vocab.find { it.id == id }?.ruby
            ?: ""

    private fun formLabel(id: String): String =
        content.forms.find { it.id == id }?.labelRuby ?: when (id) {
            "godan" -> "うどうし"
            "ichidan" -> "るどうし"
            "irregular" -> "ふきそく"
            "i" -> "いけいようし"
            "na" -> "なけいようし"
            else -> id
        }

    private fun weakItems(): List<WeakItem> {
        val misses = linkedMapOf<String, Int>()
        fun add(label: String, n: Int = 1) { misses[label] = (misses[label] ?: 0) + n }
        progress.drills.filter { !it.correct }.forEach {
            add("${wordOf(it.itemId)} · ${formLabel(it.formId)}")
        }
        progress.quizzes.forEach { ev -> ev.misses.forEach { add(labelFor(it)) } }
        content.homework.forEach { hw ->
            hw.items.filter { !it.correct }.forEach { item ->
                add("${wordOf(item.verbId).ifBlank { item.id }} · ${item.expected}")
            }
        }
        return misses.entries.sortedByDescending { it.value }.take(10).map { (label, n) ->
            WeakItem(label, "", n)
        }
    }

    private fun homeworkScores(): List<ScorePoint> {
        val seeded = content.homework.map {
            ScorePoint(it.titleRuby.ifBlank { it.id }, it.correct, it.total, it.date)
        }
        val local = progress.homework.map {
            val title = content.worksheets.find { sheet -> sheet.id == it.worksheetId }?.titleRuby ?: it.worksheetId
            ScorePoint(title, it.correct, it.total, it.day)
        }
        return seeded + local
    }

    private fun classScores(): List<ScorePoint> {
        val seeded = content.quizzes.filter { it.correct >= 0 && it.total > 0 }.map {
            ScorePoint(it.titleRuby, it.correct, it.total, it.lessonId)
        }
        val local = progress.quizzes.map { ScorePoint("Quiz", it.correct, it.total, it.day) }
        return seeded + local
    }

    private fun labelFor(id: String): String {
        fun blank(text: String) = text.ifBlank { "Missed item" }
        return when {
            id.startsWith("vocab:") -> {
                val vid = id.removePrefix("vocab:").substringBeforeLast(":")
                blank(content.vocab.find { it.id == vid }?.ruby ?: "")
            }
            id.startsWith("hw:") -> {
                val item = content.homework.flatMap { it.items }.find { it.id == id.removePrefix("hw:") }
                blank(item?.promptRuby ?: "")
            }
            id.startsWith("ws:") -> {
                val item = content.worksheets.flatMap { it.items }.find { it.id == id.removePrefix("ws:") }
                blank(item?.promptRuby ?: "")
            }
            id.startsWith("class:") -> {
                val rest = id.removePrefix("class:")
                val item = content.quizzes.find { it.id == rest.substringBefore(":") }
                    ?.items?.find { it.id == rest.substringAfter(":") }
                blank(item?.promptRuby ?: "")
            }
            id.startsWith("g:") -> blank(content.grammar.find { it.id == id.removePrefix("g:") }?.clozeRuby ?: "")
            else -> "Missed item"
        }
    }
}

private fun DeckCard.toCard() = StudyCard(id, lessonId, word, definition, meaning, sentence, sentenceEn)
