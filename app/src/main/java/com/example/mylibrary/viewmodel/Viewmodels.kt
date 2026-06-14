package com.example.mylibrary.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.mylibrary.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// ── HomeViewModel ─────────────────────────────────────────────────────────────
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LibraryRepository(app)

    val units = repo.allUnits.stateIn(
        scope         = viewModelScope,
        started       = SharingStarted.WhileSubscribed(5000),
        initialValue  = emptyList()
    )

    fun addUnit(name: String, code: String) = viewModelScope.launch {
        repo.addUnit(
            CourseUnitEntity(
                id   = UUID.randomUUID().toString(),
                name = name,
                code = code
            )
        )
    }

    fun deleteUnit(unit: CourseUnitEntity) = viewModelScope.launch {
        repo.deleteUnit(unit)
    }
}

class HomeViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(app) as T
    }
}

// ── UnitViewModel ─────────────────────────────────────────────────────────────
class UnitViewModel(app: Application, val unitId: String) : AndroidViewModel(app) {

    private val repo = LibraryRepository(app)

    val notes = repo.notesFor(unitId).stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val papers = repo.papersFor(unitId).stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteNote(note: NoteEntity) = viewModelScope.launch { repo.deleteNote(note) }
    fun deletePaper(paper: PaperEntity) = viewModelScope.launch { repo.deletePaper(paper) }
}

class UnitViewModelFactory(
    private val app: Application,
    private val unitId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return UnitViewModel(app, unitId) as T
    }
}

// ── NoteViewModel ─────────────────────────────────────────────────────────────
class NoteViewModel(app: Application, private val unitId: String) : AndroidViewModel(app) {

    private val repo = LibraryRepository(app)

    fun saveNote(id: String?, title: String, content: String) = viewModelScope.launch {
        repo.saveNote(
            NoteEntity(
                id        = id ?: UUID.randomUUID().toString(),
                unitId    = unitId,
                title     = title,
                content   = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadNote(noteId: String): NoteEntity? = repo.getNoteById(noteId)
}

class NoteViewModelFactory(
    private val app: Application,
    private val unitId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NoteViewModel(app, unitId) as T
    }
}