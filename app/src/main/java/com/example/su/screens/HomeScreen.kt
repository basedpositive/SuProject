package com.example.su.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.su.R
import com.example.su.models.Video
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random


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
    val filteredVideos = remember { mutableStateListOf<Video>() }
    val db = FirebaseFirestore.getInstance()

    val searchQuery = remember { mutableStateOf("") }
    val isSearchVisible = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val sortOptions = listOf("Просмотры", "Дата загрузки", "Лайки", "Популярность")
    val selectedSortOption = remember { mutableStateOf(sortOptions[0]) }
    var expanded by remember { mutableStateOf(false) }

    val bloggerSansFamily = FontFamily(
        Font(R.font.blogger_sans, FontWeight.Normal),
        Font(R.font.bloggersans_bold, FontWeight.Bold),
    )

    LaunchedEffect(Unit) {
        launch {
            db.snapshotFlow("videos").collect { snapshot ->
                videos.clear()
                snapshot.documents.forEach { document ->
                    val video = document.toObject(Video::class.java)?.apply {
                        id = document.id
                        userName = "Загружается..."
                    }
                    if (video != null) {
                        val userDoc = db.collection("users").document(video.userId).get().await()
                        video.userName = userDoc.getString("username") ?: "Неизвестный пользователь"
                        videos.add(video)
                    }
                }
                videos.shuffle(Random(System.currentTimeMillis()))
                updateFilteredVideos(searchQuery.value, videos, filteredVideos, selectedSortOption.value)
            }
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        if (isSearchVisible.value) {
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { query ->
                    searchQuery.value = query
                    updateFilteredVideos(query, videos, filteredVideos, selectedSortOption.value)
                },
                label = { Text("Что ищите?") },
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = { isSearchVisible.value = false }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "search icon")
                    }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        isSearchVisible.value = false
                    }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "close search")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
        } else {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Смотри, учись и развивайся",
                        fontFamily = bloggerSansFamily,
                        fontWeight = FontWeight.Normal
                    )
                    IconButton(onClick = { isSearchVisible.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "search icon"
                        )
                    }
                }
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)) {
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Сортировать по:")
                    Box {
                        Text(
                            text = selectedSortOption.value,
                            modifier = Modifier
                                .clickable { expanded = true }
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedSortOption.value = option
                                        expanded = false
                                        updateFilteredVideos(searchQuery.value, videos, filteredVideos, option)
                                    },
                                    text = { Text(option) }
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyColumn {
            items(filteredVideos) { video ->
                VideoItem(video, navController)
            }
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
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable { navController.navigate("detailed/${video.id}") }
            .background(MaterialTheme.colorScheme.surface)
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
        Row(
            modifier = Modifier
            .padding(start = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "channel picture"
            )
            Column(modifier = Modifier
                .padding(start = 8.dp)) {
                Text(
                    text = video.videoName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
                )
                Text(
                    text = video.userName ?: "Гость",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun updateFilteredVideos(query: String, videos: List<Video>, filteredVideos: SnapshotStateList<Video>, sortOption: String) {
    filteredVideos.clear()
    val lowerCaseQuery = query.lowercase()
    val filtered = videos.filter { video ->
        video.videoName.lowercase().contains(lowerCaseQuery) || video.description.lowercase().contains(lowerCaseQuery)
    }

    filteredVideos.addAll(
        when (sortOption) {
            "Просмотры" -> filtered.sortedByDescending { it.views }
            "Дата загрузки" -> filtered.sortedByDescending { it.dateUploaded }
            "Лайки" -> filtered.sortedByDescending { it.likes }
            "Популярность" -> filtered.sortedByDescending { calculatePopularity(it) }
            else -> filtered.shuffled()
        }
    )
}

private fun calculatePopularity(video: Video): Double {
    // Формула для расчета популярности (пример)
    val viewsWeight = 0.4
    val likesWeight = 0.4
    val commentsWeight = 0.2

    return (viewsWeight * video.views) + (likesWeight * video.likes)
}



