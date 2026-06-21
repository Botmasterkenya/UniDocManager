package com.example.mylibrary.ui.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.mylibrary.data.CourseUnitEntity
import com.example.mylibrary.data.LibraryRepository
import com.example.mylibrary.navigation.Routes
import com.example.mylibrary.ui.home.HomeScreen
import com.example.mylibrary.viewmodel.HomeViewModel
import com.example.mylibrary.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Colors ────────────────────────────────────────────────────────────────────
private val DarkBg        = Color(0xFF141414)
private val BottomBarBg   = Color(0xFF1A1A1A)
private val NetflixRed    = Color(0xFFE50914)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF999999)
private val CardBg        = Color(0xFF1F1F1F)
private val NoteBlue      = Color(0xFF4A90D9)
private val FolderAmber   = Color(0xFFE8A838)

// ── Bottom nav destinations ───────────────────────────────────────────────────
sealed class BottomNavItem(
    val route:         String,
    val label:         String,
    val selectedIcon:  ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home   : BottomNavItem("tab_home",   "Home",   Icons.Filled.Home,        Icons.Outlined.Home)
    object Search : BottomNavItem("tab_search", "Search", Icons.Filled.Search,       Icons.Outlined.Search)
    object Recent : BottomNavItem("tab_recent", "Recent", Icons.Filled.AccessTime,   Icons.Outlined.AccessTime)
    object Profile: BottomNavItem("tab_profile","Profile",Icons.Filled.Person,       Icons.Outlined.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Search,
    BottomNavItem.Recent,
    BottomNavItem.Profile
)

// ── MainScaffold — wraps everything with the bottom bar ──────────────────────
@Composable
fun MainScaffold(rootNavController: NavHostController) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = BottomBarBg,
                tonalElevation = 0.dp,
                modifier       = Modifier.height(64.dp)
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            if (!selected) {
                                tabNavController.navigate(item.route) {
                                    popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier           = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(item.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = NetflixRed,
                            selectedTextColor   = NetflixRed,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor      = NetflixRed.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = tabNavController,
            startDestination = BottomNavItem.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(onOpenUnit = { unit ->
                    rootNavController.navigate(Routes.unitRoute(unit.id, unit.name, unit.code))
                })
            }
            composable(BottomNavItem.Search.route) {
                SearchTab(onOpenUnit = { unit ->
                    rootNavController.navigate(Routes.unitRoute(unit.id, unit.name, unit.code))
                })
            }
            composable(BottomNavItem.Recent.route) {
                RecentTab()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileTab()
            }
        }
    }
}

// ── Search Tab ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(onOpenUnit: (CourseUnitEntity) -> Unit) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as Application)
    )
    val allUnits by vm.units.collectAsState()
    var query    by remember { mutableStateOf("") }

    val filtered = remember(query, allUnits) {
        if (query.isBlank()) allUnits
        else allUnits.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(16.dp)
    ) {
        Text("Search", color = NetflixRed, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Search units, notes...", color = TextSecondary) },
            leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon  = {
                if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                }
            },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = NetflixRed,
                unfocusedBorderColor = Color(0xFF2A2A2A),
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = NetflixRed,
                focusedContainerColor   = CardBg,
                unfocusedContainerColor = CardBg
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text  = if (query.isBlank()) "No units added yet" else "No results for \"$query\"",
                        color = TextSecondary, fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { unit ->
                    SearchResultCard(unit = unit, onClick = { onOpenUnit(unit) })
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(unit: CourseUnitEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FolderAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text("📂", fontSize = 22.sp) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(unit.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (unit.code.isNotBlank())
                Text(unit.code.uppercase(), color = NetflixRed, fontSize = 12.sp, letterSpacing = 1.sp)
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = TextSecondary)
        }
    }
}

// ── Recent Tab ────────────────────────────────────────────────────────────────
@Composable
fun RecentTab() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var recentNotes by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            val repo  = LibraryRepository(context)
            val units = repo.allUnits.first()
            val notes = mutableListOf<Pair<String, Long>>()
            units.forEach { unit ->
                repo.notesFor(unit.id).first().forEach { note ->
                    notes.add(Pair("${note.title}  ·  ${unit.code.ifBlank { unit.name }}", note.updatedAt))
                }
            }
            recentNotes = notes.sortedByDescending { it.second }.take(20)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).padding(16.dp)
    ) {
        Text("Recent", color = NetflixRed, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (recentNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕐", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No recent activity yet", color = TextSecondary, fontSize = 14.sp)
                    Text("Start writing notes to see them here", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentNotes) { (label, timestamp) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(NoteBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Text("📝", fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp)),
                                color = TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}