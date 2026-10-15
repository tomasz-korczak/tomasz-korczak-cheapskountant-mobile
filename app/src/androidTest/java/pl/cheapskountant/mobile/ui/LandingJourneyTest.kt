package pl.cheapskountant.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.repository.DaySection
import pl.cheapskountant.mobile.data.repository.MonthSection
import pl.cheapskountant.mobile.ui.landing.DayListSection
import pl.cheapskountant.mobile.ui.landing.MonthSummarySection
import pl.cheapskountant.mobile.ui.theme.CheapskountantTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class LandingJourneyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun emptyKnownMonthShowsZeroAndEmptySentence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CheapskountantTheme {
                MonthSummarySection(
                    yearMonth = YearMonth.of(2026, 10),
                    section = MonthSection("PLN", BigDecimal.ZERO.setScale(2), emptyList(), showCached = false, known = true),
                    onPickMonth = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.expenses)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.empty_month)).assertIsDisplayed()
        rule.onNodeWithText("PLN").assertIsDisplayed()
    }

    @Test
    fun emptyDayShowsEmptySentence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CheapskountantTheme {
                DayListSection(
                    date = LocalDate.of(2026, 10, 16),
                    section = DaySection(emptyList(), showCached = false, known = false),
                    onPickDay = {},
                )
            }
        }
        rule.onNodeWithText(context.getString(R.string.empty_day)).assertIsDisplayed()
    }
}
