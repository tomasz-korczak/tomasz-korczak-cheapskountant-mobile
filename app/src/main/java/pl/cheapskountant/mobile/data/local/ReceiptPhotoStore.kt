package pl.cheapskountant.mobile.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

class ReceiptPhotoStore(private val context: Context) {
    fun import(
        localId: String,
        position: Int,
        displayName: String,
        bytes: ByteArray,
        mime: String,
    ): ReceiptPhotoEntity? {
        val normalized = normalize(bytes, mime) ?: return null
        val directory = directory(localId)
        directory.mkdirs()
        val safeName = displayName.ifBlank { "photo-$position.jpg" }.replace(File.separatorChar, '-')
        val file = File(directory, "$position-$safeName")
        file.writeBytes(normalized)
        return ReceiptPhotoEntity(
            localId = localId,
            position = position,
            relativePath = file.relativeTo(context.filesDir).invariantSeparatorsPath,
            displayName = displayName.ifBlank { safeName },
            byteSize = normalized.size.toLong(),
        )
    }

    fun read(relativePath: String): ByteArray = File(context.filesDir, relativePath).readBytes()

    fun delete(localId: String) {
        directory(localId).deleteRecursively()
    }

    private fun directory(localId: String) = File(context.filesDir, "receipt-queue/$localId")

    private fun normalize(bytes: ByteArray, mime: String): ByteArray? {
        val type = mime.lowercase()
        val allowed = type == "image/jpeg" || type == "image/jpg" || type == "image/png" || type == "image/webp"
        if (!allowed) return null
        if (bytes.size <= MAX_BYTES) return bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        var quality = 85
        var compressed = bytes
        while (quality >= 20) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            compressed = stream.toByteArray()
            if (compressed.size <= MAX_BYTES) {
                bitmap.recycle()
                return compressed
            }
            quality -= 15
        }
        bitmap.recycle()
        return null
    }

    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
    }
}
