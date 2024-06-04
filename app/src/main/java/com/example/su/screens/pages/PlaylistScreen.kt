package com.example.su.screens.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.R
import com.example.su.models.Playlist
import com.example.su.models.User
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, db: FirebaseFirestore, userId: String) {
    val playlists = remember { mutableStateListOf<Playlist>() }
    val subscriptions = remember { mutableStateListOf<User>() }

    LaunchedEffect(userId) {
        // Загрузка плейлистов
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

        // Загрузка подписок
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val subscriptionIds = document.get("subscriptions") as? List<String> ?: emptyList()
                for (subscriptionId in subscriptionIds) {
                    db.collection("users").document(subscriptionId).get()
                        .addOnSuccessListener { subDoc ->
                            val user = subDoc.toObject(User::class.java)?.apply { id = subDoc.id }
                            if (user != null) {
                                subscriptions.add(user)
                            }
                        }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Библиотека") },
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
                Text(
                    text = "Подписки",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyRow() {
                    items(subscriptions) { user ->
                        SubscriptionItem(navController, user)
                    }
                }
            Column() {
                Text(
                    text = "Плейлисты",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(playlists) { playlist ->
                        PlaylistItem(playlist)
                        Divider()
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
            Icon(painter = painterResource(id = R.drawable.profile), contentDescription = "Профиль")
        }
        Text(text = user.username, style = MaterialTheme.typography.titleSmall)
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

