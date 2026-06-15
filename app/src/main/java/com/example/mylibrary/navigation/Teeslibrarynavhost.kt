package com.example.mylibrary.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mylibrary.navigation.Routes.dec
import com.example.mylibrary.ui.navigation.MainScaffold
import com.example.mylibrary.ui.note.NoteEditorScreen
import com.example.mylibrary.ui.onboarding.OnboardingScreen
import com.example.mylibrary.ui.onboarding.isOnboarded
import com.example.mylibrary.ui.paper.PaperPickerScreen
import com.example.mylibrary.ui.paper.PdfViewerScreen
import com.example.mylibrary.ui.splash.SplashScreen
import com.example.mylibrary.ui.unit.UnitDetailScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH      = "splash"
    const val ONBOARDING  = "onboarding"
    const val MAIN        = "main"
    const val UNIT        = "unit/{unitId}/{unitName}/{unitCode}"
    const val NOTE_EDITOR = "note_editor/{unitId}?noteId={noteId}"
    const val PAPER_PICK  = "paper_pick/{unitId}"
    const val PDF_VIEWER  = "pdf_viewer/{title}/{filePath}"

    fun unitRoute(unitId: String, unitName: String, unitCode: String) =
        "unit/${unitId.enc()}/${unitName.enc()}/${unitCode.enc()}"

    fun noteEditorRoute(unitId: String, noteId: String? = null) =
        if (noteId != null) "note_editor/${unitId.enc()}?noteId=${noteId.enc()}"
        else                "note_editor/${unitId.enc()}?noteId="

    fun paperPickRoute(unitId: String) = "paper_pick/${unitId.enc()}"

    fun pdfViewerRoute(title: String, filePath: String) =
        "pdf_viewer/${title.enc()}/${filePath.enc()}"

    private fun String.enc() = URLEncoder.encode(this, "UTF-8")
    fun String.dec()         = URLDecoder.decode(this, "UTF-8")
}

@Composable
fun TeesLibraryNavHost(navController: NavHostController) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // ── Splash ─────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(onSplashFinished = {
                val next = if (isOnboarded(context)) Routes.MAIN else Routes.ONBOARDING
                navController.navigate(next) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        // ── Onboarding ─────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        // ── Main shell (bottom nav) ────────────────────────────────────────────
        composable(Routes.MAIN) {
            MainScaffold(rootNavController = navController)
        }

        // ── Unit detail ────────────────────────────────────────────────────────
        composable(Routes.UNIT) { back ->
            val unitId   = back.arguments?.getString("unitId")?.dec()   ?: ""
            val unitName = back.arguments?.getString("unitName")?.dec() ?: ""
            val unitCode = back.arguments?.getString("unitCode")?.dec() ?: ""
            UnitDetailScreen(
                unitId     = unitId,
                unitName   = unitName,
                unitCode   = unitCode,
                onBack     = { navController.popBackStack() },
                onOpenNote = { note -> navController.navigate(Routes.noteEditorRoute(unitId, note.id)) },
                onAddNote  = { navController.navigate(Routes.noteEditorRoute(unitId)) },
                onAddPaper = { navController.navigate(Routes.paperPickRoute(unitId)) },
                onOpenPaper = { paper ->
                    navController.navigate(Routes.pdfViewerRoute(paper.title, paper.filePath))
                }
            )
        }

        // ── Note editor ────────────────────────────────────────────────────────
        composable(Routes.NOTE_EDITOR) { back ->
            val unitId = back.arguments?.getString("unitId")?.dec() ?: ""
            val noteId = back.arguments?.getString("noteId")?.dec()?.ifBlank { null }
            NoteEditorScreen(unitId = unitId, noteId = noteId, onBack = { navController.popBackStack() })
        }

        // ── Paper picker ───────────────────────────────────────────────────────
        composable(Routes.PAPER_PICK) { back ->
            val unitId = back.arguments?.getString("unitId")?.dec() ?: ""
            PaperPickerScreen(unitId = unitId, onBack = { navController.popBackStack() })
        }

        // ── PDF viewer ─────────────────────────────────────────────────────────
        composable(Routes.PDF_VIEWER) { back ->
            val title    = back.arguments?.getString("title")?.dec()    ?: ""
            val filePath = back.arguments?.getString("filePath")?.dec() ?: ""
            PdfViewerScreen(title = title, filePath = filePath, onBack = { navController.popBackStack() })
        }
    }
}