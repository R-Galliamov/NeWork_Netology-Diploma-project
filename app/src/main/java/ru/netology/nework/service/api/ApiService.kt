package ru.netology.nework.service.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import ru.netology.nework.model.Post
import ru.netology.nework.model.Token

private const val BASE_URL = "https://netomedia.ru/"

fun okhttp(vararg interceptors: Interceptor): OkHttpClient = OkHttpClient.Builder()
    .apply {
        interceptors.forEach {
            this.addInterceptor(it)
        }
    }
    .build()

fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface ApiService {
    @GET("api/posts/")
    suspend fun getAllPosts(): Response<List<Post>>

    @FormUrlEncoded
    @POST("api/users/authentication/")
    suspend fun updateAuth(
        @Field("login") login: String,
        @Field("password") password: String
    ): Response<Token>
}

