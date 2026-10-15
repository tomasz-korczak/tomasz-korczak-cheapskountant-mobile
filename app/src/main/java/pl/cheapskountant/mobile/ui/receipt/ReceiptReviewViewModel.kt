package pl.cheapskountant.mobile.ui.receipt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import pl.cheapskountant.mobile.R
import pl.cheapskountant.mobile.data.repository.LandingRepository
import pl.cheapskountant.mobile.data.repository.ReceiptRepository
import pl.cheapskountant.mobile.data.repository.ReviewSaveResult
import pl.cheapskountant.mobile.domain.ReceiptDocument
import pl.cheapskountant.mobile.domain.ReceiptSaveRefusal

class ReceiptReviewViewModel(
    private val receipts: ReceiptRepository,
    private val landing: LandingRepository,
    private val localId: String,
    private val direct: Boolean,
) : ViewModel() {
    var document by mutableStateOf<JsonObject?>(null)
        private set
    var categories by mutableStateOf<List<String>>(emptyList())
        private set
    var checked by mutableStateOf(setOf<Int>())
        private set
    var chosenCategory by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<Int?>(null)
        private set
    var sendFailed by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            landing.refreshCategories()
            landing.observeCategories().collect { rows -> categories = rows.map { it.name } }
        }
        viewModelScope.launch {
            val row = receipts.load(localId)
            val text = row?.receiptJson
            if (!text.isNullOrBlank()) document = ReceiptDocument.parse(text)
        }
    }

    fun toggle(index: Int) {
        checked = if (index in checked) checked - index else checked + index
    }

    fun choose(category: String) {
        chosenCategory = category
        message = null
    }

    fun changeSelected() {
        val doc = document ?: return
        when {
            checked.isEmpty() -> message = R.string.no_line_selected
            chosenCategory.isNullOrBlank() -> message = R.string.no_category
            else -> {
                document = ReceiptDocument.setCategories(doc, checked, chosenCategory!!)
                message = null
            }
        }
    }

    fun changeAll() {
        val doc = document ?: return
        if (chosenCategory.isNullOrBlank()) {
            message = R.string.no_category
        } else {
            document = ReceiptDocument.setAllCategories(doc, chosenCategory!!)
            message = null
        }
    }

    fun remove(index: Int) {
        val doc = document ?: return
        document = ReceiptDocument.removeLine(doc, index)
        checked = checked.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
    }

    fun save(onDone: () -> Unit) {
        val doc = document ?: return
        viewModelScope.launch {
            when (val result = receipts.confirm(localId, doc)) {
                ReviewSaveResult.Accepted, ReviewSaveResult.Parked -> onDone()
                ReviewSaveResult.Failed -> sendFailed = true
                is ReviewSaveResult.Refused -> message = when (result.refusal) {
                    ReceiptSaveRefusal.NoLines -> R.string.receipt_needs_line
                    ReceiptSaveRefusal.UnlistedCategory -> R.string.unlisted_category
                }
            }
        }
    }

    fun back(onLanding: () -> Unit, onPhotos: () -> Unit) {
        viewModelScope.launch {
            if (direct) {
                receipts.delete(localId)
                onLanding()
            } else {
                receipts.revertToBaseline(localId)
                onPhotos()
            }
        }
    }

    companion object {
        fun factory(receipts: ReceiptRepository, landing: LandingRepository, localId: String, direct: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReceiptReviewViewModel(receipts, landing, localId, direct) as T
            }
    }
}
