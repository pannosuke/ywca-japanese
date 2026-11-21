package dev.ericallerton.yoruben.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ericallerton.yoruben.anki.AnkiClient
import dev.ericallerton.yoruben.core.CardNotes
import dev.ericallerton.yoruben.core.DrillPrompt
import dev.ericallerton.yoruben.core.GradedLine
import dev.ericallerton.yoruben.core.ProgressFile
import dev.ericallerton.yoruben.core.Question
import dev.ericallerton.yoruben.core.QuizRequest
import dev.ericallerton.yoruben.core.QuizSession
import dev.ericallerton.yoruben.core.Study
import dev.ericallerton.yoruben.core.StudyCard
import dev.ericallerton.yoruben.core.StudyContent
import dev.ericallerton.yoruben.data.ContentRepository
import dev.ericallerton.yoruben.data.ProgressStore
import dev.ericallerton.yoruben.data.shareProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random

enum class Place { HOME, REVIEW, QUIZ, DRILL, READ, READER, STATS, SHEET }

class AppModel(app: Application) : AndroidViewModel(app) {
    private val repo = ContentRepository(app)
    private val store = ProgressStore(app)
    private val anki = AnkiClient(app)

    var content by mutableStateOf(repo.load())
        private set
    var progress by mutableStateOf(store.load(content.profile.id))
        private set
    var place by mutableStateOf(Place.HOME)
        private set
    var askPermission by mutableStateOf(false)
    var ankiReady by mutableStateOf(anki.availability())
        private set
    var ankiDue by mutableStateOf<Int?>(null)
        private set

    private var queue by mutableStateOf<List<StudyCard>>(emptyList())
    private var reviewAt by mutableIntStateOf(0)
    private var revealed by mutableStateOf(false)
    private var reviewSource by mutableStateOf("")
    private var offerSaved by mutableStateOf(false)
    private var reviewNote by mutableStateOf("")
    private var grades by mutableStateOf(listOf(0, 0, 0, 0, 0))
    private var cardShownAt = 0L

    var quizSetup by mutableStateOf(true)
        private set
    var lessonId by mutableStateOf<String?>(null)
        private set
    var sources by mutableStateOf(setOf("deck", "worksheet", "homework", "class"))
        private set
    var weakOnly by mutableStateOf(false)
        private set
    var quizCount by mutableIntStateOf(8)
        private set
    private var session: QuizSession? = null
    private var shown: Question? = null
    private var quizTyped by mutableStateOf("")
    private var quizPicked by mutableStateOf("")
    private var quizVerdict by mutableStateOf<Boolean?>(null)
    private var quizDone by mutableStateOf(false)
    private var quizEmpty by mutableStateOf(false)
    private var quizStep by mutableIntStateOf(0)

    private var drill by mutableStateOf<DrillPrompt?>(null)
    private var drillTyped by mutableStateOf("")
    private var bank by mutableStateOf<List<String>>(emptyList())
    var tileMode by mutableStateOf(false)
        private set
    private var drillVerdict by mutableStateOf<Boolean?>(null)
    private var drillAsked by mutableIntStateOf(0)
    private var drillCorrect by mutableIntStateOf(0)

    private var readerId by mutableStateOf<String?>(null)
    private var picks by mutableStateOf<List<Int>>(emptyList())
    private var readerScore by mutableStateOf<Int?>(null)

    var sheetId by mutableStateOf<String?>(null)
        private set
    private var sheetAnswers by mutableStateOf<List<String>>(emptyList())
    private var graded by mutableStateOf<List<GradedLine>?>(null)

    init {
        viewModelScope.launch {
            val fresh = repo.refresh()
            if (fresh != null && fresh.manifest.version >= content.manifest.version) content = fresh
            ankiReady = anki.availability()
            if (ankiReady == AnkiClient.Availability.READY) {
                val n = withContext(Dispatchers.IO) {
                    anki.dueCards(content.profile.deckName, content.profile.noteType, 200)?.size
                }
                if (n != null) ankiDue = n
            }
        }
    }

    fun homeUi(): HomeUi {
        val now = System.currentTimeMillis()
        val stats = study().stats(now)
        val review = when (ankiReady) {
            AnkiClient.Availability.READY -> ankiDue?.let { "$it due" } ?: "AnkiDroid"
            AnkiClient.Availability.NEEDS_PERMISSION -> "Needs permission"
            AnkiClient.Availability.NOT_INSTALLED -> "${study().dueLocal(now, 200).size} due"
        }
        val weak = content.homework.sumOf { result -> result.items.count { !it.correct } }
        val stories = content.readers.size
        return HomeUi(
            date = pretty(LocalDate.now().toString()),
            sections = listOf(
                "Review" to review,
                "Quiz" to if (weak > 0) "$weak weak" else "5–10 questions",
                "Conjugation" to "Te-form first",
                "Read" to if (stories == 1) "1 story" else "$stories stories",
                "Stats" to if (stats.streak == 0) "No streak yet" else "${stats.streak}-day streak",
            ),
        )
    }

    fun open(index: Int) {
        when (index) {
            0 -> startReview()
            1 -> openQuiz()
            2 -> openDrill()
            3 -> place = Place.READ
            4 -> place = Place.STATS
        }
    }

    fun close() {
        place = Place.HOME
    }

    fun startReview() {
        ankiReady = anki.availability()
        when (ankiReady) {
            AnkiClient.Availability.NEEDS_PERMISSION -> askPermission = true
            AnkiClient.Availability.READY -> loadAnki()
            AnkiClient.Availability.NOT_INSTALLED -> loadLocal()
        }
    }

    fun onPermission(granted: Boolean) {
        ankiReady = anki.availability()
        if (granted) loadAnki() else loadLocal()
    }

    fun reveal() {
        revealed = true
    }

    fun grade(rating: Int) {
        val card = queue.getOrNull(reviewAt) ?: return
        val now = System.currentTimeMillis()
        if (card.fromAnki) {
            val saved = anki.answer(card.noteId, card.ord, rating, (now - cardShownAt).coerceAtLeast(0))
            if (!saved) {
                reviewNote = "AnkiDroid did not save that answer."
                return
            }
            update { it.recordAnki(card, rating, now) }
        } else {
            update { it.gradeLocal(card, rating, now) }
        }
        reviewNote = ""
        grades = grades.toMutableList().also { it[rating] = it[rating] + 1 }
        reviewAt++
        revealed = false
        cardShownAt = now
    }

    fun reviewSaved() = loadLocal()

    fun reviewUi(): ReviewUi {
        val done = queue.isNotEmpty() && reviewAt >= queue.size
        val card = queue.getOrNull(reviewAt)
        val summary = if (done) {
            val streak = study().stats(System.currentTimeMillis()).streak
            listOf(
                "Again" to grades[1].toString(),
                "Hard" to grades[2].toString(),
                "Good" to grades[3].toString(),
                "Easy" to grades[4].toString(),
                "Streak" to if (streak == 0) "No streak yet" else "$streak days",
            )
        } else null
        val parsed = CardNotes.parse(card?.note.orEmpty())
        return ReviewUi(
            counter = if (card == null) "" else "${reviewAt + 1} of ${queue.size} · $reviewSource",
            word = card?.word.orEmpty(),
            revealed = revealed,
            meaning = card?.meaning.orEmpty(),
            definition = card?.definition.orEmpty(),
            sentence = card?.sentence.orEmpty(),
            sentenceEn = card?.sentenceEn.orEmpty(),
            empty = if (queue.isEmpty()) "Nothing is due." else null,
            offerSaved = offerSaved,
            summary = summary,
            status = reviewNote,
            cardNote = parsed.prose,
            kanjiIntro = parsed.intro,
            kanji = parsed.items,
        )
    }

    fun openQuiz() {
        quizSetup = true
        quizDone = false
        quizEmpty = false
        place = Place.QUIZ
    }

    fun setLesson(id: String?) { lessonId = id }
    fun toggleSource(id: String) {
        sources = if (id in sources) sources - id else sources + id
    }
    fun toggleWeak() { weakOnly = !weakOnly }
    fun setCount(n: Int) { quizCount = n }

    fun startQuiz() {
        val built = study().buildQuiz(
            QuizRequest(sources, lessonId, weakOnly, quizCount, System.nanoTime()),
        )
        session = built
        shown = built.current()
        quizTyped = ""
        quizPicked = ""
        quizVerdict = null
        quizDone = false
        quizEmpty = built.current() == null
        quizSetup = false
        quizStep++
    }

    fun pickQuiz(choice: String) { quizPicked = choice }
    fun typeQuiz(text: String) { quizTyped = text }

    fun checkQuiz() {
        val live = session ?: return
        val question = shown ?: return
        if (quizVerdict != null) return
        val raw = if (question.choices.isEmpty()) quizTyped else quizPicked
        quizVerdict = live.submit(raw)
        quizStep++
    }

    fun nextQuiz() {
        val live = session ?: return
        quizVerdict = null
        quizTyped = ""
        quizPicked = ""
        if (live.done()) {
            update { it.finishQuiz(live, System.currentTimeMillis(), lessonId ?: "") }
            quizDone = true
        } else {
            shown = live.current()
        }
        quizStep++
    }

    fun quizUi(): QuizUi {
        quizStep
        val live = session
        val question = shown
        val misses = live?.answered?.filter { !it.second }?.map { it.first.prompt }?.distinct().orEmpty()
        return QuizUi(
            setup = quizSetup,
            lessons = listOf("" to "All") + content.lessons.map { it.id to pretty(it.date) },
            lessonId = lessonId,
            sources = sources,
            weakOnly = weakOnly,
            count = quizCount,
            counter = if (live == null || question == null || quizDone) "" else {
                val shown = if (quizVerdict != null) live.answered.size else live.answered.size + 1
                "$shown of ${live.totalPlanned}"
            },
            prompt = question?.prompt.orEmpty(),
            promptEn = question?.promptEn.orEmpty(),
            choices = question?.choices.orEmpty(),
            picked = quizPicked,
            typed = quizTyped,
            verdict = quizVerdict,
            expected = question?.answer.orEmpty(),
            reask = question?.reask == true,
            done = quizDone,
            summary = if (live == null) "" else "${live.correct} of ${live.answered.size}",
            misses = misses,
            empty = quizEmpty,
        )
    }

    fun openDrill() {
        drillAsked = 0
        drillCorrect = 0
        place = Place.DRILL
        roll(System.nanoTime())
    }

    fun typeDrill(text: String) { drillTyped = text }
    fun toggleTiles() { tileMode = !tileMode }
    fun tapTile(index: Int) {
        val tile = bank.getOrNull(index) ?: return
        drillTyped += tile
        bank = bank.filterIndexed { i, _ -> i != index }
    }

    fun checkDrill() {
        val prompt = drill ?: return
        if (drillVerdict != null) return
        var ok = false
        update { ok = it.gradeDrill(prompt, drillTyped, System.currentTimeMillis()) }
        drillVerdict = ok
        drillAsked++
        if (ok) drillCorrect++
    }

    fun nextDrill() = roll(System.nanoTime())

    fun drillUi(): DrillUi {
        val prompt = drill
        return DrillUi(
            counter = if (drillAsked == 0) "" else "$drillCorrect of $drillAsked",
            word = prompt?.word.orEmpty(),
            form = prompt?.formLabel.orEmpty(),
            prompt = prompt?.prompt.orEmpty(),
            typed = drillTyped,
            tiles = bank,
            tileMode = tileMode,
            verdict = drillVerdict,
            expected = prompt?.answerRuby.orEmpty(),
            rule = prompt?.ruleRuby.orEmpty(),
            ruleEn = prompt?.ruleEn.orEmpty(),
        )
    }

    fun stories(): List<StoryItem> = content.readers.map { reader ->
        val last = progress.readers.lastOrNull { it.readerId == reader.id }
        val detail = if (last == null) "Level ${reader.level}" else "Level ${reader.level} · ${last.correct} of ${last.total}"
        StoryItem(reader.id, reader.titleRuby, detail)
    }

    fun openReader(id: String) {
        val reader = content.readers.find { it.id == id } ?: return
        readerId = id
        picks = List(reader.questions.size) { -1 }
        readerScore = null
        place = Place.READER
    }

    fun closeReader() { place = Place.READ }

    fun pickReader(question: Int, choice: Int) {
        if (readerScore != null) return
        picks = picks.toMutableList().also { if (question in it.indices) it[question] = choice }
    }

    fun checkReader() {
        val reader = content.readers.find { it.id == readerId } ?: return
        if (readerScore != null) return
        val map = reader.questions.mapIndexed { index, question -> question.id to picks.getOrElse(index) { -1 } }.toMap()
        var score = 0
        update { score = it.gradeReader(reader, map, System.currentTimeMillis()) }
        readerScore = score
    }

    fun readUi(): ReadUi {
        val reader = content.readers.find { it.id == readerId }
        val checked = readerScore != null
        return ReadUi(
            title = reader?.titleRuby.orEmpty(),
            level = "Level ${reader?.level ?: 0}",
            body = reader?.bodyRuby.orEmpty(),
            questions = reader?.questions.orEmpty().mapIndexed { index, question ->
                ReadQuestion(
                    prompt = question.promptRuby,
                    choices = question.choices.map { it.ruby },
                    picked = picks.getOrElse(index) { -1 },
                    correct = question.choices.indexOfFirst { it.correct },
                    checked = checked,
                )
            },
            score = readerScore?.let { "$it of ${reader?.questions?.size ?: 0}" },
        )
    }

    fun statsUi(): StatsUi {
        val snap = study().stats(System.currentTimeMillis())
        val lead = StatBlock(
            "",
            listOf(
                "Streak" to if (snap.streak == 0) "—" else "${snap.streak} days",
                "Retention" to percent(snap.retention),
                "Today" to snap.reviewsToday.toString(),
                "Conjugation" to percent(snap.drillAccuracy),
            ),
        )
        fun block(title: String, lines: List<Pair<String, String>>) =
            if (lines.isEmpty()) null else StatBlock(title, lines)
        fun rate(label: String, correct: Int, total: Int) = label to "$correct of $total"
        return StatsUi(
            listOfNotNull(
                lead,
                block("Lessons", snap.byLesson.map { rate(it.label, it.correct, it.total) }),
                block("Sources", snap.bySource.map { rate(it.label, it.correct, it.total) }),
                block("Forms", snap.byForm.map { rate(it.label, it.correct, it.total) }),
                block("Verb groups", snap.byGroup.map { rate(it.label, it.correct, it.total) }),
                block("Weak", snap.weak.map { it.label to "${it.misses}" }),
                block("Homework", snap.homework.map { rate(it.label, it.correct, it.total) }),
                block("Quizzes", snap.classQuizzes.map { rate(it.label, it.correct, it.total) }),
            ),
        )
    }

    fun share() {
        shareProgress(getApplication(), study().exportJson(System.currentTimeMillis()))
    }

    fun openSheets() {
        sheetId = null
        graded = null
        place = Place.SHEET
    }

    fun openSheet(index: Int) {
        val sheet = content.worksheets.getOrNull(index) ?: return
        sheetId = sheet.id
        sheetAnswers = List(sheet.items.size) { "" }
        graded = null
    }

    fun typeSheet(index: Int, text: String) {
        sheetAnswers = sheetAnswers.toMutableList().also { if (index in it.indices) it[index] = text }
    }

    fun gradeSheet() {
        val sheet = content.worksheets.find { it.id == sheetId } ?: return
        val map = sheet.items.mapIndexed { index, item -> item.id to sheetAnswers.getOrElse(index) { "" } }.toMap()
        var lines = emptyList<GradedLine>()
        update { lines = it.gradeWorksheet(sheet, map, System.currentTimeMillis()) }
        graded = lines
    }

    fun closeSheet() {
        if (sheetId != null) {
            sheetId = null
            graded = null
        } else {
            place = Place.QUIZ
        }
    }

    fun sheetUi(): SheetUi {
        if (sheetId == null) {
            return SheetUi(
                title = "",
                lines = emptyList(),
                summary = "",
                listing = content.worksheets.map { sheet ->
                    val past = progress.homework.lastOrNull { it.worksheetId == sheet.id }
                    val detail = past?.let { "${it.correct} of ${it.total}" } ?: pretty(sheet.date)
                    sheet.titleRuby to detail
                },
            )
        }
        val sheet = content.worksheets.find { it.id == sheetId }
        val lines = sheet?.items.orEmpty().mapIndexed { index, item ->
            val mark = graded?.getOrNull(index)
            SheetLine(
                prompt = item.promptRuby,
                typed = mark?.given ?: sheetAnswers.getOrElse(index) { "" },
                expected = item.expected,
                correct = mark?.correct,
            )
        }
        val summary = graded?.let { marks ->
            "${marks.count { it.correct }} of ${marks.size}"
        }.orEmpty()
        return SheetUi(sheet?.titleRuby.orEmpty(), lines, summary, null)
    }

    private fun loadAnki() {
        viewModelScope.launch {
            val cards = withContext(Dispatchers.IO) {
                anki.dueCards(content.profile.deckName, content.profile.noteType, 20)
            }
            if (cards == null) loadLocal()
            else openQueue(cards, "AnkiDroid", cards.isEmpty() && study().dueLocal(System.currentTimeMillis(), 1).isNotEmpty())
        }
    }

    private fun loadLocal() {
        openQueue(study().dueLocal(System.currentTimeMillis(), 20), "On this phone", false)
    }

    private fun openQueue(cards: List<StudyCard>, source: String, offer: Boolean) {
        queue = cards
        reviewAt = 0
        revealed = false
        reviewSource = source
        offerSaved = offer
        reviewNote = ""
        grades = listOf(0, 0, 0, 0, 0)
        cardShownAt = System.currentTimeMillis()
        place = Place.REVIEW
    }

    private fun roll(seed: Long) {
        val prompt = study().nextDrill(seed) ?: return
        drill = prompt
        drillTyped = ""
        bank = tiles(prompt.answer, seed)
        drillVerdict = null
    }

    private fun tiles(answer: String, seed: Long): List<String> {
        val chars = answer.map { it.toString() }.toMutableList()
        if ("っ" !in chars) chars += "っ"
        if ("ん" !in chars) chars += "ん"
        return chars.shuffled(Random(seed))
    }

    private fun study() = Study(content, progress)

    private fun update(block: (Study) -> Unit) {
        val next = Study(content, progress)
        block(next)
        progress = next.progress
        store.save(progress)
    }

    private fun pretty(iso: String): String = try {
        val date = LocalDate.parse(iso)
        "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"
    } catch (_: Exception) {
        iso
    }

    private fun percent(value: Double?): String =
        if (value == null) "—" else "${kotlin.math.round(value * 100).toInt()}%"
}

@androidx.compose.runtime.Composable
fun StudyRoot(model: AppModel) {
    val known = model.content.knownKanji
    when (model.place) {
        Place.HOME -> HomeScreen(model.homeUi(), model::open, model::startReview)
        Place.REVIEW -> ReviewScreen(model.reviewUi(), known, model::close, model::reveal, model::grade, model::reviewSaved)
        Place.QUIZ -> QuizScreen(
            model.quizUi(), known, model::close, model::setLesson, model::toggleSource, model::toggleWeak,
            model::setCount, model::startQuiz, model::pickQuiz, model::typeQuiz, model::checkQuiz, model::nextQuiz, model::openSheets,
        )
        Place.DRILL -> DrillScreen(
            model.drillUi(), known, model::close, model::typeDrill, model::tapTile, model::toggleTiles, model::checkDrill, model::nextDrill,
        )
        Place.READ -> ReadListScreen(model.stories(), known, model::close, model::openReader)
        Place.READER -> ReadScreen(model.readUi(), known, model::closeReader, model::pickReader, model::checkReader)
        Place.STATS -> StatsScreen(model.statsUi(), known, model::close, model::share)
        Place.SHEET -> SheetScreen(model.sheetUi(), known, model::closeSheet, model::openSheet, model::typeSheet, model::gradeSheet)
    }
}
