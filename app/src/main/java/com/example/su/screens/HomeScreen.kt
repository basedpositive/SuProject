package com.example.su.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.placeholder
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.su.models.Video
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.su.R
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch



fun FirebaseFirestore.snapshotFlow(collectionPath: String): Flow<QuerySnapshot> = callbackFlow {
    val listener = collection(collectionPath).addSnapshotListener { snapshot, e ->
        if (e != null) {
            close(e)
        } else if (snapshot != null) {
            trySend(snapshot).isSuccess
        }
    }
    awaitClose { listener.remove() }
}

@Composable
fun HomeScreen(navController: NavController) {
    val videos = remember { mutableStateListOf<Video>() }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        launch {
            db.snapshotFlow("videos").collect { snapshot ->
                videos.clear()
                snapshot.documents.forEach { document ->
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
        }
    }

    LazyColumn {
        items(videos) { video ->
            VideoItem(video, navController)
        }
    }
}

@Composable
fun VideoItem(video: Video, navController: NavController) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(data = video.previewUrl).apply {
            crossfade(true)
        }.build()
    )
    val isLoading = painter.state is AsyncImagePainter.State.Loading

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { navController.navigate("detailed/${video.id}") }
    ) {
        Image(
            painter = painter,
            contentDescription = "Превью видео",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .height(238.dp)
                .placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
        )
        Column(modifier = Modifier
            .padding(start = 8.dp)) {
            Text(
                text = video.videoName,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
            )
            Text(
                text = video.userName ?: "Гость",
                modifier = Modifier.placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


