package com.example.su.screens.pages

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.su.R
import com.example.su.models.Playlist
import com.example.su.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun DetailedPage(videoId: String, navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val video = remember { mutableStateOf<Video?>(null) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var showDialog by remember { mutableStateOf(false) }
    val isLiked = remember { mutableStateOf(false) }
    val isPlaylisted = remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    val isSubscribed = remember { mutableStateOf(false) }

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
                val likedByList = snapshot.get("likedBy") as? List<String>
                if (likedByList == null) {
                    // Если поле "likedBy" не существует, создаём его
                    db.collection("videos").document(snapshot.id)
                        .update("likedBy", listOf<String>())
                        .addOnSuccessListener {
                            isLiked.value = currentUser?.uid in (snapshot.get("likedBy") as? List<String> ?: listOf())
                        }
                        .addOnFailureListener {}
                } else {
                    // Поле "likedBy" существует
                    isLiked.value = currentUser?.uid in likedByList
                }

                video.value = snapshot.toObject(Video::class.java)?.apply { id = snapshot.id }
            }
        }

        onDispose {
            listener.remove()
        }
    }

    LaunchedEffect(videoId) {
        // Инициализируем состояние подписки
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { document ->
                val subscriptions = document.get("subscriptions") as? List<String> ?: emptyList()
                isSubscribed.value = video.value?.userId in subscriptions
            }
        }
    }

    video.value?.let { vid ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VideoPlayer(videoUrl = vid.videoUrl, navController, context = context)

            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = vid.videoName, style = MaterialTheme.typography.titleLarge)

                Box(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                    Row {
                        Text(
                            text = "Описание: ${vid.description}",
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Row {
                    Icon(painter = painterResource(id = R.drawable.view), contentDescription = "view icon")
                    Text(text = "${vid.views}", style = MaterialTheme.typography.titleMedium)
                    Icon(painter = painterResource(id = R.drawable.thumb_up), contentDescription = "thumb_up")
                    Text(text = "${vid.likes}")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        IconButton(onClick = {
                            navController.navigate("userPage/${vid.userId}")
                        }) {
                            Icon(painter = painterResource(id = R.drawable.profile), contentDescription = "Профиль")
                        }
                    }
                    Row {
                        Button(onClick = {
                            currentUser?.uid?.let { uid ->
                                val userDocRef = db.collection("users").document(uid)
                                val videoOwnerDocRef = db.collection("users").document(vid.userId)

                                if (isSubscribed.value) {
                                    // Отписаться
                                    userDocRef.update("subscriptions", FieldValue.arrayRemove(vid.userId))
                                    videoOwnerDocRef.update("subscribers", FieldValue.arrayRemove(uid))
                                } else {
                                    // Подписаться
                                    userDocRef.update("subscriptions", FieldValue.arrayUnion(vid.userId))
                                    videoOwnerDocRef.update("subscribers", FieldValue.arrayUnion(uid))
                                }

                                isSubscribed.value = !isSubscribed.value
                            }
                        },
                            modifier = Modifier.border(
                                border = if (isSubscribed.value) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent)
                            )
                        ) {
                            Text(if (isSubscribed.value) "Отписаться" else "Подписаться")
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {
                            toggleLike(db, vid, currentUser)
                        }) {
                            if (isLiked.value) {
                                Column {
                                    Icon(modifier = Modifier.size(38.dp), painter = painterResource(id = R.drawable.liked), contentDescription = "unliked press")
                                }
                            } else {
                                Column {
                                    Icon(modifier = Modifier.size(38.dp), painter = painterResource(id = R.drawable.unliked), contentDescription = "liked press")
                                }
                            }
                        }
                        if (isLiked.value) {
                            Text(text = "Убрать", style = MaterialTheme.typography.titleSmall)
                        } else {
                            Text(text = "Нрав.", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Column {
                        IconButton(
                            onClick = { showDialog = true }
                        ) {
                            if (isPlaylisted.value) { /* не работает */
                                Column {
                                    Icon(modifier = Modifier.size(38.dp), painter = painterResource(id = R.drawable.category_filled), contentDescription = "unliked press")
                                }
                            } else {
                                Column {
                                    Icon(modifier = Modifier.size(38.dp), painter = painterResource(id = R.drawable.add_category), contentDescription = "liked press")
                                }
                            }
                        }
                        /* if (isPlaylisted.value) { /* не работает */
                            Text(text = "В плейлист", style = MaterialTheme.typography.titleSmall)
                        } else {
                            Text(text = "В плейлист", style = MaterialTheme.typography.titleSmall)
                        } */
                    }
                }
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



fun toggleSub(db: FirebaseFirestore, video: Video, user: FirebaseUser?) {
    if (user == null) return
    val videoRef = db.collection("videos").document(video.id)
    val userRef = db.collection("users").document(user.uid)

    db.runTransaction { transaction ->
        val videoSnapshot = transaction.get(videoRef)
        val userSnapshot = transaction.get(userRef)
        val subscribeTo = userSnapshot.get("likedVideos") as? List<String> ?: listOf()

        if (user.uid in subscribeTo) {
            // Подписка
            transaction.update(userRef, "subscribeTo", FieldValue.arrayRemove(video.id))
        } else {
            // Отписка
            transaction.update(userRef, "subscribeTo", FieldValue.arrayUnion(video.id))
        }
    }.addOnSuccessListener {
        Log.d("toggleSub", "Transaction success!")
    }.addOnFailureListener { e ->
        Log.w("toggleSub", "Transaction failure.", e)
    }
}

fun toggleLike(db: FirebaseFirestore, video: Video, user: FirebaseUser?) {
    if (user == null) return
    val videoRef = db.collection("videos").document(video.id)
    val userRef = db.collection("users").document(user.uid)

    db.runTransaction { transaction ->
        val videoSnapshot = transaction.get(videoRef)
        val userSnapshot = transaction.get(userRef)
        val likedBy = videoSnapshot.get("likedBy") as List<String> ?: listOf()
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
                    Box(
                        modifier = Modifier
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = playlistName,
                            onValueChange = { playlistName= it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black),
                            decorationBox = { innerTextField ->
                                if (playlistName.isEmpty()) {
                                    Text(
                                        text = "Название нового плейлиста",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
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
                                Box(
                                    modifier = Modifier
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                        Text(text = playlist.name)
                                }
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
fun VideoPlayer(videoUrl: String, navController: NavController, context: Context) {
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
    ) {
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
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = {
                navController.navigate("fullscreen/${Uri.encode(videoUrl)}")
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Enter Fullscreen")
        }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.playWhenReady = true
    }
}


