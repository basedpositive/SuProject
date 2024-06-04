package com.example.su

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.su.models.Screen
import com.example.su.screens.CategoryListScreen
import com.example.su.screens.HomeScreen
import com.example.su.screens.ProfileScreen
import com.example.su.screens.UploadScreen
import com.example.su.screens.auth.LoginScreen
import com.example.su.screens.auth.RegistrationScreen
import com.example.su.screens.pages.DetailedPage
import com.example.su.screens.pages.FullScreenVideoPlayer
import com.example.su.screens.pages.PlaylistScreen
import com.example.su.screens.pages.SettingsScreen
import com.example.su.screens.pages.UserPageScreen
import com.example.su.ui.theme.SuTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val isDarkTheme = preferences.getBoolean("dark_theme", false)

        FirebaseApp.initializeApp(this)
        setContent {
            SuTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
}

@Composable
fun MainScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val navController = rememberNavController()
    val db = FirebaseFirestore.getInstance()
    val preferences = LocalContext.current.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        content = { innerPadding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
                composable("home") { HomeScreen(navController) }
                composable("categoryList") { CategoryListScreen(navController, db) }
                composable("uploadVideo") { UploadScreen(auth, navController) }
                composable("profile") { ProfileScreen(navController, auth = auth) }
                composable("login") { LoginScreen(auth) { navController.popBackStack() } }
                composable("registration") { RegistrationScreen(auth) { navController.popBackStack() } }
                composable("detailed/{videoId}", arguments = listOf(navArgument("videoId") { type = NavType.StringType })) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                    DetailedPage(videoId = videoId, navController)
                }
                composable("settings") { SettingsScreen(preferences, auth, navController) }
                composable("playlists/{userId}", arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    PlaylistScreen(navController, db, userId)
                }
                composable("fullscreen/{videoUrl}", arguments = listOf(navArgument("videoUrl") { type = NavType.StringType })) { backStackEntry ->
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    FullScreenVideoPlayer(videoUrl = videoUrl, navController, context = LocalContext.current)
                }
                composable("userPage/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    UserPageScreen(navController, userId, FirebaseFirestore.getInstance())
                }
            }
        }
    )
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.CategoryList,
        Screen.Upload,
        Screen.Profile
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.height(80.dp)
    ) {
        val currentRoute = currentRoute(navController)
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = if (isSelected) screen.filledIcon else screen.unfilledIcon),
                        contentDescription = null
                    )
                },
                label = { Text(screen.title) },
                alwaysShowLabel = false,
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                    indicatorColor = MaterialTheme.colorScheme.onError
                )
            )
        }
    }
}


@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

