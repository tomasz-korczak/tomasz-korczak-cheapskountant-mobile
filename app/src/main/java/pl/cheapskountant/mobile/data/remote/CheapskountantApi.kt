package pl.cheapskountant.mobile.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface CheapskountantApi {
    @GET("api/expense")
    suspend fun summarizeExpenses(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<List<CategoryTotalDto>>

    @GET("api/expense/daily")
    suspend fun listDailyExpenses(
        @Query("payment_date") paymentDate: String,
    ): Response<List<DailyExpenseDto>>

    @GET("api/expenses")
    suspend fun listHouseholdCategories(): Response<List<CategoryNameDto>>

    @Multipart
    @POST("api/transcription")
    suspend fun transcribeReceipt(
        @Part images: List<MultipartBody.Part>,
    ): Response<JsonElement>

    @POST("api/receipt")
    suspend fun createReceipt(@Body body: JsonElement): Response<JsonElement>

    @POST("api/expense")
    suspend fun storeExpenses(@Body body: JsonElement): Response<JsonElement>
}
