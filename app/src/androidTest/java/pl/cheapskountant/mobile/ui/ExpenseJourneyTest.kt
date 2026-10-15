package pl.cheapskountant.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.cheapskountant.mobile.MainActivity
import pl.cheapskountant.mobile.R

class ExpenseJourneyTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun registerExpenseOpensABlankRowAndExplainsAnEmptySave() {
        rule.onNodeWithText(rule.activity.getString(R.string.register_expense)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.save)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.expense_list_empty)).assertIsDisplayed()
    }
}
