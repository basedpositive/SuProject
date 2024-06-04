package com.example.su.models

data class Subscribe(
    var id: String = "",
    var users: Map<String, Map<String, String>> = mapOf()
)

