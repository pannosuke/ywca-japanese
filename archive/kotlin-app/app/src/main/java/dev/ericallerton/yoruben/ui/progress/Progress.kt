package dev.ericallerton.yoruben.ui.progress

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericallerton.yoruben.BuildConfig
import dev.ericallerton.yoruben.YorubenApp
import dev.ericallerton.yoruben.anki.AnkiDroidReader
import dev.ericallerton.yoruben.anki.AnkiLoad
import dev.ericallerton.yoruben.core.LessonAnkiStats
import dev.ericallerton.yoruben.core.dateRuby
import dev.ericallerton.yoruben.data.db.QuizResultEntity
import dev.ericallerton.yoruben.ui.FuriganaText
import dev.ericallerton.yoruben.ui.adventure.AdventureStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressUi(
    val results: List<QuizResultEntity> = emptyList(),
    val anki: AnkiLoad = AnkiLoad.NotInstalled,
)

class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val curriculum = (app as YorubenApp).curriculum
    private val reader = AnkiDroidReader(app)
    private val _ui = MutableStateFlow(ProgressUi())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            (app as YorubenApp).database.quizDao().observeAll().collect { rows ->
                _ui.value = _ui.value.copy(results = rows)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val tags = curriculum.lessons.associate { it.date to it.ankiTag }
            val loaded = reader.read(tags, curriculum.profile.ankiDeck)
            _ui.value = _ui.value.copy(anki = loaded)
        }
    }
}

@Composable
fun ProgressScreen(vm: ProgressViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val curriculum = (context.applicationContext as YorubenApp).curriculum
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refresh()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("すすみ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "よるべんの ドリルと、AnkiDroid の YWCA デッキを ならべて みます。Anki の カードは よみとるだけです。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { AdventureStats() }
        item { QuizSummaryCard(ui.results, curriculum.profile.weakSpots.map { it.id to it.titleRuby }) }
        item {
            AnkiHeader(ui.anki, onRequest = { permission.launch(AnkiDroidReader.PERMISSION) }, onSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }, onRefresh = vm::refresh)
        }
        val loaded = ui.anki as? AnkiLoad.Loaded
        if (loaded != null) {
            items(curriculum.lessonsNewestFirst, key = { "anki-${it.id}" }) { lesson ->
                val stats = loaded.summary.lessons.firstOrNull { it.lessonDate == lesson.date }
                val quiz = ui.results.filter { it.lessonDate == lesson.date }
                LessonProgressRow(curriculum.teacher(lesson.teacherId).nameRuby, lesson, stats, quiz)
            }
        }
        item {
            Text(
                "よるべん ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuizSummaryCard(results: List<QuizResultEntity>, weakTitles: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("ドリル", style = MaterialTheme.typography.titleMedium)
            if (results.isEmpty()) {
                Text("まだ ドリルが ありません。れんしゅうから はじめられます。", style = MaterialTheme.typography.bodyMedium)
            } else {
                val correct = results.count { it.correct }
                Text("$correct / ${results.size}", style = MaterialTheme.typography.headlineSmall)
                Text("これまでの こたえあわせ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                weakTitles.forEach { (id, title) ->
                    val rows = results.filter { it.weakSpotId == id }
                    if (rows.isNotEmpty()) {
                        val ok = rows.count { it.correct }
                        FuriganaText(title, MaterialTheme.typography.bodyLarge)
                        Text("$ok / ${rows.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnkiHeader(
    anki: AnkiLoad,
    onRequest: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AnkiDroid", style = MaterialTheme.typography.titleMedium)
            when (anki) {
                AnkiLoad.NotInstalled -> Text(
                    "AnkiDroid は この スマホに ありません。ドリルの きろくは ここに のこります。YWCA デッキの まいすうも みたいときは、AnkiDroid を いれて ください。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AnkiLoad.NeedsPermission -> {
                    Text(
                        "YWCA デッキを よむ きょかが まだです。カードの ないようは かえません。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onRequest) { Text("よみとりを ゆるす") }
                    OutlinedButton(onClick = onSettings) { Text("せっていを ひらく") }
                }
                is AnkiLoad.Failed -> {
                    Text(anki.message, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onRefresh) { Text("もう いちど") }
                }
                is AnkiLoad.Loaded -> {
                    val summary = anki.summary
                    if (!summary.deckFound) {
                        Text(
                            "YWCA という なまえの デッキは みつかりませんでした。class:: の タグが ついた カードだけ かぞえています。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            "デッキ: ${summary.deckNames.joinToString("、")}。カード ${summary.matchedCards} まい。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    OutlinedButton(onClick = onRefresh) { Text("よみなおす") }
                }
            }
        }
    }
}

@Composable
private fun LessonProgressRow(
    teacherRuby: String,
    lesson: dev.ericallerton.yoruben.core.Lesson,
    stats: LessonAnkiStats?,
    quiz: List<QuizResultEntity>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            FuriganaText(dateRuby(lesson.date, lesson.weekday), MaterialTheme.typography.titleMedium)
            FuriganaText(teacherRuby + "せんせい", MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            FuriganaText(lesson.topicRuby, MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            if (stats == null || stats.total == 0) {
                Text("Anki: ${lesson.ankiTag} の カードは まだ ありません。", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Anki ${stats.total} まい · あたらしい ${stats.newCount} · がくしゅうちゅう ${stats.learning} · ふくしゅう ${stats.review} · やすみ ${stats.suspended}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (quiz.isEmpty()) {
                Text("ドリル: まだ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    "ドリル: ${quiz.count { it.correct }} / ${quiz.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
