package dev.ericallerton.yoruben.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

val Tokyo: ZoneId = ZoneId.of("Asia/Tokyo")

data class ClassBrief(
    val nextDate: LocalDate,
    val nextWeekday: String,
    val teacher: Teacher,
    val focusRuby: String,
    val focusEn: String,
    val timeLabel: String,
    val commuteRuby: String,
    val commuteEn: String,
    val lastLesson: Lesson?,
    val lastWithTeacher: Lesson?,
    val reviewWords: List<Word>,
    val reviewGrammar: List<GrammarPoint>,
    val weakSpots: List<WeakSpot>,
)

object ClassBriefs {
    private val classEnd: LocalTime = LocalTime.of(20, 30)

    fun prepare(curriculum: Curriculum, today: LocalDate, time: LocalTime): ClassBrief {
        val next = nextSlot(curriculum, today, time)
        val past = curriculum.lessons.filter { lesson ->
            val date = LocalDate.parse(lesson.date)
            date < today || (date == today && time >= classEnd)
        }
        val last = past.maxByOrNull { it.date }
        val lastSame = past.filter { it.teacherId == next.second.teacherId }.maxByOrNull { it.date }
        val reviewDates = listOfNotNull(last, lastSame).map { it.date }.toSet()
        val fromLessons = curriculum.words.filter { it.studying && it.lessonDate in reviewDates }
        val fromWeak = curriculum.words.filter { it.studying && it.id in curriculum.weakWordIds }
        val words = (fromWeak + fromLessons).distinctBy { it.id }.take(8)
        val grammar = listOfNotNull(last, lastSame)
            .flatMap { it.grammar }
            .sortedByDescending { if (it.weakSpotId != null) 1 else 0 }
            .distinctBy { it.id }
            .take(4)
        return ClassBrief(
            nextDate = next.first,
            nextWeekday = next.second.weekday,
            teacher = curriculum.teacher(next.second.teacherId),
            focusRuby = next.second.focusRuby,
            focusEn = next.second.focusEn,
            timeLabel = "${next.second.start}–${next.second.end}",
            commuteRuby = curriculum.profile.commute.summaryRuby,
            commuteEn = curriculum.profile.commute.summaryEn,
            lastLesson = last,
            lastWithTeacher = lastSame,
            reviewWords = words,
            reviewGrammar = grammar,
            weakSpots = curriculum.profile.weakSpots,
        )
    }

    private fun nextSlot(
        curriculum: Curriculum,
        today: LocalDate,
        time: LocalTime,
    ): Pair<LocalDate, ScheduleSlot> {
        val byDay = curriculum.profile.schedule.associateBy { dayOfWeek(it.weekday) }
        var cursor = today
        repeat(8) {
            val slot = byDay[cursor.dayOfWeek]
            if (slot != null && (cursor > today || time < classEnd)) {
                return cursor to slot
            }
            cursor = cursor.plusDays(1)
        }
        val fallback = curriculum.profile.schedule.first()
        return today to fallback
    }

    private fun dayOfWeek(weekday: String): DayOfWeek = when (weekday) {
        "Mon" -> DayOfWeek.MONDAY
        "Tue" -> DayOfWeek.TUESDAY
        "Wed" -> DayOfWeek.WEDNESDAY
        "Thu" -> DayOfWeek.THURSDAY
        "Fri" -> DayOfWeek.FRIDAY
        "Sat" -> DayOfWeek.SATURDAY
        "Sun" -> DayOfWeek.SUNDAY
        else -> DayOfWeek.TUESDAY
    }
}
