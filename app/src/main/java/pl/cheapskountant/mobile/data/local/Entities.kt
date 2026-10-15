package pl.cheapskountant.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "household_category")
data class HouseholdCategoryEntity(
    @PrimaryKey val name: String,
    val position: Int,
)

@Entity(tableName = "month_snapshot")
data class MonthSnapshotEntity(
    @PrimaryKey val yearMonth: String,
    val currency: String,
    val totalAmount: String,
    val fetchedAt: Long,
    val showCachedCopy: Boolean,
)

@Entity(tableName = "month_category", primaryKeys = ["yearMonth", "category"])
data class MonthCategoryEntity(
    val yearMonth: String,
    val category: String,
    val amount: String,
    val position: Int,
)

@Entity(tableName = "day_snapshot")
data class DaySnapshotEntity(
    @PrimaryKey val date: String,
    val fetchedAt: Long,
    val showCachedCopy: Boolean,
)

@Entity(tableName = "day_entry", primaryKeys = ["date", "position"])
data class DayEntryEntity(
    val date: String,
    val position: Int,
    val type: String,
    val serverId: Long,
    val name: String,
    val amount: String,
)

@Entity(tableName = "receipt_outbox")
data class ReceiptOutboxEntity(
    @PrimaryKey val localId: String,
    val phase: String,
    val status: String,
    val receiptJson: String?,
    val baselineJson: String?,
    val errorReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "receipt_photo", primaryKeys = ["localId", "position"])
data class ReceiptPhotoEntity(
    val localId: String,
    val position: Int,
    val relativePath: String,
    val displayName: String,
    val byteSize: Long,
)

@Entity(tableName = "expense_outbox")
data class ExpenseOutboxEntity(
    @PrimaryKey val localId: String,
    val status: String,
    val errorReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "expense_outbox_line", primaryKeys = ["localId", "position"])
data class ExpenseOutboxLineEntity(
    val localId: String,
    val position: Int,
    val description: String?,
    val amount: String,
    val paymentDate: String,
    val currency: String,
    val category: String,
)
