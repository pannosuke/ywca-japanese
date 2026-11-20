package dev.ericallerton.yoruben

import dev.ericallerton.yoruben.core.Conjugator
import dev.ericallerton.yoruben.core.ContentChecks
import dev.ericallerton.yoruben.core.Fsrs
import dev.ericallerton.yoruben.core.FsrsCard
import dev.ericallerton.yoruben.core.ProgressFile
import dev.ericallerton.yoruben.core.Question
import dev.ericallerton.yoruben.core.QuizRequest
import dev.ericallerton.yoruben.core.QuizSession
import dev.ericallerton.yoruben.core.Ruby
import dev.ericallerton.yoruben.core.Study
import dev.ericallerton.yoruben.core.StudyContent
import dev.ericallerton.yoruben.core.VerbGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StudyTest {
    private val root = File("content")
    private val content = StudyContent.load(root)

    @Test
    fun schemaFilesAndRequiredForms() {
        content.manifest.files.forEach { name ->
            assertTrue("$name missing", File(root, name).isFile)
        }
        assertEquals(1, content.manifest.schema)
        assertTrue(content.lessons.isNotEmpty())
        assertTrue(content.vocab.size > 50)
        assertTrue(content.verbs.size > 20)
        assertEquals("YWCA", content.profile.deckName)
        assertEquals("YWCA Vocab", content.profile.noteType)
        assertTrue(content.profile.knownKanji.isEmpty())
        val forms = content.forms.filter { it.enabled }.map { it.id }.toSet()
        listOf(
            "masu", "dict", "masen", "mashita", "masen_deshita", "te", "te_kudasai",
            "i_negative", "i_past", "i_past_negative", "na_negative", "na_past", "na_past_negative",
        ).forEach { assertTrue(it, it in forms && Conjugator.supports(it)) }
        content.readers.forEach { reader ->
            assertTrue(reader.id, reader.questions.size in 3..5)
        }
        content.homework.forEach { result ->
            assertEquals(result.id, result.items.count { it.correct }, result.correct)
            assertEquals(result.id, result.items.size, result.total)
        }
        assertTrue(content.deck.cards.isNotEmpty())
    }

    @Test
    fun furiganaOnDisplayTextAndSource() {
        val contentHits = ContentChecks.displayViolations(root, emptySet())
        assertTrue(contentHits.joinToString("\n"), contentHits.isEmpty())
        val codeHits = ContentChecks.kotlinViolations(File("app/src/main/java"), emptySet())
        assertTrue(codeHits.joinToString("\n"), codeHits.isEmpty())
    }

    @Test
    fun knownKanjiDropsItsReading() {
        val pieces = Ruby.parse("行[い]く", setOf('行'))
        assertTrue(pieces.none { it.reading.isNotEmpty() })
        assertTrue(Ruby.bareKanji("行く").isNotEmpty())
        assertTrue(Ruby.bareKanji("行[い]く").isEmpty())
    }

    @Test
    fun conjugationMatchesExceptionsAndWorksheet() {
        content.exceptions.forEach { (kana, forms) ->
            val group = Conjugator.groupOf(forms["group"] ?: "godan")
            forms.filterKeys { it != "group" }.forEach { (form, expected) ->
                assertEquals("$kana $form", expected, Conjugator.answer(kana, group, form))
            }
        }
        assertEquals("いって", Conjugator.te("いく", VerbGroup.GODAN))
        assertEquals("だして", Conjugator.te("だす", VerbGroup.GODAN))
        assertEquals("はなして", Conjugator.te("はなす", VerbGroup.GODAN))
        assertEquals("して", Conjugator.te("する", VerbGroup.IRREGULAR))
        assertEquals("きて", Conjugator.te("くる", VerbGroup.IRREGULAR))
        assertEquals("みて", Conjugator.te("みる", VerbGroup.ICHIDAN))
        assertEquals("のんで", Conjugator.te("のむ", VerbGroup.GODAN))
        assertEquals("かいて", Conjugator.te("かく", VerbGroup.GODAN))
        content.worksheets.flatMap { it.items }.forEach { item ->
            val verb = content.verbs.find { it.id == item.verbId } ?: return@forEach
            assertEquals(item.id, item.expected, Conjugator.te(verb.kana, Conjugator.groupOf(verb.group)))
        }
    }

    @Test
    fun adjectiveFormsAcceptClassPolite() {
        assertEquals("たかかった", Conjugator.answer("たかい", VerbGroup.GODAN, "i_past"))
        assertTrue(Conjugator.matches("たかかったです", Conjugator.accepts("たかい", VerbGroup.GODAN, "i_past")))
        assertEquals("たかくない", Conjugator.answer("たかい", VerbGroup.GODAN, "i_negative"))
        assertEquals("しずかだった", Conjugator.answer("しずか", VerbGroup.GODAN, "na_past"))
        assertTrue(Conjugator.matches("しずかでした", Conjugator.accepts("しずか", VerbGroup.GODAN, "na_past")))
        assertTrue(Conjugator.matches("しずかじゃありませんでした", Conjugator.accepts("しずか", VerbGroup.GODAN, "na_past_negative")))
    }

    @Test
    fun againIsDueBeforeGood() {
        val now = 1_700_000_000_000L
        val again = Fsrs.review(FsrsCard(), 1, now)
        val good = Fsrs.review(FsrsCard(), 3, now)
        assertTrue(again.dueMs < good.dueMs)
        assertTrue(again.dueMs - now < 10 * 60_000L)
        assertTrue(good.dueMs - now >= 86_400_000L)
        val later = Fsrs.review(good, 1, good.dueMs)
        assertTrue(later.dueMs - good.dueMs < good.dueMs - now)
    }

    @Test
    fun quizReasksAMissOnce() {
        val session = QuizSession(
            listOf(
                Question("a", "fill_blank", "なまえを ____", answer = "かいて", source = "worksheet"),
                Question("b", "fill_blank", "ほんを ____", answer = "よんで", source = "worksheet"),
            ),
        )
        assertTrue(!session.submit("まちがい"))
        assertTrue(session.submit("よんで"))
        val again = session.current()
        assertEquals("a", again?.id)
        assertTrue(again?.reask == true)
        assertTrue(session.submit("かいて"))
        assertTrue(session.done())
        assertEquals(2, session.correct)
    }

    @Test
    fun drillPrefersTheWeakTeForm() {
        val study = Study(content, ProgressFile(profileId = "eric"))
        var te = 0
        var masu = 0
        var weakTe = 0
        repeat(500) { seed ->
            val prompt = study.nextDrill(seed.toLong()) ?: return@repeat
            if (prompt.formId == "te") te++
            if (prompt.formId == "masu") masu++
            if (prompt.itemId == "iku" && prompt.formId == "te") weakTe++
            if (prompt.itemId == "dasu" && prompt.formId == "te") weakTe++
        }
        assertTrue("te=$te masu=$masu weak=$weakTe", te > masu * 2 && weakTe > 0)
    }

    @Test
    fun exportNamesTheProfile() {
        val json = Study(content, ProgressFile(profileId = "eric")).exportJson(0L)
        assertTrue(json.contains("\"profile_id\": \"eric\"") || json.contains("\"profile_id\":\"eric\""))
    }
}
