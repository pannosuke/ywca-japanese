package dev.ericallerton.yoruben

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
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
                note = "",
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
        ReadScreen(
            ReadUi(
                title = "てください",
                level = "Level 1",
                body = "せんせいは 「なまえを 書[か]いてください」と いいました。エリックは なまえを 書[か]きました。",
                questions = listOf(
                    ReadQuestion("せんせいは なにを かきましたか。", listOf("なまえ", "ほん", "え"), 0, 0, false),
                    ReadQuestion("つぎに なにを しますか。", listOf("ほんを よむ", "みずを のむ", "ねる"), -1, 0, false),
                ),
                score = null,
            ),
            known = emptySet(),
            onClose = {},
            onPick = { _, _ -> },
            onCheck = {},
        )
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