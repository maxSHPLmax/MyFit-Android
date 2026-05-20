package com.maxshpl.myfit.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.diary.DiaryDateRepository
import com.maxshpl.myfit.settings.DailyTargets
import com.maxshpl.myfit.settings.TargetsRepository
import com.maxshpl.myfit.settings.settingsDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HistoryPeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
}

data class HistoryUiState(
    val period: HistoryPeriod,
    val days: List<DayHistoryItem>,
    val targets: DailyTargets,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val diaryDateRepository: DiaryDateRepository,
    targetsRepository: TargetsRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(HistoryPeriod.WEEK)
    val period: StateFlow<HistoryPeriod> = _period

    private val daysFlow = _period.flatMapLatest { period ->
        val today = LocalDate.now()
        val start = today.minusDays((period.days - 1).toLong())
        historyRepository.observeRange(start, today)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        _period,
        daysFlow,
        targetsRepository.targets,
    ) { period, days, targets ->
        HistoryUiState(period = period, days = days, targets = targets)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HistoryUiState(
            period = HistoryPeriod.WEEK,
            days = emptyList(),
            targets = DailyTargets.Default,
        ),
    )

    fun setPeriod(period: HistoryPeriod) {
        _period.value = period
    }

    /**
     * Подготовка к навигации на Diary: пишем дату в DataStore.
     * DiaryViewModel подписан на этот repository и подтянет изменение
     * автоматически (см. DiaryViewModel.init).
     */
    fun openDiary(date: LocalDate) {
        viewModelScope.launch {
            diaryDateRepository.setLastViewedDate(date)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(application)
                HistoryViewModel(
                    historyRepository = HistoryRepository(
                        diaryEntryDao = db.diaryEntryDao(),
                        activityLogDao = db.activityLogDao(),
                    ),
                    diaryDateRepository = DiaryDateRepository(application.settingsDataStore),
                    targetsRepository = TargetsRepository(application.settingsDataStore),
                )
            }
        }
    }
}
