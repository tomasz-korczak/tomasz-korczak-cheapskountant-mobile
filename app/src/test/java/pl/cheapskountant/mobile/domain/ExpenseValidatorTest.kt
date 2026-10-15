package pl.cheapskountant.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class ExpenseValidatorTest {
    private val categories = setOf("Jedzenie")
    private val locale = Locale("pl")
    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun acceptsBoundaryAmountPastAndFutureDatesAndBlankDescription() {
        val result = ExpenseValidator.validate(
            listOf(
                draft(amount = "999999,99", date = LocalDate.of(2020, 1, 1)),
                draft(amount = "1", date = LocalDate.of(2030, 5, 2), description = ""),
            ),
            categories,
            locale,
        )
        val ok = result as ExpenseValidation.Ok
        assertEquals(0, BigDecimal("999999.99").compareTo(ok.lines[0].amount))
        assertNull(ok.lines[1].description)
        assertEquals("2020-01-01", ok.lines[0].paymentDate)
        assertEquals("2030-05-02", ok.lines[1].paymentDate)
    }

    @Test
    fun rejectsMissingZeroAndNegativeAmounts() {
        assertIssue(draft(amount = ""), ExpenseIssue.AmountMissing)
        assertIssue(draft(amount = "0"), ExpenseIssue.AmountMissing)
        assertIssue(draft(amount = "-1"), ExpenseIssue.AmountMissing)
    }

    @Test
    fun rejectsTooManyDigitsAndNonNumericAmounts() {
        assertIssue(draft(amount = "1000000"), ExpenseIssue.AmountRejected)
        assertIssue(draft(amount = "1,234"), ExpenseIssue.AmountRejected)
        assertIssue(draft(amount = "12,345"), ExpenseIssue.AmountRejected)
        assertIssue(draft(amount = "abc"), ExpenseIssue.AmountRejected)
    }

    @Test
    fun rejectsLongAndWhitespaceDescriptions() {
        assertIssue(draft(description = "a".repeat(101)), ExpenseIssue.DescriptionRejected)
        assertIssue(draft(description = "   "), ExpenseIssue.DescriptionSpaces)
    }

    @Test
    fun rejectsMissingCategoryEmptyListAndMissingCategoryTable() {
        assertIssue(draft(category = null), ExpenseIssue.CategoryMissing)
        assertEquals(
            ExpenseIssue.ListEmpty,
            (ExpenseValidator.validate(emptyList(), categories, locale) as ExpenseValidation.Bad).issue,
        )
        assertEquals(
            ExpenseIssue.CategoriesUnavailable,
            (ExpenseValidator.validate(listOf(draft()), emptySet(), locale) as ExpenseValidation.Bad).issue,
        )
    }

    @Test
    fun sentencesMatchTheSpec() {
        assertEquals(
            "Enter an amount greater than zero",
            ExpenseValidator.explain(ExpenseIssue.AmountMissing, Locale.ENGLISH),
        )
        assertEquals(
            "Description cannot be only spaces",
            ExpenseValidator.explain(ExpenseIssue.DescriptionSpaces, Locale.ENGLISH),
        )
        assertEquals(
            "Opis nie może składać się z samych spacji",
            ExpenseValidator.explain(ExpenseIssue.DescriptionSpaces, Locale("pl")),
        )
    }

    private fun assertIssue(row: ExpenseDraft, issue: ExpenseIssue) {
        val result = ExpenseValidator.validate(listOf(row), categories, locale)
        assertTrue(result is ExpenseValidation.Bad)
        assertEquals(issue, (result as ExpenseValidation.Bad).issue)
    }

    private fun draft(
        description: String = "Warzywa",
        amount: String = "12,50",
        date: LocalDate = today,
        category: String? = "Jedzenie",
    ) = ExpenseDraft(description, amount, date, "PLN", category)
}
