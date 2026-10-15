package pl.cheapskountant.mobile.ui.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.AppContainer
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    container: AppContainer,
    onReceiptPhoto: () -> Unit,
    onRegisterExpense: () -> Unit,
) {
    val viewModel: LandingViewModel = viewModel(factory = LandingViewModel.factory(container.landing))
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val day by viewModel.selectedDay.collectAsStateWithLifecycle()
    val monthSection by viewModel.monthSection.collectAsStateWithLifecycle()
    val daySection by viewModel.daySection.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf(false) }
    var pickDay by remember { mutableStateOf(false) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses)) },
                actions = {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.configuration)) },
                            onClick = { menu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = { menu = false },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            MonthSummarySection(month, monthSection, onPickMonth = { pickMonth = true })
            Button(
                onClick = onReceiptPhoto,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.receipt_photo))
            }
            OutlinedButton(
                onClick = onRegisterExpense,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.register_expense))
            }
            DayListSection(day, daySection, onPickDay = { pickDay = true })
        }
    }

    if (pickMonth) {
        YearMonthDialog(month, locale, onDismiss = { pickMonth = false }) {
            viewModel.setMonth(it)
            pickMonth = false
        }
    }
    if (pickDay) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDay = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.setDay(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDay = false
                }) { Text(stringResource(R.string.save)) }
            },
        ) { DatePicker(state) }
    }
}

@Composable
private fun YearMonthDialog(
    current: YearMonth,
    locale: Locale,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit,
) {
    var year by remember { mutableStateOf(current.year) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = { year -= 1 }) { Text("‹") }
                Text(year.toString())
                TextButton(onClick = { year += 1 }) { Text("›") }
            }
        },
        text = {
            Column {
                Month.values().forEach { month ->
                    TextButton(onClick = { onSelect(YearMonth.of(year, month)) }) {
                        Text(month.getDisplayName(TextStyle.FULL_STANDALONE, locale))
                    }
                }
            }
        },
    )
}
