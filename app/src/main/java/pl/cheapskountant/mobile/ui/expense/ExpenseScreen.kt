package pl.cheapskountant.mobile.ui.expense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.AppContainer
import pl.cheapskountant.mobile.domain.ExpenseDraft
import pl.cheapskountant.mobile.domain.ExpenseIssue
import pl.cheapskountant.mobile.domain.MoneyFormat
import pl.cheapskountant.mobile.domain.OutboxTransitions
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(container: AppContainer, onBack: () -> Unit, onFinished: () -> Unit) {
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.factory(container.expenses, container.landing))
    val outbox by viewModel.outbox.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var dateIndex by remember { mutableStateOf<Int?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            viewModel.rows.forEachIndexed { index, row ->
                ExpenseRow(
                    row = row,
                    categories = viewModel.categories,
                    onChange = { viewModel.update(index, it) },
                    onDate = { dateIndex = index },
                    onRemove = { viewModel.removeRow(index) },
                )
            }
            TextButton(onClick = viewModel::addRow) { Text(stringResource(R.string.add_row)) }
            viewModel.issue?.let { Text(stringResource(it.res())) }
            if (viewModel.sendFailed) Text(stringResource(R.string.send_failed))
            Button(onClick = { viewModel.save(locale, onFinished) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
            if (outbox.isNotEmpty()) {
                Text(stringResource(R.string.waiting_expenses), modifier = Modifier.padding(top = 16.dp))
                outbox.forEach { item ->
                    Text(stringResource(statusRes(item.status)))
                    item.lines.forEach { line ->
                        Text(
                            "${line.category} ${MoneyFormat.format(BigDecimal(line.amount), line.currency, locale)} ${line.currency}",
                        )
                    }
                    if (item.status != OutboxTransitions.SENDING) {
                        TextButton(onClick = { viewModel.delete(item.localId) }) { Text(stringResource(R.string.delete)) }
                    }
                    if (item.status == OutboxTransitions.ERROR) {
                        TextButton(onClick = { viewModel.resubmit(item.localId) }) { Text(stringResource(R.string.resubmit)) }
                    }
                }
            }
        }
    }
    dateIndex?.let { index ->
        val row = viewModel.rows[index]
        val state = rememberDatePickerState(
            initialSelectedDateMillis = row.paymentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { dateIndex = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.update(index, row.copy(paymentDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    dateIndex = null
                }) { Text(stringResource(R.string.save)) }
            },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseRow(
    row: ExpenseDraft,
    categories: List<String>,
    onChange: (ExpenseDraft) -> Unit,
    onDate: () -> Unit,
    onRemove: () -> Unit,
) {
    var currencies by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = row.description,
            onValueChange = { onChange(row.copy(description = it)) },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = row.amountText,
            onValueChange = { onChange(row.copy(amountText = it)) },
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onDate) {
            Text("${stringResource(R.string.payment_date)} ${row.paymentDate}")
        }
        ExposedDropdownMenuBox(expanded = currencies, onExpandedChange = { currencies = it }) {
            OutlinedTextField(
                value = row.currency,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.currency)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencies) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            DropdownMenu(expanded = currencies, onDismissRequest = { currencies = false }) {
                listOf("PLN", "EUR", "USD").forEach { currency ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onChange(row.copy(currency = currency))
                            currencies = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(expanded = categoryMenu, onExpandedChange = { categoryMenu = it }) {
            OutlinedTextField(
                value = row.category.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenu) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                categories.forEach { category ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onChange(row.copy(category = category))
                            categoryMenu = false
                        },
                    )
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

private fun ExpenseIssue.res(): Int = when (this) {
    ExpenseIssue.CategoriesUnavailable -> R.string.categories_unavailable
    ExpenseIssue.ListEmpty -> R.string.expense_list_empty
    ExpenseIssue.AmountMissing -> R.string.amount_missing
    ExpenseIssue.AmountRejected -> R.string.amount_rejected
    ExpenseIssue.DescriptionRejected -> R.string.description_rejected
    ExpenseIssue.DescriptionSpaces -> R.string.description_spaces
    ExpenseIssue.CategoryMissing -> R.string.no_category
}

private fun statusRes(status: String): Int = when (status) {
    OutboxTransitions.ERROR -> R.string.error
    else -> R.string.waiting_to_send
}
