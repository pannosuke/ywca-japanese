package dev.ericallerton.yoruben

import android.app.Application
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.data.CurriculumRepository
import dev.ericallerton.yoruben.data.db.AppDatabase

class YorubenApp : Application() {
    lateinit var curriculum: Curriculum
        private set
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        curriculum = CurriculumRepository.load(this)
        database = AppDatabase.create(this)
    }
}
