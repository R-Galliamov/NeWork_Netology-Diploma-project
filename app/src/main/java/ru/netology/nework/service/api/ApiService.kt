package ru.netology.nework.service.api

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.dto.AuthState

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

    //Posts
    @GET("api/posts/")
    suspend fun getAllPosts(): Response<List<Post>>

    @POST("api/posts/{id}/likes/")
    suspend fun likeById(@Path("id") id: Int): Response<Post>

    @DELETE("api/posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Int): Response<Post>

    //Events
    @GET("api/events/")
    suspend fun getAllEvents(): Response<List<Event>>

    //Users
    @GET("api/users/")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/users/{user_id}/")
    suspend fun getUserById(@Path("user_id") userId: Int): Response<User>

    //Auth
    @FormUrlEncoded
    @POST("api/users/authentication/")
    suspend fun updateAuth(
        @Field("login") login: String,
        @Field("password") password: String
    ): Response<AuthState>

    @FormUrlEncoded
    @POST("api/users/registration/")
    suspend fun registerUser(
        @Field("login") login: RequestBody,
        @Field("password") password: RequestBody,
        @Field("name") name: RequestBody,
    ): Response<AuthState>

    @Multipart
    @POST("api/users/registration")
    suspend fun registerUser(
        @Part("login") login: RequestBody,
        @Part("password") password: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part,
    ): Response<AuthState>

    @Multipart
    @POST("media")
    suspend fun upload(@Part media: MultipartBody.Part): Response<AuthState>
}

