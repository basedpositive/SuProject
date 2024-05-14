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
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import com.example.su.models.Playlist
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue

@Composable
fun DetailedPage(videoId: String, navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val video = remember { mutableStateOf<Video?>(null) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var showDialog by remember { mutableStateOf(false) }
    val isLiked = remember { mutableStateOf(false) }

    DisposableEffect(videoId) {
        val docRef = db.collection("videos").document(videoId)

        docRef.get().addOnSuccessListener { document ->
            video.value = document.toObject(Video::class.java)?.apply { id = document.id }
            docRef.update("views", FieldValue.increment(1))
            isLiked.value = currentUser?.uid in (document.get("likedBy") as? List<String> ?: listOf())
        }

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("DetailedPage", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                video.value = snapshot.toObject(Video::class.java)?.apply { id = snapshot.id }
                isLiked.value = currentUser?.uid in (snapshot.get("likedBy") as? List<String> ?: listOf())
            }
        }

        onDispose {
            listener.remove()
        }
    }

    video.value?.let { vid ->
        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            VideoPlayer(videoUrl = vid.videoUrl, context = context)
            Text(text = vid.videoName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Просмотры: ${vid.views}")
            Text(text = "Лайки: ${vid.likes}")
            Button(onClick = {
                toggleLike(db, vid, currentUser)
            }) {
                Text(if (isLiked.value) "Убрать лайк" else "Поставить лайк")
            }
            Button(onClick = { showDialog = true }) {
                Text("Добавить в плейлист")
            }
        }
    } ?: Text("Загрузка видео...")

    if (showDialog) {
        PlaylistDialog(
            db = db,
            user = currentUser,
            video = video.value!!,
            onDismissRequest = { showDialog = false }
        )
    }
}


fun toggleLike(db: FirebaseFirestore, video: Video, user: FirebaseUser?) {
    if (user == null) return
    val videoRef = db.collection("videos").document(video.id)
    val userRef = db.collection("users").document(user.uid)

    db.runTransaction { transaction ->
        val videoSnapshot = transaction.get(videoRef)
        val userSnapshot = transaction.get(userRef)
        val likedBy = videoSnapshot.get("likedBy") as List<String>
        val likedVideos = userSnapshot.get("likedVideos") as? List<String> ?: listOf()

        if (user.uid in likedBy) {
            // Убираем лайк
            transaction.update(videoRef, "likedBy", FieldValue.arrayRemove(user.uid))
            transaction.update(videoRef, "likes", FieldValue.increment(-1))
            transaction.update(userRef, "likedVideos", FieldValue.arrayRemove(video.id))
        } else {
            // Ставим лайк
            transaction.update(videoRef, "likedBy", FieldValue.arrayUnion(user.uid))
            transaction.update(videoRef, "likes", FieldValue.increment(1))
            transaction.update(userRef, "likedVideos", FieldValue.arrayUnion(video.id))
        }
    }.addOnSuccessListener {
        Log.d("toggleLike", "Transaction success!")
    }.addOnFailureListener { e ->
        Log.w("toggleLike", "Transaction failure.", e)
    }
}


@Composable
fun PlaylistDialog(
    db: FirebaseFirestore,
    user: FirebaseUser?,
    video: Video,
    onDismissRequest: () -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isCreatingNewPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            db.collection("users").document(it.uid).collection("playlists").get()
                .addOnSuccessListener { snapshot ->
                    playlists = snapshot.documents.mapNotNull { document ->
                        document.toObject(Playlist::class.java)?.apply { id = document.id }
                    }
                }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Gray,
            contentColor = contentColorFor(backgroundColor = Color.Green)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Выберите плейлист или создайте новый", style = MaterialTheme.typography.titleMedium)
                if (isCreatingNewPlaylist) {
                    TextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Название нового плейлиста") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        user?.let {
                            val newPlaylist = mapOf(
                                "name" to playlistName,
                                "videos" to mapOf<String, Map<String, String>>()
                            )
                            db.collection("users").document(it.uid).collection("playlists")
                                .add(newPlaylist)
                                .addOnSuccessListener { documentReference ->
                                    val createdPlaylist = Playlist(
                                        id = documentReference.id,
                                        name = playlistName,
                                        videos = mapOf()
                                    )
                                    playlists = playlists + createdPlaylist
                                    isCreatingNewPlaylist = false
                                    playlistName = ""
                                }
                        }
                    }) {
                        Text("Сохранить")
                    }
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addVideoToPlaylist(db, user, playlist.id, video)
                                        onDismissRequest()
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(text = playlist.name)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { isCreatingNewPlaylist = true }) {
                        Text("Создать новый плейлист")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismissRequest) {
                    Text("Отмена")
                }
            }
        }
    }
}

fun addVideoToPlaylist(db: FirebaseFirestore, user: FirebaseUser?, playlistId: String, video: Video) {
    if (user == null) return
    val playlistRef = db.collection("users").document(user.uid).collection("playlists").document(playlistId)
    val videoData = mapOf(
        "videoUrl" to video.videoUrl,
        "videoName" to video.videoName,
    )
    playlistRef.update("videos.${video.id}", videoData)
        .addOnSuccessListener {
            Log.d("PlaylistDialog", "Video added to playlist successfully.")
        }
        .addOnFailureListener {
            Log.e("PlaylistDialog", "Error adding video to playlist", it)
        }
}



@OptIn(UnstableApi::class) @Composable
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
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                controllerAutoShow = true
            }
        },
        modifier = Modifier.fillMaxWidth().height(238.dp)
    )

    // Начать воспроизведение при готовности
    LaunchedEffect(exoPlayer) {
        exoPlayer.playWhenReady = true
    }
}



