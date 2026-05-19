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
    val activityLogs: List<ActivityLogRow>,
    val burnedKcal: Double,
)

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val targetsRepository: TargetsRepository,
    private val activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<DiaryUiState> = combine(
        repository.rowsForDate(today),
        repository.totalsForDate(today),
        targetsRepository.targets,
        activityLogRepository.rowsForDate(today),
        activityLogRepository.sumKcalForDate(today),
    ) { rows, totals, targets, activityLogs, burnedKcal ->
        DiaryUiState(
            date = today,
            rows = rows,
            totals = totals,
            targets = targets,
            activityLogs = activityLogs,
            burnedKcal = burnedKcal,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DiaryUiState(
            date = today,
            rows = emptyList(),
            totals = DayTotals.Empty,
            targets = DailyTargets.Default,
            activityLogs = emptyList(),
            burnedKcal = 0.0,
        ),
    )

    fun delete(entryId: Long) {
        viewModelScope.launch { repository.deleteById(entryId) }
    }

    fun deleteActivityLog(logId: Long) {
        viewModelScope.launch { activityLogRepository.deleteById(logId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(application)
                DiaryViewModel(
                    repository = DiaryRepository(db.diaryEntryDao()),
                    targetsRepository = TargetsRepository(application.settingsDataStore),
                    activityLogRepository = ActivityLogRepository(db.activityLogDao()),
                )
            }
        }
    }
}
