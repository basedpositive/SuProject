package com.example.su.models

import java.io.Serializable

data class Video(
    var id: String = "", // Дефолтное значение для обеспечения конструктора без аргументов
    var userId: String = "",
    var userName: String? = null,
    var videoName: String = "",
    var previewUrl: String = ""
) : Serializable

