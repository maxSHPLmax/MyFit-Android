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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val repository: DiaryRepository,
    private val targetsRepository: TargetsRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val dateRepository: DiaryDateRepository,
) : ViewModel() {

    private val initialDate: LocalDate = LocalDate.now()

    private val _selectedDate = MutableStateFlow(initialDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val rowsFlow = _selectedDate.flatMapLatest { repository.rowsForDate(it) }
    private val totalsFlow = _selectedDate.flatMapLatest { repository.totalsForDate(it) }
    private val activityLogsFlow = _selectedDate.flatMapLatest { activityLogRepository.rowsForDate(it) }
    private val burnedFlow = _selectedDate.flatMapLatest { activityLogRepository.sumKcalForDate(it) }

    val uiState: StateFlow<DiaryUiState> = combine(
        _selectedDate,
        rowsFlow,
        totalsFlow,
        targetsRepository.targets,
        activityLogsFlow,
        burnedFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DiaryUiState(
            date = values[0] as LocalDate,
            rows = values[1] as List<DiaryRow>,
            totals = values[2] as DayTotals,
            targets = values[3] as DailyTargets,
            activityLogs = values[4] as List<ActivityLogRow>,
            burnedKcal = values[5] as Double,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DiaryUiState(
            date = initialDate,
            rows = emptyList(),
            totals = DayTotals.Empty,
            targets = DailyTargets.Default,
            activityLogs = emptyList(),
            burnedKcal = 0.0,
        ),
    )

    init {
        viewModelScope.launch {
            _selectedDate.value = dateRepository.lastViewedDate.first()
        }
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch { dateRepository.setLastViewedDate(date) }
    }

    fun goPrev() = setDate(_selectedDate.value.minusDays(1))

    fun goNext() = setDate(_selectedDate.value.plusDays(1))

    fun goToday() = setDate(LocalDate.now())

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
                    dateRepository = DiaryDateRepository(application.settingsDataStore),
                )
            }
        }
    }
}
