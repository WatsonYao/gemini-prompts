package world.life.prompts.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import world.life.prompts.model.GeminiRequest
import world.life.prompts.model.GeminiResponse

interface GeminiApiService {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("X-goog-api-key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}