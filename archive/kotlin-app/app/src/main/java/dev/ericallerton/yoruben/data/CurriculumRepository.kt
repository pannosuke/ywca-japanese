package dev.ericallerton.yoruben.data

import android.content.Context
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.core.CurriculumJson

object CurriculumRepository {
    fun load(context: Context): Curriculum {
        val profile = context.assets.open("data/profile.json").bufferedReader().use { it.readText() }
        val lessons = context.assets.open("data/lessons.json").bufferedReader().use { it.readText() }
        val words = context.assets.open("data/words.json").bufferedReader().use { it.readText() }
        return CurriculumJson.decode(profile, lessons, words)
    }
}
