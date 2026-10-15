package pl.cheapskountant.mobile.ui.photos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.cheapskountant.mobile.data.repository.IncomingPhoto
import pl.cheapskountant.mobile.data.repository.PhotoSaveResult
import pl.cheapskountant.mobile.data.repository.ReceiptRepository

class PhotoViewModel(private val receipts: ReceiptRepository) : ViewModel() {
    var selected by mutableStateOf<List<IncomingPhoto>>(emptyList())
        private set
    var message by mutableStateOf<Int?>(null)
        private set
    var reading by mutableStateOf(false)
        private set
    val waiting = receipts.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(photo: IncomingPhoto, rejected: Boolean, overLimit: Boolean, limitMessage: Int, ok: Boolean) {
        when {
            overLimit -> message = limitMessage
            rejected || !ok -> message = null
            else -> {
                selected = selected + photo
                message = null
            }
        }
    }

    fun remove(index: Int) {
        selected = selected.filterIndexed { i, _ -> i != index }
    }

    fun save(onOpenReview: (String) -> Unit, noPhotos: Int, limit: Int) {
        if (selected.isEmpty()) {
            message = noPhotos
            return
        }
        if (selected.size > 5) {
            message = limit
            return
        }
        val photos = selected
        reading = true
        message = null
        viewModelScope.launch {
            when (val result = receipts.transcribe(photos)) {
                is PhotoSaveResult.OpenReview -> {
                    selected = emptyList()
                    reading = false
                    onOpenReview(result.localId)
                }
                PhotoSaveResult.Parked -> {
                    selected = emptyList()
                    reading = false
                }
                PhotoSaveResult.Failed -> {
                    reading = false
                    message = pl.cheapskountant.mobile.R.string.send_failed
                }
            }
        }
    }

    fun delete(localId: String) {
        viewModelScope.launch { receipts.delete(localId) }
    }

    fun resubmit(localId: String) {
        viewModelScope.launch { receipts.resubmit(localId) }
    }

    fun clearMessage() {
        message = null
    }

    companion object {
        fun factory(receipts: ReceiptRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PhotoViewModel(receipts) as T
        }
    }
}
