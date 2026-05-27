package com.example.todo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Załącznik przechowywany lokalnie w pamięci aplikacji
@Serializable
data class Attachment(
    @SerialName("uri") val localPath: String,
    val mimeType: String? = null,
    val name: String? = null
)