package com.miku.agent

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AgentApi {
    @GET("/health")
    suspend fun health(): Response<ResponseBody>

    @GET("/api/health")
    suspend fun apiHealth(): Response<ResponseBody>

    @POST("/api/chat")
    suspend fun sendMessage(@Body request: AgentRequest): AgentResponse
}

object ApiClient {
    private var retrofit: Retrofit? = null
    
    fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun setBaseUrl(url: String) {
        val normalizedUrl = normalizeBaseUrl(url)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun getApi(): AgentApi? = retrofit?.create(AgentApi::class.java)
}
