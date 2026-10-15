package pl.cheapskountant.mobile.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.cheapskountant.mobile.data.local.AppDatabase
import pl.cheapskountant.mobile.data.local.DayEntryEntity
import pl.cheapskountant.mobile.data.local.DaySnapshotEntity
import pl.cheapskountant.mobile.data.local.HouseholdCategoryEntity
import pl.cheapskountant.mobile.data.local.MonthCategoryEntity
import pl.cheapskountant.mobile.data.local.MonthSnapshotEntity
import pl.cheapskountant.mobile.data.remote.CheapskountantApi
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.data.sync.NetworkStatus
import pl.cheapskountant.mobile.domain.CalendarDates
import pl.cheapskountant.mobile.domain.MonthInput
import pl.cheapskountant.mobile.domain.MonthMath
import pl.cheapskountant.mobile.domain.SendOutcome
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class MonthSection(
    val currency: String,
    val total: BigDecimal,
    val rows: List<MonthCategoryEntity>,
    val showCached: Boolean,
    val known: Boolean,
)

data class DaySection(
    val entries: List<DayEntryEntity>,
    val showCached: Boolean,
    val known: Boolean,
)

class LandingRepository(
    private val db: AppDatabase,
    private val api: CheapskountantApi,
    private val remote: RemoteCall,
    private val network: NetworkStatus,
) {
    fun observeMonth(yearMonth: YearMonth): Flow<MonthSection> {
        val key = yearMonth.toString()
        return combine(db.months().observeSnapshot(key), db.months().observeCategories(key)) { snapshot, rows ->
            if (snapshot == null) {
                MonthSection("PLN", BigDecimal.ZERO.setScale(2), emptyList(), false, false)
            } else {
                MonthSection(
                    currency = snapshot.currency,
                    total = BigDecimal(snapshot.totalAmount),
                    rows = rows,
                    showCached = snapshot.showCachedCopy,
                    known = true,
                )
            }
        }
    }

    fun observeDay(date: LocalDate): Flow<DaySection> {
        val key = date.toString()
        return combine(db.days().observeSnapshot(key), db.days().observeEntries(key)) { snapshot, entries ->
            if (snapshot == null) {
                DaySection(emptyList(), false, false)
            } else {
                DaySection(entries, snapshot.showCachedCopy, true)
            }
        }
    }

    fun observeCategories(): Flow<List<HouseholdCategoryEntity>> = db.categories().observe()

    suspend fun refreshMonth(yearMonth: YearMonth) {
        val key = yearMonth.toString()
        val (from, to) = CalendarDates.monthRange(yearMonth)
        when (val outcome = remote.execute(network.online()) { api.summarizeExpenses(from, to) }) {
            is SendOutcome.Success -> {
                val body = outcome.body ?: return markMonthCached(key)
                val figures = MonthMath.summarize(body.map { MonthInput(it.category, it.currency, it.amount) })
                db.withTransaction {
                    db.months().upsertSnapshot(
                        MonthSnapshotEntity(key, figures.currency, figures.total.toPlainString(), System.currentTimeMillis(), false),
                    )
                    db.months().deleteCategories(key)
                    db.months().insertCategories(
                        figures.categories.map {
                            MonthCategoryEntity(key, it.category, it.amount.toPlainString(), it.position)
                        },
                    )
                }
            }
            else -> markMonthCached(key)
        }
    }

    suspend fun refreshDay(date: LocalDate) {
        val key = date.toString()
        when (val outcome = remote.execute(network.online()) { api.listDailyExpenses(key) }) {
            is SendOutcome.Success -> {
                val body = outcome.body ?: return markDayCached(key)
                db.withTransaction {
                    db.days().upsertSnapshot(DaySnapshotEntity(key, System.currentTimeMillis(), false))
                    db.days().deleteEntries(key)
                    db.days().insertEntries(
                        body.mapIndexed { index, row ->
                            DayEntryEntity(key, index, row.type, row.id, row.name, row.amount.setScale(2).toPlainString())
                        },
                    )
                }
            }
            else -> markDayCached(key)
        }
    }

    suspend fun refreshCategories() {
        when (val outcome = remote.execute(network.online()) { api.listHouseholdCategories() }) {
            is SendOutcome.Success -> {
                val body = outcome.body ?: return
                db.withTransaction {
                    db.categories().clear()
                    db.categories().insertAll(body.mapIndexed { index, row -> HouseholdCategoryEntity(row.name, index) })
                }
            }
            else -> Unit
        }
    }

    private suspend fun markMonthCached(key: String) {
        if (db.months().snapshot(key) != null) db.months().markCached(key)
    }

    private suspend fun markDayCached(key: String) {
        if (db.days().snapshot(key) != null) db.days().markCached(key)
    }
}
