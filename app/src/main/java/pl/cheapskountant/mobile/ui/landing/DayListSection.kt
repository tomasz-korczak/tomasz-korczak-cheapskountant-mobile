package pl.cheapskountant.mobile.ui.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.repository.DaySection
import pl.cheapskountant.mobile.domain.CalendarDates
import pl.cheapskountant.mobile.domain.MoneyFormat
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun DayListSection(
    date: LocalDate,
    section: DaySection,
    onPickDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextButton(onClick = onPickDay) {
            Text(CalendarDates.formatDay(date, locale))
        }
        if (section.showCached) {
            Text(stringResource(R.string.last_saved_copy))
        }
        if (section.entries.isEmpty()) {
            Text(stringResource(R.string.empty_day))
        }
        section.entries.forEach { entry ->
            val label = if (entry.type == "receipt") {
                stringResource(R.string.receipt)
            } else {
                stringResource(R.string.day_expense)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(entry.name, modifier = Modifier.weight(1f))
                Text(label, modifier = Modifier.padding(horizontal = 8.dp))
                Text(MoneyFormat.format(BigDecimal(entry.amount), "PLN", locale, dayList = true))
            }
        }
    }
}
