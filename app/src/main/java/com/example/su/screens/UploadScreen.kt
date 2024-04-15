package com.example.su.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UploadVideoButton() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
        }
        val pickerIntent = Intent.createChooser(intent, "Выберите видео")
        context.startActivity(pickerIntent)
    }) {
        Text("Выбрать видео")
    }
}

@Composable
fun UploadScreen(auth: FirebaseAuth, navController: NavController) {
    val user = auth.currentUser
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            UploadVideoButton()
        } else {
            Text("Для загрузки видео войдите в систему.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("login") }) {
                Text("Войти")
            }
        }
    }
}