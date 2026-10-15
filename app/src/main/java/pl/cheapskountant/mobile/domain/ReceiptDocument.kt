package pl.cheapskountant.mobile.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ReceiptLineView(
    val index: Int,
    val name: String,
    val total: String,
    val discount: String?,
    val category: String,
)

sealed interface ReceiptSaveRefusal {
    data object NoLines : ReceiptSaveRefusal
    data object UnlistedCategory : ReceiptSaveRefusal
}

object ReceiptDocument {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): JsonObject = json.parseToJsonElement(text).jsonObject

    fun tradeName(doc: JsonObject): String = text(doc["seller"]?.jsonObject?.get("tradeName"))

    fun issuedDate(doc: JsonObject): String = CalendarDates.issuedDate(text(doc["receipt"]?.jsonObject?.get("issuedAt")))

    fun currency(doc: JsonObject): String = text(doc["receipt"]?.jsonObject?.get("currency"))

    fun amountDue(doc: JsonObject): String = text(doc["totals"]?.jsonObject?.get("amountDue"))

    fun lines(doc: JsonObject): List<ReceiptLineView> {
        val items = doc["items"] as? JsonArray ?: return emptyList()
        return items.mapIndexed { index, element ->
            val item = element.jsonObject
            ReceiptLineView(
                index = index,
                name = text(item["description"]),
                total = text(item["total"]),
                discount = item["discount"]?.jsonObject?.get("total")?.jsonPrimitive?.contentOrNull,
                category = text(item["category"]),
            )
        }
    }

    fun setCategories(doc: JsonObject, indexes: Set<Int>, category: String): JsonObject {
        val items = doc["items"] as? JsonArray ?: return doc
        val updated = buildJsonArray {
            items.forEachIndexed { index, element ->
                if (index in indexes) {
                    val fields = element.jsonObject.toMutableMap()
                    fields["category"] = JsonPrimitive(category)
                    add(JsonObject(fields))
                } else {
                    add(element)
                }
            }
        }
        return replace(doc, "items", updated)
    }

    fun setAllCategories(doc: JsonObject, category: String): JsonObject {
        val count = (doc["items"] as? JsonArray)?.size ?: 0
        return setCategories(doc, (0 until count).toSet(), category)
    }

    fun removeLine(doc: JsonObject, index: Int): JsonObject {
        val items = doc["items"] as? JsonArray ?: return doc
        val updated = buildJsonArray {
            items.forEachIndexed { itemIndex, element ->
                if (itemIndex != index) add(element)
            }
        }
        return replace(doc, "items", updated)
    }

    fun forPost(doc: JsonObject): JsonObject {
        val fields = doc.toMutableMap()
        fields.remove("id")
        return JsonObject(fields)
    }

    fun refusal(doc: JsonObject, categories: Set<String>): ReceiptSaveRefusal? {
        val items = doc["items"] as? JsonArray ?: return ReceiptSaveRefusal.NoLines
        if (items.isEmpty()) return ReceiptSaveRefusal.NoLines
        val unlisted = items.any { element ->
            val category = element.jsonObject["category"]?.jsonPrimitive?.contentOrNull
            category == null || category !in categories
        }
        return if (unlisted) ReceiptSaveRefusal.UnlistedCategory else null
    }

    private fun replace(doc: JsonObject, key: String, value: JsonElement): JsonObject {
        val fields = doc.toMutableMap()
        fields[key] = value
        return JsonObject(fields)
    }

    private fun text(element: JsonElement?): String = element?.jsonPrimitive?.contentOrNull.orEmpty()
}
