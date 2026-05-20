package com.maxshpl.myfit.activities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.core.formatMacro
import com.maxshpl.myfit.data.AppDatabase
import com.maxshpl.myfit.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditActivityFormState(
    val name: String = "",
    val kcalPerMin: String = "",
    val errors: AddEditActivityErrors = AddEditActivityErrors(),
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val saveCompleted: Boolean = false,
    val notFound: Boolean = false,
)

data class AddEditActivityErrors(
    val name: String? = null,
    val kcalPerMin: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || kcalPerMin != null
}

class AddEditActivityViewModel(
    private val repository: ActivityRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val activityId: Long? =
        savedStateHandle.get<Long>(NavArgs.ACTIVITY_ID)?.takeIf { it > 0L }
    private val isEditMode: Boolean = activityId != null
    private var original: Activity? = null

    private val _state = MutableStateFlow(
        AddEditActivityFormState(isEditMode = isEditMode, isLoading = isEditMode),
    )
    val state: StateFlow<AddEditActivityFormState> = _state.asStateFlow()

    init {
        if (activityId != null) loadActivity(activityId)
    }

    private fun loadActivity(id: Long) {
        viewModelScope.launch {
            val activity = repository.getById(id)
            if (activity == null) {
                _state.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            original = activity
            _state.update {
                it.copy(
                    name = activity.name,
                    kcalPerMin = formatMacro(activity.kcalPerMin),
                    isLoading = false,
                )
            }
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onKcalPerMinChange(value: String) =
        _state.update { it.copy(kcalPerMin = value, errors = it.errors.copy(kcalPerMin = null)) }

    fun save() {
        val current = _state.value
        val errors = validate(current)
        if (errors.hasErrors) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            val activity = Activity(
                id = original?.id ?: 0L,
                name = current.name.trim(),
                kcalPerMin = current.kcalPerMin.normalizeDouble(),
            )
            val result =
                if (original == null) repository.insert(activity)
                else repository.update(activity)
            when (result) {
                is SaveActivityResult.Success ->
                    _state.update { it.copy(saveCompleted = true) }
                SaveActivityResult.DuplicateName ->
                    _state.update {
                        it.copy(
                            errors = it.errors.copy(
                                name = "Активность с таким именем уже существует",
                            ),
                        )
                    }
            }
        }
    }

    private fun validate(s: AddEditActivityFormState): AddEditActivityErrors =
        AddEditActivityErrors(
            name = if (s.name.isBlank()) "Введите название" else null,
            kcalPerMin = validatePositiveDouble(s.kcalPerMin),
        )

    private fun validatePositiveDouble(raw: String): String? {
        if (raw.isBlank()) return "Заполните"
        val parsed = raw.replace(',', '.').toDoubleOrNull() ?: return "Число"
        if (parsed <= 0.0) return "Больше 0"
        return null
    }

    private fun String.normalizeDouble(): Double = replace(',', '.').toDouble()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                val dao = AppDatabase.get(app).activityDao()
                AddEditActivityViewModel(
                    repository = ActivityRepository(dao),
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
