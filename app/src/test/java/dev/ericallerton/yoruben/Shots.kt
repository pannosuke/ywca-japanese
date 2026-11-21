package dev.ericallerton.yoruben

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.ericallerton.yoruben.core.CardNotes
import dev.ericallerton.yoruben.core.Conjugator
import dev.ericallerton.yoruben.core.StudyContent
import dev.ericallerton.yoruben.core.VerbGroup
import java.io.File
import dev.ericallerton.yoruben.ui.DrillScreen
import dev.ericallerton.yoruben.ui.DrillUi
import dev.ericallerton.yoruben.ui.HomeScreen
import dev.ericallerton.yoruben.ui.HomeUi
import dev.ericallerton.yoruben.ui.QuizScreen
import dev.ericallerton.yoruben.ui.QuizUi
import dev.ericallerton.yoruben.ui.ReadQuestion
import dev.ericallerton.yoruben.ui.ReadScreen
import dev.ericallerton.yoruben.ui.ReadUi
import dev.ericallerton.yoruben.ui.ReviewScreen
import dev.ericallerton.yoruben.ui.ReviewUi
import dev.ericallerton.yoruben.ui.StatBlock
import dev.ericallerton.yoruben.ui.StatsScreen
import dev.ericallerton.yoruben.ui.StatsUi
import dev.ericallerton.yoruben.ui.YorubenTheme
import org.junit.Rule
import org.junit.Test

class Shots {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
    )

    @Test fun home() = shot("home") {
        HomeScreen(
            HomeUi(
                "20 November",
                listOf(
                    "Review" to "18 due",
                    "Quiz" to "3 weak",
                    "Conjugation" to "Te-form first",
                    "Read" to "2 stories",
                    "Stats" to "4-day streak",
                ),
            ),
            onOpen = {},
            onStart = {},
        )
    }

    @Test fun reviewKanji() = shot("review-kanji") {
        val card = StudyContent.load(File("content")).deck.cards.first { it.word.startsWith("生活") }
        val note = CardNotes.parse(card.note)
        ReviewScreen(
            ReviewUi(
                counter = "3 of 18 · On this phone",
                word = card.word,
                revealed = true,
                meaning = card.meaning,
                definition = card.definition,
                sentence = card.sentence,
                sentenceEn = card.sentenceEn,
                empty = null,
                offerSaved = false,
                summary = null,
                status = "",
                cardNote = note.prose,
                kanjiIntro = note.intro,
                kanji = note.items,
            ),
            known = emptySet(),
            onClose = {},
            onReveal = {},
            onGrade = {},
            onSaved = {},
        )
    }

    @Test fun review() = shot("review") {
        ReviewScreen(
            ReviewUi(
                counter = "4 of 18 · AnkiDroid",
                word = "行[い]きます",
                revealed = true,
                meaning = "go",
                definition = "ある ばしょへ うごく。",
                sentence = "えきへ 行[い]きます。",
                sentenceEn = "I go to the station.",
                empty = null,
                offerSaved = false,
                summary = null,
                status = "",
            ),
            known = emptySet(),
            onClose = {},
            onReveal = {},
            onGrade = {},
            onSaved = {},
        )
    }

    @Test fun quiz() = shot("quiz") {
        QuizScreen(
            QuizUi(
                setup = false,
                lessons = emptyList(),
                lessonId = null,
                sources = emptySet(),
                weakOnly = false,
                count = 8,
                counter = "2 of 8",
                prompt = "行[い]く の て形[けい] は？",
                promptEn = "",
                choices = listOf("いて", "いって", "いいた", "いくて"),
                picked = "",
                typed = "",
                verdict = null,
                expected = "いって",
                reask = false,
                done = false,
                summary = "",
                misses = emptyList(),
                empty = false,
            ),
            known = emptySet(),
            onClose = {},
            onLesson = {},
            onSource = {},
            onWeak = {},
            onCount = {},
            onStart = {},
            onPick = {},
            onType = {},
            onCheck = {},
            onNext = {},
            onSheets = {},
        )
    }

    @Test fun conjugationCorrect() = shot("conjugation-correct") {
        DrillScreen(drill(verdict = true, typed = "だして", word = "出[だ]す"), emptySet(), {}, {}, {}, {}, {}, {})
    }

    @Test fun conjugationDictWrong() = shot("conjugation-dict-wrong") {
        val (rule, en) = Conjugator.rule("すわる", VerbGroup.GODAN, "dict")
        DrillScreen(
            DrillUi(
                counter = "1 of 2",
                word = "座[すわ]ります",
                form = "じしょけい",
                prompt = "ます を じしょけい に",
                typed = "すわります",
                tiles = emptyList(),
                tileMode = false,
                verdict = false,
                expected = Conjugator.spellRuby("座[すわ]る", "すわる", "すわる"),
                rule = rule,
                ruleEn = en,
            ),
            emptySet(), {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun conjugationKanji() = shot("conjugation-kanji") {
        val (rule, en) = Conjugator.rule("すわる", VerbGroup.GODAN, "dict")
        DrillScreen(
            DrillUi(
                counter = "1 of 2",
                word = "座[すわ]ります",
                form = "じしょけい",
                prompt = "ます を じしょけい に",
                typed = "座る",
                tiles = emptyList(),
                tileMode = false,
                verdict = true,
                expected = Conjugator.spellRuby("座[すわ]る", "すわる", "すわる"),
                rule = rule,
                ruleEn = en,
            ),
            emptySet(), {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun conjugationWrong() = shot("conjugation-wrong") {
        DrillScreen(
            drill(
                verdict = false,
                typed = "いて",
                word = "行[い]く",
                expected = "いって",
                rule = "「行[い]く」だけ て形[けい]は「って」。ほかの「く」は「いて」。",
                ruleEn = "iku is the exception: te-form is itte, not ite.",
            ),
            emptySet(), {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun reader() = shot("reader") {
        ReadScreen(readStory(picked = 1, checked = false), emptySet(), {}, { _, _ -> }, {})
    }

    @Test fun readerAnswered() = shot("reader-answered") {
        ReadScreen(readStory(picked = 1, checked = true), emptySet(), {}, { _, _ -> }, {})
    }

    @Test fun stats() = shot("stats") {
        StatsScreen(
            StatsUi(
                listOf(
                    StatBlock(
                        "",
                        listOf(
                            "Streak" to "4 days",
                            "Retention" to "86%",
                            "Today" to "12",
                            "Conjugation" to "71%",
                        ),
                    ),
                    StatBlock(
                        "Weak",
                        listOf(
                            "行[い]く · て形[けい]" to "3",
                            "出[だ]す · て形[けい]" to "2",
                        ),
                    ),
                    StatBlock("Homework", listOf("てください" to "11 of 14")),
                ),
            ),
            known = emptySet(),
            onClose = {},
            onShare = {},
        )
    }

    private fun readStory(picked: Int, checked: Boolean) = ReadUi(
        title = "えきまで",
        level = "Level 0",
        body = "エリックは えきへ 行[い]きます。でんしゃに 乗[の]ります。",
        questions = listOf(
            ReadQuestion(
                "エリックは なにに のりますか。",
                listOf("でんしゃ", "ひこうき", "じてんしゃ"),
                picked,
                0,
                checked,
            ),
        ),
        score = if (checked) "0 of 1" else null,
    )

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.snapshot(name) {
            YorubenTheme(dark = false) { content() }
        }
    }

    private fun drill(
        verdict: Boolean?,
        typed: String,
        word: String,
        expected: String = "だして",
        rule: String = "うどうし。「す」は「して」。",
        ruleEn: String = "u-verb. su becomes shite.",
    ) = DrillUi(
        counter = "4 of 6",
        word = word,
        form = "て形[けい]",
        prompt = "じしょけい を て形[けい] に",
        typed = typed,
        tiles = emptyList(),
        tileMode = false,
        verdict = verdict,
        expected = expected,
        rule = rule,
        ruleEn = ruleEn,
    )
}