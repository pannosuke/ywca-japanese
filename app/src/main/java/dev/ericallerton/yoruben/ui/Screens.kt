@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.ericallerton.yoruben.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HomeUi(val date: String, val sections: List<Pair<String, String>>)

data class ReviewUi(
    val counter: String,
    val word: String,
    val revealed: Boolean,
    val meaning: String,
    val definition: String,
    val sentence: String,
    val sentenceEn: String,
    val empty: String?,
    val offerSaved: Boolean,
    val summary: List<Pair<String, String>>?,
    val note: String,
)

data class QuizUi(
    val setup: Boolean,
    val lessons: List<Pair<String, String>>,
    val lessonId: String?,
    val sources: Set<String>,
    val weakOnly: Boolean,
    val count: Int,
    val counter: String,
    val prompt: String,
    val promptEn: String,
    val choices: List<String>,
    val picked: String,
    val typed: String,
    val verdict: Boolean?,
    val expected: String,
    val reask: Boolean,
    val done: Boolean,
    val summary: String,
    val misses: List<String>,
    val empty: Boolean,
)

data class DrillUi(
    val counter: String,
    val word: String,
    val form: String,
    val prompt: String,
    val typed: String,
    val tiles: List<String>,
    val tileMode: Boolean,
    val verdict: Boolean?,
    val expected: String,
    val rule: String,
    val ruleEn: String,
)

data class StoryItem(val id: String, val title: String, val detail: String)

data class ReadQuestion(val prompt: String, val choices: List<String>, val picked: Int, val correct: Int, val checked: Boolean)

data class ReadUi(val title: String, val level: String, val body: String, val questions: List<ReadQuestion>, val score: String?)

data class StatBlock(val title: String, val lines: List<Pair<String, String>>)

data class StatsUi(val blocks: List<StatBlock>)

data class SheetLine(val prompt: String, val typed: String, val expected: String, val correct: Boolean?)

data class SheetUi(val title: String, val lines: List<SheetLine>, val summary: String, val listing: List<Pair<String, String>>?)

@Composable
fun HomeScreen(ui: HomeUi, onOpen: (Int) -> Unit, onStart: () -> Unit) {
    Frame(bottom = { Thumb("Start review", onStart) }) {
        Text("よるべん", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Quiet(ui.date)
        Spacer(Modifier.height(36.dp))
        ui.sections.forEachIndexed { index, (title, subtitle) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(index) }
                    .padding(vertical = 18.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Quiet(subtitle)
            }
            Hairline()
        }
    }
}

@Composable
fun ReviewScreen(
    ui: ReviewUi,
    known: Set<Char>,
    onClose: () -> Unit,
    onReveal: () -> Unit,
    onGrade: (Int) -> Unit,
    onSaved: () -> Unit,
) {
    Frame(
        bottom = {
            when {
                ui.summary != null || ui.empty != null -> Thumb("Close", onClose)
                !ui.revealed -> Thumb("Show answer", onReveal)
                else -> GradeRow(onGrade)
            }
        },
    ) {
        Close(onClose)
        Text("Review", style = MaterialTheme.typography.headlineMedium)
        if (ui.counter.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Quiet(ui.counter)
        }
        Spacer(Modifier.height(36.dp))
        if (ui.empty != null) {
            Text(ui.empty, style = MaterialTheme.typography.bodyLarge)
            if (ui.offerSaved) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Review the saved deck",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable(onClick = onSaved),
                )
            }
            return@Frame
        }
        if (ui.summary != null) {
            ui.summary.forEach { (label, value) ->
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
            }
            return@Frame
        }
        Column(
            Modifier.pointerInput(ui.word, ui.revealed) {
                if (!ui.revealed) return@pointerInput
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            acc > 120f -> onGrade(3)
                            acc < -120f -> onGrade(1)
                        }
                    },
                    onHorizontalDrag = { _, drag -> acc += drag },
                )
            },
        ) {
            RubyText(ui.word, MaterialTheme.typography.headlineLarge, known)
            AnimatedVisibility(ui.revealed, enter = fadeIn(tween(180))) {
                Column {
                    Spacer(Modifier.height(28.dp))
                    if (ui.meaning.isNotBlank()) {
                        Text(ui.meaning, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                    }
                    if (ui.definition.isNotBlank()) {
                        MaybeRuby(ui.definition, MaterialTheme.typography.bodyLarge, known)
                        Spacer(Modifier.height(16.dp))
                    }
                    if (ui.sentence.isNotBlank()) {
                        MaybeRuby(ui.sentence, MaterialTheme.typography.bodyLarge, known)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (ui.sentenceEn.isNotBlank()) Quiet(ui.sentenceEn)
                    Spacer(Modifier.height(28.dp))
                    Quiet("Swipe left Again, right Good")
                }
            }
        }
        if (ui.note.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(ui.note, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun QuizScreen(
    ui: QuizUi,
    known: Set<Char>,
    onClose: () -> Unit,
    onLesson: (String?) -> Unit,
    onSource: (String) -> Unit,
    onWeak: () -> Unit,
    onCount: (Int) -> Unit,
    onStart: () -> Unit,
    onPick: (String) -> Unit,
    onType: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSheets: () -> Unit,
) {
    val action = when {
        ui.setup -> "Start quiz" to onStart
        ui.empty || ui.done -> "Close" to onClose
        ui.verdict == null -> "Check" to onCheck
        else -> "Next" to onNext
    }
    Frame(bottom = { Thumb(action.first, action.second, enabled = ui.setup || ui.done || ui.verdict != null || ui.picked.isNotBlank() || ui.typed.isNotBlank() || ui.choices.isEmpty()) }) {
        Close(onClose)
        Text("Quiz", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        if (ui.setup) {
            Quiet("Lesson")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.lessons.forEach { (id, label) ->
                    Selectable(label, (id.isEmpty() && ui.lessonId == null) || id == ui.lessonId) {
                        onLesson(id.ifEmpty { null })
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Quiet("Source")
            Spacer(Modifier.height(8.dp))
            listOf(
                "deck" to "Deck",
                "worksheet" to "Worksheets",
                "homework" to "Homework",
                "class" to "Class quizzes",
            ).forEach { (id, label) ->
                Selectable(label, id in ui.sources) { onSource(id) }
            }
            Spacer(Modifier.height(20.dp))
            Selectable(if (ui.weakOnly) "Weak only" else "All items", ui.weakOnly, onWeak)
            Spacer(Modifier.height(20.dp))
            Quiet("Length")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(5, 8, 10).forEach { n ->
                    Selectable("$n", ui.count == n) { onCount(n) }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Grade a worksheet",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable(onClick = onSheets),
            )
            return@Frame
        }
        if (ui.empty) {
            Text("Nothing matches this filter.", style = MaterialTheme.typography.bodyLarge)
            return@Frame
        }
        if (ui.done) {
            Text(ui.summary, style = MaterialTheme.typography.headlineMedium)
            if (ui.misses.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Quiet("Missed")
                Spacer(Modifier.height(12.dp))
                ui.misses.forEach { miss ->
                    MaybeRuby(miss, MaterialTheme.typography.bodyLarge, known)
                    Spacer(Modifier.height(12.dp))
                }
            }
            return@Frame
        }
        Quiet(ui.counter)
        if (ui.reask) {
            Spacer(Modifier.height(4.dp))
            Quiet("Asked again")
        }
        Spacer(Modifier.height(24.dp))
        MaybeRuby(ui.prompt, MaterialTheme.typography.headlineMedium, known)
        if (ui.promptEn.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Quiet(ui.promptEn)
        }
        Spacer(Modifier.height(24.dp))
        if (ui.choices.isEmpty()) {
            AnswerField(ui.typed, onType, onCheck)
        } else {
            ui.choices.forEach { choice ->
                val color = when {
                    ui.verdict != null && choice == ui.expected -> MaterialTheme.colorScheme.primary
                    ui.verdict == false && choice == ui.picked -> MaterialTheme.colorScheme.error
                    choice == ui.picked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onBackground
                }
                MaybeRuby(
                    choice,
                    MaterialTheme.typography.bodyLarge.copy(color = color),
                    known,
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = ui.verdict == null) { onPick(choice) }
                        .padding(vertical = 14.dp),
                )
                Hairline()
            }
        }
        AnimatedVisibility(ui.verdict != null, enter = fadeIn(tween(160))) {
            Column {
                Spacer(Modifier.height(20.dp))
                Verdict(ui.verdict == true)
                if (ui.verdict == false) {
                    Spacer(Modifier.height(8.dp))
                    MaybeRuby(ui.expected, MaterialTheme.typography.titleLarge, known)
                }
            }
        }
    }
}

@Composable
fun DrillScreen(
    ui: DrillUi,
    known: Set<Char>,
    onClose: () -> Unit,
    onType: (String) -> Unit,
    onTile: (Int) -> Unit,
    onToggleTiles: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
) {
    val waiting = ui.verdict == null
    Frame(bottom = { Thumb(if (waiting) "Check" else "Next", if (waiting) onCheck else onNext, enabled = !waiting || ui.typed.isNotBlank()) }) {
        Close(onClose)
        Text("Conjugation", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Quiet(ui.counter)
        Spacer(Modifier.height(36.dp))
        RubyText(ui.word, MaterialTheme.typography.headlineLarge, known)
        Spacer(Modifier.height(8.dp))
        MaybeRuby(ui.form, MaterialTheme.typography.titleMedium, known)
        Spacer(Modifier.height(4.dp))
        MaybeRuby(
            ui.prompt,
            MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            known,
        )
        Spacer(Modifier.height(24.dp))
        AnswerField(ui.typed, onType, onCheck, enabled = waiting)
        Spacer(Modifier.height(12.dp))
        Text(
            if (ui.tileMode) "Keyboard" else "Tiles",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onToggleTiles),
        )
        if (ui.tileMode && waiting) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ui.tiles.forEachIndexed { index, tile ->
                    Text(
                        tile,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clickable { onTile(index) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        AnimatedVisibility(ui.verdict != null, enter = fadeIn(tween(160))) {
            Column {
                Spacer(Modifier.height(24.dp))
                Verdict(ui.verdict == true)
                if (ui.verdict == false) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.expected, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.height(16.dp))
                MaybeRuby(ui.rule, MaterialTheme.typography.bodyLarge, known)
                Spacer(Modifier.height(8.dp))
                Quiet(ui.ruleEn)
            }
        }
    }
}

@Composable
fun ReadListScreen(items: List<StoryItem>, known: Set<Char>, onClose: () -> Unit, onOpen: (String) -> Unit) {
    Frame(bottom = { Inset() }) {
        Close(onClose)
        Text("Read", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        if (items.isEmpty()) {
            Text("No stories yet.", style = MaterialTheme.typography.bodyLarge)
        }
        items.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item.id) }
                    .padding(vertical = 18.dp),
            ) {
                MaybeRuby(item.title, MaterialTheme.typography.titleMedium, known)
                Spacer(Modifier.height(4.dp))
                Quiet(item.detail)
            }
            Hairline()
        }
    }
}

@Composable
fun ReadScreen(ui: ReadUi, known: Set<Char>, onClose: () -> Unit, onPick: (Int, Int) -> Unit, onCheck: () -> Unit) {
    Frame(bottom = { Thumb(if (ui.score == null) "Check" else "Close", if (ui.score == null) onCheck else onClose) }) {
        Close(onClose)
        MaybeRuby(ui.title, MaterialTheme.typography.headlineMedium, known)
        Spacer(Modifier.height(8.dp))
        Quiet(ui.level)
        Spacer(Modifier.height(28.dp))
        MaybeRuby(ui.body, MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp), known)
        Spacer(Modifier.height(36.dp))
        Quiet("Questions")
        Spacer(Modifier.height(8.dp))
        ui.questions.forEachIndexed { qIndex, question ->
            Spacer(Modifier.height(20.dp))
            MaybeRuby(question.prompt, MaterialTheme.typography.titleMedium, known)
            Spacer(Modifier.height(8.dp))
            question.choices.forEachIndexed { cIndex, choice ->
                val color = when {
                    question.checked && cIndex == question.correct -> MaterialTheme.colorScheme.primary
                    question.checked && cIndex == question.picked && cIndex != question.correct -> MaterialTheme.colorScheme.error
                    cIndex == question.picked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onBackground
                }
                MaybeRuby(
                    choice,
                    MaterialTheme.typography.bodyLarge.copy(color = color),
                    known,
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !question.checked) { onPick(qIndex, cIndex) }
                        .padding(vertical = 10.dp),
                )
            }
        }
        if (ui.score != null) {
            Spacer(Modifier.height(24.dp))
            Text(ui.score, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun StatsScreen(ui: StatsUi, known: Set<Char>, onClose: () -> Unit, onShare: () -> Unit) {
    Frame(bottom = { Thumb("Share progress", onShare) }) {
        Close(onClose)
        Text("Stats", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        ui.blocks.forEach { block ->
            if (block.title.isNotBlank()) {
                Quiet(block.title)
                Spacer(Modifier.height(12.dp))
            }
            block.lines.forEach { (label, value) ->
                MaybeRuby(label, MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), known)
                Spacer(Modifier.height(2.dp))
                MaybeRuby(value, MaterialTheme.typography.titleLarge, known)
                Spacer(Modifier.height(18.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SheetScreen(
    ui: SheetUi,
    known: Set<Char>,
    onClose: () -> Unit,
    onOpen: (Int) -> Unit,
    onType: (Int, String) -> Unit,
    onGrade: () -> Unit,
) {
    val listing = ui.listing
    val finished = ui.summary.isNotBlank()
    Frame(bottom = {
        if (listing != null) Inset() else Thumb(if (finished) "Close" else "Grade", if (finished) onClose else onGrade)
    }) {
        Close(onClose)
        Text(if (listing != null) "Homework" else "Worksheet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (listing == null) MaybeRuby(ui.title, MaterialTheme.typography.titleMedium, known)
        if (ui.summary.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(ui.summary, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))
        if (listing != null) {
            listing.forEachIndexed { index, (title, detail) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(index) }
                        .padding(vertical = 18.dp),
                ) {
                    MaybeRuby(title, MaterialTheme.typography.titleMedium, known)
                    Spacer(Modifier.height(4.dp))
                    Quiet(detail)
                }
                Hairline()
            }
            return@Frame
        }
        ui.lines.forEachIndexed { index, line ->
            Spacer(Modifier.height(20.dp))
            MaybeRuby(line.prompt, MaterialTheme.typography.bodyLarge, known)
            Spacer(Modifier.height(8.dp))
            if (line.correct == null) {
                AnswerField(line.typed, { onType(index, it) }, onGrade)
            } else {
                Text(
                    line.typed.ifBlank { "—" },
                    color = if (line.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!line.correct) {
                    Spacer(Modifier.height(4.dp))
                    Quiet(line.expected)
                }
            }
        }
    }
}

@Composable
private fun Frame(bottom: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottom,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@Composable
private fun Thumb(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        ) {
            Text(text, fontFamily = Jp, fontSize = 16.sp)
        }
    }
}

@Composable
private fun GradeRow(onGrade: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(1 to "Again", 2 to "Hard", 3 to "Good", 4 to "Easy").forEach { (ease, label) ->
            if (ease == 3) {
                Button(
                    onClick = { onGrade(ease) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(label, fontFamily = Jp, fontSize = 14.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { onGrade(ease) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(label, fontFamily = Jp, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
private fun Inset() {
    Spacer(Modifier.navigationBarsPadding().height(8.dp))
}

@Composable
private fun Close(onClose: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(
        "Close",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.clickable(onClick = onClose),
    )
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Quiet(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Hairline() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
}

@Composable
private fun Selectable(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun Verdict(correct: Boolean) {
    Text(
        if (correct) "Correct" else "Incorrect",
        color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun AnswerField(value: String, onValue: (String) -> Unit, onDone: () -> Unit, enabled: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.titleLarge,
        placeholder = { Text("Answer", fontFamily = Jp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}
