package pl.cheapskountant.mobile.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

enum class ExpenseIssue {
    CategoriesUnavailable,
    ListEmpty,
    AmountMissing,
    AmountRejected,
    DescriptionRejected,
    DescriptionSpaces,
    CategoryMissing,
}

data class ExpenseDraft(
    val description: String = "",
    val amountText: String = "",
    val paymentDate: LocalDate = LocalDate.now(),
    val currency: String = "PLN",
    val category: String? = null,
)

data class ValidExpenseLine(
    val description: String?,
    val amount: BigDecimal,
    val paymentDate: String,
    val currency: String,
    val category: String,
)

sealed interface ExpenseValidation {
    data class Ok(val lines: List<ValidExpenseLine>) : ExpenseValidation
    data class Bad(val issue: ExpenseIssue) : ExpenseValidation
}

object ExpenseValidator {
    private val max = BigDecimal("999999.99")

    fun explain(issue: ExpenseIssue, locale: Locale): String {
        val polish = locale.language != "en"
        return when (issue) {
            ExpenseIssue.CategoriesUnavailable ->
                if (polish) "Kategorie będą dostępne po ich pobraniu" else "Categories are unavailable until they can be retrieved"
            ExpenseIssue.ListEmpty ->
                if (polish) "Dodaj co najmniej jeden kompletny wydatek" else "Add at least one complete expense"
            ExpenseIssue.AmountMissing ->
                if (polish) "Podaj kwotę większą od zera" else "Enter an amount greater than zero"
            ExpenseIssue.AmountRejected ->
                if (polish) "Użyj co najwyżej 6 cyfr przed znakiem dziesiętnym i 2 po nim" else "Use at most 6 digits before the decimal mark and 2 after"
            ExpenseIssue.DescriptionRejected ->
                if (polish) "Opis może mieć co najwyżej 100 znaków" else "Description can have at most 100 characters"
            ExpenseIssue.DescriptionSpaces ->
                if (polish) "Opis nie może składać się z samych spacji" else "Description cannot be only spaces"
            ExpenseIssue.CategoryMissing ->
                if (polish) "Wybierz kategorię" else "Choose a category"
        }
    }

    fun validate(
        rows: List<ExpenseDraft>,
        categories: Set<String>,
        locale: Locale,
    ): ExpenseValidation {
        if (categories.isEmpty()) {
            return ExpenseValidation.Bad(ExpenseIssue.CategoriesUnavailable)
        }
        if (rows.isEmpty()) {
            return ExpenseValidation.Bad(ExpenseIssue.ListEmpty)
        }
        val lines = mutableListOf<ValidExpenseLine>()
        for (row in rows) {
            val description = row.description
            if (description.isNotEmpty() && description.all { it.isWhitespace() }) {
                return ExpenseValidation.Bad(ExpenseIssue.DescriptionSpaces)
            }
            if (description.length > 100) {
                return ExpenseValidation.Bad(ExpenseIssue.DescriptionRejected)
            }
            val amount = parseAmount(row.amountText, locale)
            if (amount == null) {
                return ExpenseValidation.Bad(amountIssue(row.amountText, locale))
            }
            val category = row.category
            if (category.isNullOrBlank() || category !in categories) {
                return ExpenseValidation.Bad(ExpenseIssue.CategoryMissing)
            }
            lines += ValidExpenseLine(
                description = description.ifBlank { null },
                amount = amount,
                paymentDate = row.paymentDate.toString(),
                currency = row.currency,
                category = category,
            )
        }
        return ExpenseValidation.Ok(lines)
    }

    private fun amountIssue(text: String, locale: Locale): ExpenseIssue {
        if (text.isBlank()) return ExpenseIssue.AmountMissing
        val parsed = tryParse(text, locale) ?: return ExpenseIssue.AmountRejected
        return if (parsed.signum() <= 0) ExpenseIssue.AmountMissing else ExpenseIssue.AmountRejected
    }

    private fun parseAmount(text: String, locale: Locale): BigDecimal? {
        val parsed = tryParse(text, locale) ?: return null
        if (parsed.signum() <= 0) return null
        if (parsed > max) return null
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val normalized = strip(text, symbols)
        val fraction = normalized.substringAfter('.', "")
        val integer = normalized.substringBefore('.').trimStart('0').ifEmpty { "0" }
        if (fraction.length > 2 || integer.length > 6) return null
        return parsed.setScale(2, RoundingMode.HALF_UP)
    }

    private fun tryParse(text: String, locale: Locale): BigDecimal? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val normalized = strip(text, symbols)
        if (normalized.isEmpty() || normalized == "-" || normalized == ".") return null
        return try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun strip(text: String, symbols: DecimalFormatSymbols): String {
        val grouping = symbols.groupingSeparator
        val decimal = symbols.decimalSeparator
        return text.trim()
            .replace(grouping.toString(), "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace(decimal, '.')
    }
}
