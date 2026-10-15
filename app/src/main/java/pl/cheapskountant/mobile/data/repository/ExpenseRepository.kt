package pl.cheapskountant.mobile.data.repository

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import pl.cheapskountant.mobile.data.local.AppDatabase
import pl.cheapskountant.mobile.data.local.ExpenseOutboxEntity
import pl.cheapskountant.mobile.data.local.ExpenseOutboxLineEntity
import pl.cheapskountant.mobile.data.remote.CheapskountantApi
import pl.cheapskountant.mobile.data.remote.ExpensePayload
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.data.sync.NetworkStatus
import pl.cheapskountant.mobile.data.sync.OutboxSync
import pl.cheapskountant.mobile.domain.ExpenseDraft
import pl.cheapskountant.mobile.domain.ExpenseIssue
import pl.cheapskountant.mobile.domain.ExpenseValidation
import pl.cheapskountant.mobile.domain.ExpenseValidator
import pl.cheapskountant.mobile.domain.OutboxTransitions
import pl.cheapskountant.mobile.domain.SendOutcome
import pl.cheapskountant.mobile.domain.ValidExpenseLine
import android.content.Context
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

data class ExpenseOutboxItem(
    val localId: String,
    val status: String,
    val lines: List<ExpenseOutboxLineEntity>,
)

sealed interface ExpenseSaveResult {
    data object Accepted : ExpenseSaveResult
    data object Parked : ExpenseSaveResult
    data class Invalid(val issue: ExpenseIssue) : ExpenseSaveResult
    data object Failed : ExpenseSaveResult
}

class ExpenseRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val api: CheapskountantApi,
    private val remote: RemoteCall,
    private val network: NetworkStatus,
) {
    fun observe(): Flow<List<ExpenseOutboxItem>> {
        return combine(db.expenses().observe(), db.expenses().observeLines()) { rows, lines ->
            rows.map { row ->
                ExpenseOutboxItem(row.localId, row.status, lines.filter { it.localId == row.localId })
            }
        }
    }

    suspend fun save(rows: List<ExpenseDraft>, locale: Locale): ExpenseSaveResult = withContext(NonCancellable) {
        val categories = db.categories().all().map { it.name }.toSet()
        when (val validation = ExpenseValidator.validate(rows, categories, locale)) {
            is ExpenseValidation.Bad -> ExpenseSaveResult.Invalid(validation.issue)
            is ExpenseValidation.Ok -> store(validation.lines, network.online())
        }
    }

    suspend fun delete(localId: String) = withContext(NonCancellable) {
        val row = db.expenses().get(localId) ?: return@withContext
        if (!OutboxTransitions.canDelete(row.status)) return@withContext
        db.expenses().deleteLines(localId)
        db.expenses().delete(localId)
    }

    suspend fun resubmit(localId: String) = withContext(NonCancellable) {
        val row = db.expenses().get(localId) ?: return@withContext
        if (row.status != OutboxTransitions.ERROR) return@withContext
        db.expenses().update(row.copy(status = OutboxTransitions.resubmitExpense(), errorReason = null, updatedAt = System.currentTimeMillis()))
        OutboxSync.enqueue(context)
    }

    suspend fun syncWaiting(): Boolean {
        if (!network.online()) return false
        val rows = db.expenses().waiting()
        if (rows.isEmpty()) return false
        for (row in rows) {
            if (!network.online()) return false
            val current = db.expenses().get(row.localId) ?: continue
            if (!OutboxTransitions.canSend(current.status)) continue
            db.expenses().update(current.copy(status = OutboxTransitions.SENDING, updatedAt = System.currentTimeMillis()))
            val lines = db.expenses().lines(row.localId).map { it.toValid() }
            val outcome = remote.execute(true) { api.storeExpenses(ExpensePayload.encode(lines)) }
            apply(row.localId, outcome)
            if (outcome is SendOutcome.NoNetwork) return false
        }
        return true
    }

    private suspend fun store(lines: List<ValidExpenseLine>, online: Boolean): ExpenseSaveResult {
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        val status = if (online) OutboxTransitions.SENDING else OutboxTransitions.WAITING
        db.expenses().insert(ExpenseOutboxEntity(localId, status, null, now, now))
        db.expenses().insertLines(lines.mapIndexed { index, line -> line.toEntity(localId, index) })
        if (!online) {
            OutboxSync.enqueue(context)
            return ExpenseSaveResult.Parked
        }
        val outcome = remote.execute(true) { api.storeExpenses(ExpensePayload.encode(lines)) }
        return apply(localId, outcome)
    }

    private suspend fun apply(localId: String, outcome: SendOutcome<*>): ExpenseSaveResult {
        val row = db.expenses().get(localId) ?: return ExpenseSaveResult.Failed
        return when (outcome) {
            is SendOutcome.Success -> {
                db.expenses().deleteLines(localId)
                db.expenses().delete(localId)
                ExpenseSaveResult.Accepted
            }
            SendOutcome.NoNetwork -> {
                db.expenses().update(row.copy(status = OutboxTransitions.WAITING, updatedAt = System.currentTimeMillis()))
                OutboxSync.enqueue(context)
                ExpenseSaveResult.Parked
            }
            is SendOutcome.HttpError -> {
                db.expenses().update(
                    row.copy(status = OutboxTransitions.ERROR, errorReason = outcome.reason, updatedAt = System.currentTimeMillis()),
                )
                ExpenseSaveResult.Failed
            }
            SendOutcome.NoStatus -> {
                db.expenses().update(row.copy(status = OutboxTransitions.ERROR, updatedAt = System.currentTimeMillis()))
                ExpenseSaveResult.Failed
            }
        }
    }

    private fun ValidExpenseLine.toEntity(localId: String, position: Int) = ExpenseOutboxLineEntity(
        localId = localId,
        position = position,
        description = description,
        amount = amount.setScale(2).toPlainString(),
        paymentDate = paymentDate,
        currency = currency,
        category = category,
    )

    private fun ExpenseOutboxLineEntity.toValid() = ValidExpenseLine(
        description = description,
        amount = BigDecimal(amount),
        paymentDate = paymentDate,
        currency = currency,
        category = category,
    )
}
