package com.maxshpl.myfit.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.activities.Activity
import com.maxshpl.myfit.activities.ActivityRepository
import com.maxshpl.myfit.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddActivityLogUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val activities: List<Activity> = emptyList(),
    val selectedActivity: Activity? = null,
    val durationText: String = "",
    val durationError: String? = null,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
)

class AddActivityLogViewModel(
    private val activityRepository: ActivityRepository,
    private val activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    private val _query = MutableStateFlow("")
    private val _selectedActivity = MutableStateFlow<Activity?>(null)
    private val _durationText = MutableStateFlow("")
    private val _durationError = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _saveCompleted = MutableStateFlow(false)

    val uiState: StateFlow<AddActivityLogUiState> = combine(
        activityRepository.activities,
        _query,
        _selectedActivity,
        combine(_durationText, _durationError, _isSaving, _saveCompleted, ::DurationBlock),
    ) { activities, query, selected, duration ->
        val filtered = if (query.isBlank()) activities
        else activities.filter { it.name.contains(query.trim(), ignoreCase = true) }
        AddActivityLogUiState(
            isLoading = false,
            query = query,
            activities = filtered,
            selectedActivity = selected,
            durationText = duration.text,
            durationError = duration.error,
            isSaving = duration.isSaving,
            saveCompleted = duration.saveCompleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AddActivityLogUiState(),
    )

    fun setQuery(value: String) = _query.update { value }

    fun selectActivity(activity: Activity) {
        _selectedActivity.update { activity }
        _durationText.update { "" }
        _durationError.update { null }
    }

    fun clearSelection() {
        _selectedActivity.update { null }
        _durationText.update { "" }
        _durationError.update { null }
    }

    fun setDurationText(value: String) {
        _durationText.update { value }
        if (_durationError.value != null) _durationError.update { null }
    }

    fun save() {
        val activity = _selectedActivity.value ?: return
        val duration = parseDuration(_durationText.value)
        if (duration == null) {
            _durationError.update { "Введите число больше 0 (например, 30 или 12,5)" }
            return
        }
        if (_isSaving.value) return
        _isSaving.update { true }
        viewModelScope.launch {
            activityLogRepository.add(today, activity.id, duration)
            _saveCompleted.update { true }
        }
    }

    private fun parseDuration(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return value.takeIf { it > 0.0 }
    }

    private data class DurationBlock(
        val text: String,
        val error: String?,
        val isSaving: Boolean,
        val saveCompleted: Boolean,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(app)
                AddActivityLogViewModel(
                    activityRepository = ActivityRepository(db.activityDao()),
                    activityLogRepository = ActivityLogRepository(db.activityLogDao()),
                )
            }
        }
    }
}
