package com.example.su.screens.pages

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.models.User
import com.example.su.models.Video
import com.example.su.screens.VideoItem
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserPageScreen(navController: NavController, userId: String, db: FirebaseFirestore) {
    val user = remember { mutableStateOf<User?>(null) }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                user.value = document.toObject(User::class.java)?.apply { id = document.id }
            }
    }

    user.value?.let { u ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp)
            )
            Text(text = u.username, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            UserVideosViewPage(navController = navController, userId = userId, db = db)
        }
    } ?: Text("Загрузка профиля...")
}

@Composable
fun UserVideosViewPage(navController: NavController, userId: String, db: FirebaseFirestore) {
    val videos = remember { mutableStateListOf<Video>() }

    LaunchedEffect(userId) {
        db.collection("videos").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                val tempVideos = mutableListOf<Video>()
                snapshot.documents.forEach { document ->
                    val video = document.toObject(Video::class.java)?.apply {
                        id = document.id
                        userName = "Загружается..." // Инициализируем временное значение
                    }
                    video?.let { tempVideos.add(it) }
                }
                Log.d("Firestore", "Видео загружены: ${tempVideos.size}")

                tempVideos.forEach { video ->
                    db.collection("users").document(video.userId).get()
                        .addOnSuccessListener { userDoc ->
                            video.userName = userDoc.getString("username") ?: "Неизвестный пользователь"
                            videos.add(video)
                            Log.d("Firestore", "Видео добавлено: ${video.id}")
                        }
                }
            }
    }

    LazyColumn {
        items(videos) { video ->
            VideoItem(video, navController)
        }
    }
}

