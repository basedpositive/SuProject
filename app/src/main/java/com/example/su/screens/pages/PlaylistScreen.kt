package com.example.su.screens.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.R
import com.example.su.models.Playlist
import com.example.su.models.User
import com.example.su.models.Video
import com.example.su.screens.VideoItem
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


fun CollectionReference.snapshotFlow() = callbackFlow {
    val listenerRegistration = addSnapshotListener { snapshot, e ->
        if (e != null) {
            close(e)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot).isSuccess
        }
    }
    awaitClose { listenerRegistration.remove() }
}

fun DocumentReference.snapshotFlow() = callbackFlow {
    val listenerRegistration = addSnapshotListener { snapshot, e ->
        if (e != null) {
            close(e)
            return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
            trySend(snapshot).isSuccess
        }
    }
    awaitClose { listenerRegistration.remove() }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, db: FirebaseFirestore, userId: String) {
    val playlists = remember { mutableStateListOf<Playlist>() }
    val subscriptions = remember { mutableStateListOf<User>() }
    val videos = remember { mutableStateListOf<Video>() }

    LaunchedEffect(userId) {
        // Загружаем плейлисты
        launch {
            db.collection("users").document(userId).collection("playlists")
                .snapshotFlow()
                .collect { snapshot ->
                    playlists.clear()
                    for (document in snapshot.documents) {
                        val playlist = document.toObject(Playlist::class.java)?.apply {
                            id = document.id
                        }
                        if (playlist != null) {
                            playlists.add(playlist)
                            // Загружаем видео для каждого плейлиста
                            playlist.videos.forEach { videoId ->
                                val videoDoc = db.collection("videos").document(videoId).get().await()
                                val video = videoDoc.toObject(Video::class.java)?.apply { id = videoDoc.id }
                                if (video != null) {
                                    videos.add(video)
                                }
                            }
                        }
                    }
                }
        }

        // Загружаем подписки
        launch {
            db.collection("users").document(userId)
                .snapshotFlow()
                .collect { document ->
                    val subscriptionIds = document.get("subscriptions") as? List<String> ?: emptyList()
                    subscriptions.clear()
                    for (subscriptionId in subscriptionIds) {
                        val subDoc = db.collection("users").document(subscriptionId).get().await()
                        val user = subDoc.toObject(User::class.java)?.apply { id = subDoc.id }
                        if (user != null) {
                            subscriptions.add(user)
                        }
                    }
                }
        }

        // Загружаем все видео
        launch {
            db.collection("videos")
                .snapshotFlow()
                .collect { snapshot ->
                    videos.clear()
                    for (document in snapshot.documents) {
                        val video = document.toObject(Video::class.java)?.apply {
                            id = document.id
                        }
                        if (video != null) {
                            videos.add(video)
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Плейлисты и подписки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column {
                Text(
                    text = "Подписки",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyRow {
                    items(subscriptions) { user ->
                        SubscriptionItem(navController, user)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Плейлисты",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(playlist.videos) { videoId ->
                                val video = videos.find { it.id == videoId }
                                if (video != null) {
                                    VideoItem(video, navController)
                                } else {
                                    Text("Видео не найдено", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItem(navController: NavController, user: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = {
            navController.navigate("userPage/${user.id}")
        }) {
            Icon(modifier = Modifier.size(38.dp),
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "Профиль")
        }
        Text(text = if (user.username.length > 5) "${user.username.take(5)}..." else user.username, style = MaterialTheme.typography.titleSmall)
    }
}

