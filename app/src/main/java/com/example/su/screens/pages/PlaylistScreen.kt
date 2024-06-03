package com.example.su.screens.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.models.Playlist
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, db: FirebaseFirestore, userId: String) {
    val playlists = remember { mutableStateListOf<Playlist>() }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("playlists").get()
            .addOnSuccessListener { snapshot ->
                playlists.clear()
                for (document in snapshot.documents) {
                    val playlist = document.toObject(Playlist::class.java)?.apply {
                        id = document.id
                    }
                    if (playlist != null) {
                        playlists.add(playlist)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Библеотека") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(playlist)
                Divider()
            }
        }
    }
}

@Composable
fun PlaylistItem(playlist: Playlist) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = playlist.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Количество видео: ${playlist.videos}")
    }
}

