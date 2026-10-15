package pl.cheapskountant.mobile.domain

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptDocumentTest {
    private val original = """
        {
          "id": "temp",
          "seller": {"tradeName": "Sklep"},
          "receipt": {"issuedAt": "2026-10-12T18:41:00+02:00", "currency": "PLN"},
          "totals": {"taxAmount": "0.00", "grossAmount": "15.00", "amountDue": "44.87"},
          "items": [
            {
              "description": "Milk",
              "quantity": 1,
              "unitPrice": "10.00",
              "total": "10.00",
              "taxCategory": "A",
              "category": "Unknown",
              "discount": {"description": "rabat", "total": "-1.00"}
            },
            {
              "description": "Bread",
              "quantity": 1,
              "unitPrice": "5.00",
              "total": "5.00",
              "taxCategory": "A",
              "category": "Jedzenie"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun readsHeaderWithoutShiftingTheIssuedDate() {
        val doc = ReceiptDocument.parse(original)
        assertEquals("Sklep", ReceiptDocument.tradeName(doc))
        assertEquals("2026-10-12", ReceiptDocument.issuedDate(doc))
        assertEquals("PLN", ReceiptDocument.currency(doc))
        assertEquals("44.87", ReceiptDocument.amountDue(doc))
    }

    @Test
    fun discountIsTheNegativeTotalOrBlank() {
        val lines = ReceiptDocument.lines(ReceiptDocument.parse(original))
        assertEquals("-1.00", lines[0].discount)
        assertNull(lines[1].discount)
    }

    @Test
    fun categoryChangeAndRemovalKeepTotalsOrderAndOtherFields() {
        val doc = ReceiptDocument.parse(original)
        val changed = ReceiptDocument.setCategories(doc, setOf(0), "Jedzenie")
        val removed = ReceiptDocument.removeLine(changed, 1)
        val posted = ReceiptDocument.forPost(removed)
        assertFalse(posted.containsKey("id"))
        assertEquals("44.87", posted["totals"]!!.jsonObject["amountDue"]!!.jsonPrimitive.content)
        assertEquals("0.00", posted["totals"]!!.jsonObject["taxAmount"]!!.jsonPrimitive.content)
        val items = posted["items"]!!.jsonArray
        assertEquals(1, items.size)
        assertEquals("Milk", items[0].jsonObject["description"]!!.jsonPrimitive.content)
        assertEquals("Jedzenie", items[0].jsonObject["category"]!!.jsonPrimitive.content)
        assertEquals("10.00", items[0].jsonObject["total"]!!.jsonPrimitive.content)
        assertEquals("-1.00", items[0].jsonObject["discount"]!!.jsonObject["total"]!!.jsonPrimitive.content)
    }

    @Test
    fun blocksUnknownAndEmptyItemLists() {
        val doc = ReceiptDocument.parse(original)
        val categories = setOf("Jedzenie")
        assertTrue(ReceiptDocument.refusal(doc, categories) is ReceiptSaveRefusal.UnlistedCategory)
        val listed = ReceiptDocument.setAllCategories(doc, "Jedzenie")
        assertNull(ReceiptDocument.refusal(listed, categories))
        val empty = ReceiptDocument.removeLine(ReceiptDocument.removeLine(doc, 1), 0)
        assertTrue(ReceiptDocument.refusal(empty, categories) is ReceiptSaveRefusal.NoLines)
    }
}
