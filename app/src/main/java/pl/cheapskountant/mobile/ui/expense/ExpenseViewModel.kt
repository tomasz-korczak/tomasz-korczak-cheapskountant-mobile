package pl.cheapskountant.mobile.ui.expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.cheapskountant.mobile.data.repository.ExpenseRepository
import pl.cheapskountant.mobile.data.repository.ExpenseSaveResult
import pl.cheapskountant.mobile.data.repository.LandingRepository
import pl.cheapskountant.mobile.domain.ExpenseDraft
import pl.cheapskountant.mobile.domain.ExpenseIssue
import java.util.Locale

class ExpenseViewModel(
    private val expenses: ExpenseRepository,
    private val landing: LandingRepository,
) : ViewModel() {
    var rows by mutableStateOf(listOf(ExpenseDraft()))
        private set
    var issue by mutableStateOf<ExpenseIssue?>(null)
        private set
    var sendFailed by mutableStateOf(false)
        private set
    var categories by mutableStateOf<List<String>>(emptyList())
        private set
    val outbox = expenses.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            landing.refreshCategories()
            landing.observeCategories().collect { categories = it.map { row -> row.name } }
        }
    }

    fun update(index: Int, row: ExpenseDraft) {
        rows = rows.mapIndexed { i, current -> if (i == index) row else current }
        issue = null
    }

    fun addRow() {
        rows = rows + ExpenseDraft()
    }

    fun removeRow(index: Int) {
        rows = rows.filterIndexed { i, _ -> i != index }
    }

    fun save(locale: Locale, onFinished: () -> Unit) {
        viewModelScope.launch {
            when (val result = expenses.save(rows, locale)) {
                ExpenseSaveResult.Accepted -> onFinished()
                ExpenseSaveResult.Parked -> {
                    rows = listOf(ExpenseDraft())
                    issue = null
                    sendFailed = false
                }
                is ExpenseSaveResult.Invalid -> {
                    issue = result.issue
                    sendFailed = false
                }
                ExpenseSaveResult.Failed -> {
                    issue = null
                    sendFailed = true
                }
            }
        }
    }

    fun delete(localId: String) {
        viewModelScope.launch { expenses.delete(localId) }
    }

    fun resubmit(localId: String) {
        viewModelScope.launch { expenses.resubmit(localId) }
    }

    companion object {
        fun factory(expenses: ExpenseRepository, landing: LandingRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(expenses, landing) as T
        }
    }
}
