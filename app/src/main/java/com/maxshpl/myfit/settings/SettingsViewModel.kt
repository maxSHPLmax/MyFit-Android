package com.maxshpl.myfit.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TargetsFormState(
    val isReady: Boolean = false,
    val kcalText: String = "",
    val proteinText: String = "",
    val fatText: String = "",
    val carbsText: String = "",
    val kcalError: String? = null,
    val proteinError: String? = null,
    val fatError: String? = null,
    val carbsError: String? = null,
    val isSaving: Boolean = false,
) {
    val isValid: Boolean
        get() = parsePositive(kcalText) != null &&
            parsePositive(proteinText) != null &&
            parsePositive(fatText) != null &&
            parsePositive(carbsText) != null

    companion object {
        fun parsePositive(text: String): Int? =
            text.trim().toIntOrNull()?.takeIf { it > 0 }
    }
}

class SettingsViewModel(
    private val themeRepository: ThemePreferencesRepository,
    private val targetsRepository: TargetsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ThemeMode.SYSTEM,
    )

    private val _targetsForm = MutableStateFlow(TargetsFormState())
    val targetsForm: StateFlow<TargetsFormState> = _targetsForm.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = targetsRepository.targets.first()
            _targetsForm.update {
                TargetsFormState(
                    isReady = true,
                    kcalText = initial.kcal.toString(),
                    proteinText = initial.proteinG.toString(),
                    fatText = initial.fatG.toString(),
                    carbsText = initial.carbsG.toString(),
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    fun setKcal(value: String) = updateField { it.copy(kcalText = value, kcalError = null) }
    fun setProtein(value: String) = updateField { it.copy(proteinText = value, proteinError = null) }
    fun setFat(value: String) = updateField { it.copy(fatText = value, fatError = null) }
    fun setCarbs(value: String) = updateField { it.copy(carbsText = value, carbsError = null) }

    fun saveTargets() {
        val current = _targetsForm.value
        if (current.isSaving) return
        val k = TargetsFormState.parsePositive(current.kcalText)
        val p = TargetsFormState.parsePositive(current.proteinText)
        val f = TargetsFormState.parsePositive(current.fatText)
        val c = TargetsFormState.parsePositive(current.carbsText)
        if (k == null || p == null || f == null || c == null) {
            _targetsForm.update {
                it.copy(
                    kcalError = if (k == null) ERR_POSITIVE else null,
                    proteinError = if (p == null) ERR_POSITIVE else null,
                    fatError = if (f == null) ERR_POSITIVE else null,
                    carbsError = if (c == null) ERR_POSITIVE else null,
                )
            }
            return
        }
        _targetsForm.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            targetsRepository.setTargets(
                DailyTargets(kcal = k, proteinG = p, fatG = f, carbsG = c),
            )
            _targetsForm.update { it.copy(isSaving = false) }
        }
    }

    private inline fun updateField(transform: (TargetsFormState) -> TargetsFormState) {
        _targetsForm.update(transform)
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val ERR_POSITIVE = "Введите число больше 0"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                SettingsViewModel(
                    themeRepository = ThemePreferencesRepository(app.settingsDataStore),
                    targetsRepository = TargetsRepository(app.settingsDataStore),
                )
            }
        }
    }
}
