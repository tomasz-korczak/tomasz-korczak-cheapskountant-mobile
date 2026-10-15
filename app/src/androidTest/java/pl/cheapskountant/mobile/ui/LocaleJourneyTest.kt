package pl.cheapskountant.mobile.ui

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pl.cheapskountant.mobile.R
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleJourneyTest {
    @Test
    fun polishAndOtherLanguagesUsePolishText() {
        assertEquals("Zapisz", text(Locale("pl"), R.string.save))
        assertEquals("Zapisz", text(Locale("de"), R.string.save))
        assertEquals("Wydatki", text(Locale("pl"), R.string.expenses))
    }

    @Test
    fun englishUsesEnglishText() {
        assertEquals("Save", text(Locale.ENGLISH, R.string.save))
        assertEquals("Expenses", text(Locale.ENGLISH, R.string.expenses))
        assertEquals("Receipt photo", text(Locale.ENGLISH, R.string.receipt_photo))
    }

    @Test
    fun catalogsExposeTheSameKeys() {
        val keys = listOf(
            "menu", "configuration", "about", "expenses", "receipt_photo", "register_expense", "receipt",
            "day_expense", "take_photo", "choose_gallery", "photo_count", "same_receipt", "remove_photo",
            "save", "back", "select_line", "name", "amount", "discount", "category", "total", "remove_line",
            "change_selected", "change_all", "description", "payment_date", "currency", "add_row",
            "waiting_receipts", "waiting_expenses", "waiting_to_send", "ready_to_edit", "error", "edit",
            "delete", "resubmit", "last_saved_copy", "empty_month", "empty_day", "no_photos", "photo_limit",
            "no_category", "unlisted_category", "no_line_selected", "categories_unavailable", "amount_missing",
            "amount_rejected", "description_rejected", "description_spaces", "receipt_needs_line",
            "expense_list_empty", "send_failed", "reading_receipt",
        )
        val polish = context(Locale("pl"))
        val english = context(Locale.ENGLISH)
        keys.forEach { key ->
            val id = polish.resources.getIdentifier(key, "string", polish.packageName)
            assertEquals(key, true, id != 0)
            polish.getString(id)
            english.getString(id)
        }
    }

    private fun text(locale: Locale, id: Int) = context(locale).getString(id)

    private fun context(locale: Locale) = InstrumentationRegistry.getInstrumentation().targetContext.let { base ->
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        base.createConfigurationContext(config)
    }
}
