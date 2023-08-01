package ru.netology.nework.service.api

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.nework.auth.AppAuth

fun loggingInterceptor() =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

fun authInterceptor(auth: AppAuth) = fun(chain: Interceptor.Chain): Response {
    auth.tokenStateFlow.value.token?.let { token ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        return chain.proceed(newRequest)
    }
    return chain.proceed(chain.request())
}