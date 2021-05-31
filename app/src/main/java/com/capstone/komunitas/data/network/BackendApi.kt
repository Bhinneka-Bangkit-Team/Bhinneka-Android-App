package com.capstone.komunitas.data.network

import com.capstone.komunitas.data.network.responses.AudioResponse
import com.capstone.komunitas.data.network.responses.AuthResponse
import com.capstone.komunitas.data.network.responses.ChatResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File

interface BackendApi {
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun userLogin(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("auth/register")
    suspend fun userRegister(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("firstName") firstName: String,
        @Field("lastName") lastName: String
    ): Response<AuthResponse>

    @GET("chat/get")
    suspend fun getChat(
        @Header("Authorization") accessToken: String
    ): Response<ChatResponse>

    @FormUrlEncoded
    @POST("chat/store")
    suspend fun sendChat(
        @Header("Authorization") accessToken: String,
        @Field("text") text: String,
        @Field("isSpeaker") isSpeaker: Int,
        @Field("lang") lang: String
    ): Response<ChatResponse>


    @FormUrlEncoded
    @POST("google/stt")
    suspend fun sendAudio(
    @Header("Authorization") accessToken: String,
    @Field("file") file: File,
    @Field("lang") lang: String
    ):Response<AudioResponse>

    @Multipart
    @POST("google/stt")
    suspend fun sendAudioTest(
    @Header("Authorization") accessToken: String,
    @Part file: MultipartBody.Part,
    @Part("lang") lang: RequestBody
    ):Response<AudioResponse>

    @FormUrlEncoded
    @POST("google/tts")
    suspend fun getAudio(
        @Header("Authorization") accessToken: String,
        @Field("text") text: String,
        @Field("lang") lang: String
    ):Response<ResponseBody>

    companion object {
        operator fun invoke(
            networkConnectionInterceptor: NetworkConnectionInterceptor
        ): BackendApi {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://api-dot-folkloric-ocean-308008.et.r.appspot.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BackendApi::class.java)
        }
    }
}