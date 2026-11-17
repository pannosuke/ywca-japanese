package dev.ericallerton.yoruben.core

import kotlin.random.Random

enum class RoomKind { BATTLE, ELITE, TREASURE, SHOP, SHRINE, MYSTERY, BOSS }

enum class Relic { LANTERN, BOTTLE, CHARM, GEM_BAG, SHIELD }

enum class RunOutcome { PLAY, DEAD, VICTORY }

data class RoomOffer(val floor: Int, val index: Int, val kind: RoomKind)

data class MysteryLine(val ruby: String, val en: String)

data class Choice(val id: String, val ruby: String)

data class Question(
    val verb: Verb,
    val phaseGroup: Boolean,
    val form: VerbForm,
    val promptRuby: String,
    val promptEn: String,
    val choices: List<Choice>,
    val answerId: String,
    val ruleId: String,
    val explainRuby: String,
    val explainEn: String,
    val revealGroup: Boolean,
)

data class Miss(val verbId: String, val ruleId: String)

data class RunState(
    val seed: Long,
    val floor: Int,
    val hearts: Int,
    val maxHearts: Int,
    val relics: Set<Relic>,
    val gemsEarned: Int,
    val misses: List<Miss>,
    val offers: List<RoomOffer>,
    val room: RoomKind?,
    val queue: List<Question>,
    val current: Question?,
    val revealed: Boolean,
    val lastCorrect: Boolean?,
    val pendingRelic: Relic?,
    val shop: List<Pair<Relic, Int>>,
    val mystery: MysteryLine?,
    val noteRuby: String,
    val outcome: RunOutcome,
    val bottleLeft: Boolean,
    val shieldLeft: Boolean,
)

object RunMap {
    const val DEMO_SEED = 17L
    private val CHOICES = listOf(
        RoomKind.BATTLE,
        RoomKind.ELITE,
        RoomKind.TREASURE,
        RoomKind.SHOP,
        RoomKind.SHRINE,
        RoomKind.MYSTERY,
    )

    fun offers(seed: Long, floor: Int): List<RoomOffer> {
        if (floor % 3 == 0) return listOf(RoomOffer(floor, 0, RoomKind.BOSS))
        val random = Random(seed xor floor.toLong().shl(32).or(floor * 0x9E3779B9L))
        val count = 2 + random.nextInt(2)
        return CHOICES.shuffled(random).take(count).mapIndexed { index, kind ->
            RoomOffer(floor, index, kind)
        }
    }
}

object RunEngine {
    const val MAX_FLOOR = 9

    fun bossSeconds(relics: Set<Relic>): Int = if (Relic.CHARM in relics) 35 else 20

    fun start(seed: Long, startRelics: Set<Relic>, hearts: Int = 3): RunState {
        return fresh(seed, 1, hearts, hearts, startRelics, 0, emptyList(), startRelics.contains(Relic.BOTTLE), startRelics.contains(Relic.SHIELD))
    }

    fun choose(state: RunState, index: Int, config: RunConfig): RunState {
        if (state.outcome != RunOutcome.PLAY || state.current != null) return state
        val offer = state.offers.getOrNull(index) ?: return state
        val random = Random(state.seed xor (state.floor * 31L + index))
        return when (offer.kind) {
            RoomKind.SHRINE -> state.copy(
                room = offer.kind,
                hearts = (state.hearts + 1).coerceAtMost(state.maxHearts),
                noteRuby = "いのちが ひとつ もどった。",
                revealed = true,
            )
            RoomKind.TREASURE -> openTreasure(state, offer.kind, random)
            RoomKind.SHOP -> state.copy(
                room = offer.kind,
                shop = shopStock(state, random),
                noteRuby = "どうぐを かえる。",
            )
            RoomKind.MYSTERY -> {
                val line = config.mysteries.getOrNull(random.nextInt(config.mysteries.size.coerceAtLeast(1)))
                    ?: MysteryLine("ともしびが ちかくで ひかっている。", "A small light flickers nearby.")
                state.copy(room = offer.kind, mystery = line, noteRuby = line.ruby)
            }
            RoomKind.BATTLE -> enterQuestions(state, offer.kind, battleQueue(config, random, state.relics))
            RoomKind.ELITE -> enterQuestions(state, offer.kind, formQueue(config, random, state.relics, 2))
            RoomKind.BOSS -> enterQuestions(state, offer.kind, formQueue(config, random, state.relics, 4))
        }
    }

    fun answer(state: RunState, choiceId: String): RunState {
        val question = state.current ?: return state
        if (state.revealed || state.outcome != RunOutcome.PLAY) return state
        val correct = choiceId == question.answerId
        var next = state.copy(revealed = true, lastCorrect = correct)
        if (correct) {
            val gain = 3 * if (Relic.GEM_BAG in state.relics) 2 else 1
            next = next.copy(gemsEarned = next.gemsEarned + gain)
        } else {
            next = hurt(next).copy(misses = next.misses + Miss(question.verb.id, question.ruleId))
        }
        return if (next.hearts <= 0) next.copy(outcome = RunOutcome.DEAD) else next
    }

    fun expire(state: RunState): RunState {
        val question = state.current ?: return state
        if (state.revealed) return state
        val hurt = hurt(state.copy(revealed = true, lastCorrect = false))
            .copy(misses = state.misses + Miss(question.verb.id, question.ruleId))
        return if (hurt.hearts <= 0) hurt.copy(outcome = RunOutcome.DEAD) else hurt
    }

    fun buyRelic(state: RunState, relic: Relic, gems: Int): Pair<RunState, Int> {
        val price = state.shop.firstOrNull { it.first == relic }?.second ?: return state to 0
        if (gems < price || relic in state.relics) return state to 0
        return state.copy(
            relics = state.relics + relic,
            shop = state.shop.filterNot { it.first == relic },
            bottleLeft = state.bottleLeft || relic == Relic.BOTTLE,
            shieldLeft = state.shieldLeft || relic == Relic.SHIELD,
        ) to price
    }

    fun advance(state: RunState, config: RunConfig): RunState {
        if (state.outcome != RunOutcome.PLAY) return state
        if (state.current != null && !state.revealed) return state
        if (state.queue.isNotEmpty()) {
            return state.copy(current = state.queue.first(), queue = state.queue.drop(1), revealed = false, lastCorrect = null)
        }
        val nextFloor = state.floor + 1
        if (state.room == RoomKind.BOSS && state.floor >= MAX_FLOOR) {
            return state.copy(outcome = RunOutcome.VICTORY, current = null, noteRuby = "洞[ほら]の おくを ぬけた。")
        }
        return fresh(
            state.seed,
            nextFloor,
            state.hearts,
            state.maxHearts,
            state.relics,
            state.gemsEarned,
            state.misses,
            state.bottleLeft,
            state.shieldLeft,
        )
    }

    fun practice(mode: String, forms: Set<VerbForm>, history: List<AnswerRecord>, now: Long): List<Question> {
        val random = Random(1)
        val config = RunConfig(
            verbs = when {
                mode == "group1" -> VerbCatalog.forPractice(VerbGroup.GODAN)
                mode == "group2" -> VerbCatalog.forPractice(VerbGroup.ICHIDAN)
                mode == "group3" -> VerbCatalog.forPractice(VerbGroup.IRREGULAR)
                mode.startsWith("region-") -> VerbCatalog.all.filter { it.lessonDate == mode.removePrefix("region-") }.ifEmpty { VerbCatalog.all }
                else -> VerbCatalog.all
            },
            forms = if (mode == "preview") setOf(VerbForm.TA) else forms.ifEmpty { setOf(VerbForm.MASU) },
            history = history,
            mysteries = emptyList(),
            now = now,
        )
        if (mode == "te") {
            val verb = VerbCatalog.byId("kaku")
            return listOf(groupQuestion(verb), formQuestion(verb, VerbForm.TE, random, emptySet())) +
                formQueue(config, random, emptySet(), 3)
        }
        if (mode == "boss") return formQueue(config, random, emptySet(), 4)
        return List(4) {
            val form = config.forms.random(random)
            val verb = pickVerb(config, random, form)
            listOf(groupQuestion(verb), formQuestion(verb, form, random, emptySet()))
        }.flatten()
    }

    fun missCounts(state: RunState): List<Pair<String, Int>> {
        return state.misses.groupingBy { it.ruleId }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    private fun fresh(
        seed: Long,
        floor: Int,
        hearts: Int,
        maxHearts: Int,
        relics: Set<Relic>,
        gems: Int,
        misses: List<Miss>,
        bottle: Boolean,
        shield: Boolean,
    ): RunState {
        return RunState(
            seed = seed,
            floor = floor,
            hearts = hearts,
            maxHearts = maxHearts,
            relics = relics,
            gemsEarned = gems,
            misses = misses,
            offers = RunMap.offers(seed, floor),
            room = null,
            queue = emptyList(),
            current = null,
            revealed = false,
            lastCorrect = null,
            pendingRelic = null,
            shop = emptyList(),
            mystery = null,
            noteRuby = "",
            outcome = RunOutcome.PLAY,
            bottleLeft = bottle,
            shieldLeft = shield,
        )
    }

    private fun enterQuestions(state: RunState, kind: RoomKind, questions: List<Question>): RunState {
        return state.copy(
            room = kind,
            current = questions.firstOrNull(),
            queue = questions.drop(1),
            revealed = false,
            pendingRelic = null,
            mystery = null,
        )
    }

    private fun openTreasure(state: RunState, kind: RoomKind, random: Random): RunState {
        val pool = Relic.entries.filter { it !in state.relics }
        if (pool.isEmpty()) {
            val gain = 6 * if (Relic.GEM_BAG in state.relics) 2 else 1
            return state.copy(room = kind, gemsEarned = state.gemsEarned + gain, noteRuby = "からっぽの はこ。ほたる玉[だま]が のこっていた。", revealed = true)
        }
        val relic = pool[random.nextInt(pool.size)]
        return state.copy(
            room = kind,
            relics = state.relics + relic,
            pendingRelic = relic,
            bottleLeft = state.bottleLeft || relic == Relic.BOTTLE,
            shieldLeft = state.shieldLeft || relic == Relic.SHIELD,
            noteRuby = relicName(relic),
            revealed = true,
        )
    }

    private fun shopStock(state: RunState, random: Random): List<Pair<Relic, Int>> {
        val pool = Relic.entries.filter { it !in state.relics }.shuffled(random)
        return pool.take(2).mapIndexed { index, relic -> relic to (8 + index * 4) }
    }

    private fun battleQueue(config: RunConfig, random: Random, relics: Set<Relic>): List<Question> {
        val verb = pickVerb(config, random, config.forms.first())
        val form = pickForm(config, verb, random)
        return listOf(groupQuestion(verb), formQuestion(verb, form, random, relics))
    }

    private fun formQueue(config: RunConfig, random: Random, relics: Set<Relic>, count: Int): List<Question> {
        return List(count) {
            val form = config.forms.random(random)
            val verb = pickVerb(config, random, form)
            formQuestion(verb, form, random, relics)
        }
    }

    private fun pickVerb(config: RunConfig, random: Random, form: VerbForm): Verb {
        val pool = config.verbs.ifEmpty { VerbCatalog.all }
        val weights = pool.map { Weighting.weight(it, Conjugator.ruleId(it, form), config.history, config.now) }
        return Weighting.pick(pool, weights, random)
    }

    private fun pickForm(config: RunConfig, verb: Verb, random: Random): VerbForm {
        val forms = config.forms.toList()
        val weights = forms.map { Weighting.weight(verb, Conjugator.ruleId(verb, it), config.history, config.now) }
        return Weighting.pick(forms, weights, random)
    }

    private fun groupQuestion(verb: Verb): Question {
        val (explainRuby, explainEn) = Conjugator.groupExplain(verb)
        return Question(
            verb = verb,
            phaseGroup = true,
            form = VerbForm.MASU,
            promptRuby = "${verb.dictRuby} は どの グループ？",
            promptEn = "Which group is this verb?",
            choices = listOf(
                Choice("1", "グループ1 うだん"),
                Choice("2", "グループ2 るを とる"),
                Choice("3", "グループ3 する・来[く]る"),
            ),
            answerId = when (verb.group) {
                VerbGroup.GODAN -> "1"
                VerbGroup.ICHIDAN -> "2"
                VerbGroup.IRREGULAR -> "3"
            },
            ruleId = Conjugator.groupId(verb),
            explainRuby = explainRuby,
            explainEn = explainEn,
            revealGroup = false,
        )
    }

    private fun formQuestion(verb: Verb, form: VerbForm, random: Random, relics: Set<Relic>): Question {
        val done = Conjugator.conjugate(verb, form)
        val source = when (form) {
            VerbForm.MASU, VerbForm.TE, VerbForm.TA -> verb.dictRuby
            else -> Conjugator.conjugate(verb, VerbForm.MASU).ruby
        }
        val ask = when (form) {
            VerbForm.MASU -> "ます"
            VerbForm.MASEN -> "ません"
            VerbForm.MASHITA -> "ました"
            VerbForm.MASEN_DESHITA -> "ませんでした"
            VerbForm.TE -> "て形[けい]"
            VerbForm.TA -> "た形[けい]"
        }
        val choices = Conjugator.choices(verb, form, random).map { kana ->
            Choice(kana, Conjugator.rubyFor(verb, kana))
        }
        return Question(
            verb = verb,
            phaseGroup = false,
            form = form,
            promptRuby = "$source の $ask は？",
            promptEn = verb.glossEn,
            choices = choices,
            answerId = done.kana,
            ruleId = done.ruleId,
            explainRuby = done.explainRuby,
            explainEn = done.explainEn,
            revealGroup = Relic.LANTERN in relics,
        )
    }

    private fun hurt(state: RunState): RunState {
        if (state.shieldLeft && Relic.SHIELD in state.relics) {
            return state.copy(shieldLeft = false, noteRuby = "れんの 盾[たて]が いちど だけ ふせいだ。")
        }
        val left = state.hearts - 1
        if (left <= 0 && state.bottleLeft && Relic.BOTTLE in state.relics) {
            return state.copy(hearts = 1, bottleLeft = false, noteRuby = "いのちの 小瓶[こびん]が ひらいた。")
        }
        return state.copy(hearts = left.coerceAtLeast(0))
    }

    fun relicName(relic: Relic): String = when (relic) {
        Relic.LANTERN -> "ヒントの 灯籠[とうろう]"
        Relic.BOTTLE -> "いのちの 小瓶[こびん]"
        Relic.CHARM -> "時[とき]の 守[まも]り"
        Relic.GEM_BAG -> "ほたる袋[ぶくろ]"
        Relic.SHIELD -> "れんの 盾[たて]"
    }

    fun relicEn(relic: Relic): String = when (relic) {
        Relic.LANTERN -> "Shows the verb group."
        Relic.BOTTLE -> "Once, when the last heart would break, it leaves one."
        Relic.CHARM -> "More seconds on a boss round."
        Relic.GEM_BAG -> "Gem rewards are doubled."
        Relic.SHIELD -> "Blocks one lost heart."
    }
}

data class RunConfig(
    val verbs: List<Verb>,
    val forms: Set<VerbForm>,
    val history: List<AnswerRecord>,
    val mysteries: List<MysteryLine>,
    val now: Long,
)
