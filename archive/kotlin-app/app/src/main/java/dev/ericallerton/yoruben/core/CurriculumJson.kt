package dev.ericallerton.yoruben.core

import kotlinx.serialization.json.Json

object CurriculumJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun decode(profileJson: String, lessonsJson: String, wordsJson: String): Curriculum {
        val profile = json.decodeFromString(Profile.serializer(), profileJson)
        val lessons = json.decodeFromString(LessonFile.serializer(), lessonsJson)
        val words = json.decodeFromString(WordFile.serializer(), wordsJson)
        return Curriculum(profile, lessons.lessons, words.words)
    }
}
