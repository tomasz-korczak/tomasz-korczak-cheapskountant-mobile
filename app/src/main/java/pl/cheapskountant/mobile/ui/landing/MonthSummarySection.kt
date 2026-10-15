package pl.cheapskountant.mobile.ui.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.repository.MonthSection
import pl.cheapskountant.mobile.domain.MoneyFormat
import pl.cheapskountant.mobile.ui.theme.PapierTrack
import pl.cheapskountant.mobile.ui.theme.ShareColors
import java.math.BigDecimal
import java.time.YearMonth

@Composable
fun MonthSummarySection(
    yearMonth: YearMonth,
    section: MonthSection,
    onPickMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextButton(onClick = onPickMonth) {
            Text(pl.cheapskountant.mobile.domain.CalendarDates.formatMonth(yearMonth, locale))
        }
        Text(stringResource(R.string.expenses))
        Text(MoneyFormat.format(section.total, section.currency, locale))
        Text(section.currency)
        if (section.showCached) {
            Text(stringResource(R.string.last_saved_copy))
        }
        if (section.rows.isEmpty()) {
            Text(stringResource(R.string.empty_month))
        }
        section.rows.forEachIndexed { index, row ->
            val amount = BigDecimal(row.amount)
            val positive = amount.signum() > 0
            val percent = if (section.total.signum() == 0) {
                null
            } else {
                amount.multiply(BigDecimal(100)).divide(section.total, 0, java.math.RoundingMode.HALF_UP).toInt()
            }
            val ratio = if (positive && section.total.signum() != 0) {
                amount.divide(section.total, 6, java.math.RoundingMode.HALF_UP).toFloat()
            } else {
                0f
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.category, modifier = Modifier.weight(1f))
                    if (!positive) {
                        Text(MoneyFormat.format(amount, section.currency, locale))
                    }
                    if (percent != null) {
                        Text("$percent%")
                    }
                }
                ShareBar(if (ratio > 0f) ratio else 0f, ShareColors[index % ShareColors.size])
            }
        }
    }
}

@Composable
private fun ShareBar(ratio: Float, color: androidx.compose.ui.graphics.Color) {
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = color,
        trackColor = PapierTrack,
    )
}
