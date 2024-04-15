package com.example.su

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.su.models.Screen
import com.example.su.screens.CategoryListScreen
import com.example.su.screens.HomeScreen
import com.example.su.screens.ProfileScreen
import com.example.su.screens.UploadScreen
import com.example.su.screens.auth.LoginScreen
import com.example.su.screens.auth.RegistrationScreen
import com.example.su.ui.theme.SuTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            SuTheme {
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
    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        content = { innerPadding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
                composable("home") { HomeScreen() }
                composable("categoryList") { CategoryListScreen() }
                composable("uploadVideo") { UploadScreen(auth, navController) }
                composable("profile") { ProfileScreen(navController, auth = auth) }
                composable("login") { LoginScreen(auth) { navController.popBackStack() } }
                composable("registration") { RegistrationScreen(auth) { navController.popBackStack() } }
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
    NavigationBar {
        val currentRoute = currentRoute(navController)
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}
