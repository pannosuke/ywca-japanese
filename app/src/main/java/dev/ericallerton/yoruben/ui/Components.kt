package dev.ericallerton.yoruben.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.core.Lesson
import dev.ericallerton.yoruben.core.Word
import dev.ericallerton.yoruben.core.dateRuby

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@Composable
fun DictionaryGloss(japanese: String, english: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(japanese, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (english.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(english, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WordCard(word: Word, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (word.studying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            FuriganaText(word.ruby, MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            DictionaryGloss(word.glossJa, word.glossEn)
            word.watchOut?.let { watch ->
                Spacer(Modifier.height(8.dp))
                Text("おぼえかた", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                FuriganaText(watch, MaterialTheme.typography.bodyMedium)
            }
            if (word.studying) {
                Spacer(Modifier.height(6.dp))
                Text("べんきょうちゅう", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TeacherLine(curriculum: Curriculum, lesson: Lesson, modifier: Modifier = Modifier) {
    val teacher = curriculum.teacher(lesson.teacherId)
    val inferred = if (lesson.teacherInferred) "（すいそく）" else ""
    FuriganaText(
        markup = "${teacher.nameRuby}${teacher.honorificRuby}$inferred",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
fun DateLine(lesson: Lesson, modifier: Modifier = Modifier) {
    val inferred = if (lesson.dateInferred) " 日付[ひづけ]は すいそく" else ""
    FuriganaText(
        markup = dateRuby(lesson.date, lesson.weekday) + inferred,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}
