package com.maxshpl.myfit.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxshpl.myfit.health.HealthConnectAvailability
import com.maxshpl.myfit.health.HealthConnectHolder
import com.maxshpl.myfit.health.HealthConnectPreferencesRepository
import com.maxshpl.myfit.health.HealthConnectRepository
import com.maxshpl.myfit.reminders.MealKind
import com.maxshpl.myfit.reminders.RemindersConfig
import com.maxshpl.myfit.reminders.RemindersRepository
import com.maxshpl.myfit.reminders.ReminderScheduler
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
    private val remindersRepository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
    private val healthConnectRepository: HealthConnectRepository,
    private val healthConnectPreferences: HealthConnectPreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ThemeMode.SYSTEM,
    )

    val remindersConfig: StateFlow<RemindersConfig> = remindersRepository.currentConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RemindersConfig.Default,
    )

    /**
     * Switch в Settings.HealthConnectSection отражает только это значение.
     * Локального optimistic state нет: если пользователь тапнул ON и отказал в
     * permissions через системный диалог HC — setEnabled(true) не вызывается,
     * DataStore остаётся false, Switch автоматически возвращается в OFF.
     */
    val healthConnectEnabled: StateFlow<Boolean> = healthConnectPreferences.enabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = false,
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

    /**
     * Главный toggle напоминаний. При enabled=true scheduler ставит все
     * enabled-слоты; при false — отменяет все. Permission flow (POST_NOTIFICATIONS
     * на Android 13+) живёт в UI — VM просто следует команде.
     */
    fun setRemindersMainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            remindersRepository.setMainEnabled(enabled)
            val config = remindersRepository.currentConfig.first()
            if (enabled) {
                reminderScheduler.scheduleAll(config)
            } else {
                reminderScheduler.cancelAll()
            }
        }
    }

    fun setRemindersSlotEnabled(kind: MealKind, enabled: Boolean) {
        viewModelScope.launch {
            remindersRepository.setSlotEnabled(kind, enabled)
            rescheduleIfMainEnabled()
        }
    }

    fun setRemindersSlotTime(kind: MealKind, hour: Int, minute: Int) {
        viewModelScope.launch {
            remindersRepository.setSlotTime(kind, hour, minute)
            rescheduleIfMainEnabled()
        }
    }

    private suspend fun rescheduleIfMainEnabled() {
        val config = remindersRepository.currentConfig.first()
        if (config.enabled) {
            reminderScheduler.scheduleAll(config)
        }
    }

    /**
     * UI вызывает в LifecycleEventObserver.ON_RESUME — пользователь мог уйти
     * в системные настройки exact alarms и вернуться. Compose не реагирует
     * на изменение permission'а сам, поэтому проверяем на каждом resume.
     */
    fun canScheduleExact(): Boolean = reminderScheduler.canScheduleExact()

    /**
     * Snapshot чтения SDK status. UI обновляет его на ON_RESUME — пользователь
     * мог установить/обновить HC через Play Store. Аналогично canScheduleExact.
     */
    fun healthConnectAvailability(): HealthConnectAvailability =
        healthConnectRepository.availability()

    suspend fun hasHealthConnectPermissions(): Boolean =
        healthConnectRepository.hasAllPermissions()

    /**
     * Включение/выключение чтения HC. Из UI вызывается ТОЛЬКО когда permissions
     * подтверждены (для true) или безусловно (для false). Если permissions нет
     * и юзер тапнул ON — UI запускает launcher и вызовет setEnabled(true) только
     * после granted callback. См. требование Lead'а из обсуждения коммита 5.
     */
    fun setHealthConnectEnabled(value: Boolean) {
        viewModelScope.launch { healthConnectPreferences.setEnabled(value) }
    }

    fun scheduleTestReminder() {
        reminderScheduler.scheduleTest(TEST_REMINDER_DELAY_MS)
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
        private const val TEST_REMINDER_DELAY_MS = 60_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("APPLICATION_KEY missing in CreationExtras")
                SettingsViewModel(
                    themeRepository = ThemePreferencesRepository(app.settingsDataStore),
                    targetsRepository = TargetsRepository(app.settingsDataStore),
                    remindersRepository = RemindersRepository(app.settingsDataStore),
                    reminderScheduler = ReminderScheduler(app),
                    healthConnectRepository = HealthConnectHolder.get(app),
                    healthConnectPreferences = HealthConnectPreferencesRepository(
                        app.settingsDataStore,
                    ),
                )
            }
        }
    }
}
