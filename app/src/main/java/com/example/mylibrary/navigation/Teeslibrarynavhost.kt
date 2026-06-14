package com.example.mylibrary.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mylibrary.navigation.Routes.dec
import com.example.mylibrary.ui.splash.SplashScreen
import com.example.mylibrary.ui.home.HomeScreen
import com.example.mylibrary.ui.note.NoteEditorScreen
import com.example.mylibrary.ui.paper.PaperPickerScreen
import com.example.mylibrary.ui.unit.UnitDetailScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH      = "splash"
    const val HOME        = "home"
    const val UNIT        = "unit/{unitId}/{unitName}/{unitCode}"
    const val NOTE_EDITOR = "note_editor/{unitId}?noteId={noteId}"
    const val PAPER_PICK  = "paper_pick/{unitId}"

    fun unitRoute(unitId: String, unitName: String, unitCode: String) =
        "unit/${unitId.enc()}/${unitName.enc()}/${unitCode.enc()}"

    fun noteEditorRoute(unitId: String, noteId: String? = null) =
        if (noteId != null) "note_editor/${unitId.enc()}?noteId=${noteId.enc()}"
        else                "note_editor/${unitId.enc()}?noteId="

    fun paperPickRoute(unitId: String) = "paper_pick/${unitId.enc()}"

    private fun String.enc() = URLEncoder.encode(this, "UTF-8")
    fun String.dec()         = URLDecoder.decode(this, "UTF-8")
}

@Composable
fun TeesLibraryNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // Splash
        composable(Routes.SPLASH) {
            SplashScreen(onSplashFinished = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        // Home
        composable(Routes.HOME) {
            HomeScreen(onOpenUnit = { unit ->
                navController.navigate(Routes.unitRoute(unit.id, unit.name, unit.code))
            })
        }

        // Unit detail
        composable(Routes.UNIT) { back ->
            val unitId   = back.arguments?.getString("unitId")?.dec()   ?: ""
            val unitName = back.arguments?.getString("unitName")?.dec() ?: ""
            val unitCode = back.arguments?.getString("unitCode")?.dec() ?: ""
            UnitDetailScreen(
                unitId   = unitId,
                unitName = unitName,
                unitCode = unitCode,
                onBack   = { navController.popBackStack() },
                onOpenNote = { note ->
                    navController.navigate(Routes.noteEditorRoute(unitId, note.id))
                },
                onAddNote  = { navController.navigate(Routes.noteEditorRoute(unitId)) },
                onAddPaper = { navController.navigate(Routes.paperPickRoute(unitId)) }
            )
        }

        // Note editor (new or edit)
        composable(Routes.NOTE_EDITOR) { back ->
            val unitId = back.arguments?.getString("unitId")?.dec() ?: ""
            val noteId = back.arguments?.getString("noteId")?.dec()?.ifBlank { null }
            NoteEditorScreen(
                unitId = unitId,
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        // Paper picker
        composable(Routes.PAPER_PICK) { back ->
            val unitId = back.arguments?.getString("unitId")?.dec() ?: ""
            PaperPickerScreen(
                unitId = unitId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}