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

/**
 * Retrofit interface for backend endpoints used by the Android app.
 */
interface AgentApi {
    @GET("/health")
    suspend fun health(): Response<ResponseBody>

    @GET("/api/health")
    suspend fun apiHealth(): Response<ResponseBody>

    @POST("/api/chat")
    suspend fun sendMessage(@Body request: AgentRequest): AgentResponse
}

/**
 * Retrofit client holder.
 *
 * Base URL can be updated at runtime from the endpoint connection panel.
 */
object ApiClient {
    private var retrofit: Retrofit? = null
    
    /** Normalizes host input into a valid Retrofit base URL. */
    fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    /** Rebuilds Retrofit client for the latest endpoint URL. */
    fun setBaseUrl(url: String) {
        val normalizedUrl = normalizeBaseUrl(url)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Longer read/call timeouts reduce false timeout errors for slower hosted agents.
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
    
    /** Returns a typed API service when initialized via [setBaseUrl]. */
    fun getApi(): AgentApi? = retrofit?.create(AgentApi::class.java)
}
