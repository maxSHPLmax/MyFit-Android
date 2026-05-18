package com.maxshpl.myfit.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.settings.DailyTargets
import com.maxshpl.myfit.settings.TargetsRepository
import com.maxshpl.myfit.settings.settingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DiaryUiState(
    val date: LocalDate,
    val rows: List<DiaryRow>,
    val totals: DayTotals,
    val targets: DailyTargets,
    // Сожжённые калории за день. KAN-17 заменит константу на Flow<Double>
    // от ActivityLog; UI уже использует это поле для расчёта Баланса.
    val burnedKcal: Double,
)

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val targetsRepository: TargetsRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<DiaryUiState> = combine(
        repository.rowsForDate(today),
        repository.totalsForDate(today),
        targetsRepository.targets,
    ) { rows, totals, targets ->
        DiaryUiState(
            date = today,
            rows = rows,
            totals = totals,
            targets = targets,
            burnedKcal = BURNED_KCAL_STUB,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DiaryUiState(
            date = today,
            rows = emptyList(),
            totals = DayTotals.Empty,
            targets = DailyTargets.Default,
            burnedKcal = BURNED_KCAL_STUB,
        ),
    )

    fun delete(entryId: Long) {
        viewModelScope.launch { repository.deleteById(entryId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val BURNED_KCAL_STUB = 0.0

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val dao = AppDatabase.get(application).diaryEntryDao()
                DiaryViewModel(
                    repository = DiaryRepository(dao),
                    targetsRepository = TargetsRepository(application.settingsDataStore),
                )
            }
        }
    }
}
