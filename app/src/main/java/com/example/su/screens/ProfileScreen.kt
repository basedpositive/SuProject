package com.example.su.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController, auth: FirebaseAuth) {
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    username = document.getString("username") ?: "Аноним"
                    email = user.email ?: "Не указан"
                }
                .addOnFailureListener {
                    username = "Ошибка загрузки данных"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize().padding(vertical = 8.dp)
    ) {
        if (user != null) {
            ProfileHeader(username = username, email = email, navController, userId = user.uid)
            ProfileContent(navController = navController, auth = auth, db = db)
            LogoutButton(onLogout = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            })
        } else {
            SignInButtons(navController = navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Previews(){

}

@Composable
fun ProfileHeader(username: String, email: String, navController: NavController, userId: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Добавляем этот параметр для равномерного распределения пространства
    ) {
        Column(
            modifier = Modifier.weight(1f) // Этот модификатор заставит Column занять все доступное пространство, кроме места для иконки
                .padding(start = 16.dp)
        ) {
            Text(text = username, style = MaterialTheme.typography.headlineMedium)
            Text(text = email)
        }
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Playlist Picture",
            modifier = Modifier
                .size(25.dp)
                .clickable { navController.navigate("playlists/$userId") }
        )
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings Picture",
            modifier = Modifier
                .size(25.dp)
                .clickable { navController.navigate("settings") }
        )
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Profile Picture",
            modifier = Modifier.size(90.dp)
        )
    }
}

@Composable
fun ProfileContent(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    var activeTab by remember { mutableStateOf("Подписки") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabButton(
                text = "Подписки",
                isActive = activeTab == "Подписки",
                onClick = { activeTab = "Подписки" }
            )
            TabButton(
                text = "Мои видео",
                isActive = activeTab == "Мои видео",
                onClick = { activeTab = "Мои видео" }
            )
        }

        // Контент в зависимости от выбранной вкладки
        when (activeTab) {
            "Подписки" -> LikedVideosView(navController, auth, db)
            "Мои видео" -> UserVideosView(navController, auth, db)
        }
    }
}

@Composable
fun LikedVideosView(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val user = auth.currentUser
    val videos = remember { mutableStateListOf<Video>() }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                val likedVideos = document.get("likedVideos") as? List<String> ?: listOf()
                likedVideos.forEach { videoId ->
                    db.collection("videos").document(videoId).get().addOnSuccessListener { videoDoc ->
                        videoDoc.toObject(Video::class.java)?.let { video ->
                            video.id = videoDoc.id
                            video.userName = "Загружается..." // Инициализируем временное значение
                            db.collection("users").document(video.userId).get().addOnSuccessListener { userDoc ->
                                video.userName = userDoc.getString("username") ?: "Неизвестный пользователь"
                                videos.add(video)
                            }
                        }
                    }
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

@Composable
fun TabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color.LightGray else Color.Gray
        )
    ) {
        Text(text)
    }
}

@Composable
fun UserVideosView(navController: NavController, auth: FirebaseAuth, db: FirebaseFirestore) {
    val user = auth.currentUser
    val videos = remember { mutableStateListOf<Video>() }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            db.collection("videos").whereEqualTo("userId", userId).get().addOnSuccessListener { snapshot ->
                videos.clear()
                snapshot.documents.forEach { document ->
                    document.toObject(Video::class.java)?.let { video ->
                        video.id = document.id
                        video.userName = "Загружается..." // Инициализируем временное значение
                        db.collection("users").document(video.userId).get().addOnSuccessListener { userDoc ->
                            video.userName = userDoc.getString("username") ?: "Неизвестный пользователь"
                            videos.add(video)
                        }
                    }
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


@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(onClick = onLogout) {
        Text(text = "Выйти")
    }
}

@Composable
fun SignInButtons(navController: NavController) {
    Button(onClick = { navController.navigate("login") }) {
        Text(text = "Войти")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { navController.navigate("registration") }) {
        Text(text = "Регистрация")
    }
}


