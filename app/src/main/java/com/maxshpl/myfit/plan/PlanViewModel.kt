package com.maxshpl.myfit.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val repository: PlanRepository,
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek)
    val selectedDay: StateFlow<DayOfWeek> = _selectedDay

    val meals: StateFlow<List<PlannedMealRow>> = _selectedDay
        .flatMapLatest { repository.observeMealsForDay(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    fun setSelectedDay(day: DayOfWeek) {
        _selectedDay.value = day
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { repository.deleteMealById(id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(application)
                PlanViewModel(
                    repository = PlanRepository(
                        planDao = db.planDao(),
                        diaryEntryDao = db.diaryEntryDao(),
                    ),
                )
            }
        }
    }
}
