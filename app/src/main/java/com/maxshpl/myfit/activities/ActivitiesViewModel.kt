package com.maxshpl.myfit.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.data.AppDatabase
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

class ActivitiesViewModel(
    private val repository: ActivityRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

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
        viewModelScope.launch { repository.delete(activity) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val dao = AppDatabase.get(application).activityDao()
                ActivitiesViewModel(ActivityRepository(dao))
            }
        }
    }
}
