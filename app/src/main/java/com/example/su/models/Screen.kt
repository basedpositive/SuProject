package com.example.su.models

import com.example.su.R

sealed class Screen(val route: String, val filledIcon: Int, val unfilledIcon: Int, val title: String) {
    object Home : Screen("home", R.drawable.home_filled, R.drawable.home, "Главная")
    object CategoryList : Screen("categoryList", R.drawable.category_filled, R.drawable.category, "Категории")
    object Upload : Screen("uploadVideo", R.drawable.upload, R.drawable.upload, "Загрузить")
    object Profile : Screen("profile", R.drawable.profile, R.drawable.profile, "Профиль")
}
