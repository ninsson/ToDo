package com.example.todo.data

import kotlinx.serialization.Serializable

// Prostą reprezentacją załącznika: URI + typ
@Serializable
data class Attachment(
    val uri: String,
    val mimeType: String? = null,
    val name: String? = null
)