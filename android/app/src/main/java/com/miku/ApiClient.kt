package com.miku.agent

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AgentApi {
    @POST("/api/chat")
    suspend fun sendMessage(@Body request: AgentRequest): AgentResponse
}

object ApiClient {
    private var retrofit: Retrofit? = null
    
    fun setBaseUrl(url: String) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun getApi(): AgentApi? = retrofit?.create(AgentApi::class.java)
}
