package pl.cheapskountant.mobile.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

object DecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Decimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val json = decoder as? JsonDecoder ?: return BigDecimal(decoder.decodeString())
        return BigDecimal(json.decodeJsonElement().jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val json = encoder as? JsonEncoder
        if (json != null) {
            json.encodeJsonElement(JsonUnquotedLiteral(value.stripTrailingZeros().toPlainString()))
        } else {
            encoder.encodeString(value.toPlainString())
        }
    }
}

@Serializable
data class CategoryTotalDto(
    val category: String,
    val currency: String,
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
)

@Serializable
data class DailyExpenseDto(
    val type: String,
    val date: String,
    val id: Long,
    val name: String,
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
)

@Serializable
data class CategoryNameDto(val name: String)
