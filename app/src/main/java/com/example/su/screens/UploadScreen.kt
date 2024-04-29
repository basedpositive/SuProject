package com.example.su.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.su.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(auth: FirebaseAuth, navController: NavController) {
    val context = LocalContext.current
    val user = auth.currentUser
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // ActivityResultLaunchers
    val startForResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoUri = result.data?.data
        }
    }
    val startForResultPreview = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            previewUri = result.data?.data
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            TextField(
                value = videoName,
                onValueChange = { videoName = it },
                label = { Text("Название") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "video/*"
                }
                val pickerIntent = Intent.createChooser(intent, "Выберите видео")
                startForResult.launch(pickerIntent)
            }) {
                Text("Выбрать видео")
            }
            /* videoUri?.let {
                Text("Выбранное видео: ${it.path}")  // Показываем путь к файлу
            }*/

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                val pickerIntent = Intent.createChooser(intent, "Выберите превью")
                startForResultPreview.launch(pickerIntent)
            }) {
                Text("Выбрать превью")
            }
            /* previewUri?.let {
                Text("Выбранное превью: ${it.path}")  // Показываем путь к файлу
            } */

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (videoUri != null && previewUri != null) {
                    uploadVideoAndData(videoUri, videoName, description, previewUri, user)
                } else {
                    // Отобразить сообщение об ошибке, если видео или превью не выбраны
                }
            }) {
                Text("Загрузить видео")
            }
        } else {
            Text("Для загрузки видео войдите в систему.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("login") }) {
                Text("Войти")
            }
        }
    }
}

fun uploadVideoAndData(videoUri: Uri?, videoName: String, description: String, previewUri: Uri?, user: FirebaseUser) {
    val storageRef = FirebaseStorage.getInstance().reference
    val videoRef = storageRef.child("videos/${videoUri?.lastPathSegment}")
    val previewRef = storageRef.child("previews/${previewUri?.lastPathSegment}")

    videoUri?.let {
        videoRef.putFile(it).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            videoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val videoUrl = task.result.toString()
                previewUri?.let {
                    previewRef.putFile(it).continueWithTask  { previewTask ->
                        if (!previewTask.isSuccessful) {
                            previewTask.exception?.let {
                                throw it
                            }
                        }
                        previewRef.downloadUrl
                    }.addOnCompleteListener { previewTask ->
                        if (previewTask.isSuccessful) {
                            val previewUrl = previewTask.result.toString()
                            val videoData = hashMapOf(
                                "userId" to user.uid,
                                "videoUrl" to videoUrl,
                                "previewUrl" to previewUrl,
                                "videoName" to videoName,
                                "description" to description
                            )
                            FirebaseFirestore.getInstance().collection("videos").add(videoData)
                                .addOnSuccessListener {
                                    Log.d("UploadScreen", "Video successfully uploaded and data saved!")
                                }
                                .addOnFailureListener {
                                    Log.w("UploadScreen", "Error uploading video data", it)
                                }
                        }
                    }
                }
            }
        }
    }
}