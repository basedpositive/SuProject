package com.example.su.models

import java.io.Serializable

data class Video(
    var id: String = "",
    var userId: String = "",
    var userName: String? = null,
    var videoName: String = "",
    var previewUrl: String = "",

    var videoUrl: String = "",
    var description: String = "",
    var views: Int = 0,
    var likesCount: Int = 0,
    var dateUploaded: Long = System.currentTimeMillis(),
) : Serializable

