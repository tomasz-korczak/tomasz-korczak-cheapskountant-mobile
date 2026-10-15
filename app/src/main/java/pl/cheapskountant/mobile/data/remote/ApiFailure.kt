package pl.cheapskountant.mobile.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ApiFailure(val reason: String?, val explanation: String?) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String?): ApiFailure? {
            if (body.isNullOrBlank()) return null
            return try {
                val obj = json.parseToJsonElement(body).jsonObject
                ApiFailure(
                    reason = obj["reason"]?.jsonPrimitive?.contentOrNull,
                    explanation = obj["explanation"]?.jsonPrimitive?.contentOrNull,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
