package com.example.su.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController, auth: FirebaseAuth) {
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    var username by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    username = document.getString("username") ?: "Аноним"
                }
                .addOnFailureListener {
                    username = "Ошибка загрузки данных"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            Text(text = "Привет, $username!")
            Button(onClick = {
                auth.signOut()
                navController.navigate("profile") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }) {
                Text(text = "Выйти")
            }
        } else {
            Text(text = "Пожалуйста, войдите в систему.")
            Button(onClick = {
                navController.navigate("login")
            }) {
                Text(text = "Войти")
            }
            Button(onClick = {
                navController.navigate("registration")
            }) {
                Text(text = "Регистрация")
            }
        }
    }
}