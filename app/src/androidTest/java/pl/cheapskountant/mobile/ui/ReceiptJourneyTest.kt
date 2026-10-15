package pl.cheapskountant.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.cheapskountant.mobile.MainActivity
import pl.cheapskountant.mobile.R

class ReceiptJourneyTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun photoScreenBlocksSaveWithoutAPhoto() {
        rule.onNodeWithText(rule.activity.getString(R.string.receipt_photo)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.save)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.no_photos)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.same_receipt)).assertIsDisplayed()
    }
}
