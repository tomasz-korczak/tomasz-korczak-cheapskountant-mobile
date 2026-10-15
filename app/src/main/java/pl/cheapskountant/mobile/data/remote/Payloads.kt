package pl.cheapskountant.mobile.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import pl.cheapskountant.mobile.domain.ValidExpenseLine
import java.math.RoundingMode

object ExpensePayload {
    fun encode(lines: List<ValidExpenseLine>): JsonArray = buildJsonArray {
        lines.forEach { line ->
            add(
                buildJsonObject {
                    put(
                        "amount",
                        JsonUnquotedLiteral(line.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()),
                    )
                    put("paymentDate", line.paymentDate)
                    put("category", line.category)
                    put("currency", line.currency)
                    line.description?.let { put("description", it) }
                },
            )
        }
    }
}

object ReceiptParts {
    fun image(displayName: String, mime: String, bytes: ByteArray): MultipartBody.Part {
        return MultipartBody.Part.createFormData(
            "images",
            displayName,
            bytes.toRequestBody(mime.toMediaType()),
        )
    }
}
