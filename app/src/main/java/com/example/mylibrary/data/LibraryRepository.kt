package com.example.mylibrary.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class LibraryRepository(context: Context) {

    private val db       = AppDatabase.getInstance(context)
    private val unitDao  = db.courseUnitDao()
    private val noteDao  = db.noteDao()
    private val paperDao = db.paperDao()

    // ── Units ─────────────────────────────────────────────────────────────────
    val allUnits: Flow<List<CourseUnitEntity>> = unitDao.getAllUnits()

    suspend fun addUnit(unit: CourseUnitEntity)    = unitDao.insertUnit(unit)
    suspend fun deleteUnit(unit: CourseUnitEntity) = unitDao.deleteUnit(unit)

    // ── Notes ─────────────────────────────────────────────────────────────────
    fun notesFor(unitId: String): Flow<List<NoteEntity>> = noteDao.getNotesForUnit(unitId)

    suspend fun saveNote(note: NoteEntity)         = noteDao.insertNote(note)
    suspend fun deleteNote(note: NoteEntity)       = noteDao.deleteNote(note)
    suspend fun getNoteById(id: String)            = noteDao.getNoteById(id)

    // ── Papers ────────────────────────────────────────────────────────────────
    fun papersFor(unitId: String): Flow<List<PaperEntity>> = paperDao.getPapersForUnit(unitId)

    suspend fun savePaper(paper: PaperEntity)      = paperDao.insertPaper(paper)
    suspend fun deletePaper(paper: PaperEntity)    = paperDao.deletePaper(paper)
}