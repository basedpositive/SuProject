package com.example.su.models

import java.io.Serializable

data class Video(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var videoName: String = "",
    var previewUrl: String = "",

    var videoUrl: String = "",
    var description: String = "",
    var views: Int = 0,
    var likes: Int = 0,
    var dateUploaded: Long = System.currentTimeMillis(),

    var likedBy: List<String> = listOf()
) : Serializable

