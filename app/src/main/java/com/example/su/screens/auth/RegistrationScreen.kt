package com.example.su.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegistrationScreen(auth: FirebaseAuth, onRegistrationSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имя пользователя") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { registerUser(username, email, password, auth, onRegistrationSuccess, { message = it }) }) {
            Text("Зарегистрироваться")
        }
        if (message.isNotEmpty()) {
            Text(message, color = Color.Red)
        }
    }
}


private fun registerUser(username: String, email: String, password: String, auth: FirebaseAuth, onSuccess: () -> Unit, onError: (String) -> Unit) {
    if (username.isBlank()) {
        onError("Пожалуйста, введите имя пользователя")
        return
    }
    if (email.isNotBlank() && password.isNotBlank()) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email
                    )
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Не удалось сохранить данные пользователя в Firestore")
                        }
                } else {
                    onError(task.exception?.message ?: "Ошибка регистрации")
                }
            }
    } else {
        onError("Пожалуйста, введите email и пароль")
    }
}