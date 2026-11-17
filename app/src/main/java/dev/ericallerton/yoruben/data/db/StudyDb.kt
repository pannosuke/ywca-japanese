package dev.ericallerton.yoruben.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val kind: String,
    val correct: Boolean,
    val lessonDate: String?,
    val weakSpotId: String?,
    val prompt: String,
    val given: String,
    val expected: String,
    val atEpochMs: Long,
)

@Entity(tableName = "debriefs")
data class DebriefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classDate: String,
    val teacherId: String,
    val mood: String,
    val howItWent: String,
    val newWords: String,
    val confusions: String,
    val savedAtEpochMs: Long,
)

@Dao
interface QuizDao {
    @Insert
    suspend fun insert(row: QuizResultEntity)

    @Query("SELECT * FROM quiz_results ORDER BY atEpochMs DESC")
    fun observeAll(): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results ORDER BY atEpochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<QuizResultEntity>
}

@Dao
interface DebriefDao {
    @Insert
    suspend fun insert(row: DebriefEntity): Long

    @Query("SELECT * FROM debriefs ORDER BY classDate DESC, savedAtEpochMs DESC")
    fun observeAll(): Flow<List<DebriefEntity>>

    @Query("DELETE FROM debriefs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [QuizResultEntity::class, DebriefEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizDao(): QuizDao
    abstract fun debriefDao(): DebriefDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "yoruben.db").build()
        }
    }
}
