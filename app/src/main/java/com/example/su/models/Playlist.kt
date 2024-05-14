package com.example.su.models

data class Playlist(
    var id: String = "",
    var name: String = "",
    var videos: Map<String, Map<String, String>> = mapOf()
)

