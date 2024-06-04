package com.example.su.models

data class User(
    var id: String = "",
    var username: String = "",
    var email: String = "",
    var likedVideos: List<String> = listOf(),
    var subscriptions: List<String> = listOf(),
    var subscribers: List<String> = listOf()
)
