package dev.ericallerton.yoruben.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

val StudyJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class Manifest(val schema: Int = 1, val version: Int = 1, val files: List<String> = emptyList())

@Serializable
data class Profile(
    val schema: Int = 1,
    val id: String = "eric",
    @SerialName("display_name") val displayName: String = "Eric",
    @SerialName("known_kanji") val knownKanji: List<String> = emptyList(),
    @SerialName("deck_name") val deckName: String = "YWCA",
    @SerialName("note_type") val noteType: String = "YWCA Vocab",
    @SerialName("note_fields") val noteFields: List<String> = emptyList(),
)

@Serializable
data class Lesson(
    val id: String,
    val date: String = "",
    @SerialName("teacher_id") val teacherId: String = "",
    @SerialName("topic_ruby") val topicRuby: String = "",
    @SerialName("topic_en") val topicEn: String = "",
    @SerialName("anki_tag") val ankiTag: String = "",
)

@Serializable
data class LessonsFile(val schema: Int = 1, val lessons: List<Lesson> = emptyList())

@Serializable
data class Vocab(
    val id: String,
    @SerialName("lesson_id") val lessonId: String = "",
    val ruby: String = "",
    val kana: String = "",
    @SerialName("gloss_ja") val glossJa: String = "",
    @SerialName("gloss_en") val glossEn: String = "",
    @SerialName("sentence_ruby") val sentenceRuby: String = "",
    @SerialName("sentence_en") val sentenceEn: String = "",
    val note: String = "",
)

@Serializable
data class VocabFile(val schema: Int = 1, val items: List<Vocab> = emptyList())

@Serializable
data class Verb(
    val id: String,
    val ruby: String = "",
    val kana: String = "",
    val group: String = "godan",
    @SerialName("gloss_ja") val glossJa: String = "",
    @SerialName("gloss_en") val glossEn: String = "",
    @SerialName("lesson_id") val lessonId: String = "",
)

@Serializable
data class VerbsFile(val schema: Int = 1, val verbs: List<Verb> = emptyList())

@Serializable
data class Adjective(
    val id: String,
    val kind: String = "i",
    val ruby: String = "",
    val kana: String = "",
    @SerialName("gloss_ja") val glossJa: String = "",
    @SerialName("gloss_en") val glossEn: String = "",
    @SerialName("lesson_id") val lessonId: String = "",
)

@Serializable
data class AdjectivesFile(val schema: Int = 1, val adjectives: List<Adjective> = emptyList())

@Serializable
data class ConjForm(
    val id: String,
    @SerialName("label_ruby") val labelRuby: String = "",
    @SerialName("prompt_ruby") val promptRuby: String = "",
    @SerialName("applies_to") val appliesTo: String = "verb",
    val enabled: Boolean = true,
)

@Serializable
data class FormsFile(val schema: Int = 1, val forms: List<ConjForm> = emptyList())

@Serializable
data class GrammarPoint(
    val id: String,
    @SerialName("lesson_id") val lessonId: String = "",
    @SerialName("pattern_ruby") val patternRuby: String = "",
    @SerialName("cloze_ruby") val clozeRuby: String = "",
    val answer: String = "",
    val accept: List<String> = emptyList(),
    @SerialName("gloss_en") val glossEn: String = "",
)

@Serializable
data class GrammarFile(val schema: Int = 1, val points: List<GrammarPoint> = emptyList())

@Serializable
data class WorksheetItem(
    val id: String,
    @SerialName("prompt_ruby") val promptRuby: String = "",
    val expected: String = "",
    val accept: List<String> = emptyList(),
    @SerialName("verb_id") val verbId: String = "",
    val kind: String = "fill_blank",
)

@Serializable
data class Worksheet(
    val schema: Int = 1,
    val id: String,
    val date: String = "",
    @SerialName("teacher_id") val teacherId: String = "",
    @SerialName("title_ruby") val titleRuby: String = "",
    @SerialName("lesson_id") val lessonId: String = "",
    val items: List<WorksheetItem> = emptyList(),
)

@Serializable
data class HomeworkItem(
    val id: String,
    @SerialName("prompt_ruby") val promptRuby: String = "",
    val expected: String = "",
    val answer: String = "",
    val correct: Boolean = false,
    @SerialName("error_type") val errorType: String = "",
    @SerialName("verb_id") val verbId: String = "",
)

@Serializable
data class HomeworkResult(
    val schema: Int = 1,
    val id: String,
    @SerialName("worksheet_id") val worksheetId: String = "",
    val date: String = "",
    @SerialName("teacher_id") val teacherId: String = "",
    @SerialName("title_ruby") val titleRuby: String = "",
    val note: String = "",
    val correct: Int = 0,
    val total: Int = 0,
    val items: List<HomeworkItem> = emptyList(),
)

@Serializable
data class QuizItem(
    val id: String,
    val kind: String = "choose_form",
    @SerialName("prompt_ruby") val promptRuby: String = "",
    val answer: String = "",
    val choices: List<String> = emptyList(),
)

@Serializable
data class ClassQuiz(
    val schema: Int = 1,
    val id: String,
    @SerialName("lesson_id") val lessonId: String = "",
    @SerialName("title_ruby") val titleRuby: String = "",
    val items: List<QuizItem> = emptyList(),
    val correct: Int = -1,
    val total: Int = -1,
)

@Serializable
data class ReaderChoice(val ruby: String = "", val correct: Boolean = false)

@Serializable
data class ReaderQuestion(
    val id: String,
    @SerialName("prompt_ruby") val promptRuby: String = "",
    val choices: List<ReaderChoice> = emptyList(),
)

@Serializable
data class Reader(
    val schema: Int = 1,
    val id: String,
    val level: Int = 0,
    @SerialName("title_ruby") val titleRuby: String = "",
    @SerialName("lesson_id") val lessonId: String = "",
    @SerialName("body_ruby") val bodyRuby: String = "",
    val questions: List<ReaderQuestion> = emptyList(),
)

@Serializable
data class DeckCard(
    val id: String,
    @SerialName("lesson_id") val lessonId: String = "",
    val word: String = "",
    val definition: String = "",
    val meaning: String = "",
    val sentence: String = "",
    @SerialName("sentence_en") val sentenceEn: String = "",
    val note: String = "",
)

@Serializable
data class DeckFile(
    val schema: Int = 1,
    @SerialName("deck_name") val deckName: String = "YWCA",
    @SerialName("note_type") val noteType: String = "YWCA Vocab",
    val cards: List<DeckCard> = emptyList(),
)

data class StudyContent(
    val manifest: Manifest,
    val profile: Profile,
    val lessons: List<Lesson>,
    val vocab: List<Vocab>,
    val verbs: List<Verb>,
    val adjectives: List<Adjective>,
    val forms: List<ConjForm>,
    val grammar: List<GrammarPoint>,
    val worksheets: List<Worksheet>,
    val homework: List<HomeworkResult>,
    val quizzes: List<ClassQuiz>,
    val readers: List<Reader>,
    val deck: DeckFile,
    val exceptions: Map<String, Map<String, String>>,
) {
    val knownKanji: Set<Char> = profile.knownKanji.flatMap { it.toList() }.toSet()
    val lessonName: Map<String, String> = lessons.associate { it.id to it.topicRuby }

    companion object {
        fun load(root: File): StudyContent {
            val manifest = StudyJson.decodeFromString<Manifest>(File(root, "manifest.json").readText())
            require(manifest.schema == 1) { "unsupported schema ${manifest.schema}" }
            fun text(name: String) = File(root, name).readText()
            val worksheets = mutableListOf<Worksheet>()
            val homework = mutableListOf<HomeworkResult>()
            val quizzes = mutableListOf<ClassQuiz>()
            val readers = mutableListOf<Reader>()
            manifest.files.forEach { name ->
                when {
                    name.startsWith("worksheets/") -> worksheets += StudyJson.decodeFromString<Worksheet>(text(name))
                    name.startsWith("homework/") -> homework += StudyJson.decodeFromString<HomeworkResult>(text(name))
                    name.startsWith("quizzes/") -> quizzes += StudyJson.decodeFromString<ClassQuiz>(text(name))
                    name.startsWith("readers/") -> readers += StudyJson.decodeFromString<Reader>(text(name))
                }
            }
            val exceptions = if (manifest.files.contains("exceptions.json")) {
                val raw = StudyJson.decodeFromString<JsonObject>(text("exceptions.json"))
                val verbs = raw["verbs"]?.jsonObject ?: JsonObject(emptyMap())
                verbs.mapValues { (_, value) ->
                    value.jsonObject.mapNotNull { (k, v) ->
                        v.jsonPrimitive.contentOrNull?.let { k to it }
                    }.toMap()
                }
            } else emptyMap()
            return StudyContent(
                manifest = manifest,
                profile = StudyJson.decodeFromString(text("profile.json")),
                lessons = StudyJson.decodeFromString<LessonsFile>(text("lessons.json")).lessons,
                vocab = StudyJson.decodeFromString<VocabFile>(text("vocab.json")).items,
                verbs = StudyJson.decodeFromString<VerbsFile>(text("verbs.json")).verbs,
                adjectives = StudyJson.decodeFromString<AdjectivesFile>(text("adjectives.json")).adjectives,
                forms = StudyJson.decodeFromString<FormsFile>(text("forms.json")).forms,
                grammar = StudyJson.decodeFromString<GrammarFile>(text("grammar.json")).points,
                worksheets = worksheets,
                homework = homework,
                quizzes = quizzes,
                readers = readers,
                deck = StudyJson.decodeFromString(text("deck.json")),
                exceptions = exceptions,
            )
        }
    }
}

object ContentChecks {
    val displayKeys = setOf(
        "ruby", "gloss_ja", "sentence", "sentence_ruby", "word", "definition",
        "body_ruby", "pattern_ruby", "cloze_ruby", "label_ruby", "prompt_ruby",
        "title_ruby", "topic_ruby",
    )

    fun displayViolations(root: File, known: Set<Char>): List<String> {
        val manifest = StudyJson.decodeFromString<Manifest>(File(root, "manifest.json").readText())
        val out = mutableListOf<String>()
        manifest.files.filter { it.endsWith(".json") && it != "exceptions.json" }.forEach { name ->
            walk(StudyJson.parseToJsonElement(File(root, name).readText()), name, out, known)
        }
        return out
    }

    private fun walk(
        element: kotlinx.serialization.json.JsonElement,
        path: String,
        out: MutableList<String>,
        known: Set<Char>,
    ) {
        when (element) {
            is kotlinx.serialization.json.JsonObject -> element.forEach { (key, value) ->
                if (key in displayKeys && value is kotlinx.serialization.json.JsonPrimitive) {
                    val text = value.contentOrNull ?: return@forEach
                    Ruby.bareKanji(text, known).forEach { out += "$path $key: $it" }
                } else walk(value, "$path/$key", out, known)
            }
            is kotlinx.serialization.json.JsonArray -> element.forEachIndexed { i, value ->
                walk(value, "$path[$i]", out, known)
            }
            else -> Unit
        }
    }

    fun kotlinViolations(sourceRoot: File, known: Set<Char>): List<String> {
        if (!sourceRoot.exists()) return emptyList()
        val out = mutableListOf<String>()
        sourceRoot.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            literals(file.readText()).forEach { literal ->
                Ruby.bareKanji(literal, known).forEach { out += "${file.name}: $it" }
            }
        }
        return out
    }

    fun literals(source: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < source.length) {
            if (source.startsWith("//", i)) {
                val nl = source.indexOf('\n', i)
                i = if (nl < 0) source.length else nl + 1
                continue
            }
            if (source.startsWith("/*", i)) {
                val end = source.indexOf("*/", i + 2)
                i = if (end < 0) source.length else end + 2
                continue
            }
            if (source.startsWith("\"\"\"", i)) {
                val end = source.indexOf("\"\"\"", i + 3)
                if (end < 0) break
                out += source.substring(i + 3, end)
                i = end + 3
                continue
            }
            if (source[i] == '"') {
                val buf = StringBuilder()
                i++
                while (i < source.length && source[i] != '"') {
                    if (source[i] == '\\' && i + 1 < source.length) {
                        buf.append(source[i + 1])
                        i += 2
                    } else {
                        buf.append(source[i])
                        i++
                    }
                }
                out += buf.toString()
                i++
                continue
            }
            i++
        }
        return out
    }
}
