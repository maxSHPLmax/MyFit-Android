package com.maxshpl.myfit.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.plan.PlanRepository
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

data class PlannedMealOnDiary(
    val id: Long,
    val name: String,
    val time: String?,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val isApplied: Boolean,
)

data class DiaryUiState(
    val date: LocalDate,
    val rows: List<DiaryRow>,
    val totals: DayTotals,
    val targets: DailyTargets,
    val activityLogs: List<ActivityLogRow>,
    val burnedKcal: Double,
    val plannedMeals: List<PlannedMealOnDiary>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val repository: DiaryRepository,
    private val targetsRepository: TargetsRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val dateRepository: DiaryDateRepository,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val initialDate: LocalDate = LocalDate.now()

    private val _selectedDate = MutableStateFlow(initialDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val rowsFlow = _selectedDate.flatMapLatest { repository.rowsForDate(it) }
    private val totalsFlow = _selectedDate.flatMapLatest { repository.totalsForDate(it) }
    private val activityLogsFlow = _selectedDate.flatMapLatest { activityLogRepository.rowsForDate(it) }
    private val burnedFlow = _selectedDate.flatMapLatest { activityLogRepository.sumKcalForDate(it) }

    // План + статус "съел" — один источник даты для обоих внутренних потоков,
    // чтобы при переключении даты meals и appliedIds не разъезжались.
    private val plannedMealsFlow = _selectedDate.flatMapLatest { date ->
        combine(
            planRepository.observeMealsForDay(date.dayOfWeek),
            planRepository.observeAppliedMealIds(date),
        ) { meals, appliedIds ->
            meals.map { m ->
                PlannedMealOnDiary(
                    id = m.id,
                    name = m.name,
                    time = m.time,
                    kcal = m.kcal,
                    protein = m.protein,
                    fat = m.fat,
                    carbs = m.carbs,
                    isApplied = m.id in appliedIds,
                )
            }
        }
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        _selectedDate,
        rowsFlow,
        totalsFlow,
        targetsRepository.targets,
        activityLogsFlow,
        burnedFlow,
        plannedMealsFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DiaryUiState(
            date = values[0] as LocalDate,
            rows = values[1] as List<DiaryRow>,
            totals = values[2] as DayTotals,
            targets = values[3] as DailyTargets,
            activityLogs = values[4] as List<ActivityLogRow>,
            burnedKcal = values[5] as Double,
            plannedMeals = values[6] as List<PlannedMealOnDiary>,
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
            plannedMeals = emptyList(),
        ),
    )

    init {
        // Cold start: применяем 3-day stale fallback однократно — после долгого
        // перерыва открываем сегодня, а не "позавчера". Дальше repository — чистый
        // источник истины: явные click-through из других экранов уважаются.
        //
        // Если fallback сработал, синхронизируем DataStore: setLastViewedDate await'ится
        // до collect, и первый emit от .currentDate будет уже resolved-значением →
        // защита `!= _selectedDate.value` спокойно даст no-op. Без sync write был бы
        // цикл: collect получает stale → fallback в repository → today → но в store
        // всё ещё stale → emit stale → ... (бесконечный пинг-понг).
        //
        // Защита от race на последующих emit'ах: пишем _selectedDate только если
        // значение из DataStore отличается от текущего. Иначе параллельный setDate(X)
        // + collect могли бы откатить UI.
        viewModelScope.launch {
            val raw = dateRepository.currentDate.first()
            val today = LocalDate.now()
            val resolved = if (raw.isBefore(today.minusDays(STALE_THRESHOLD_DAYS))) {
                today
            } else {
                raw
            }
            _selectedDate.value = resolved
            if (resolved != raw) {
                dateRepository.setLastViewedDate(resolved)
            }
            dateRepository.currentDate.collect { fromStore ->
                if (fromStore != _selectedDate.value) {
                    _selectedDate.value = fromStore
                }
            }
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

    fun applyPlannedMeal(mealId: Long) {
        viewModelScope.launch {
            planRepository.applyMealToDate(mealId, _selectedDate.value)
        }
    }

    fun unapplyPlannedMeal(mealId: Long) {
        viewModelScope.launch {
            planRepository.unapplyMealFromDate(mealId, _selectedDate.value)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        // После N дней простоя cold start открывает сегодня, не последнюю дату.
        // Применяется только в init, не в repository. См. init блок.
        private const val STALE_THRESHOLD_DAYS = 3L

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
                    planRepository = PlanRepository(
                        planDao = db.planDao(),
                        diaryEntryDao = db.diaryEntryDao(),
                    ),
                )
            }
        }
    }
}
