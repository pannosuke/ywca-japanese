package dev.ericallerton.yoruben.ui.lessons

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.core.GrammarPoint
import dev.ericallerton.yoruben.core.Lesson
import dev.ericallerton.yoruben.ui.DateLine
import dev.ericallerton.yoruben.ui.FuriganaText
import dev.ericallerton.yoruben.ui.SectionLabel
import dev.ericallerton.yoruben.ui.TeacherLine
import dev.ericallerton.yoruben.ui.WordCard

@Composable
fun LessonsScreen(curriculum: Curriculum, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("レッスン", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            FuriganaText(curriculum.profile.schoolRuby, MaterialTheme.typography.bodyMedium)
            Text(curriculum.profile.schoolEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FuriganaText("火[か]・金[きん] 19:00–20:30", MaterialTheme.typography.bodyMedium)
        }
        items(curriculum.lessonsNewestFirst, key = { it.id }) { lesson ->
            LessonRow(curriculum, lesson, onClick = { onOpen(lesson.date) })
        }
    }
}

@Composable
private fun LessonRow(curriculum: Curriculum, lesson: Lesson, onClick: () -> Unit) {
    val study = curriculum.wordsFor(lesson.date).count { it.studying }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            DateLine(lesson)
            TeacherLine(curriculum, lesson)
            Spacer(Modifier.height(8.dp))
            FuriganaText(lesson.topicRuby, MaterialTheme.typography.titleLarge)
            Text(lesson.topicEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (study > 0) {
                Spacer(Modifier.height(8.dp))
                Text("べんきょうちゅう $study", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LessonDetailScreen(curriculum: Curriculum, date: String) {
    val lesson = curriculum.lessons.find { it.date == date }
    if (lesson == null) {
        Text("その ひの レッスンは まだ ない。", modifier = Modifier.padding(24.dp))
        return
    }
    val words = curriculum.wordsFor(date).sortedByDescending { it.studying }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DateLine(lesson)
            TeacherLine(curriculum, lesson)
            Spacer(Modifier.height(8.dp))
            FuriganaText(lesson.topicRuby, MaterialTheme.typography.headlineSmall)
            Text(lesson.topicEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionLabel("ことば") }
        items(words, key = { it.id }) { WordCard(it) }
        item { SectionLabel("ぶんぽう") }
        items(lesson.grammar, key = { it.id }) { GrammarCard(it) }
        item { SectionLabel("メモ") }
        items(lesson.notes) { note ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                FuriganaText(note, MaterialTheme.typography.bodyLarge, Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun GrammarCard(point: GrammarPoint) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            FuriganaText(point.patternRuby, MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(point.explanationJa, style = MaterialTheme.typography.bodyLarge)
            Text(point.explanationEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("れい", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            FuriganaText(point.exampleRuby, MaterialTheme.typography.bodyLarge)
            Text(point.exampleEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
