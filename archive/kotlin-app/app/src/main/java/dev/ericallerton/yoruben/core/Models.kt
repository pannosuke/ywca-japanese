package dev.ericallerton.yoruben.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("schema_version") val schemaVersion: Int,
    val student: String,
    @SerialName("city_ruby") val cityRuby: String,
    @SerialName("city_en") val cityEn: String,
    @SerialName("school_ruby") val schoolRuby: String,
    @SerialName("school_en") val schoolEn: String,
    @SerialName("anki_deck") val ankiDeck: String,
    @SerialName("anki_tag_prefix") val ankiTagPrefix: String,
    @SerialName("kanji_study") val kanjiStudy: String = "not_started",
    val commute: Commute,
    val schedule: List<ScheduleSlot>,
    val teachers: List<Teacher>,
    @SerialName("weak_spots") val weakSpots: List<WeakSpot> = emptyList(),
)

@Serializable
data class Commute(
    @SerialName("summary_ruby") val summaryRuby: String,
    @SerialName("summary_en") val summaryEn: String,
)

@Serializable
data class ScheduleSlot(
    val weekday: String,
    val start: String,
    val end: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("focus_en") val focusEn: String,
    @SerialName("focus_ruby") val focusRuby: String,
)

@Serializable
data class Teacher(
    val id: String,
    @SerialName("name_ruby") val nameRuby: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("honorific_ruby") val honorificRuby: String = "せんせい",
)

@Serializable
data class WeakSpot(
    val id: String,
    @SerialName("title_ruby") val titleRuby: String,
    @SerialName("reminder_ja") val reminderJa: String,
    @SerialName("reminder_en") val reminderEn: String,
    val weight: Int = 4,
    @SerialName("word_ids") val wordIds: List<String> = emptyList(),
    val items: List<FixedQuiz> = emptyList(),
)

@Serializable
data class FixedQuiz(
    val id: String,
    val mode: String,
    @SerialName("prompt_ruby") val promptRuby: String,
    @SerialName("prompt_en") val promptEn: String = "",
    @SerialName("explain_ruby") val explainRuby: String,
    @SerialName("explain_en") val explainEn: String = "",
    val choices: List<QuizChoice> = emptyList(),
    @SerialName("answer_id") val answerId: String = "",
    val accept: List<String> = emptyList(),
    val mistakes: List<MistakeHint> = emptyList(),
    @SerialName("lesson_date") val lessonDate: String? = null,
)

@Serializable
data class QuizChoice(
    val id: String,
    val ruby: String,
    val en: String = "",
)

@Serializable
data class MistakeHint(
    val answer: String,
    @SerialName("hint_en") val hintEn: String,
)

@Serializable
data class LessonFile(
    @SerialName("schema_version") val schemaVersion: Int,
    val lessons: List<Lesson>,
)

@Serializable
data class Lesson(
    val id: String,
    val date: String,
    val weekday: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_inferred") val teacherInferred: Boolean = false,
    @SerialName("date_inferred") val dateInferred: Boolean = false,
    @SerialName("anki_tag") val ankiTag: String,
    @SerialName("topic_ruby") val topicRuby: String,
    @SerialName("topic_en") val topicEn: String,
    val source: String = "",
    val pages: List<Int> = emptyList(),
    val grammar: List<GrammarPoint> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
data class GrammarPoint(
    val id: String,
    @SerialName("pattern_ruby") val patternRuby: String,
    @SerialName("explanation_ja") val explanationJa: String,
    @SerialName("explanation_en") val explanationEn: String,
    @SerialName("example_ruby") val exampleRuby: String,
    @SerialName("example_en") val exampleEn: String,
    @SerialName("cloze_ruby") val clozeRuby: String,
    @SerialName("cloze_answer") val clozeAnswer: String,
    @SerialName("cloze_accept") val clozeAccept: List<String> = emptyList(),
    @SerialName("cloze_hint_en") val clozeHintEn: String = "",
    @SerialName("weak_spot_id") val weakSpotId: String? = null,
    val mistakes: List<MistakeHint> = emptyList(),
)

@Serializable
data class WordFile(
    @SerialName("schema_version") val schemaVersion: Int,
    val words: List<Word>,
)

@Serializable
data class Word(
    val id: String,
    val word: String,
    val ruby: String,
    val kana: String,
    @SerialName("gloss_ja") val glossJa: String,
    @SerialName("gloss_en") val glossEn: String,
    val status: String,
    @SerialName("lesson_date") val lessonDate: String,
    @SerialName("teacher_id") val teacherId: String = "",
    @SerialName("source_sheet") val sourceSheet: String = "",
    val accept: List<String> = emptyList(),
    @SerialName("watch_out") val watchOut: String? = null,
    @SerialName("status_note") val statusNote: String? = null,
) {
    val studying: Boolean get() = status == "study"
}

data class Curriculum(
    val profile: Profile,
    val lessons: List<Lesson>,
    val words: List<Word>,
) {
    val lessonsNewestFirst: List<Lesson> = lessons.sortedByDescending { it.date }
    val wordsById: Map<String, Word> = words.associateBy { it.id }
    val weakWordIds: Set<String> = profile.weakSpots.flatMap { it.wordIds }.toSet()

    fun teacher(id: String): Teacher = profile.teachers.first { it.id == id }

    fun wordsFor(date: String): List<Word> = words.filter { it.lessonDate == date }
}

fun dateRuby(iso: String, weekday: String): String {
    val parts = iso.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return iso
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return iso
    val wd = when (weekday) {
        "Mon" -> "月[げつ]"
        "Tue" -> "火[か]"
        "Wed" -> "水[すい]"
        "Thu" -> "木[もく]"
        "Fri" -> "金[きん]"
        "Sat" -> "土[ど]"
        "Sun" -> "日[にち]"
        else -> weekday
    }
    return "${month}月[がつ]${day}日[にち]（$wd）"
}
