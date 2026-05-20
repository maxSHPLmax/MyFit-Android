package com.maxshpl.myfit.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.activities.Activity
import com.maxshpl.myfit.activities.ActivityRepository
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class AddActivityLogUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingLogId: Long? = savedStateHandle.get<Long>(NavArgs.LOG_ID)
    private val targetDate: LocalDate = parseDateArg(savedStateHandle[NavArgs.DATE])

    private var editingLog: ActivityLog? = null

    private val _isEditing = MutableStateFlow(editingLogId != null)
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
        _isEditing,
    ) { activities, query, selected, duration, isEditing ->
        val filtered = if (query.isBlank()) activities
        else activities.filter { it.name.contains(query.trim(), ignoreCase = true) }
        AddActivityLogUiState(
            isLoading = false,
            isEditing = isEditing,
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
        initialValue = AddActivityLogUiState(isEditing = editingLogId != null),
    )

    init {
        if (editingLogId != null) {
            viewModelScope.launch {
                val log = activityLogRepository.getById(editingLogId) ?: return@launch
                val activity = activityRepository.getById(log.activityId) ?: return@launch
                editingLog = log
                _selectedActivity.update { activity }
                _durationText.update { durationToText(log.durationMinutes) }
            }
        }
    }

    fun setQuery(value: String) = _query.update { value }

    fun selectActivity(activity: Activity) {
        if (_isEditing.value) return
        _selectedActivity.update { activity }
        _durationText.update { "" }
        _durationError.update { null }
    }

    fun clearSelection() {
        if (_isEditing.value) return
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
            val existing = editingLog
            if (existing != null) {
                activityLogRepository.update(existing.copy(durationMinutes = duration))
            } else {
                activityLogRepository.add(targetDate, activity.id, duration)
            }
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

    private fun durationToText(minutes: Double): String =
        if (minutes % 1.0 == 0.0) minutes.toInt().toString() else "%.1f".format(minutes)

    private data class DurationBlock(
        val text: String,
        val error: String?,
        val isSaving: Boolean,
        val saveCompleted: Boolean,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        private fun parseDateArg(raw: String?): LocalDate = raw?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        } ?: LocalDate.now()

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(app)
                AddActivityLogViewModel(
                    activityRepository = ActivityRepository(db.activityDao()),
                    activityLogRepository = ActivityLogRepository(db.activityLogDao()),
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
