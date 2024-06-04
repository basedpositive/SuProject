package com.example.su.screens.pages

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(preferences: SharedPreferences, auth: FirebaseAuth, navController: NavController) {
    val isDarkTheme = remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbarReRun by remember { mutableStateOf(false) }

    LaunchedEffect(showSnackbarReRun) {
        if (showSnackbarReRun) {
            snackbarHostState.showSnackbar("Требуется перезагрузка приложения.")
            showSnackbarReRun = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Настройки", style = MaterialTheme.typography.headlineMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Темная тема")
                    Switch(
                        checked = isDarkTheme.value,
                        onCheckedChange = { checked ->
                            isDarkTheme.value = checked
                            with(preferences.edit()) {
                                putBoolean("dark_theme", checked)
                                apply()
                                showSnackbarReRun = true
                            }
                        }
                    )
                }
                LogoutButton(onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                })
            }
        }
    }

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(onClick = onLogout) {
        Text(text = "Выйти")
    }
}