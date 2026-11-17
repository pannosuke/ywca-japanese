package dev.ericallerton.yoruben.ui.adventure

import android.app.Application
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericallerton.yoruben.YorubenApp
import dev.ericallerton.yoruben.core.BuyResult
import dev.ericallerton.yoruben.core.Cosmetic
import dev.ericallerton.yoruben.core.CosmeticSlot
import dev.ericallerton.yoruben.core.Cosmetics
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.core.FormGate
import dev.ericallerton.yoruben.core.Ledger
import dev.ericallerton.yoruben.core.Levels
import dev.ericallerton.yoruben.core.Miss
import dev.ericallerton.yoruben.core.Question
import dev.ericallerton.yoruben.core.Relic
import dev.ericallerton.yoruben.core.Rewards
import dev.ericallerton.yoruben.core.RoomKind
import dev.ericallerton.yoruben.core.RunConfig
import dev.ericallerton.yoruben.core.RunEngine
import dev.ericallerton.yoruben.core.RunMap
import dev.ericallerton.yoruben.core.RunOutcome
import dev.ericallerton.yoruben.core.RunState
import dev.ericallerton.yoruben.core.Songs
import dev.ericallerton.yoruben.core.VerbCatalog
import dev.ericallerton.yoruben.core.VerbForm
import dev.ericallerton.yoruben.core.dateRuby
import dev.ericallerton.yoruben.data.GameStore
import dev.ericallerton.yoruben.ui.FuriganaText
import dev.ericallerton.yoruben.ui.fx.Chimes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private fun ruleRuby(ruleId: String): String = when (ruleId) {
    "te-ite" -> "「く」は「いて」"
    "te-iku" -> "行[い]く だけ「って」"
    "te-ide" -> "「ぐ」は「いで」"
    "te-shite" -> "「す」は「して」"
    "te-tte" -> "「う・つ・る」は「って」"
    "te-nde" -> "「む・ぶ・ぬ」は「んで」"
    "te-ichidan" -> "グループ2の て形[けい]"
    "te-suru" -> "する は「して」"
    "te-kuru" -> "来[く]る は「来[き]て」"
    "group-1" -> "グループ1"
    "group-2" -> "グループ2"
    "group-3" -> "グループ3"
    "masu-godan" -> "グループ1の ます"
    "masu-ichidan" -> "グループ2の ます"
    "masu-suru" -> "する の ます"
    "masu-kuru" -> "来[く]る の ます"
    "neg-polite" -> "ません"
    "past-polite" -> "ました"
    "past-neg-polite" -> "ませんでした"
    else -> ruleId
}

private fun slotLabel(slot: CosmeticSlot): String = when (slot) {
    CosmeticSlot.TUNIC -> "ころも"
    CosmeticSlot.SHIELD -> "たて"
    CosmeticSlot.SWORD -> "つるぎ"
    CosmeticSlot.COMPANION -> "ともしび"
    CosmeticSlot.THEME -> "いろ"
    CosmeticSlot.FRAME -> "わく"
    CosmeticSlot.TITLE -> "なまえ"
    CosmeticSlot.DECOR -> "かざり"
    CosmeticSlot.JINGLE -> "ねいろ"
}

private fun roomRuby(kind: RoomKind): String = when (kind) {
    RoomKind.BATTLE -> "たたかい"
    RoomKind.ELITE -> "つよい へや"
    RoomKind.TREASURE -> "たから"
    RoomKind.SHOP -> "みせ"
    RoomKind.SHRINE -> "ほこら"
    RoomKind.MYSTERY -> "ふしぎ"
    RoomKind.BOSS -> "ボス"
}

private fun unlockedDates(dates: List<String>, cleared: Set<String>): Set<String> {
    if (dates.isEmpty()) return emptySet()
    val open = mutableSetOf(dates.first())
    for (index in 1 until dates.size) {
        if (dates[index - 1] in cleared) open += dates[index]
    }
    return open
}

@Composable
private fun StatRow(ledger: Ledger, hearts: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        HeartRow(hearts ?: 3, 3)
        Text("Lv ${Levels.level(ledger.xp)}", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            GemMark(MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(4.dp))
            Text("${ledger.gems}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AdventureStats(vm: MapViewModel = viewModel()) {
    val ledger by vm.ledger.collectAsState(Cosmetics.starter())
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FuriganaText("灯森[あかりのもり]", MaterialTheme.typography.titleMedium)
            Text("Lv ${Levels.level(ledger.xp)} · れんぞく ${ledger.streak} にち · ほたる ${ledger.gems}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MapScreen(
    curriculum: Curriculum,
    onVillage: () -> Unit,
    onCave: () -> Unit,
    onSongs: () -> Unit,
    onRegion: (String) -> Unit,
    vm: MapViewModel = viewModel(),
) {
    val ledger by vm.ledger.collectAsState(Cosmetics.starter())
    val cleared by vm.cleared.collectAsState(emptyList())
    val dates = curriculum.lessons.map { it.date }.distinct().sorted()
    val open = unlockedDates(dates, cleared.toSet())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GroveBanner(Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(8.dp))
            FuriganaText("灯森[あかりのもり]", MaterialTheme.typography.headlineMedium)
            val title = Cosmetics.byId(ledger.equipped(CosmeticSlot.TITLE))
            if (title != null) FuriganaText(title.nameRuby, MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text("れんぞく ${ledger.streak} にち", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            StatRow(ledger)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVillage) { Text("さと") }
                Button(onClick = onCave) { Text("ほら") }
                OutlinedButton(onClick = onSongs) { Text("ふえ") }
            }
        }
        items(curriculum.lessons.sortedBy { it.date }, key = { it.id }) { lesson ->
            val unlocked = lesson.date in open
            Card(
                modifier = Modifier.fillMaxWidth().then(
                    if (unlocked) Modifier.clickable { onRegion(lesson.date) } else Modifier,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (lesson.date in cleared) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    FuriganaText(dateRuby(lesson.date, lesson.weekday), MaterialTheme.typography.titleMedium)
                    FuriganaText(lesson.topicRuby, MaterialTheme.typography.bodyLarge)
                    Text(
                        if (!unlocked) "まだ とざしている" else if (lesson.date in cleared) "あるいた" else "はいれる",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

class MapViewModel(app: Application) : AndroidViewModel(app) {
    private val game = (app as YorubenApp).game
    val ledger = game.observe()
    val cleared = game.observeClears()

    init {
        viewModelScope.launch { game.ensure() }
    }
}

@Composable
fun VillageScreen(onShop: () -> Unit, onWardrobe: () -> Unit, onRun: () -> Unit, vm: MapViewModel = viewModel()) {
    val ledger by vm.ledger.collectAsState(Cosmetics.starter())
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FuriganaText("灯[あかり]の 里[さと]", MaterialTheme.typography.headlineMedium)
        HeroPreview(ledger, Modifier.fillMaxWidth().height(180.dp))
        val decor = Cosmetics.byId(ledger.equipped(CosmeticSlot.DECOR))
        if (decor != null) FuriganaText(decor.nameRuby, MaterialTheme.typography.bodyLarge)
        StatRow(ledger)
        Button(onClick = onShop, modifier = Modifier.fillMaxWidth()) { Text("みせ") }
        OutlinedButton(onClick = onWardrobe, modifier = Modifier.fillMaxWidth()) { Text("そうび") }
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) { Text("ぼうけんに でる") }
        Text("ぼうけんの そうびは みための ため。べんきょうは いつでも できる。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CaveScreen(onDrill: (String) -> Unit, onRun: () -> Unit, onSongs: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FuriganaText("活用[かつよう]の 洞[ほら]", MaterialTheme.typography.headlineMedium)
        Text("まず グループを みて、それから かたを かえる。", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { onDrill("group1") }, modifier = Modifier.fillMaxWidth()) { Text("グループ1 うだん") }
        Button(onClick = { onDrill("group2") }, modifier = Modifier.fillMaxWidth()) { Text("グループ2 るを とる") }
        Button(onClick = { onDrill("group3") }, modifier = Modifier.fillMaxWidth()) { Text("グループ3 する・くる") }
        OutlinedButton(onClick = { onDrill("te") }, modifier = Modifier.fillMaxWidth()) { Text("てけいの へや") }
        Button(onClick = { onDrill("boss") }, modifier = Modifier.fillMaxWidth()) { Text("ボス") }
        OutlinedButton(onClick = { onDrill("preview") }, modifier = Modifier.fillMaxWidth()) { Text("たけいの プレビュー") }
        OutlinedButton(onClick = onRun, modifier = Modifier.fillMaxWidth()) { Text("ラン") }
        OutlinedButton(onClick = onSongs, modifier = Modifier.fillMaxWidth()) { Text("こもれぶえ") }
    }
}

data class DrillUi(
    val hearts: Int = 3,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val correct: Boolean? = null,
    val done: Boolean = false,
    val seconds: Int = 20,
    val boss: Boolean = false,
    val misses: List<Miss> = emptyList(),
    val ledger: Ledger = Cosmetics.starter(),
)

class DrillViewModel(app: Application) : AndroidViewModel(app) {
    private val game = (app as YorubenApp).game
    private val lessons = (app as YorubenApp).curriculum.lessons
    private val chimes = Chimes(app)
    private val _ui = MutableStateFlow(DrillUi())
    val ui = _ui.asStateFlow()
    private var started = ""

    fun start(mode: String) {
        if (started == mode && _ui.value.questions.isNotEmpty()) return
        started = mode
        viewModelScope.launch {
            game.ensure()
            val forms = FormGate.covered(lessons, GameStore.tokyoDay())
            val questions = RunEngine.practice(mode, forms, game.history(), System.currentTimeMillis())
            _ui.value = DrillUi(questions = questions, boss = mode == "boss", ledger = game.ledger())
        }
    }

    fun pick(id: String) {
        val state = _ui.value
        val question = state.questions.getOrNull(state.index) ?: return
        if (state.revealed || state.done) return
        val correct = id == question.answerId
        chimes.answer(correct)
        val hearts = if (correct) state.hearts else state.hearts - 1
        val misses = if (correct) state.misses else state.misses + Miss(question.verb.id, question.ruleId)
        _ui.value = state.copy(revealed = true, correct = correct, hearts = hearts, misses = misses, done = hearts <= 0)
        viewModelScope.launch {
            game.noteDrill(question.verb.id, question.ruleId, correct, if (correct) Rewards.DRILL_CORRECT else 0, if (correct) Rewards.XP_CORRECT else Rewards.XP_WRONG)
            _ui.value = _ui.value.copy(ledger = game.ledger())
        }
    }

    fun advance(regionDate: String?) {
        val state = _ui.value
        if (!state.revealed || state.done) return
        if (state.index >= state.questions.lastIndex) {
            _ui.value = state.copy(done = true)
            if (regionDate != null && state.hearts > 0) {
                viewModelScope.launch { game.clearRegion(regionDate) }
            }
            return
        }
        _ui.value = state.copy(index = state.index + 1, revealed = false, correct = null, seconds = 20)
    }

    fun expire() {
        val state = _ui.value
        if (state.revealed || state.done) return
        val question = state.questions.getOrNull(state.index) ?: return
        chimes.answer(false)
        val hearts = state.hearts - 1
        _ui.value = state.copy(
            revealed = true,
            correct = false,
            hearts = hearts,
            seconds = 0,
            misses = state.misses + Miss(question.verb.id, question.ruleId),
            done = hearts <= 0,
        )
    }

    fun revealed(): Boolean = _ui.value.revealed

    override fun onCleared() {
        chimes.release()
    }
}

@Composable
fun DrillScreen(mode: String, vm: DrillViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val view = LocalView.current
    LaunchedEffect(mode) { vm.start(mode) }
    LaunchedEffect(ui.index, ui.boss) {
        if (!ui.boss) return@LaunchedEffect
        var left = 20
        while (left > 0) {
            delay(1000)
            if (vm.revealed()) return@LaunchedEffect
            left--
        }
        vm.expire()
    }
    LaunchedEffect(ui.correct) {
        if (ui.correct == true) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        if (ui.correct == false) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
    val region = mode.removePrefix("region-").takeIf { mode.startsWith("region-") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow(ui.ledger, ui.hearts)
        if (ui.boss) Text("のこり ${ui.seconds} びょう", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        val question = ui.questions.getOrNull(ui.index)
        if (ui.done && question != null && ui.hearts <= 0) {
            SummaryBlock(ui.misses, won = false)
        } else if (ui.done) {
            Text("ここは ぬけた。", style = MaterialTheme.typography.headlineSmall)
            SummaryBlock(ui.misses, won = true)
        } else if (question != null) {
            QuestionCard(question, ui.revealed, ui.correct, ui.ledger.equipped(CosmeticSlot.FRAME)) { vm.pick(it) }
            if (ui.revealed && ui.hearts > 0) {
                Button(onClick = { vm.advance(region) }, modifier = Modifier.fillMaxWidth()) { Text("つぎへ") }
            }
        }
    }
}

@Composable
private fun QuestionCard(question: Question, revealed: Boolean, correct: Boolean?, frameId: String, onPick: (String) -> Unit) {
    val border = when (frameId) {
        "frame-gold" -> MaterialTheme.colorScheme.secondary
        "frame-stone" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }
    Card(modifier = Modifier.fillMaxWidth().border(2.dp, border, MaterialTheme.shapes.large)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FuriganaText(question.promptRuby, MaterialTheme.typography.headlineSmall)
            Text(question.promptEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (question.revealGroup) {
                val (ruby, _) = dev.ericallerton.yoruben.core.Conjugator.groupExplain(question.verb)
                FuriganaText(ruby, MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            question.choices.forEach { choice ->
                val selectedRight = revealed && choice.id == question.answerId
                OutlinedButton(onClick = { onPick(choice.id) }, modifier = Modifier.fillMaxWidth(), enabled = !revealed) {
                    FuriganaText(
                        choice.ruby,
                        MaterialTheme.typography.titleMedium,
                        color = if (selectedRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (revealed) {
                Text(if (correct == true) "せいかい" else "つぎは こう", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                FuriganaText(question.explainRuby, MaterialTheme.typography.bodyLarge)
                Text(question.explainEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Songs.forRule(question.ruleId)?.let { song ->
                    FuriganaText(song.verseRuby, MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryBlock(misses: List<Miss>, won: Boolean) {
    Text(if (won) "クリア" else "いのちが きれた", style = MaterialTheme.typography.headlineSmall)
    Text("つまずいた ルール", style = MaterialTheme.typography.titleMedium)
    if (misses.isEmpty()) {
        Text("つまずきは なかった。", style = MaterialTheme.typography.bodyMedium)
    }
    misses.groupingBy { it.ruleId }.eachCount().entries.sortedByDescending { it.value }.forEach { (rule, count) ->
        FuriganaText(ruleRuby(rule), MaterialTheme.typography.bodyLarge)
        Text("$count かい", style = MaterialTheme.typography.bodySmall)
    }
}

class RunViewModel(app: Application) : AndroidViewModel(app) {
    private val game = (app as YorubenApp).game
    private val curriculum = (app as YorubenApp).curriculum
    private val chimes = Chimes(app)
    private val _ui = MutableStateFlow<RunState?>(null)
    val ui = _ui.asStateFlow()
    private val _ledger = MutableStateFlow(Cosmetics.starter())
    val ledger = _ledger.asStateFlow()
    private var config = RunConfig(VerbCatalog.all, setOf(VerbForm.MASU), emptyList(), emptyList(), 0L)
    private var started = false

    fun start(demo: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            game.ensure()
            _ledger.value = game.ledger()
            val forms = FormGate.covered(curriculum.lessons, GameStore.tokyoDay())
            config = RunConfig(
                verbs = VerbCatalog.all,
                forms = forms,
                history = game.history(),
                mysteries = curriculum.lessons.map { dev.ericallerton.yoruben.core.MysteryLine(it.topicRuby, it.topicEn) },
                now = System.currentTimeMillis(),
            )
            val seed = if (demo == "play") System.nanoTime() else RunMap.DEMO_SEED
            var state = RunEngine.start(seed, emptySet())
            if (demo == "treasure") {
                val index = state.offers.indexOfFirst { it.kind == RoomKind.TREASURE }.coerceAtLeast(0)
                state = RunEngine.choose(state, index, config)
            }
            if (demo == "dead") state = scriptDefeat(state)
            _ui.value = state
        }
    }

    fun choose(index: Int) {
        val state = _ui.value ?: return
        _ui.value = RunEngine.choose(state, index, config)
    }

    fun pick(id: String) {
        val state = _ui.value ?: return
        val next = RunEngine.answer(state, id)
        chimes.answer(next.lastCorrect == true)
        _ui.value = next
        persist(state, next)
    }

    fun buy(relic: Relic) {
        val state = _ui.value ?: return
        viewModelScope.launch {
            val ledger = game.ledger()
            val (next, cost) = RunEngine.buyRelic(state, relic, ledger.gems)
            if (cost > 0 && game.spend(cost)) {
                _ui.value = next
                _ledger.value = game.ledger()
            }
        }
    }

    fun advance() {
        val state = _ui.value ?: return
        val next = RunEngine.advance(state, config)
        if (next.outcome == RunOutcome.VICTORY) {
            chimes.jingle(_ledger.value.equipped(CosmeticSlot.JINGLE))
            viewModelScope.launch { game.grant(Rewards.BOSS_CLEAR, Rewards.XP_CORRECT * 2) }
        }
        _ui.value = next
    }

    private fun persist(before: RunState, after: RunState) {
        val question = before.current ?: return
        val correct = after.lastCorrect == true
        val gained = (after.gemsEarned - before.gemsEarned).coerceAtLeast(0)
        viewModelScope.launch {
            game.noteDrill(question.verb.id, question.ruleId, correct, gained, if (correct) Rewards.XP_CORRECT else Rewards.XP_WRONG)
            _ledger.value = game.ledger()
        }
    }

    private fun scriptDefeat(start: RunState): RunState {
        var state = start
        repeat(8) {
            if (state.outcome != RunOutcome.PLAY) return state
            if (state.current == null) {
                val index = state.offers.indexOfFirst { it.kind == RoomKind.BATTLE || it.kind == RoomKind.ELITE || it.kind == RoomKind.BOSS }
                if (index < 0) {
                    state = RunEngine.advance(state, config)
                    return@repeat
                }
                state = RunEngine.choose(state, index, config)
            }
            val question = state.current ?: return state
            val wrong = question.choices.first { it.id != question.answerId }.id
            state = RunEngine.answer(state, wrong)
            if (state.outcome == RunOutcome.PLAY) state = RunEngine.advance(state, config)
        }
        return state
    }

    override fun onCleared() {
        chimes.release()
    }
}

@Composable
fun RunScreen(demo: String, vm: RunViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val ledger by vm.ledger.collectAsState()
    val view = LocalView.current
    LaunchedEffect(demo) { vm.start(demo) }
    val run = state
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (run == null) {
            Text("どうくつを つくっている…", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        Text("${run.floor} かい", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        StatRow(ledger, run.hearts)
        when {
            run.outcome != RunOutcome.PLAY -> {
                Text(if (run.outcome == RunOutcome.VICTORY) "ぬけた" else "ランは ここで おわり", style = MaterialTheme.typography.headlineSmall)
                SummaryBlock(run.misses, run.outcome == RunOutcome.VICTORY)
                if (run.outcome == RunOutcome.VICTORY) {
                    Button(onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }) { Text("ファンファーレ") }
                }
            }
            run.pendingRelic != null -> {
                FuriganaText("てに いれた", MaterialTheme.typography.headlineSmall)
                FuriganaText(RunEngine.relicName(run.pendingRelic), MaterialTheme.typography.headlineMedium)
                Text(RunEngine.relicEn(run.pendingRelic), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = vm::advance, modifier = Modifier.fillMaxWidth()) { Text("つぎの かいへ") }
            }
            run.mystery != null -> {
                FuriganaText(run.mystery.ruby, MaterialTheme.typography.headlineSmall)
                Text(run.mystery.en, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = vm::advance, modifier = Modifier.fillMaxWidth()) { Text("つぎへ") }
            }
            run.shop.isNotEmpty() && run.current == null -> {
                Text("この かいの みせ", style = MaterialTheme.typography.titleMedium)
                run.shop.forEach { (relic, price) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            FuriganaText(RunEngine.relicName(relic), MaterialTheme.typography.titleMedium)
                            Text(RunEngine.relicEn(relic), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { vm.buy(relic) }) { Text("$price") }
                        }
                    }
                }
                OutlinedButton(onClick = vm::advance, modifier = Modifier.fillMaxWidth()) { Text("さきへ") }
            }
            run.current != null -> {
                QuestionCard(run.current, run.revealed, run.lastCorrect, ledger.equipped(CosmeticSlot.FRAME), vm::pick)
                if (run.revealed && run.outcome == RunOutcome.PLAY) {
                    Button(onClick = vm::advance, modifier = Modifier.fillMaxWidth()) { Text("つぎへ") }
                }
                if (run.noteRuby.isNotBlank()) FuriganaText(run.noteRuby, MaterialTheme.typography.bodyMedium)
            }
            run.revealed && run.room == RoomKind.SHRINE -> {
                FuriganaText(run.noteRuby.ifBlank { "いのちが もどった。" }, MaterialTheme.typography.headlineSmall)
                Button(onClick = vm::advance, modifier = Modifier.fillMaxWidth()) { Text("つぎの かいへ") }
            }
            else -> {
                Text("つぎの へやを えらぶ", style = MaterialTheme.typography.titleMedium)
                run.offers.forEach { offer ->
                    Card(Modifier.fillMaxWidth().clickable { vm.choose(offer.index) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(roomRuby(offer.kind), style = MaterialTheme.typography.headlineSmall)
                            Text("${offer.floor} かい", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(onWardrobe: () -> Unit, vm: ShopViewModel = viewModel()) {
    val ledger by vm.ledger.collectAsState(Cosmetics.starter())
    val day = GameStore.tokyoEpochDay()
    val deal = Cosmetics.dailyDeal(day)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            FuriganaText("灯[あかり]の みせ", MaterialTheme.typography.headlineMedium)
            StatRow(ledger)
            FuriganaText("おかねは いらない。ほたる玉[だま]だけ。べんきょうは いつでも できる。", MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onWardrobe) { Text("そうび") }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Text("きょうの とく", style = MaterialTheme.typography.labelLarge)
                    FuriganaText(deal.nameRuby, MaterialTheme.typography.titleLarge)
                    Text("${Cosmetics.priceOf(deal, day)}  （もとの ${deal.price}）", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { vm.buy(deal.id) }) { Text("かう") }
                }
            }
        }
        items(Cosmetics.catalog.filter { it.price > 0 }, key = { it.id }) { item ->
            CosmeticRow(item, ledger, Cosmetics.priceOf(item, day), vm::buy)
        }
    }
}

@Composable
private fun CosmeticRow(item: Cosmetic, ledger: Ledger, price: Int, onBuy: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            FuriganaText(item.nameRuby, MaterialTheme.typography.titleMedium)
            Text(item.nameEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.id in ledger.owned) {
                Text("もっている", color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = { onBuy(item.id) }) { Text("$price") }
            }
        }
    }
}

class ShopViewModel(app: Application) : AndroidViewModel(app) {
    private val game = (app as YorubenApp).game
    private val chimes = Chimes(app)
    val ledger = game.observe()

    init {
        viewModelScope.launch { game.ensure() }
    }

    fun buy(id: String) {
        viewModelScope.launch {
            val result = game.buy(id)
            if (result is BuyResult.Ok) chimes.answer(true)
        }
    }

    fun equip(id: String) {
        viewModelScope.launch { game.equip(id) }
    }

    fun play(id: String) {
        chimes.jingle(id)
    }

    override fun onCleared() {
        chimes.release()
    }
}

@Composable
fun WardrobeScreen(vm: ShopViewModel = viewModel()) {
    val ledger by vm.ledger.collectAsState(Cosmetics.starter())
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            FuriganaText("そうび", MaterialTheme.typography.headlineMedium)
            HeroPreview(ledger, Modifier.fillMaxWidth().height(180.dp))
            val title = Cosmetics.byId(ledger.equipped(CosmeticSlot.TITLE))
            if (title != null) FuriganaText(title.nameRuby, MaterialTheme.typography.titleMedium)
        }
        CosmeticSlot.entries.forEach { slot ->
            item {
                Text(slotLabel(slot), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            items(Cosmetics.catalog.filter { it.slot == slot && it.id in ledger.owned }, key = { it.id }) { item ->
                val worn = ledger.equipped(item.slot) == item.id
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        FuriganaText(item.nameRuby, MaterialTheme.typography.titleMedium)
                        if (worn) Text("そうびちゅう", color = MaterialTheme.colorScheme.primary)
                        else OutlinedButton(onClick = { vm.equip(item.id) }) { Text("つける") }
                        if (item.slot == CosmeticSlot.JINGLE) {
                            OutlinedButton(onClick = { vm.play(item.id) }) { Text("きく") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongsScreen(vm: ShopViewModel = viewModel()) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FuriganaText("こもれ笛[ぶえ]", MaterialTheme.typography.headlineMedium) }
        items(Songs.all, key = { it.id }) { song ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FuriganaText(song.titleRuby, MaterialTheme.typography.titleLarge)
                    FuriganaText(song.verseRuby, MaterialTheme.typography.bodyLarge)
                    Text(song.verseEn, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { vm.play(if (song.id == "te-ide") "jingle-deep" else "jingle-soft") }) { Text("ふく") }
                }
            }
        }
    }
}
