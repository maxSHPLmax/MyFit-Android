package com.maxshpl.myfit.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.diary.ActivityLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ActivitiesUiState {
    data object Loading : ActivitiesUiState
    data class Success(val activities: List<Activity>) : ActivitiesUiState
    data class Error(val message: String) : ActivitiesUiState
}

data class ActivityDeleteBlocked(val activity: Activity, val logCount: Int)

class ActivitiesViewModel(
    private val repository: ActivityRepository,
    private val activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _deleteBlocked = MutableStateFlow<ActivityDeleteBlocked?>(null)
    val deleteBlocked: StateFlow<ActivityDeleteBlocked?> = _deleteBlocked.asStateFlow()

    val uiState: StateFlow<ActivitiesUiState> = combine(
        repository.activities,
        _query,
    ) { all, q ->
        val filtered = if (q.isBlank()) all
        else all.filter { it.name.contains(q.trim(), ignoreCase = true) }
        ActivitiesUiState.Success(filtered) as ActivitiesUiState
    }
        .catch { emit(ActivitiesUiState.Error(it.message ?: "Не удалось загрузить активности")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ActivitiesUiState.Loading,
        )

    fun setQuery(value: String) = _query.update { value }

    fun delete(activity: Activity) {
        viewModelScope.launch {
            when (repository.delete(activity)) {
                DeleteActivityResult.Success -> Unit
                DeleteActivityResult.InUse -> {
                    val count = activityLogRepository.countByActivity(activity.id)
                    _deleteBlocked.update { ActivityDeleteBlocked(activity, count) }
                }
            }
        }
    }

    fun dismissDeleteBlocked() {
        _deleteBlocked.update { null }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val db = AppDatabase.get(application)
                ActivitiesViewModel(
                    repository = ActivityRepository(db.activityDao()),
                    activityLogRepository = ActivityLogRepository(db.activityLogDao()),
                )
            }
        }
    }
}
