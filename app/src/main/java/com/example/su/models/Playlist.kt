package com.example.su.models

data class Playlist(
    var id: String = "",
    var name: String = "",
    var videos: List<String> = emptyList() // Список ID видео
)

