package dev.ericallerton.yoruben

import dev.ericallerton.yoruben.anki.ankiField
import dev.ericallerton.yoruben.core.CardNotes
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
import org.junit.Assert.assertFalse
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
            "masu", "dict", "masen", "mashita", "masen_deshita", "te", "te_kudasai", "ta", "nai",
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
            val te = ConjugationReference.te(verb.kana, verb.group)
            assertEquals(item.id, te, item.expected)
            assertEquals(item.id, te, Conjugator.te(verb.kana, Conjugator.groupOf(verb.group)))
        }
        content.homework.flatMap { it.items }.forEach { item ->
            val verb = content.verbs.find { it.id == item.verbId } ?: return@forEach
            val prompt = item.promptRuby
            if ("ください" in prompt || "て形" in prompt) {
                assertEquals(item.id, ConjugationReference.te(verb.kana, verb.group), item.expected)
            }
        }
    }

    @Test
    fun everyVerbMatchesTheReference() {
        val forms = listOf(
            "dict", "masu", "masen", "mashita", "masen_deshita", "te", "ta", "nai", "te_kudasai",
        )
        content.verbs.forEach { verb ->
            val group = Conjugator.groupOf(verb.group)
            forms.forEach { form ->
                val expected = ConjugationReference.answer(verb.kana, verb.group, form)
                assertEquals("${verb.id} $form", expected, Conjugator.answer(verb.kana, group, form))
                val accepted = Conjugator.accepts(verb.kana, group, form, verb.ruby)
                assertTrue("${verb.id} $form kana", Conjugator.matches(expected, accepted))
                val kanji = ConjugationReference.kanji(verb.ruby, verb.kana, expected)
                assertTrue("${verb.id} $form $kanji", Conjugator.matches(kanji, accepted))
                assertDirection(verb.kana, group, form)
            }
        }
    }

    @Test
    fun explanationsNameTheDirection() {
        val (dictRuby, dictEn) = Conjugator.rule("すわる", VerbGroup.GODAN, "dict")
        assertEquals(
            "「ます」を とります。 いだん を うだん に もどします。 うどうしです。 る で おわっても、 るどうし では ありません。",
            dictRuby,
        )
        assertEquals(
            "Drop ます and change the i-row back to the u-row. This is an u-verb. It ends in ru, but it is an u-verb.",
            dictEn,
        )
        assertFalse(dictRuby.contains("いだん に して"))
        assertFalse(dictEn.contains("add masu"))
        val (masuRuby, _) = Conjugator.rule("すわる", VerbGroup.GODAN, "masu")
        assertTrue(masuRuby.contains("いだん に して「ます」"))
        assertFalse(masuRuby.contains("もど"))
        val (hairu, hairuEn) = Conjugator.rule("はいる", VerbGroup.GODAN, "te")
        assertEquals("うどうしです。 る で おわっても「って」です。 るどうし では ありません。", hairu)
        assertTrue(hairuEn.contains("tte, not te"))
        val (iku, ikuEn) = Conjugator.rule("いく", VerbGroup.GODAN, "te")
        assertTrue(iku.contains("「行[い]く」だけ"))
        assertTrue(iku.contains("って"))
        assertTrue(ikuEn.contains("itte, not ite"))
        assertEquals("suru becomes shi + masu.", Conjugator.rule("べんきょうする", VerbGroup.IRREGULAR, "masu").second)
        assertEquals("suru becomes shite.", Conjugator.rule("べんきょうする", VerbGroup.IRREGULAR, "te").second)
        assertTrue(Conjugator.rule("くる", VerbGroup.IRREGULAR, "nai").first.contains("来[こ]ない"))
        assertTrue(Conjugator.rule("みる", VerbGroup.ICHIDAN, "dict").first.contains("るどうしです"))
        assertFalse(Conjugator.rule("みる", VerbGroup.ICHIDAN, "dict").first.contains("ではありません"))
        val (past, _) = Conjugator.rule("たかい", VerbGroup.GODAN, "i_past")
        assertTrue(past.contains("かった"))
        assertFalse(past.contains("くない"))
    }

    @Test
    fun kanjiAndKanaBothPass() {
        val sit = Conjugator.accepts("すわる", VerbGroup.GODAN, "dict", "座[すわ]る")
        assertTrue(Conjugator.matches("すわる", sit))
        assertTrue(Conjugator.matches("座る", sit))
        assertTrue(Conjugator.matches("　座る　", sit))
        assertTrue(Conjugator.matches("座 る", sit))
        assertTrue(Conjugator.matches("ｽﾜﾙ", sit))
        assertTrue(Conjugator.matches("スワル", sit))
        assertFalse(Conjugator.matches("すわります", sit))
        assertTrue(Conjugator.matches("座って", Conjugator.accepts("すわる", VerbGroup.GODAN, "te", "座[すわ]る")))
        assertTrue(Conjugator.matches("行って", Conjugator.accepts("いく", VerbGroup.GODAN, "te", "行[い]く")))
        assertTrue(Conjugator.matches("来ない", Conjugator.accepts("くる", VerbGroup.IRREGULAR, "nai", "来[く]る")))
        assertTrue(Conjugator.matches("こない", Conjugator.accepts("くる", VerbGroup.IRREGULAR, "nai", "来[く]る")))
        assertTrue(Conjugator.matches("来て", Conjugator.accepts("くる", VerbGroup.IRREGULAR, "te", "来[く]る")))
        assertTrue(Conjugator.matches("勉強します", Conjugator.accepts("べんきょうする", VerbGroup.IRREGULAR, "masu", "勉強[べんきょう]する")))
        assertTrue(Conjugator.matches("勉強して", Conjugator.accepts("べんきょうする", VerbGroup.IRREGULAR, "te", "勉強[べんきょう]する")))
        assertFalse(Conjugator.matches("りょこうしります", Conjugator.accepts("りょこうする", VerbGroup.IRREGULAR, "masu", "旅行[りょこう]する")))
        assertFalse(Conjugator.matches("りょこうすって", Conjugator.accepts("りょこうする", VerbGroup.IRREGULAR, "te", "旅行[りょこう]する")))
        assertEquals("座[すわ]る", Conjugator.spellRuby("座[すわ]る", "すわる", "すわる"))
        assertEquals("来[こ]ない", Conjugator.spellRuby("来[く]る", "くる", "こない"))
        assertEquals("行[い]って", Conjugator.spellRuby("行[い]く", "いく", "いって"))
        listOf("しる" to "godan", "はいる" to "godan", "かえる" to "godan", "きる" to "godan").forEach { (kana, group) ->
            assertEquals("${kana}ます".let { ConjugationReference.answer(kana, group, "masu") }, Conjugator.answer(kana, VerbGroup.GODAN, "masu"))
            assertEquals(ConjugationReference.answer(kana, group, "te"), Conjugator.te(kana, VerbGroup.GODAN))
            assertEquals(ConjugationReference.answer(kana, group, "nai"), Conjugator.answer(kana, VerbGroup.GODAN, "nai"))
        }
        assertEquals("しります", Conjugator.answer("しる", VerbGroup.GODAN, "masu"))
        assertEquals("しって", Conjugator.te("しる", VerbGroup.GODAN))
        assertEquals("しらない", Conjugator.answer("しる", VerbGroup.GODAN, "nai"))
        assertEquals("はいって", Conjugator.te("はいる", VerbGroup.GODAN))
        assertEquals("かえって", Conjugator.te("かえる", VerbGroup.GODAN))
        assertEquals("きって", Conjugator.te("きる", VerbGroup.GODAN))
        assertEquals("かわない", Conjugator.answer("かう", VerbGroup.GODAN, "nai"))
        assertEquals("りょこうします", Conjugator.answer("りょこうする", VerbGroup.IRREGULAR, "masu"))
        assertEquals("りょこうして", Conjugator.te("りょこうする", VerbGroup.IRREGULAR))
        assertEquals("高くない", Conjugator.spell("高[たか]い", "たかい", "たかくない"))
        assertEquals("高かったです", Conjugator.spell("高[たか]い", "たかい", "たかかったです"))
    }

    @Test
    fun furiganaStaysWithItsWord() {
        val one = Ruby.words("書[か]いてください")
        assertEquals(1, one.size)
        assertEquals(listOf("書", "いてください"), one[0].map { it.base })
        val two = Ruby.words("なまえを 書[か]いて")
        assertEquals(2, two.size)
        assertEquals(" ", two[0].last().base)
        assertEquals("書", two[1][0].base)
        assertEquals("いて", two[1][1].base)
    }

    private fun assertDirection(kana: String, group: VerbGroup, form: String) {
        val (ruby, en) = Conjugator.rule(kana, group, form)
        assertTrue("$kana $form has bare kanji: $ruby", Ruby.bareKanji(ruby).isEmpty())
        when (form) {
            "dict" -> {
                assertTrue("$kana $ruby", ruby.contains("もど") || ruby.contains("とって「る」"))
                assertFalse(ruby, ruby.contains("いだん に して"))
                assertFalse(en, en.contains("add masu") || en.contains("add masen"))
            }
            "masu" -> {
                assertTrue(ruby, ruby.contains("ます"))
                assertFalse(ruby, ruby.contains("もど"))
                assertTrue(en, en.contains("masu"))
            }
            "masen" -> assertTrue(ruby, ruby.contains("ません") && !ruby.contains("ませんでした"))
            "mashita" -> assertTrue(ruby, ruby.contains("ました"))
            "masen_deshita" -> assertTrue(ruby, ruby.contains("ませんでした"))
            "te" -> assertTrue(ruby, ruby.contains("て") || ruby.contains("で"))
            "te_kudasai" -> {
                assertTrue(ruby, ruby.contains("ください"))
                assertFalse(ruby, ruby.contains("た形"))
            }
            "ta" -> {
                assertTrue(ruby, ruby.contains("た") || ruby.contains("だ"))
                assertFalse(ruby, ruby.contains("て形"))
            }
            "nai" -> {
                assertTrue(ruby, ruby.contains("ない"))
                assertFalse(ruby, ruby.contains("ます"))
            }
        }
        if (group == VerbGroup.GODAN && kana.endsWith("る")) {
            assertTrue("$kana $form $ruby", ruby.contains("るどうし では ありません"))
        }
        if (group == VerbGroup.ICHIDAN) {
            assertFalse("$kana $form $ruby", ruby.contains("ではありません"))
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

    @Test
    fun noteFieldIsSeparateFromTheStatusString() {
        val names = listOf("Word", "Definition", "Meaning", "Sentence", "SentenceEN", "Note")
        val fields = listOf("生活[せいかつ]", "まいにちの くらし。", "life", "", "", "prose\n<div class=\"kanji-mnemonic\">x</div>")
        assertEquals(fields[5], ankiField(names, fields, "Note", 5))
        assertEquals("from-index", ankiField(emptyList(), List(6) { if (it == 5) "from-index" else "" }, "Note", 5))
        assertEquals("named", ankiField(listOf("note"), listOf("named"), "Note", 5))
    }

    @Test
    fun deckNotesMatchTheMnemonicExport() {
        val withMnemonic = content.deck.cards.filter { "kanji-mnemonic" in it.note }
        assertEquals(46, withMnemonic.size)
        val distinct = withMnemonic.map { it.note }.toSet()
        assertEquals(45, distinct.size)
        withMnemonic.forEach { card ->
            val parsed = CardNotes.parse(card.note)
            assertTrue(card.id, parsed.hasKanji)
            assertFalse(card.id, "<" in parsed.prose || ">" in parsed.prose)
            (listOf(parsed.intro) + parsed.items.flatMap { listOf(it.kanji, it.parts, it.english, it.japanese) }).forEach { line ->
                assertFalse(card.id + line, "<" in line || ">" in line)
            }
            parsed.items.forEach { item ->
                assertTrue(card.id, Ruby.bareKanji(item.kanji).isEmpty())
                assertTrue(card.id + item.japanese, Ruby.bareKanji(item.japanese).isEmpty())
            }
            val study = card.toPublic()
            assertEquals(card.note, study)
        }
        val life = content.deck.cards.first { it.word.startsWith("生活") }
        val parsed = CardNotes.parse(life.note)
        assertTrue(parsed.prose.contains("暮[く]らす"))
        assertEquals(listOf("生[せい]", "活[かつ]"), parsed.items.map { it.kanji })
        assertEquals("土 earth + a sprout", parsed.items[0].parts)
        assertEquals("A plant pushing up through the soil — life beginning.", parsed.items[0].english)
        assertEquals("土[つち]から 芽[め]が 出[で]る。", parsed.items[0].japanese)
        assertTrue(Ruby.bareKanji(parsed.items[0].japanese).isEmpty())
        val night = CardNotes.parse(content.deck.cards.first { it.word.startsWith("昨夜") }.note)
        assertTrue(night.intro.contains("ゆうべ"))
        assertEquals("昨[さく]", night.items[0].kanji)
        val plain = CardNotes.parse("クラスの メモ。 <b>太字</b>")
        assertEquals("クラスの メモ。 太字", plain.prose)
        assertTrue(plain.items.isEmpty())
        val hostile = CardNotes.parse("メモ <script>alert(1)</script><!--hide-->&amp; ok")
        assertEquals("メモ & ok", hostile.prose)
        assertFalse("<" in hostile.prose)
    }

    private fun dev.ericallerton.yoruben.core.DeckCard.toPublic() =
        dev.ericallerton.yoruben.core.Study(content, ProgressFile()).dueLocal(0L, 500)
            .first { it.id == id }.note
}
