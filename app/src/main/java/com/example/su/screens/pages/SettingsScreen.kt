package com.example.su.screens.pages

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(preferences: SharedPreferences) {
    val isDarkTheme = remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }

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
                    }
                }
            )
        }
    }
}
