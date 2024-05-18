package com.example.su.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(auth: FirebaseAuth, navController: NavController) {
    val context = LocalContext.current
    val user = auth.currentUser
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var showSnackbarError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val categories = listOf("Без категорий", "Образование", "Музыка", "Спорт", "Технологии", "Хобби")
    var selectedCategory by remember { mutableStateOf(categories[0]) }  // По умолчанию выбираем первую категорию
    var expanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(showSnackbar, showSnackbarError) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar("Видео успешно загружено")
            showSnackbar = false
        }
        if (showSnackbarError) {
            snackbarHostState.showSnackbar("Ошибка!")
            showSnackbarError = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (user != null) {
                TextField(
                    value = videoName,
                    onValueChange = { videoName = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "video/*"
                    }
                    val pickerIntent = Intent.createChooser(intent, "Выберите видео")
                    startForResult.launch(pickerIntent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Выбрать видео")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                    }
                    val pickerIntent = Intent.createChooser(intent, "Выберите превью")
                    startForResultPreview.launch(pickerIntent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Выбрать превью")
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Категория") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEachIndexed { index, category ->
                            DropdownMenuItem(
                                text = { Text(text = category) },
                                onClick = {
                                    selectedCategory = categories[index]
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (videoUri != null && previewUri != null) {
                        isLoading = true

                        // Фильтрация изображения
                        filterImage(previewUri!!, context) { isImageSafe ->
                            if (isImageSafe) {
                                // Фильтрация текста
                                filterTextContent(videoName + " " + description) { isTextSafe ->
                                    if (isTextSafe) {
                                        uploadVideoAndData(videoUri, videoName, description, selectedCategory, previewUri, user) {
                                            isLoading = false
                                            showSnackbar = true
                                        }
                                    } else {
                                        isLoading = false
                                        showSnackbarError = true
                                    }
                                }
                            } else {
                                isLoading = false
                                showSnackbarError = true
                            }
                        }
                    } else {
                        showSnackbarError = true
                    }
                }) {
                    Text("Загрузить видео")
                }


                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally))
                }
            } else {
                Text("Для загрузки видео войдите в систему.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("login") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Войти")
                }
            }
        }
    }
}

fun uploadVideoAndData(
    videoUri: Uri?,
    videoName: String,
    description: String,
    selectedCategory: String,
    previewUri: Uri?,
    user: FirebaseUser,
    onUploadComplete: () -> Unit
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val videoRef = storageRef.child("videos/${videoUri?.lastPathSegment}")
    val previewRef = storageRef.child("previews/${previewUri?.lastPathSegment}")

    videoUri?.let {
        videoRef.putFile(it).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            videoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val videoUrl = task.result.toString()
                previewUri?.let {
                    previewRef.putFile(it).continueWithTask { previewTask ->
                        if (!previewTask.isSuccessful) {
                            previewTask.exception?.let { throw it }
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
                                "description" to description,
                                "category" to selectedCategory
                            )
                            FirebaseFirestore.getInstance().collection("videos").add(videoData)
                                .addOnSuccessListener {
                                    Log.d("UploadScreen", "Video successfully uploaded and data saved!")
                                    onUploadComplete()
                                }
                                .addOnFailureListener {
                                    Log.w("UploadScreen", "Error uploading video data", it)
                                    onUploadComplete()
                                }
                        } else {
                            onUploadComplete()
                        }
                    }
                }
            } else {
                onUploadComplete()
            }
        }
    } ?: onUploadComplete()
}

fun filterImage(imageUri: Uri, context: Context, onSuccess: (Boolean) -> Unit) {
    val image = InputImage.fromFilePath(context, imageUri)
    val labeler: ImageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    labeler.process(image)
        .addOnSuccessListener { labels ->
            var containsExplicitContent = false
            for (label in labels) {
                if (label.text == "Adult" || label.text == "Explicit Content") {
                    containsExplicitContent = true
                    break
                }
            }
            onSuccess(!containsExplicitContent)  // Возвращает true если контент безопасен
        }
        .addOnFailureListener { e ->
            Log.e("ImageLabeling", "Error processing image", e)
            onSuccess(false)  // Возвращает false если возникла ошибка
        }
}

fun filterTextContent(text: String, onSuccess: (Boolean) -> Unit) {
    val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    languageIdentifier.identifyLanguage(text)
        .addOnSuccessListener { languageCode ->
            if (languageCode == "und") {
                Log.e("LanguageID", "Can't identify language.")
                onSuccess(false)
            } else {
                val containsBadWords = containsBadWords(text)
                onSuccess(!containsBadWords)  // Возвращает true если контент безопасен
            }
        }
        .addOnFailureListener { e ->
            Log.e("LanguageID", "Error identifying language", e)
            onSuccess(false)  // Возвращает false если возникла ошибка
        }
}

fun containsBadWords(text: String): Boolean {
    // Пример списка плохих слов
    val badWords = listOf("dick", "nigga", "nigger")
    return badWords.any { text.contains(it, ignoreCase = true) }
}

