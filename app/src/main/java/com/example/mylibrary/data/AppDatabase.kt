package com.example.mylibrary.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "course_units")
data class CourseUnitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes", foreignKeys = [
    ForeignKey(
        entity        = CourseUnitEntity::class,
        parentColumns = ["id"],
        childColumns  = ["unitId"],
        onDelete      = ForeignKey.CASCADE
    )
])
data class NoteEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "papers", foreignKeys = [
    ForeignKey(
        entity        = CourseUnitEntity::class,
        parentColumns = ["id"],
        childColumns  = ["unitId"],
        onDelete      = ForeignKey.CASCADE
    )
])
data class PaperEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val filePath: String,
    val mimeType: String,
    val addedAt: Long = System.currentTimeMillis()
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface CourseUnitDao {
    @Query("SELECT * FROM course_units ORDER BY createdAt DESC")
    fun getAllUnits(): Flow<List<CourseUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: CourseUnitEntity)

    @Delete
    suspend fun deleteUnit(unit: CourseUnitEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE unitId = :unitId ORDER BY updatedAt DESC")
    fun getNotesForUnit(unitId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?
}

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers WHERE unitId = :unitId ORDER BY addedAt DESC")
    fun getPapersForUnit(unitId: String): Flow<List<PaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity)

    @Delete
    suspend fun deletePaper(paper: PaperEntity)
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities  = [CourseUnitEntity::class, NoteEntity::class, PaperEntity::class],
    version   = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseUnitDao(): CourseUnitDao
    abstract fun noteDao(): NoteDao
    abstract fun paperDao(): PaperDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tees_library.db"
                ).build().also { INSTANCE = it }
            }
    }
}