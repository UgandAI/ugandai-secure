package com.ugandai.ugandai.data.api

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignupRequest(val username: String, val password: String, val email: String)

@JsonClass(generateAdapter = true)
data class SignupResponse(val id: Int, val username: String, val email: String)

@JsonClass(generateAdapter = true)
data class TokenResponse(val access_token: String, val token_type: String)

@JsonClass(generateAdapter = true)
data class ChatRequest(val sender: String, val content: String)

@JsonClass(generateAdapter = true)
data class FarmProfileRequest(val farm_name: String?, val district: String?, val crops: String?, val farm_size: Double?)

@JsonClass(generateAdapter = true)
data class FarmProfileResponse(val id: Int, val user_id: Int, val farm_name: String?, val district: String?, val crops: String?, val farm_size: Double?)

interface UgandAIApi {
    
    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse
    
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("conversations")
    suspend fun getConversations(): List<ConversationSummaryResponse>

    @POST("conversations")
    suspend fun createConversation(): ConversationSummaryResponse

    @GET("conversations/{id}/messages")
    suspend fun getConversationMessages(@Path("id") id: Int): List<ConversationMessageResponse>
    
    @GET("profiles/farm")
    suspend fun getFarmProfiles(): List<FarmProfileResponse>
    
    @POST("profiles/farm")
    suspend fun createFarmProfile(@Body request: FarmProfileRequest): FarmProfileResponse
    
    @PUT("profiles/farm/{id}")
    suspend fun updateFarmProfile(@Path("id") id: Int, @Body request: FarmProfileRequest): FarmProfileResponse
    
    // Logbook Endpoints
    @GET("logbook/")
    suspend fun getLogbookEntries(): List<LogbookEntryResponse>
    
    @POST("logbook/")
    suspend fun createLogbookEntry(@Body request: LogbookEntryRequest): LogbookEntryResponse
    
    @PUT("logbook/{id}")
    suspend fun updateLogbookEntry(@Path("id") id: Int, @Body request: LogbookEntryRequest): LogbookEntryResponse
    
    @retrofit2.http.DELETE("logbook/{id}")
    suspend fun deleteLogbookEntry(@Path("id") id: Int)
    
    // Recommendations Endpoint
    @GET("recommendations/initial")
    suspend fun getInitialRecommendation(): RecommendationResponse
}

@JsonClass(generateAdapter = true)
data class LogbookEntryRequest(
    val activity_type: String,
    val date: String,
    val crop: String,
    val field: String,
    val note: String?
)

@JsonClass(generateAdapter = true)
data class LogbookEntryResponse(
    val id: Int,
    val activity_type: String,
    val date: String,
    val crop: String,
    val field: String,
    val note: String?,
    val user_id: Int,
    val created_at: String,
    val updated_at: String
)

@JsonClass(generateAdapter = true)
data class RecommendationResponse(
    val recommendation: String
)

@JsonClass(generateAdapter = true)
data class ConversationSummaryResponse(
    val id: Int,
    val title: String,
    val created_at: String,
    val updated_at: String
)

@JsonClass(generateAdapter = true)
data class ConversationMessageResponse(
    val id: Int,
    val role: String,
    val content: String,
    val created_at: String
)
