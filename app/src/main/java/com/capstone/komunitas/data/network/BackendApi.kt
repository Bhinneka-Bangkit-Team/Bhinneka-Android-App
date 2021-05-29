package com.capstone.komunitas.data.network

import com.capstone.komunitas.data.network.responses.AuthResponse
import com.capstone.komunitas.data.network.responses.ChatResponse
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

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