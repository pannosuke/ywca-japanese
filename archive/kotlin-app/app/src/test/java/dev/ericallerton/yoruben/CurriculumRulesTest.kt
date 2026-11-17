package dev.ericallerton.yoruben

import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.core.CurriculumJson
import dev.ericallerton.yoruben.core.RubyMarkup
import dev.ericallerton.yoruben.core.isHan
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumRulesTest {
    private val curriculum: Curriculum = loadCurriculum()

    @Test
    fun countsAndFrequencyAreNotAStudyError() {
        assertEquals(8, curriculum.lessons.size)
        assertEquals(40, curriculum.lessons.sumOf { it.grammar.size })
        assertEquals(237, curriculum.words.size)
        assertEquals(25, curriculum.words.count { it.studying })
        val frequency = curriculum.words.first { it.id == "頻度" }
        assertEquals("known", frequency.status)
        assertEquals("chinese_gloss_for_classmates", frequency.statusNote)
        val weak = curriculum.profile.weakSpots.joinToString(" ") { it.id + it.titleRuby + it.reminderJa }
        listOf("頻度", "頻率", "ひんどう").forEach { banned ->
            assertFalse(weak.contains(banned))
        }
    }

    @Test
    fun commuteDoesNotInventStopsOrCallTheDestinationUmeda() {
        val authored = buildString {
            append(curriculum.profile.commute.summaryRuby)
            append('\n')
            append(curriculum.profile.commute.summaryEn)
            curriculum.lessons.forEach { lesson ->
                lesson.notes.forEach { append('\n').append(it) }
                lesson.grammar.forEach { point ->
                    append('\n').append(point.patternRuby)
                    append('\n').append(point.explanationJa)
                    append('\n').append(point.explanationEn)
                    append('\n').append(point.exampleRuby)
                    append('\n').append(point.exampleEn)
                    append('\n').append(point.clozeRuby)
                    append('\n').append(point.clozeHintEn)
                }
            }
            curriculum.profile.weakSpots.forEach { spot ->
                append('\n').append(spot.titleRuby)
                append('\n').append(spot.reminderJa)
                append('\n').append(spot.reminderEn)
            }
        }
        listOf("吹田", "梅田", "すいた", "うめだ", "新大阪", "しんおおさか", "Umeda", "Suita").forEach { banned ->
            assertFalse("authored copy contains $banned", authored.contains(banned))
        }
        assertTrue(curriculum.profile.commute.summaryRuby.contains("茨木駅"))
        assertTrue(curriculum.profile.commute.summaryRuby.contains("大阪駅"))
        assertTrue(curriculum.profile.commute.summaryRuby.contains("快速"))
        assertTrue(curriculum.profile.commute.summaryEn.contains("M16"))
        assertTrue(curriculum.profile.commute.summaryEn.contains("E Street"))
    }

    @Test
    fun everyShownKanjiHasFuriganaAndGlossesAreKana() {
        fun ruby(label: String, text: String) {
            assertFalse("$label has bare kanji: $text", RubyMarkup.hasBareKanji(text))
        }
        fun kana(label: String, text: String) {
            assertFalse("$label has kanji: $text", text.any { it.isHan() })
        }
        ruby("city", curriculum.profile.cityRuby)
        ruby("school", curriculum.profile.schoolRuby)
        ruby("commute", curriculum.profile.commute.summaryRuby)
        curriculum.profile.weakSpots.forEach { spot ->
            ruby(spot.id, spot.titleRuby)
            kana(spot.id, spot.reminderJa)
            spot.items.forEach { item ->
                ruby(item.id, item.promptRuby)
                ruby(item.id, item.explainRuby)
                item.choices.forEach { ruby(item.id, it.ruby) }
            }
        }
        curriculum.lessons.forEach { lesson ->
            ruby(lesson.id, lesson.topicRuby)
            lesson.notes.forEach { ruby(lesson.id, it) }
            lesson.grammar.forEach { point ->
                ruby(point.id, point.patternRuby)
                ruby(point.id, point.exampleRuby)
                ruby(point.id, point.clozeRuby)
                kana(point.id, point.explanationJa)
            }
        }
        curriculum.words.forEach { word ->
            ruby(word.id, word.ruby)
            kana(word.id, word.glossJa)
            word.watchOut?.let { ruby(word.id, it) }
        }
    }

    @Test
    fun handoutStationWordsStayNeutral() {
        val suita = curriculum.words.first { it.id == "吹田" }
        val umeda = curriculum.words.first { it.id == "梅田" }
        assertTrue(suita.glossJa.contains("プリント"))
        assertTrue(umeda.glossJa.contains("プリント"))
        assertFalse(suita.glossJa.contains("おりる"))
        assertTrue(umeda.glossJa.contains("おりる えきの なまえでは ない"))
    }
}

internal fun loadCurriculum(): Curriculum {
    fun text(name: String): String {
        val candidates = listOf(
            File("src/main/assets/data/$name"),
            File("app/src/main/assets/data/$name"),
        )
        return candidates.first { it.exists() }.readText()
    }
    return CurriculumJson.decode(text("profile.json"), text("lessons.json"), text("words.json"))
}
