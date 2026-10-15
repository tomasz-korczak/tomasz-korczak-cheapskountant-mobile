package pl.cheapskountant.mobile.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutboxDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun waitingRowsStayQueryableUntilDeleted() = runBlocking {
        database.expenses().insert(
            ExpenseOutboxEntity("expense-1", "WAITING", null, 1L, 1L),
        )
        database.expenses().insertLines(
            listOf(ExpenseOutboxLineEntity("expense-1", 0, null, "12.50", "2026-10-16", "PLN", "Jedzenie")),
        )
        database.receipts().insert(
            ReceiptOutboxEntity("receipt-1", "TRANSCRIBE", "WAITING", null, null, null, 1L, 1L),
        )
        assertEquals(1, database.expenses().waiting().size)
        assertEquals(1, database.receipts().waiting().size)
        assertEquals("12.50", database.expenses().lines("expense-1").single().amount)
        database.expenses().deleteLines("expense-1")
        database.expenses().delete("expense-1")
        database.receipts().delete("receipt-1")
        assertEquals(0, database.expenses().waiting().size)
        assertEquals(0, database.receipts().waiting().size)
    }
}
