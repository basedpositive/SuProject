package com.example.su.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.su.models.Video
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.su.R
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
fun HomeScreen() {
    val videos = remember { mutableStateListOf<Video>() }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        // Используйте snapshotFlow
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
            VideoItem(video)
        }
    }
}

@Composable
fun VideoItem(video: Video) {
    Row(modifier = Modifier.padding(8.dp)) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = video.previewUrl).apply(block = fun ImageRequest.Builder.() {
                    crossfade(true)
                    // placeholder(R.drawable.placeholder)
                    // error(R.drawable.error)
                }).build()
            ),
            contentDescription = "Превью видео",
            modifier = Modifier
                .size(100.dp, 100.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically)) {
            Text(text = video.videoName, fontWeight = FontWeight.Bold)
            Text(text = "Загружено: ${video.userName ?: "Неизвестный пользователь"}")
        }
    }
}