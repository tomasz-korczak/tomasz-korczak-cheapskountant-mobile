package pl.cheapskountant.mobile.data.repository

import android.content.Context
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import pl.cheapskountant.mobile.data.local.AppDatabase
import pl.cheapskountant.mobile.data.local.ReceiptOutboxEntity
import pl.cheapskountant.mobile.data.local.ReceiptPhotoStore
import pl.cheapskountant.mobile.data.remote.CheapskountantApi
import pl.cheapskountant.mobile.data.remote.ReceiptParts
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.data.sync.NetworkStatus
import pl.cheapskountant.mobile.data.sync.OutboxSync
import pl.cheapskountant.mobile.domain.OutboxTransitions
import pl.cheapskountant.mobile.domain.ReceiptDocument
import pl.cheapskountant.mobile.domain.ReceiptSaveRefusal
import pl.cheapskountant.mobile.domain.SendOutcome
import java.util.UUID

data class IncomingPhoto(val displayName: String, val mime: String, val bytes: ByteArray)

sealed interface PhotoSaveResult {
    data class OpenReview(val localId: String) : PhotoSaveResult
    data object Parked : PhotoSaveResult
    data object Failed : PhotoSaveResult
}

sealed interface ReviewSaveResult {
    data object Accepted : ReviewSaveResult
    data object Parked : ReviewSaveResult
    data object Failed : ReviewSaveResult
    data class Refused(val refusal: ReceiptSaveRefusal) : ReviewSaveResult
}

class ReceiptRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val photos: ReceiptPhotoStore,
    private val api: CheapskountantApi,
    private val remote: RemoteCall,
    private val network: NetworkStatus,
) {
    fun observe() = db.receipts().observe()

    suspend fun transcribe(selected: List<IncomingPhoto>): PhotoSaveResult = withContext(NonCancellable) {
        if (selected.isEmpty()) return@withContext PhotoSaveResult.Failed
        val localId = UUID.randomUUID().toString()
        val stored = selected.mapIndexedNotNull { index, photo ->
            photos.import(localId, index, photo.displayName, photo.bytes, photo.mime)
        }
        if (stored.isEmpty()) {
            photos.delete(localId)
            return@withContext PhotoSaveResult.Failed
        }
        val now = System.currentTimeMillis()
        val online = network.online()
        db.receipts().insert(
            ReceiptOutboxEntity(
                localId = localId,
                phase = OutboxTransitions.TRANSCRIBE,
                status = if (online) OutboxTransitions.SENDING else OutboxTransitions.WAITING,
                receiptJson = null,
                baselineJson = null,
                errorReason = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        db.receipts().insertPhotos(stored)
        if (!online) {
            OutboxSync.enqueue(context)
            return@withContext PhotoSaveResult.Parked
        }
        val outcome = remote.execute(true) { api.transcribeReceipt(parts(localId)) }
        applyTranscription(localId, outcome)
    }

    suspend fun confirm(localId: String, document: JsonElement): ReviewSaveResult = withContext(NonCancellable) {
        val row = db.receipts().get(localId) ?: return@withContext ReviewSaveResult.Failed
        val categories = db.categories().all().map { it.name }.toSet()
        val doc = document as? kotlinx.serialization.json.JsonObject ?: return@withContext ReviewSaveResult.Failed
        val refusal = ReceiptDocument.refusal(doc, categories)
        if (refusal != null) return@withContext ReviewSaveResult.Refused(refusal)
        val posted = ReceiptDocument.forPost(doc)
        val json = posted.toString()
        val online = network.online()
        if (!online) {
            db.receipts().update(
                row.copy(
                    phase = OutboxTransitions.CONFIRM,
                    status = OutboxTransitions.WAITING,
                    receiptJson = json,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            OutboxSync.enqueue(context)
            return@withContext ReviewSaveResult.Parked
        }
        db.receipts().update(row.copy(phase = OutboxTransitions.CONFIRM, status = OutboxTransitions.SENDING, receiptJson = json, updatedAt = System.currentTimeMillis()))
        val outcome = remote.execute(true) { api.createReceipt(posted) }
        applyConfirm(localId, outcome)
    }

    suspend fun delete(localId: String) = withContext(NonCancellable) {
        val row = db.receipts().get(localId) ?: return@withContext
        if (!OutboxTransitions.canDelete(row.status)) return@withContext
        photos.delete(localId)
        db.receipts().deletePhotos(localId)
        db.receipts().delete(localId)
    }

    suspend fun resubmit(localId: String) = withContext(NonCancellable) {
        val row = db.receipts().get(localId) ?: return@withContext
        if (row.status != OutboxTransitions.ERROR) return@withContext
        val (phase, status) = OutboxTransitions.resubmitReceipt(row.phase)
        db.receipts().update(row.copy(phase = phase, status = status, errorReason = null, updatedAt = System.currentTimeMillis()))
        OutboxSync.enqueue(context)
    }

    suspend fun revertToBaseline(localId: String) = withContext(NonCancellable) {
        val row = db.receipts().get(localId) ?: return@withContext
        db.receipts().update(
            row.copy(
                receiptJson = row.baselineJson,
                status = OutboxTransitions.READY,
                phase = OutboxTransitions.TRANSCRIBE,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun load(localId: String): ReceiptOutboxEntity? = db.receipts().get(localId)

    suspend fun syncWaiting(): Boolean {
        if (!network.online()) return false
        val rows = db.receipts().waiting()
        if (rows.isEmpty()) return false
        for (row in rows) {
            if (!network.online()) return false
            val current = db.receipts().get(row.localId) ?: continue
            if (!OutboxTransitions.canSend(current.status)) continue
            db.receipts().update(current.copy(status = OutboxTransitions.SENDING, updatedAt = System.currentTimeMillis()))
            val outcome = if (current.phase == OutboxTransitions.CONFIRM) {
                val doc = ReceiptDocument.parse(current.receiptJson.orEmpty())
                remote.execute(true) { api.createReceipt(ReceiptDocument.forPost(doc)) }
            } else {
                remote.execute(true) { api.transcribeReceipt(parts(current.localId)) }
            }
            if (current.phase == OutboxTransitions.CONFIRM) {
                applyConfirm(current.localId, outcome)
            } else {
                applyTranscription(current.localId, outcome)
            }
            if (outcome is SendOutcome.NoNetwork) return false
        }
        return true
    }

    private suspend fun applyTranscription(localId: String, outcome: SendOutcome<JsonElement>): PhotoSaveResult {
        val row = db.receipts().get(localId) ?: return PhotoSaveResult.Failed
        return when (outcome) {
            is SendOutcome.Success -> {
                val body = outcome.body?.toString()
                if (body == null) {
                    db.receipts().update(row.copy(status = OutboxTransitions.ERROR, updatedAt = System.currentTimeMillis()))
                    PhotoSaveResult.Failed
                } else {
                    db.receipts().update(
                        row.copy(
                            status = OutboxTransitions.READY,
                            receiptJson = body,
                            baselineJson = body,
                            errorReason = null,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    PhotoSaveResult.OpenReview(localId)
                }
            }
            SendOutcome.NoNetwork -> {
                db.receipts().update(row.copy(status = OutboxTransitions.WAITING, updatedAt = System.currentTimeMillis()))
                OutboxSync.enqueue(context)
                PhotoSaveResult.Parked
            }
            is SendOutcome.HttpError -> {
                db.receipts().update(row.copy(status = OutboxTransitions.ERROR, errorReason = outcome.reason, updatedAt = System.currentTimeMillis()))
                PhotoSaveResult.Failed
            }
            SendOutcome.NoStatus -> {
                db.receipts().update(row.copy(status = OutboxTransitions.ERROR, updatedAt = System.currentTimeMillis()))
                PhotoSaveResult.Failed
            }
        }
    }

    private suspend fun applyConfirm(localId: String, outcome: SendOutcome<*>): ReviewSaveResult {
        val row = db.receipts().get(localId) ?: return ReviewSaveResult.Failed
        return when (outcome) {
            is SendOutcome.Success -> {
                photos.delete(localId)
                db.receipts().deletePhotos(localId)
                db.receipts().delete(localId)
                ReviewSaveResult.Accepted
            }
            SendOutcome.NoNetwork -> {
                db.receipts().update(row.copy(phase = OutboxTransitions.CONFIRM, status = OutboxTransitions.WAITING, updatedAt = System.currentTimeMillis()))
                OutboxSync.enqueue(context)
                ReviewSaveResult.Parked
            }
            is SendOutcome.HttpError -> {
                db.receipts().update(row.copy(status = OutboxTransitions.ERROR, errorReason = outcome.reason, updatedAt = System.currentTimeMillis()))
                ReviewSaveResult.Failed
            }
            SendOutcome.NoStatus -> {
                db.receipts().update(row.copy(status = OutboxTransitions.ERROR, updatedAt = System.currentTimeMillis()))
                ReviewSaveResult.Failed
            }
        }
    }

    private suspend fun parts(localId: String): List<MultipartBody.Part> {
        return db.receipts().photos(localId).map { photo ->
            val mime = when {
                photo.displayName.endsWith(".png", true) -> "image/png"
                photo.displayName.endsWith(".webp", true) -> "image/webp"
                else -> "image/jpeg"
            }
            ReceiptParts.image(photo.displayName, mime, photos.read(photo.relativePath))
        }
    }
}
