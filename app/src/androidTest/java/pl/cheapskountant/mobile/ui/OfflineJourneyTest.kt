package pl.cheapskountant.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.repository.MonthSection
import pl.cheapskountant.mobile.ui.landing.MonthSummarySection
import pl.cheapskountant.mobile.ui.theme.CheapskountantTheme
import java.math.BigDecimal
import java.time.YearMonth

class OfflineJourneyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun failedRefreshShowsTheLastSavedCopySentence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CheapskountantTheme {
                MonthSummarySection(
                    yearMonth = YearMonth.of(2026, 10),
                    section = MonthSection(
                        currency = "PLN",
                        total = BigDecimal("10.00"),
                        rows = emptyList(),
                        showCached = true,
                        known = true,
                    ),
                    onPickMonth = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.last_saved_copy)).assertIsDisplayed()
    }

    @Test
    fun periodNeverStoredShowsTheEmptySentenceWithoutTheCachedCopy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CheapskountantTheme {
                MonthSummarySection(
                    yearMonth = YearMonth.of(2026, 1),
                    section = MonthSection("PLN", BigDecimal.ZERO.setScale(2), emptyList(), showCached = false, known = false),
                    onPickMonth = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.empty_month)).assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithText(context.getString(R.string.last_saved_copy)).fetchSemanticsNodes().size)
    }
}
