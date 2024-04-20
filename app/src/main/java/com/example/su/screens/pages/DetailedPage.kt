package com.example.su.screens.pages

import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.su.models.Video
import java.text.DateFormat
import java.util.Date
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import android.widget.FrameLayout
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.*
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue

@Composable
fun DetailedPage(videoId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val video = remember { mutableStateOf<Video?>(null) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    DisposableEffect(videoId) {
        val docRef = db.collection("videos").document(videoId)
        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("DetailedPage", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                video.value = snapshot.toObject(Video::class.java)?.apply { id = snapshot.id }
            }
        }

        onDispose {
            listener.remove() // Отписываемся от слушателя при уничтожении компонента
        }
    }

    video.value?.let { vid ->
        val isLiked = currentUser?.uid in vid.likedBy
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = vid.videoName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Просмотры: ${vid.views}")
            Text(text = "Лайки: ${vid.likes}")
            Button(onClick = {
                toggleLike(db, vid, currentUser)
            }) {
                Text(if (isLiked) "Убрать лайк" else "Поставить лайк")
            }
            VideoPlayer(videoUrl = vid.videoUrl, context = context)
        }
    } ?: Text("Загрузка видео...")
}


fun toggleLike(db: FirebaseFirestore, video: Video, currentUser: FirebaseUser?) {
    currentUser?.let { user ->
        val docRef = db.collection("videos").document(video.id)
        if (user.uid in video.likedBy) {
            docRef.update("likedBy", FieldValue.arrayRemove(user.uid))
            docRef.update("likes", FieldValue.increment(-1))
        } else {
            docRef.update("likedBy", FieldValue.arrayUnion(user.uid))
            docRef.update("likes", FieldValue.increment(1))
        }
    }
}


@Composable
fun VideoPlayer(videoUrl: String, context: Context) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )

    // Начать воспроизведение при готовности
    LaunchedEffect(exoPlayer) {
        exoPlayer.playWhenReady = true
    }
}



