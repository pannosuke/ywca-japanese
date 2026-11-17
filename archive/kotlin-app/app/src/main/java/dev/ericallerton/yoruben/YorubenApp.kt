package dev.ericallerton.yoruben

import android.app.Application
import dev.ericallerton.yoruben.core.Curriculum
import dev.ericallerton.yoruben.data.CurriculumRepository
import dev.ericallerton.yoruben.data.GameStore
import dev.ericallerton.yoruben.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YorubenApp : Application() {
    lateinit var curriculum: Curriculum
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var game: GameStore
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        curriculum = CurriculumRepository.load(this)
        database = AppDatabase.create(this)
        game = GameStore(database)
        scope.launch { game.ensure() }
    }
}
