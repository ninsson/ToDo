package com.example.todo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Załącznik powiązany z zadaniem.
 *
 * Plik jest kopiowany do pamięci aplikacji, a w bazie przechowywana jest lokalna ścieżka.
 */
@Serializable
data class Attachment(
    @SerialName("uri") val localPath: String,
    val mimeType: String? = null,
    val name: String? = null
)