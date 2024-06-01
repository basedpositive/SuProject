package com.example.su.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object Home : Screen("home", Icons.Filled.Home, "Главный")
    object CategoryList : Screen("categoryList", Icons.AutoMirrored.Filled.List, "Категории")
    object Upload : Screen("uploadVideo", Icons.Filled.AddCircle, "Загрузить")
    object Profile : Screen("profile", Icons.Filled.Person, "Профиль")
}
