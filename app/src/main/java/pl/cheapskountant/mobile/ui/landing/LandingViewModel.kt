package pl.cheapskountant.mobile.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.cheapskountant.mobile.data.repository.DaySection
import pl.cheapskountant.mobile.data.repository.LandingRepository
import pl.cheapskountant.mobile.data.repository.MonthSection
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModel(private val repository: LandingRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val day = MutableStateFlow(LocalDate.now())

    val selectedMonth = month.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearMonth.now())
    val selectedDay = day.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())
    val monthSection = month.flatMapLatest { repository.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMonth())
    val daySection = day.flatMapLatest { repository.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DaySection(emptyList(), false, false))

    init {
        viewModelScope.launch { repository.refreshCategories() }
        refreshMonth()
        refreshDay()
    }

    fun setMonth(value: YearMonth) {
        if (value == month.value) return
        month.value = value
        refreshMonth()
    }

    fun setDay(value: LocalDate) {
        if (value == day.value) return
        day.value = value
        refreshDay()
    }

    private fun refreshMonth() {
        viewModelScope.launch { repository.refreshMonth(month.value) }
    }

    private fun refreshDay() {
        viewModelScope.launch { repository.refreshDay(day.value) }
    }

    private fun emptyMonth() = MonthSection("PLN", java.math.BigDecimal.ZERO.setScale(2), emptyList(), false, false)

    companion object {
        fun factory(repository: LandingRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LandingViewModel(repository) as T
        }
    }
}
