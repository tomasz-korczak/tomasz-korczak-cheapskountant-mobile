package pl.cheapskountant.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HouseholdCategoryEntity::class,
        MonthSnapshotEntity::class,
        MonthCategoryEntity::class,
        DaySnapshotEntity::class,
        DayEntryEntity::class,
        ReceiptOutboxEntity::class,
        ReceiptPhotoEntity::class,
        ExpenseOutboxEntity::class,
        ExpenseOutboxLineEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categories(): HouseholdCategoryDao
    abstract fun months(): MonthDao
    abstract fun days(): DayDao
    abstract fun receipts(): ReceiptOutboxDao
    abstract fun expenses(): ExpenseOutboxDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "cheapskountant.db").build()
        }
    }
}
