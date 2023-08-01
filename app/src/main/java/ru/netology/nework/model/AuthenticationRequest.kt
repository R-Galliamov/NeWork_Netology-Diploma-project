package ru.netology.nework.model

data class AuthenticationRequest(
    val login: String,
    val password: String,
)
