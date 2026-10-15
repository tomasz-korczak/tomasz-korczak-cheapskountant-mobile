package pl.cheapskountant.mobile.ui.receipt

import androidx.compose.foundation.background
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.AppContainer
import pl.cheapskountant.mobile.domain.ReceiptDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    container: AppContainer,
    localId: String,
    direct: Boolean,
    onLanding: () -> Unit,
    onPhotos: () -> Unit,
) {
    val viewModel: ReceiptReviewViewModel = viewModel(
        factory = ReceiptReviewViewModel.factory(container.receipts, container.landing, localId, direct),
    )
    val document = viewModel.document
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.back(onLanding, onPhotos) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (document != null) {
                Text(ReceiptDocument.tradeName(document))
                Text("${stringResource(R.string.payment_date)} ${ReceiptDocument.issuedDate(document)}")
                Text("${stringResource(R.string.currency)} ${ReceiptDocument.currency(document)}")
                Text("${stringResource(R.string.total)} ${ReceiptDocument.amountDue(document)}")
                ReceiptDocument.lines(document).forEach { line ->
                    val tint = if (line.index in viewModel.checked) {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        Modifier
                    }
                    Column(tint.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row {
                            Checkbox(
                                checked = line.index in viewModel.checked,
                                onCheckedChange = { viewModel.toggle(line.index) },
                            )
                            Text(stringResource(R.string.select_line))
                        }
                        Text("${stringResource(R.string.name)} ${line.name}")
                        Text("${stringResource(R.string.amount)} ${line.total}")
                        Text("${stringResource(R.string.discount)} ${line.discount ?: ""}")
                        Text("${stringResource(R.string.category)} ${line.category}")
                        IconButton(onClick = { viewModel.remove(line.index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_line))
                        }
                    }
                }
            }
            CategoryPicker(viewModel.categories, viewModel.chosenCategory, viewModel::choose)
            TextButton(onClick = viewModel::changeSelected) { Text(stringResource(R.string.change_selected)) }
            TextButton(onClick = viewModel::changeAll) { Text(stringResource(R.string.change_all)) }
            viewModel.message?.let { Text(stringResource(it)) }
            if (viewModel.sendFailed) Text(stringResource(R.string.send_failed))
            Button(onClick = {
                viewModel.save(if (direct) onLanding else onPhotos)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(categories: List<String>, chosen: String?, onChoose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = chosen.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            categories.forEach { category ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onChoose(category)
                        open = false
                    },
                )
            }
        }
    }
}
