package com.example.su.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.models.Video
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun CategoryListScreen(navController: NavController, db: FirebaseFirestore) {
    val categories = listOf("Без категорий", "Образование", "Музыка", "Спорт", "Технологии", "Хобби")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEach { category ->
                CategoryTab(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }
        CategoryVideosView(category = selectedCategory, navController = navController, db = db)
    }
}

@Composable
fun CategoryTab(category: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.LightGray else Color.Gray
        ),
        modifier = Modifier.padding(8.dp)
    ) {
        Text(text = category)
    }
}
@Composable
fun CategoryVideosView(category: String, navController: NavController, db: FirebaseFirestore) {
    val videos = remember { mutableStateListOf<Video>() }

    LaunchedEffect(category) {
        val videoDocuments = db.collection("videos")
            .whereEqualTo("category", category)
            .get()
            .await()

        videos.clear()
        videoDocuments.documents.forEach { document ->
            val video = document.toObject(Video::class.java)?.apply {
                id = document.id
                userName = "Загружается..." // Инициализируем временное значение
            }
            if (video != null) {
                val userDoc = db.collection("users").document(video.userId).get().await()
                video.userName = userDoc.getString("username") ?: "Неизвестный пользователь"
                videos.add(video)
            }
        }
    }

    Column {
        LazyColumn {
            items(videos) { video ->
                VideoItem(video, navController)
            }
        }
    }
}

