package ru.netology.nework.requestModel

data class AuthenticationRequest(
    val login: String,
    val password: String,
)
