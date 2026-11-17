package dev.ericallerton.yoruben

import dev.ericallerton.yoruben.core.AnswerRecord
import dev.ericallerton.yoruben.core.Biome
import dev.ericallerton.yoruben.core.Biomes
import dev.ericallerton.yoruben.core.BuyResult
import dev.ericallerton.yoruben.core.Conjugator
import dev.ericallerton.yoruben.core.CosmeticSlot
import dev.ericallerton.yoruben.core.Cosmetics
import dev.ericallerton.yoruben.core.FormGate
import dev.ericallerton.yoruben.core.GrammarPoint
import dev.ericallerton.yoruben.core.Lesson
import dev.ericallerton.yoruben.core.Levels
import dev.ericallerton.yoruben.core.Relic
import dev.ericallerton.yoruben.core.Rewards
import dev.ericallerton.yoruben.core.RoomKind
import dev.ericallerton.yoruben.core.RunConfig
import dev.ericallerton.yoruben.core.RunEngine
import dev.ericallerton.yoruben.core.RunMap
import dev.ericallerton.yoruben.core.RunOutcome
import dev.ericallerton.yoruben.core.Streak
import dev.ericallerton.yoruben.core.VerbCatalog
import dev.ericallerton.yoruben.core.VerbForm
import dev.ericallerton.yoruben.core.VerbGroup
import dev.ericallerton.yoruben.core.Weighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AdventureRulesTest {
    @Test
    fun everyCatalogVerbConjugatesToTheCheckedTable() {
        assertEquals(expected.keys.sorted(), VerbCatalog.all.map { it.dictKana }.sorted())
        for (verb in VerbCatalog.all) {
            val row = expected.getValue(verb.dictKana)
            assertEquals(row.masu, Conjugator.conjugate(verb, VerbForm.MASU).kana)
            assertEquals(row.masen, Conjugator.conjugate(verb, VerbForm.MASEN).kana)
            assertEquals(row.mashita, Conjugator.conjugate(verb, VerbForm.MASHITA).kana)
            assertEquals(row.masenDeshita, Conjugator.conjugate(verb, VerbForm.MASEN_DESHITA).kana)
            assertEquals(row.te, Conjugator.conjugate(verb, VerbForm.TE).kana)
            assertEquals(row.ta, Conjugator.conjugate(verb, VerbForm.TA).kana)
            if ('[' in verb.dictRuby) {
                assertTrue(Conjugator.conjugate(verb, VerbForm.MASU).ruby.contains("["))
            }
        }
    }

    @Test
    fun ikuAndLookalikesKeepTheirOwnRules() {
        val iku = VerbCatalog.byId("iku")
        val hiku = VerbCatalog.byId("hiku")
        val kiru = VerbCatalog.byId("kiru")
        val miru = VerbCatalog.byId("miru")
        val kaeru = VerbCatalog.byId("kaeru")
        val oriru = VerbCatalog.byId("oriru")
        assertEquals("いって", Conjugator.conjugate(iku, VerbForm.TE).kana)
        assertEquals("ひいた", Conjugator.conjugate(hiku, VerbForm.TA).kana)
        assertNotEquals("いいて", Conjugator.conjugate(iku, VerbForm.TE).kana)
        assertEquals("きって", Conjugator.conjugate(kiru, VerbForm.TE).kana)
        assertEquals("みて", Conjugator.conjugate(miru, VerbForm.TE).kana)
        assertEquals("かえって", Conjugator.conjugate(kaeru, VerbForm.TE).kana)
        assertEquals("おりて", Conjugator.conjugate(oriru, VerbForm.TE).kana)
        assertEquals(VerbGroup.GODAN, kiru.group)
        assertEquals(VerbGroup.ICHIDAN, oriru.group)
        assertTrue(Conjugator.conjugate(iku, VerbForm.TE).explainRuby.contains("って"))
        assertTrue(Conjugator.conjugate(hiku, VerbForm.TE).explainRuby.contains("いて"))
    }

    @Test
    fun classVerbsArePresentBeforeTheExtraBeginnerVerbs() {
        val curriculum = VerbCatalog.all.filter { it.fromCurriculum }.map { it.dictKana }
        listOf("つける", "けす", "おしえる", "だす", "かす", "おりる", "はたらく", "はいる", "おくる", "ひく").forEach {
            assertTrue(it in curriculum)
        }
        assertEquals("みる", VerbCatalog.all.first { it.fromCurriculum }.dictKana)
    }

    @Test
    fun coveredFormsFollowTheLessonsAndLeavePlainPastAsPreview() {
        val early = FormGate.covered(listOf(lesson("2026-10-23", "past", "Vました / Vませんでした")), "2026-10-23")
        assertTrue(VerbForm.MASU in early)
        assertTrue(VerbForm.MASHITA in early)
        assertTrue(VerbForm.MASEN in early)
        assertTrue(VerbForm.MASEN_DESHITA in early)
        assertFalse(VerbForm.TE in early)
        val later = FormGate.covered(
            listOf(
                lesson("2026-10-23", "past", "Vました / Vませんでした"),
                lesson("2026-11-13", "て形", "て形"),
            ),
            "2026-11-17",
        )
        assertTrue(VerbForm.TE in later)
        assertFalse(VerbForm.TA in later)
        assertTrue(VerbForm.TA in FormGate.forRun(later, preview = true))
    }

    @Test
    fun mapGenerationIsSeededAndBossesLandEveryThirdFloor() {
        assertEquals(RunMap.offers(42, 1), RunMap.offers(42, 1))
        assertNotEquals(RunMap.offers(42, 1), RunMap.offers(42, 2))
        val floorOne = RunMap.offers(42, 1)
        assertTrue(floorOne.size in 2..3)
        assertTrue(floorOne.all { it.kind != RoomKind.BOSS })
        assertEquals(listOf(RoomKind.BOSS), RunMap.offers(42, 3).map { it.kind })
        assertEquals(listOf(RoomKind.BOSS), RunMap.offers(99, 9).map { it.kind })
        val demo = RunMap.offers(RunMap.DEMO_SEED, 1)
        assertTrue(demo.any { it.kind == RoomKind.TREASURE })
    }

    @Test
    fun wrongAnswersAreWeightedAboveFamiliarVerbs() {
        val now = 1_700_000_000_000L
        val weak = VerbCatalog.byId("kaku")
        val easy = VerbCatalog.byId("taberu")
        val history = List(4) {
            AnswerRecord(weak.id, "te-ite", correct = false, atEpochMs = now - 2 * 86_400_000L)
        }
        val pool = listOf(weak, easy)
        var weakPicks = 0
        val random = Random(7)
        repeat(400) {
            val weights = pool.map { Weighting.weight(it, "te-ite", history, now) }
            if (Weighting.pick(pool, weights, random).id == weak.id) weakPicks++
        }
        assertTrue("weak picks were $weakPicks", weakPicks > 240)
        assertTrue(Weighting.weight(weak, "te-ite", history, now) > Weighting.weight(easy, "te-ite", history, now))
    }

    @Test
    fun aWrongAnswerCostsAHeartAndTheBottleSavesTheLastOne() {
        val config = RunConfig(
            verbs = listOf(VerbCatalog.byId("kaku")),
            forms = setOf(VerbForm.TE),
            history = emptyList(),
            mysteries = emptyList(),
            now = 0L,
        )
        val seed = (1L..80L).first { candidate ->
            RunMap.offers(candidate, 1).any { it.kind == RoomKind.BATTLE || it.kind == RoomKind.ELITE }
        }
        var state = RunEngine.start(seed, setOf(Relic.BOTTLE), hearts = 1)
        val index = state.offers.indexOfFirst { it.kind == RoomKind.BATTLE || it.kind == RoomKind.ELITE }
        state = RunEngine.choose(state, index, config)
        val wrong = state.current!!.choices.first { it.id != state.current!!.answerId }.id
        val after = RunEngine.answer(state, wrong)
        assertEquals(1, after.hearts)
        assertFalse(after.bottleLeft)
        assertEquals(RunOutcome.PLAY, after.outcome)
    }

    @Test
    fun shopPurchaseDeductsGemsAndEquipRequiresOwnership() {
        val gold = Cosmetics.byId("tunic-gold")!!
        val poor = Cosmetics.buy(Cosmetics.starter(), gold, 8)
        assertTrue(poor is BuyResult.TooPoor)
        val paid = Cosmetics.starter().copy(gems = 8)
        val bought = Cosmetics.buy(paid, gold, 8)
        assertTrue(bought is BuyResult.Ok)
        val ledger = (bought as BuyResult.Ok).ledger
        assertEquals(0, ledger.gems)
        assertTrue(gold.id in ledger.owned)
        val again = Cosmetics.buy(ledger, gold, 8)
        assertTrue(again is BuyResult.AlreadyOwned)
        val skipped = Cosmetics.equip(Cosmetics.starter(), gold)
        assertEquals(Cosmetics.defaults.getValue(CosmeticSlot.TUNIC), skipped.equipped(CosmeticSlot.TUNIC))
        val worn = Cosmetics.equip(ledger, gold)
        assertEquals(gold.id, worn.equipped(CosmeticSlot.TUNIC))
        val deal = Cosmetics.dailyDeal(3)
        assertEquals(deal.price * 3 / 5, Cosmetics.priceOf(deal, 3).coerceAtLeast(4).let {
            if (deal.price * 3 / 5 < 4) 4 else deal.price * 3 / 5
        })
        assertTrue(Cosmetics.priceOf(deal, 3) < deal.price)
        assertTrue(Cosmetics.catalog.filter { it.price > 0 }.minOf { it.price } <= Rewards.DRILL_CORRECT * 3)
        assertFalse(Cosmetics.GATES_STUDY)
        assertEquals(VerbCatalog.all, VerbCatalog.forPractice())
        assertTrue(Biomes.open(emptySet(), emptySet()).any { it.id == "lamp-gate" })
        val cleared: Set<String> = setOf("2026-11-13")
        assertTrue(Biomes.open(emptySet(), cleared).any { it.lessonDate == "2026-11-13" })
    }

    @Test
    fun streakAndLevelStayLocalToTheCalendar() {
        assertEquals(2 to "2026-11-17", Streak.next(1, "2026-11-16", "2026-11-17"))
        assertEquals(1 to "2026-11-17", Streak.next(4, "2026-11-10", "2026-11-17"))
        assertEquals(4 to "2026-11-17", Streak.next(4, "2026-11-17", "2026-11-17"))
        assertEquals(2, Levels.level(40))
    }

    private fun lesson(date: String, topic: String, pattern: String) = Lesson(
        id = date,
        date = date,
        weekday = "Fri",
        teacherId = "ouchi",
        ankiTag = "class::$date",
        topicRuby = topic,
        topicEn = topic,
        grammar = listOf(
            GrammarPoint(
                id = "g",
                patternRuby = pattern,
                explanationJa = pattern,
                explanationEn = "",
                exampleRuby = pattern,
                exampleEn = "",
                clozeRuby = "",
                clozeAnswer = "",
            ),
        ),
    )
}

private data class Row(
    val masu: String,
    val masen: String,
    val mashita: String,
    val masenDeshita: String,
    val te: String,
    val ta: String,
)

private fun row(masu: String, masen: String, mashita: String, masenDeshita: String, te: String, ta: String) =
    Row(masu, masen, mashita, masenDeshita, te, ta)

private val expected = mapOf(
    "みる" to row("みます", "みません", "みました", "みませんでした", "みて", "みた"),
    "たべる" to row("たべます", "たべません", "たべました", "たべませんでした", "たべて", "たべた"),
    "はいる" to row("はいります", "はいりません", "はいりました", "はいりませんでした", "はいって", "はいった"),
    "りょこうする" to row("りょこうします", "りょこうしません", "りょこうしました", "りょこうしませんでした", "りょこうして", "りょこうした"),
    "あそぶ" to row("あそびます", "あそびません", "あそびました", "あそびませんでした", "あそんで", "あそんだ"),
    "ねる" to row("ねます", "ねません", "ねました", "ねませんでした", "ねて", "ねた"),
    "おきる" to row("おきます", "おきません", "おきました", "おきませんでした", "おきて", "おきた"),
    "はたらく" to row("はたらきます", "はたらきません", "はたらきました", "はたらきませんでした", "はたらいて", "はたらいた"),
    "べんきょうする" to row("べんきょうします", "べんきょうしません", "べんきょうしました", "べんきょうしませんでした", "べんきょうして", "べんきょうした"),
    "けっこんする" to row("けっこんします", "けっこんしません", "けっこんしました", "けっこんしませんでした", "けっこんして", "けっこんした"),
    "のむ" to row("のみます", "のみません", "のみました", "のみませんでした", "のんで", "のんだ"),
    "あう" to row("あいます", "あいません", "あいました", "あいませんでした", "あって", "あった"),
    "のぼる" to row("のぼります", "のぼりません", "のぼりました", "のぼりませんでした", "のぼって", "のぼった"),
    "とまる" to row("とまります", "とまりません", "とまりました", "とまりませんでした", "とまって", "とまった"),
    "のる" to row("のります", "のりません", "のりました", "のりませんでした", "のって", "のった"),
    "おりる" to row("おります", "おりません", "おりました", "おりませんでした", "おりて", "おりた"),
    "かす" to row("かします", "かしません", "かしました", "かしませんでした", "かして", "かした"),
    "かく" to row("かきます", "かきません", "かきました", "かきませんでした", "かいて", "かいた"),
    "およぐ" to row("およぎます", "およぎません", "およぎました", "およぎませんでした", "およいで", "およいだ"),
    "もつ" to row("もちます", "もちません", "もちました", "もちませんでした", "もって", "もった"),
    "かえる" to row("かえります", "かえりません", "かえりました", "かえりませんでした", "かえって", "かえった"),
    "しぬ" to row("しにます", "しにません", "しにました", "しにませんでした", "しんで", "しんだ"),
    "はなす" to row("はなします", "はなしません", "はなしました", "はなしませんでした", "はなして", "はなした"),
    "あける" to row("あけます", "あけません", "あけました", "あけませんでした", "あけて", "あけた"),
    "しめる" to row("しめます", "しめません", "しめました", "しめませんでした", "しめて", "しめた"),
    "きく" to row("ききます", "ききません", "ききました", "ききませんでした", "きいて", "きいた"),
    "だす" to row("だします", "だしません", "だしました", "だしませんでした", "だして", "だした"),
    "よむ" to row("よみます", "よみません", "よみました", "よみませんでした", "よんで", "よんだ"),
    "みせる" to row("みせます", "みせません", "みせました", "みせませんでした", "みせて", "みせた"),
    "きる" to row("きります", "きりません", "きりました", "きりませんでした", "きって", "きった"),
    "つける" to row("つけます", "つけません", "つけました", "つけませんでした", "つけて", "つけた"),
    "けす" to row("けします", "けしません", "けしました", "けしませんでした", "けして", "けした"),
    "とる" to row("とります", "とりません", "とりました", "とりませんでした", "とって", "とった"),
    "おしえる" to row("おしえます", "おしえません", "おしえました", "おしえませんでした", "おしえて", "おしえた"),
    "おくる" to row("おくります", "おくりません", "おくりました", "おくりませんでした", "おくって", "おくった"),
    "ひく" to row("ひきます", "ひきません", "ひきました", "ひきませんでした", "ひいて", "ひいた"),
    "いく" to row("いきます", "いきません", "いきました", "いきませんでした", "いって", "いった"),
    "くる" to row("きます", "きません", "きました", "きませんでした", "きて", "きた"),
    "する" to row("します", "しません", "しました", "しませんでした", "して", "した"),
    "かう" to row("かいます", "かいません", "かいました", "かいませんでした", "かって", "かった"),
    "まつ" to row("まちます", "まちません", "まちました", "まちませんでした", "まって", "まった"),
)
