package dev.ericallerton.yoruben.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val gems: Int,
    val xp: Int,
    val streak: Int,
    val lastStreakDay: String,
    val equipped: String,
)

@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val itemId: String,
    val boughtAt: Long,
)

@Entity(tableName = "drill_log")
data class DrillLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verbId: String,
    val ruleId: String,
    val correct: Boolean,
    val atEpochMs: Long,
)

@Entity(tableName = "region_clears")
data class RegionClearEntity(
    @PrimaryKey val lessonDate: String,
    val atEpochMs: Long,
)

@Entity(tableName = "meta_unlocks")
data class MetaUnlockEntity(
    @PrimaryKey val unlockId: String,
    val atEpochMs: Long,
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

@Dao
interface GameDao {
    @Query("SELECT * FROM player WHERE id = 1")
    fun observePlayer(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun player(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayer(row: PlayerEntity)

    @Query("SELECT * FROM owned_items")
    fun observeOwned(): Flow<List<OwnedItemEntity>>

    @Query("SELECT * FROM owned_items")
    suspend fun owned(): List<OwnedItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwned(row: OwnedItemEntity)

    @Insert
    suspend fun insertDrill(row: DrillLogEntity)

    @Query("SELECT * FROM drill_log ORDER BY atEpochMs DESC LIMIT 200")
    suspend fun recentDrills(): List<DrillLogEntity>

    @Query("SELECT lessonDate FROM region_clears")
    fun observeClears(): Flow<List<String>>

    @Query("SELECT lessonDate FROM region_clears")
    suspend fun clears(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClear(row: RegionClearEntity)

    @Query("SELECT unlockId FROM meta_unlocks")
    suspend fun unlocks(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(row: MetaUnlockEntity)
}

@Database(
    entities = [
        QuizResultEntity::class,
        DebriefEntity::class,
        PlayerEntity::class,
        OwnedItemEntity::class,
        DrillLogEntity::class,
        RegionClearEntity::class,
        MetaUnlockEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizDao(): QuizDao
    abstract fun debriefDao(): DebriefDao
    abstract fun gameDao(): GameDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `player` (`id` INTEGER NOT NULL, `gems` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `streak` INTEGER NOT NULL, `lastStreakDay` TEXT NOT NULL, `equipped` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `owned_items` (`itemId` TEXT NOT NULL, `boughtAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drill_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `verbId` TEXT NOT NULL, `ruleId` TEXT NOT NULL, `correct` INTEGER NOT NULL, `atEpochMs` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `region_clears` (`lessonDate` TEXT NOT NULL, `atEpochMs` INTEGER NOT NULL, PRIMARY KEY(`lessonDate`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meta_unlocks` (`unlockId` TEXT NOT NULL, `atEpochMs` INTEGER NOT NULL, PRIMARY KEY(`unlockId`))",
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "yoruben.db")
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
