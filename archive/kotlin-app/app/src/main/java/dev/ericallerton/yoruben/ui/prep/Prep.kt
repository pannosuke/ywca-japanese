package dev.ericallerton.yoruben.ui.prep

import android.app.Application
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericallerton.yoruben.YorubenApp
import dev.ericallerton.yoruben.core.ClassBrief
import dev.ericallerton.yoruben.core.ClassBriefs
import dev.ericallerton.yoruben.core.Tokyo
import dev.ericallerton.yoruben.core.dateRuby
import dev.ericallerton.yoruben.data.db.DebriefEntity
import dev.ericallerton.yoruben.ui.FuriganaText
import dev.ericallerton.yoruben.ui.SectionLabel
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PrepViewModel(app: Application) : AndroidViewModel(app) {
    private val curriculum = (app as YorubenApp).curriculum
    private val dao = (app as YorubenApp).database.debriefDao()
    val debriefs = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun brief(now: ZonedDateTime = ZonedDateTime.now(Tokyo)): ClassBrief {
        return ClassBriefs.prepare(curriculum, now.toLocalDate(), now.toLocalTime())
    }

    fun save(classDate: String, teacherId: String, mood: String, how: String, words: String, confusions: String) {
        viewModelScope.launch {
            dao.insert(
                DebriefEntity(
                    classDate = classDate,
                    teacherId = teacherId,
                    mood = mood,
                    howItWent = how.trim(),
                    newWords = words.trim(),
                    confusions = confusions.trim(),
                    savedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun exportJson(rows: List<DebriefEntity>): String {
        val payload = DebriefExport(
            student = curriculum.profile.student,
            exportedAt = ZonedDateTime.now(Tokyo).toOffsetDateTime().toString(),
            debriefs = rows.map {
                DebriefExportItem(it.classDate, it.teacherId, it.mood, it.howItWent, it.newWords, it.confusions, it.savedAtEpochMs)
            },
        )
        return Json { prettyPrint = true }.encodeToString(payload)
    }
}

@Serializable
private data class DebriefExport(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val student: String,
    @SerialName("exported_at") val exportedAt: String,
    val debriefs: List<DebriefExportItem>,
)

@Serializable
private data class DebriefExportItem(
    @SerialName("class_date") val classDate: String,
    @SerialName("teacher_id") val teacherId: String,
    val mood: String,
    @SerialName("how_it_went") val howItWent: String,
    @SerialName("new_words") val newWords: String,
    val confusions: String,
    @SerialName("saved_at_epoch_ms") val savedAtEpochMs: Long,
)

@Composable
fun PrepScreen(vm: PrepViewModel = viewModel()) {
    val context = LocalContext.current
    val curriculum = (context.applicationContext as YorubenApp).curriculum
    val brief = remember { vm.brief() }
    val saved by vm.debriefs.collectAsState()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) writeText(context, uri, vm.exportJson(saved))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item {
            Text("じゅんび", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("クラスの まえに みることと、おわった あとの メモ。", style = MaterialTheme.typography.bodyMedium)
        }
        item { BriefCard(brief) }
        item { SectionLabel("クラスの あと") }
        item {
            DebriefForm(defaultDate = LocalDate.now(Tokyo).toString(), onSave = vm::save)
        }
        item { SectionLabel("ほぞんした メモ") }
        if (saved.isEmpty()) {
            item { Text("まだ ありません。クラスの あと、3ぎょうで じゅうぶんです。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            item {
                OutlinedButton(onClick = { export.launch("yoruben-debriefs.json") }) { Text("JSON で かきだす") }
            }
            items(saved.size) { index ->
                val row = saved[index]
                SavedDebrief(row, onDelete = { vm.delete(row.id) })
            }
        }
    }
}

@Composable
private fun BriefCard(brief: ClassBrief) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("つぎの クラス", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            FuriganaText(
                dateRuby(brief.nextDate.toString(), brief.nextWeekday) + "  " + brief.timeLabel,
                MaterialTheme.typography.titleLarge,
            )
            FuriganaText(
                brief.teacher.nameRuby + brief.teacher.honorificRuby + " · " + brief.focusRuby,
                MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(brief.focusEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("いきかた", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FuriganaText(brief.commuteRuby, MaterialTheme.typography.bodyLarge)
            Text(brief.commuteEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            brief.lastLesson?.let { last ->
                Spacer(Modifier.height(12.dp))
                Text("このまえの クラス", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FuriganaText(dateRuby(last.date, last.weekday), MaterialTheme.typography.bodyLarge)
                FuriganaText(last.topicRuby, MaterialTheme.typography.bodyLarge)
                Text(last.topicEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            brief.lastWithTeacher?.let { last ->
                if (last.id != brief.lastLesson?.id) {
                    Spacer(Modifier.height(8.dp))
                    Text("この せんせいの ぜんかい", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    FuriganaText(dateRuby(last.date, last.weekday) + "  " + last.topicRuby, MaterialTheme.typography.bodyLarge)
                }
            }
            if (brief.reviewWords.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("みかえす ことば", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                brief.reviewWords.forEach { word ->
                    Spacer(Modifier.height(6.dp))
                    FuriganaText(word.ruby, MaterialTheme.typography.titleMedium)
                    Text(word.glossJa, style = MaterialTheme.typography.bodyMedium)
                    Text(word.glossEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (brief.reviewGrammar.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("みかえす かたち", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                brief.reviewGrammar.forEach { point ->
                    Spacer(Modifier.height(6.dp))
                    FuriganaText(point.patternRuby, MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("にがて", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            brief.weakSpots.forEach { spot ->
                Spacer(Modifier.height(8.dp))
                FuriganaText(spot.titleRuby, MaterialTheme.typography.titleMedium)
                Text(spot.reminderJa, style = MaterialTheme.typography.bodyMedium)
                Text(spot.reminderEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebriefForm(
    defaultDate: String,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    var date by remember { mutableStateOf(defaultDate) }
    var teacher by remember { mutableStateOf("tsuji") }
    var mood by remember { mutableStateOf("ok") }
    var how by remember { mutableStateOf("") }
    var words by remember { mutableStateOf("") }
    var confusions by remember { mutableStateOf("") }
    var savedTick by remember { mutableStateOf(0) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(date, { date = it }, label = { Text("ひづけ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("せんせい", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(teacher == "tsuji", { teacher = "tsuji" }, label = { Text("ツジ") })
                FilterChip(teacher == "ouchi", { teacher = "ouchi" }, label = { Text("おおうち") })
            }
            Text("どうだった", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mood == "good", { mood = "good" }, label = { Text("うまく いった") })
                FilterChip(mood == "ok", { mood = "ok" }, label = { Text("ふつう") })
                FilterChip(mood == "hard", { mood = "hard" }, label = { Text("むずかしかった") })
            }
            OutlinedTextField(how, { how = it }, label = { Text("クラスの ようす") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(words, { words = it }, label = { Text("あたらしい ことば") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(confusions, { confusions = it }, label = { Text("わからなかった こと") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = {
                onSave(date, teacher, mood, how, words, confusions)
                how = ""
                words = ""
                confusions = ""
                savedTick += 1
            }) { Text("ほぞんする") }
            if (savedTick > 0) Text("のこしました。", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun SavedDebrief(row: DebriefEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${row.classDate} · ${row.teacherId} · ${moodLabel(row.mood)}", style = MaterialTheme.typography.titleSmall)
            if (row.howItWent.isNotBlank()) Text(row.howItWent, style = MaterialTheme.typography.bodyMedium)
            if (row.newWords.isNotBlank()) Text("ことば: ${row.newWords}", style = MaterialTheme.typography.bodyMedium)
            if (row.confusions.isNotBlank()) Text("わからない: ${row.confusions}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDelete) { Text("けす") }
        }
    }
}

private fun moodLabel(mood: String) = when (mood) {
    "good" -> "うまく いった"
    "hard" -> "むずかしかった"
    else -> "ふつう"
}

private fun writeText(context: Context, uri: android.net.Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
    }
}
