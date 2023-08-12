package ru.netology.nework.model.requestModel

data class AuthenticationRequest(
    val login: String,
    val password: String,
)
