package pl.cheapskountant.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdCategoryDao {
    @Query("SELECT * FROM household_category ORDER BY position")
    fun observe(): Flow<List<HouseholdCategoryEntity>>

    @Query("SELECT * FROM household_category ORDER BY position")
    suspend fun all(): List<HouseholdCategoryEntity>

    @Query("DELETE FROM household_category")
    suspend fun clear()

    @Insert
    suspend fun insertAll(items: List<HouseholdCategoryEntity>)
}

@Dao
interface MonthDao {
    @Query("SELECT * FROM month_snapshot WHERE yearMonth = :yearMonth")
    fun observeSnapshot(yearMonth: String): Flow<MonthSnapshotEntity?>

    @Query("SELECT * FROM month_snapshot WHERE yearMonth = :yearMonth")
    suspend fun snapshot(yearMonth: String): MonthSnapshotEntity?

    @Query("SELECT * FROM month_category WHERE yearMonth = :yearMonth ORDER BY position")
    fun observeCategories(yearMonth: String): Flow<List<MonthCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(row: MonthSnapshotEntity)

    @Query("DELETE FROM month_category WHERE yearMonth = :yearMonth")
    suspend fun deleteCategories(yearMonth: String)

    @Insert
    suspend fun insertCategories(rows: List<MonthCategoryEntity>)

    @Query("UPDATE month_snapshot SET showCachedCopy = 1 WHERE yearMonth = :yearMonth")
    suspend fun markCached(yearMonth: String)
}

@Dao
interface DayDao {
    @Query("SELECT * FROM day_snapshot WHERE date = :date")
    fun observeSnapshot(date: String): Flow<DaySnapshotEntity?>

    @Query("SELECT * FROM day_snapshot WHERE date = :date")
    suspend fun snapshot(date: String): DaySnapshotEntity?

    @Query("SELECT * FROM day_entry WHERE date = :date ORDER BY position")
    fun observeEntries(date: String): Flow<List<DayEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(row: DaySnapshotEntity)

    @Query("DELETE FROM day_entry WHERE date = :date")
    suspend fun deleteEntries(date: String)

    @Insert
    suspend fun insertEntries(rows: List<DayEntryEntity>)

    @Query("UPDATE day_snapshot SET showCachedCopy = 1 WHERE date = :date")
    suspend fun markCached(date: String)
}

@Dao
interface ReceiptOutboxDao {
    @Query("SELECT * FROM receipt_outbox ORDER BY createdAt")
    fun observe(): Flow<List<ReceiptOutboxEntity>>

    @Query("SELECT * FROM receipt_outbox WHERE localId = :localId")
    suspend fun get(localId: String): ReceiptOutboxEntity?

    @Query("SELECT * FROM receipt_outbox WHERE status = 'WAITING' ORDER BY createdAt")
    suspend fun waiting(): List<ReceiptOutboxEntity>

    @Query("SELECT * FROM receipt_photo WHERE localId = :localId ORDER BY position")
    suspend fun photos(localId: String): List<ReceiptPhotoEntity>

    @Query("SELECT * FROM receipt_photo WHERE localId = :localId ORDER BY position")
    fun observePhotos(localId: String): Flow<List<ReceiptPhotoEntity>>

    @Insert
    suspend fun insert(row: ReceiptOutboxEntity)

    @Insert
    suspend fun insertPhotos(rows: List<ReceiptPhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(row: ReceiptOutboxEntity)

    @Query("DELETE FROM receipt_outbox WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM receipt_photo WHERE localId = :localId")
    suspend fun deletePhotos(localId: String)
}

@Dao
interface ExpenseOutboxDao {
    @Query("SELECT * FROM expense_outbox ORDER BY createdAt")
    fun observe(): Flow<List<ExpenseOutboxEntity>>

    @Query("SELECT * FROM expense_outbox WHERE localId = :localId")
    suspend fun get(localId: String): ExpenseOutboxEntity?

    @Query("SELECT * FROM expense_outbox WHERE status = 'WAITING' ORDER BY createdAt")
    suspend fun waiting(): List<ExpenseOutboxEntity>

    @Query("SELECT * FROM expense_outbox_line WHERE localId = :localId ORDER BY position")
    suspend fun lines(localId: String): List<ExpenseOutboxLineEntity>

    @Query("SELECT * FROM expense_outbox_line ORDER BY localId, position")
    fun observeLines(): Flow<List<ExpenseOutboxLineEntity>>

    @Insert
    suspend fun insert(row: ExpenseOutboxEntity)

    @Insert
    suspend fun insertLines(rows: List<ExpenseOutboxLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(row: ExpenseOutboxEntity)

    @Query("DELETE FROM expense_outbox WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM expense_outbox_line WHERE localId = :localId")
    suspend fun deleteLines(localId: String)
}
