package ru.netology.nework.model.requestModel

import java.io.File

data class RegistrationRequest(
    val login: String,
    val password: String,
    val name: String,
    val avatarFile: File? = null
)
