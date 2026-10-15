package pl.cheapskountant.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CategoryLabelTest {
    @Test
    fun householdNameIsUnchangedInBothLocales() {
        assertEquals("Jedzenie", CategoryLabel.show("Jedzenie", Locale("pl")))
        assertEquals("Jedzenie", CategoryLabel.show("Jedzenie", Locale.ENGLISH))
    }
}
