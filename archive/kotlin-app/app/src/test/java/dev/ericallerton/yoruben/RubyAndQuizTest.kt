package dev.ericallerton.yoruben

import dev.ericallerton.yoruben.core.AnswerCheck
import dev.ericallerton.yoruben.core.AnkiCardRow
import dev.ericallerton.yoruben.core.AnkiDeckRow
import dev.ericallerton.yoruben.core.AnkiNoteRow
import dev.ericallerton.yoruben.core.AnkiSummarizer
import dev.ericallerton.yoruben.core.AttemptRecord
import dev.ericallerton.yoruben.core.CardBucket
import dev.ericallerton.yoruben.core.ClassBriefs
import dev.ericallerton.yoruben.core.QuizFactory
import dev.ericallerton.yoruben.core.QuizMode
import dev.ericallerton.yoruben.core.RubyMarkup
import dev.ericallerton.yoruben.core.RubyPiece
import dev.ericallerton.yoruben.core.pickWeighted
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubyAndQuizTest {
    @Test
    fun parsesOkuriganaCompoundsAndLatin() {
        val yuhi = RubyMarkup.parse("夕日[ゆうひ]")
        assertEquals(listOf(RubyPiece.Ruby("夕日", "ゆうひ")), yuhi)
        assertEquals("ゆうひ", RubyMarkup.spoken("夕日[ゆうひ]"))

        val kibishii = RubyMarkup.parse("厳[きび]しい")
        assertEquals(RubyPiece.Ruby("厳", "きび"), kibishii[0])
        assertEquals(RubyPiece.Text("しい"), kibishii[1])
        assertEquals("きびしい", RubyMarkup.spoken("厳[きび]しい"))

        val wall = RubyMarkup.spoken("万里[ばんり]の 長城[ちょうじょう]")
        assertEquals("ばんりの ちょうじょう", wall)

        assertEquals("のりもので いきます", RubyMarkup.spoken("[のりもの]で 行[い]きます"))
        assertEquals("エーせん", RubyMarkup.spoken("A[エー]線[せん]"))
        assertEquals("いばらきえき", RubyMarkup.spoken("茨木駅[いばらきえき]"))
        assertFalse(RubyMarkup.hasBareKanji("He wrote 暇[ひま]."))
        assertTrue(RubyMarkup.hasBareKanji("暇な"))
    }

    @Test
    fun gradingKeepsSmallKanaAndTheRo() {
        assertTrue(AnswerCheck.matches(listOf("サンディエゴ"), "サンディエゴ"))
        assertTrue(AnswerCheck.matches(listOf("サンディエゴ"), "サンディエゴ"))
        assertFalse(AnswerCheck.matches(listOf("サンディエゴ"), "サンジエゴ"))
        assertFalse(AnswerCheck.matches(listOf("おもしろかった"), "おもしかった"))
        assertTrue(AnswerCheck.matches(listOf("おもしろかった"), "おもしろかったです"))
        assertTrue(AnswerCheck.matches(listOf("ひま", "ひまな"), "ヒマ"))
        assertFalse(AnswerCheck.matches(listOf("ひま"), "たいくつ"))
        assertFalse(AnswerCheck.matches(listOf("ぶっか"), "ねだん"))
        val hint = AnswerCheck.mistake(
            listOf(dev.ericallerton.yoruben.core.MistakeHint("おもしかった", "the ろ stays")),
            "おもしかったです",
        )
        assertEquals("the ろ stays", hint?.hintEn)
    }

    @Test
    fun weakSpotsAreBiasedAndFrequencyIsNotDrilledAsAnError() {
        val curriculum = loadCurriculum()
        val random = Random(7)
        val pool = QuizFactory.buildPool(curriculum, QuizMode.Mix, random)
        assertTrue(pool.none { item -> item.mistakes.any { it.answer.contains("ひんどう") } })
        assertTrue(pool.none { it.id.contains("hind") || it.explainEn.contains("頻率") })
        val heavy = pool.filter { it.weakSpotId != null }.map { it.id }.toSet()
        assertTrue(heavy.contains("w-hima-mc"))
        assertTrue(heavy.contains("w-omo-fill"))
        assertTrue(heavy.contains("w-bukka-mc"))
        assertTrue(heavy.contains("w-di-mc"))

        var weakPicks = 0
        var plainPicks = 0
        repeat(400) { trial ->
            val session = QuizFactory.session(curriculum, QuizMode.Mix, 8, emptyList(), Random(trial.toLong()))
            weakPicks += session.count { it.weakSpotId != null }
            plainPicks += session.count { it.weakSpotId == null && it.weight == 1 }
        }
        assertTrue("weak picks $weakPicks should beat plain grammar picks $plainPicks", weakPicks > plainPicks)

        var heavyWins = 0
        repeat(60) { seed ->
            val picked = pickWeighted(listOf("weak", "plain"), 1, Random(seed)) { if (it == "weak") 20 else 1 }
            if (picked.single() == "weak") heavyWins++
        }
        assertTrue(heavyWins > 50)
    }

    @Test
    fun missesGetAnotherTurn() {
        val curriculum = loadCurriculum()
        val target = "word-fill-暇な"
        var hits = 0
        repeat(80) { trial ->
            val session = QuizFactory.session(
                curriculum,
                QuizMode.Words,
                6,
                List(3) { AttemptRecord(target, false) },
                Random(trial.toLong()),
            )
            if (session.any { it.id == target }) hits++
        }
        assertTrue("recent misses should surface often, got $hits", hits > 24)
    }

    @Test
    fun tuesdayAfternoonBriefIsTsujiAndStaysOnHisCommute() {
        val curriculum = loadCurriculum()
        val afternoon = ClassBriefs.prepare(curriculum, LocalDate.parse("2026-11-17"), LocalTime.of(15, 0))
        assertEquals(LocalDate.parse("2026-11-17"), afternoon.nextDate)
        assertEquals("tsuji", afternoon.teacher.id)
        assertEquals("dialogues", afternoon.focusEn)
        assertEquals("2026-11-13", afternoon.lastLesson?.date)
        assertEquals("2026-11-10", afternoon.lastWithTeacher?.date)
        val text = afternoon.commuteRuby + afternoon.commuteEn
        assertTrue(text.contains("M16"))
        assertFalse(text.contains("梅田"))
        assertFalse(text.contains("すいた"))
        assertTrue(afternoon.weakSpots.any { it.id == "hima" })
        assertTrue(afternoon.reviewWords.any { it.id == "物価" || it.id == "値段" || it.id == "暇な" })

        val after = ClassBriefs.prepare(curriculum, LocalDate.parse("2026-11-17"), LocalTime.of(21, 0))
        assertEquals(LocalDate.parse("2026-11-20"), after.nextDate)
        assertEquals("ouchi", after.teacher.id)
    }

    @Test
    fun ankiSummaryGroupsClassTagsWithoutLeavingTheDeck() {
        val decks = listOf(
            AnkiDeckRow(1, "YWCA"),
            AnkiDeckRow(2, "YWCA::Word Bank"),
            AnkiDeckRow(9, "Other"),
        )
        val notes = listOf(
            AnkiNoteRow(10, " class::2026-10-27 "),
            AnkiNoteRow(11, " class::2026-10-27 "),
            AnkiNoteRow(12, " class::2026-10-16 extra "),
            AnkiNoteRow(13, " unrelated "),
        )
        val cards = listOf(
            AnkiCardRow(1, 10, 1, 2),
            AnkiCardRow(2, 11, 2, -1),
            AnkiCardRow(3, 12, 1, 0),
            AnkiCardRow(4, 13, 1, 2),
            AnkiCardRow(5, 12, 9, 2),
        )
        val summary = AnkiSummarizer.summarize(
            decks,
            notes,
            cards,
            mapOf("2026-10-27" to "class::2026-10-27", "2026-10-16" to "class::2026-10-16"),
            "YWCA",
        )
        assertTrue(summary.deckFound)
        val oct27 = summary.lessons.first { it.lessonDate == "2026-10-27" }
        assertEquals(2, oct27.total)
        assertEquals(1, oct27.review)
        assertEquals(1, oct27.suspended)
        val oct16 = summary.lessons.first { it.lessonDate == "2026-10-16" }
        assertEquals(1, oct16.total)
        assertEquals(1, oct16.newCount)
        assertEquals(CardBucket.Learning, AnkiSummarizer.bucket(3))
    }

    @Test
    fun readerSourceOnlyQueries() {
        val src = listOf(
            File("src/main/java/dev/ericallerton/yoruben/anki/AnkiDroidReader.kt"),
            File("app/src/main/java/dev/ericallerton/yoruben/anki/AnkiDroidReader.kt"),
        ).first { it.exists() }.readText()
        listOf(".insert(", ".update(", ".delete(", "bulkInsert", "applyBatch").forEach { forbidden ->
            assertFalse(src.contains(forbidden))
        }
        assertTrue(src.contains("contentResolver.query"))
    }
}
