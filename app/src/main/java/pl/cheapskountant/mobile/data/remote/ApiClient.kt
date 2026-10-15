package pl.cheapskountant.mobile.data.remote

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import pl.cheapskountant.mobile.BuildConfig
import pl.cheapskountant.mobile.domain.SendOutcome
import retrofit2.Response as RetrofitResponse
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class BearerInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .build()
        return chain.proceed(request)
    }
}

object ApiClient {
    val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun create(
        baseUrl: String = BuildConfig.API_BASE_URL,
        apiKey: String = BuildConfig.API_KEY,
    ): CheapskountantApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(BearerInterceptor(apiKey))
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CheapskountantApi::class.java)
    }
}

class RemoteCall {
    suspend fun <T> execute(online: Boolean, call: suspend () -> RetrofitResponse<T>): SendOutcome<T> {
        if (!online) return SendOutcome.NoNetwork
        return try {
            val response = call()
            if (response.isSuccessful) {
                SendOutcome.Success(response.body())
            } else {
                val failure = ApiFailure.parse(response.errorBody()?.string())
                val explanation = failure?.explanation
                if (!explanation.isNullOrBlank()) {
                    Log.w("Cheapskountant", explanation)
                }
                SendOutcome.HttpError(response.code(), failure?.reason)
            }
        } catch (_: IOException) {
            SendOutcome.NoStatus
        }
    }
}
