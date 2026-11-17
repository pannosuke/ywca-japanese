package dev.ericallerton.yoruben.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.ericallerton.yoruben.YorubenApp
import dev.ericallerton.yoruben.ui.adventure.CaveScreen
import dev.ericallerton.yoruben.ui.adventure.DrillScreen
import dev.ericallerton.yoruben.ui.adventure.MapScreen
import dev.ericallerton.yoruben.ui.adventure.RunScreen
import dev.ericallerton.yoruben.ui.adventure.ShopScreen
import dev.ericallerton.yoruben.ui.adventure.SongsScreen
import dev.ericallerton.yoruben.ui.adventure.VillageScreen
import dev.ericallerton.yoruben.ui.adventure.WardrobeScreen
import dev.ericallerton.yoruben.ui.lessons.LessonDetailScreen
import dev.ericallerton.yoruben.ui.lessons.LessonsScreen
import dev.ericallerton.yoruben.ui.practice.PracticeHome
import dev.ericallerton.yoruben.ui.practice.QuizScreen
import dev.ericallerton.yoruben.ui.prep.PrepScreen
import dev.ericallerton.yoruben.ui.progress.ProgressScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("lessons", "レッスン", Icons.Filled.List),
    Tab("practice", "れんしゅう", Icons.Filled.Edit),
    Tab("map", "もり", Icons.Filled.Home),
    Tab("progress", "すすみ", Icons.Filled.Star),
    Tab("prep", "じゅんび", Icons.Filled.DateRange),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YorubenRoot(startRoute: String) {
    val context = LocalContext.current
    val curriculum = (context.applicationContext as YorubenApp).curriculum
    val nav = rememberNavController()
    LaunchedEffect(startRoute) {
        val current = nav.currentBackStackEntry?.destination?.route
        if (current == null || current == startRoute) return@LaunchedEffect
        nav.navigate(startRoute) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBar = route in tabs.map { it.route }
    val title = when {
        route.startsWith("lesson/") -> "レッスン"
        route.startsWith("quiz/") || route.startsWith("drill") -> "ドリル"
        route.startsWith("run") -> "ぼうけん"
        route == "shop" || route == "wardrobe" -> "みせ"
        route == "songs" -> "ふえ"
        route == "cave" || route == "village" -> "もり"
        else -> "よるべん"
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (!showBar) {
                        TextButton(onClick = { nav.popBackStack() }) {
                            Text("もどる")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                "lessons",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/lessons" }),
            ) {
                LessonsScreen(curriculum) { date -> nav.navigate("lesson/$date") }
            }
            composable(
                "lesson/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/lesson/{date}" }),
            ) { entry ->
                LessonDetailScreen(curriculum, entry.arguments?.getString("date").orEmpty())
            }
            composable(
                "practice",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/practice" }),
            ) {
                PracticeHome { mode -> nav.navigate("quiz/$mode") }
            }
            composable(
                "quiz/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/quiz/{mode}" }),
            ) { entry ->
                QuizScreen(entry.arguments?.getString("mode") ?: "mix")
            }
            composable(
                "progress",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/progress" }),
            ) { ProgressScreen() }
            composable(
                "prep",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/prep" }),
            ) { PrepScreen() }
            composable(
                "map",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/map" }),
            ) {
                MapScreen(
                    curriculum,
                    onVillage = { nav.navigate("village") },
                    onCave = { nav.navigate("cave") },
                    onSongs = { nav.navigate("songs") },
                    onRegion = { date -> nav.navigate("drill/region-$date") },
                )
            }
            composable("village") {
                VillageScreen(
                    onShop = { nav.navigate("shop") },
                    onWardrobe = { nav.navigate("wardrobe") },
                    onRun = { nav.navigate("run") },
                )
            }
            composable("cave") {
                CaveScreen(
                    onDrill = { mode -> nav.navigate("drill/$mode") },
                    onRun = { nav.navigate("run") },
                    onSongs = { nav.navigate("songs") },
                )
            }
            composable(
                "drill/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/drill/{mode}" }),
            ) { entry ->
                DrillScreen(entry.arguments?.getString("mode") ?: "group1")
            }
            composable(
                "run",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/run" }),
            ) { RunScreen("play") }
            composable(
                "run/{demo}",
                arguments = listOf(navArgument("demo") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/run/{demo}" }),
            ) { entry ->
                RunScreen(entry.arguments?.getString("demo") ?: "play")
            }
            composable(
                "shop",
                deepLinks = listOf(navDeepLink { uriPattern = "yoruben://open/shop" }),
            ) { ShopScreen(onWardrobe = { nav.navigate("wardrobe") }) }
            composable("wardrobe") { WardrobeScreen() }
            composable("songs") { SongsScreen() }
        }
    }
}
