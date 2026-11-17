package dev.ericallerton.yoruben.ui.practice

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericallerton.yoruben.YorubenApp
import dev.ericallerton.yoruben.core.AnswerCheck
import dev.ericallerton.yoruben.core.AttemptRecord
import dev.ericallerton.yoruben.core.QuizFactory
import dev.ericallerton.yoruben.core.QuizItem
import dev.ericallerton.yoruben.core.Rewards
import dev.ericallerton.yoruben.core.quizMode
import dev.ericallerton.yoruben.data.db.QuizResultEntity
import dev.ericallerton.yoruben.ui.FuriganaText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUi(
    val items: List<QuizItem> = emptyList(),
    val index: Int = 0,
    val fill: String = "",
    val picked: String? = null,
    val revealed: Boolean = false,
    val correct: Boolean? = null,
    val hint: String? = null,
    val correctCount: Int = 0,
    val done: Boolean = false,
)

class PracticeViewModel(app: Application) : AndroidViewModel(app) {
    private val curriculum = (app as YorubenApp).curriculum
    private val dao = (app as YorubenApp).database.quizDao()
    private val _ui = MutableStateFlow(QuizUi())
    val ui = _ui.asStateFlow()
    private var startedMode: String? = null

    fun start(modeRoute: String, count: Int = 8) {
        if (startedMode == modeRoute && _ui.value.items.isNotEmpty()) return
        startedMode = modeRoute
        viewModelScope.launch {
            val history = dao.recent(40).map { AttemptRecord(it.itemId, it.correct) }
            val items = QuizFactory.session(
                curriculum,
                quizMode(modeRoute),
                count,
                history,
                kotlin.random.Random(System.currentTimeMillis()),
            )
            _ui.value = QuizUi(items = items)
        }
    }

    fun pick(id: String) {
        val state = _ui.value
        if (state.revealed) return
        _ui.update { it.copy(picked = id) }
    }

    fun setFill(value: String) {
        if (_ui.value.revealed) return
        _ui.update { it.copy(fill = value) }
    }

    fun submit() {
        val state = _ui.value
        val item = state.items.getOrNull(state.index) ?: return
        if (state.revealed) return
        val correct = if (item.choices.isEmpty()) {
            AnswerCheck.matches(item.accept, state.fill)
        } else {
            state.picked != null && state.picked == item.answerId
        }
        val given = if (item.choices.isEmpty()) state.fill else state.picked.orEmpty()
        val hint = if (!correct && item.choices.isEmpty()) {
            AnswerCheck.mistake(item.mistakes, state.fill)?.hintEn
        } else {
            null
        }
        _ui.update {
            it.copy(
                revealed = true,
                correct = correct,
                hint = hint,
                correctCount = it.correctCount + if (correct) 1 else 0,
            )
        }
        viewModelScope.launch {
            dao.insert(
                QuizResultEntity(
                    itemId = item.id,
                    kind = item.kind,
                    correct = correct,
                    lessonDate = item.lessonDate,
                    weakSpotId = item.weakSpotId,
                    prompt = item.promptRuby,
                    given = given,
                    expected = item.answerId ?: item.accept.firstOrNull().orEmpty(),
                    atEpochMs = System.currentTimeMillis(),
                ),
            )
            getApplication<YorubenApp>().game.grant(
                if (correct) Rewards.DRILL_CORRECT else 0,
                if (correct) Rewards.XP_CORRECT else Rewards.XP_WRONG,
            )
        }
    }

    fun next() {
        val state = _ui.value
        if (!state.revealed) return
        if (state.index >= state.items.lastIndex) {
            _ui.update { it.copy(done = true) }
        } else {
            _ui.update {
                it.copy(index = it.index + 1, fill = "", picked = null, revealed = false, correct = null, hint = null)
            }
        }
    }
}

@Composable
fun PracticeHome(onStart: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("れんしゅう", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "にほんごが さき。ひま と たいくつ、ぶっか と ねだん、ちいさい かな、おもしろかった を すこし おおめに だします。",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        ModeCard("こんやの ドリル", "ことばと ぶんぽう、にがてを まぜる", "mix", onStart)
        ModeCard("ことば", "べんきょうちゅうの たんご", "words", onStart)
        ModeCard("ぶんぽう", "クラスの かたち", "grammar", onStart)
        ModeCard("にがて", "まちがえやすい ところだけ", "weak", onStart)
    }
}

@Composable
private fun ModeCard(title: String, body: String, mode: String, onStart: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onStart(mode) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuizScreen(mode: String, vm: PracticeViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(mode) { vm.start(mode) }
    val item = ui.items.getOrNull(ui.index)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        when {
            ui.items.isEmpty() -> Text("ドリルを よういしています。")
            ui.done -> DoneCard(ui.correctCount, ui.items.size)
            item == null -> Text("もんだいが ありません。")
            else -> Question(ui, item, vm)
        }
    }
}

@Composable
private fun Question(ui: QuizUi, item: QuizItem, vm: PracticeViewModel) {
    val progress = (ui.index + if (ui.revealed) 1 else 0).toFloat() / ui.items.size.toFloat()
    Text("${ui.index + 1} / ${ui.items.size}", style = MaterialTheme.typography.labelLarge)
    if (item.weakSpotId != null) {
        Text("にがて", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    FuriganaText(item.promptRuby, MaterialTheme.typography.headlineSmall)
    if (item.promptEn.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(item.promptEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(16.dp))
    if (item.choices.isEmpty()) {
        OutlinedTextField(
            value = ui.fill,
            onValueChange = vm::setFill,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ひらがなで") },
            enabled = !ui.revealed,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.submit() }),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.choices.forEach { choice ->
                val selected = ui.picked == choice.id
                val showRight = ui.revealed && choice.id == item.answerId
                val showWrong = ui.revealed && selected && !showRight
                val color = when {
                    showRight -> MaterialTheme.colorScheme.secondaryContainer
                    showWrong -> MaterialTheme.colorScheme.errorContainer
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !ui.revealed) { vm.pick(choice.id) },
                    colors = CardDefaults.cardColors(containerColor = color),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        FuriganaText(choice.ruby, MaterialTheme.typography.titleMedium)
                        if (choice.en.isNotBlank()) {
                            Text(choice.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    if (!ui.revealed) {
        Button(
            onClick = vm::submit,
            enabled = if (item.choices.isEmpty()) ui.fill.isNotBlank() else ui.picked != null,
        ) { Text("こたえあわせ") }
    } else {
        Text(
            if (ui.correct == true) "いいね。" else "つぎは いける。",
            style = MaterialTheme.typography.titleMedium,
            color = if (ui.correct == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        )
        ui.hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(6.dp))
        FuriganaText(item.explainRuby, MaterialTheme.typography.bodyLarge)
        if (item.explainEn.isNotBlank()) {
            Text(item.explainEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = vm::next) { Text(if (ui.index == ui.items.lastIndex) "けっか" else "つぎ") }
    }
}

@Composable
private fun DoneCard(correct: Int, total: Int) {
    Column {
        Text("おつかれさま。", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("$correct / $total", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("この けっかは、この スマホに のこしてあります。", style = MaterialTheme.typography.bodyLarge)
    }
}
