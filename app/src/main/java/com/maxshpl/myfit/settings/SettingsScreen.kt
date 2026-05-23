package com.maxshpl.myfit.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.health.connect.client.PermissionController
import com.maxshpl.myfit.BuildConfig
import com.maxshpl.myfit.core.TimePickerDialog
import com.maxshpl.myfit.health.HealthConnectAvailability
import com.maxshpl.myfit.health.HealthConnectRepository
import com.maxshpl.myfit.reminders.MealKind
import com.maxshpl.myfit.reminders.ReminderSlot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageActivitiesClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val targetsForm by viewModel.targetsForm.collectAsStateWithLifecycle()
    val remindersConfig by viewModel.remindersConfig.collectAsStateWithLifecycle()
    val healthConnectEnabled by viewModel.healthConnectEnabled.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }
    var editingTimeFor by remember { mutableStateOf<MealKind?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // canScheduleExact — runtime permission, не Flow. Пересчитываем на каждом
    // ON_RESUME: пользователь мог уйти в системные настройки exact alarms
    // и вернуться → hint card должна обновиться.
    var canScheduleExact by remember { mutableStateOf(viewModel.canScheduleExact()) }
    // HC availability — то же самое: snapshot SDK status, рефрешим на ON_RESUME
    // (юзер мог обновить HC через Play Store или включить provider).
    var healthConnectAvailability by remember {
        mutableStateOf(viewModel.healthConnectAvailability())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = viewModel.canScheduleExact()
                healthConnectAvailability = viewModel.healthConnectAvailability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(HealthConnectRepository.PERMISSIONS)) {
            // Permissions подтверждены → DataStore.enabled=true → Switch встанет в ON
            // через collectAsStateWithLifecycle. Локального state нет, реакция автоматическая.
            viewModel.setHealthConnectEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Доступ к Health Connect не предоставлен",
                )
            }
        }
    }

    val onToggleHealthConnect: (Boolean) -> Unit = onToggle@{ target ->
        if (!target) {
            viewModel.setHealthConnectEnabled(false)
            return@onToggle
        }
        // ON: сначала проверка permissions. Если есть — setEnabled(true) сразу.
        // Если нет — launcher; setEnabled вызовется только в granted callback.
        scope.launch {
            val granted = viewModel.hasHealthConnectPermissions()
            if (granted) {
                viewModel.setHealthConnectEnabled(true)
            } else {
                healthConnectPermissionLauncher.launch(HealthConnectRepository.PERMISSIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setRemindersMainEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Разрешите уведомления в настройках приложения",
                )
            }
        }
    }

    val onToggleRemindersMain: (Boolean) -> Unit = onToggle@{ target ->
        if (!target) {
            viewModel.setRemindersMainEnabled(false)
            return@onToggle
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.setRemindersMainEnabled(true)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // < Android 13: notifications не требуют runtime permission.
            viewModel.setRemindersMainEnabled(true)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("theme") {
                ThemeSection(
                    selected = themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }
            item("reminders") {
                RemindersSection(
                    config = remindersConfig,
                    canScheduleExact = canScheduleExact,
                    onToggleMain = onToggleRemindersMain,
                    onToggleSlot = viewModel::setRemindersSlotEnabled,
                    onSlotTimeClick = { kind -> editingTimeFor = kind },
                    onRequestExactAlarms = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onTestClick = viewModel::scheduleTestReminder,
                )
            }
            item("health_connect") {
                HealthConnectSection(
                    availability = healthConnectAvailability,
                    enabled = healthConnectEnabled,
                    onToggle = onToggleHealthConnect,
                    onOpenPlayStore = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(
                                "market://details?id=com.google.android.apps.healthdata",
                            )
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
            item("targets") {
                TargetsSection(
                    form = targetsForm,
                    onKcalChange = viewModel::setKcal,
                    onProteinChange = viewModel::setProtein,
                    onFatChange = viewModel::setFat,
                    onCarbsChange = viewModel::setCarbs,
                    onSave = viewModel::saveTargets,
                )
            }
            item("menu") {
                MenuSection(
                    onManageActivitiesClick = onManageActivitiesClick,
                    onAboutClick = { showAbout = true },
                )
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    editingTimeFor?.let { kind ->
        val slot = remindersConfig.slotFor(kind)
        TimePickerDialog(
            initialTime = "%02d:%02d".format(slot.hour, slot.minute),
            title = "Время напоминания: ${kind.settingsLabel}",
            onDismiss = { editingTimeFor = null },
            onConfirm = { picked ->
                val parts = picked.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: slot.hour
                val m = parts.getOrNull(1)?.toIntOrNull() ?: slot.minute
                viewModel.setRemindersSlotTime(kind, h, m)
                editingTimeFor = null
            },
        )
    }
}

@Composable
private fun RemindersSection(
    config: com.maxshpl.myfit.reminders.RemindersConfig,
    canScheduleExact: Boolean,
    onToggleMain: (Boolean) -> Unit,
    onToggleSlot: (MealKind, Boolean) -> Unit,
    onSlotTimeClick: (MealKind) -> Unit,
    onRequestExactAlarms: () -> Unit,
    onTestClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Напоминания",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Завтрак, обед, ужин, перекус в заданное время",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggleMain,
                )
            }
            if (config.enabled) {
                if (!canScheduleExact) {
                    ExactAlarmHint(
                        onRequest = onRequestExactAlarms,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                MealKind.entries.forEachIndexed { idx, kind ->
                    SlotRow(
                        kind = kind,
                        slot = config.slotFor(kind),
                        onToggle = { enabled -> onToggleSlot(kind, enabled) },
                        onTimeClick = { onSlotTimeClick(kind) },
                    )
                    if (idx < MealKind.entries.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                if (BuildConfig.DEBUG) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onTestClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Тест: уведомление через 1 минуту") }
                }
            }
        }
    }
}

@Composable
private fun SlotRow(
    kind: MealKind,
    slot: ReminderSlot,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = kind.settingsLabel,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%02d:%02d".format(slot.hour, slot.minute),
            style = MaterialTheme.typography.bodyLarge,
            color = if (slot.enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .clickable(enabled = slot.enabled, onClick = onTimeClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Switch(checked = slot.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun ExactAlarmHint(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Уведомления могут опаздывать до 15 минут",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Разрешите точные будильники, чтобы напоминания приходили вовремя.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(
                onClick = onRequest,
                modifier = Modifier.padding(top = 4.dp),
            ) { Text("Открыть настройки") }
        }
    }
}

@Composable
private fun TargetsSection(
    form: TargetsFormState,
    onKcalChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Цели на день",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TargetField(
                label = "Калории, ккал",
                value = form.kcalText,
                error = form.kcalError,
                enabled = form.isReady,
                onChange = onKcalChange,
            )
            TargetField(
                label = "Белки, г",
                value = form.proteinText,
                error = form.proteinError,
                enabled = form.isReady,
                onChange = onProteinChange,
            )
            TargetField(
                label = "Жиры, г",
                value = form.fatText,
                error = form.fatError,
                enabled = form.isReady,
                onChange = onFatChange,
            )
            TargetField(
                label = "Углеводы, г",
                value = form.carbsText,
                error = form.carbsError,
                enabled = form.isReady,
                onChange = onCarbsChange,
            )
            Button(
                onClick = onSave,
                enabled = form.isReady && !form.isSaving && form.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuSection(
    onManageActivitiesClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingsMenuItem(
                title = "Управление активностями",
                subtitle = "Справочник видов активностей",
                onClick = onManageActivitiesClick,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsMenuItem(
                title = "Об приложении",
                subtitle = "Версия и ссылки",
                onClick = onAboutClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MyFit") },
        text = {
            Column {
                Text(
                    text = "Версия ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/maxshplmax/MyFit-Android") },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("GitHub репозиторий") }
                TextButton(
                    onClick = { uriHandler.openUri("https://maxshplmax.github.io/MyFit/") },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("PWA версия") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Тема",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ThemeOption(
                label = "Системная",
                hint = "Как в настройках телефона",
                value = ThemeMode.SYSTEM,
                selected = selected,
                onSelect = onSelect,
            )
            ThemeOption(
                label = "Светлая",
                hint = null,
                value = ThemeMode.LIGHT,
                selected = selected,
                onSelect = onSelect,
            )
            ThemeOption(
                label = "Тёмная",
                hint = null,
                value = ThemeMode.DARK,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun HealthConnectSection(
    availability: HealthConnectAvailability,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenPlayStore: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Здоровье и активность",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Чтение калорий и шагов из Health Connect (Galaxy Watch, Samsung Health)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            when (availability) {
                HealthConnectAvailability.Available -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Health Connect",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = onToggle,
                        )
                    }
                }
                HealthConnectAvailability.ProviderUpdateRequired -> {
                    Text(
                        text = "Требуется обновление Health Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(
                        onClick = onOpenPlayStore,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) { Text("Открыть Play Store") }
                }
                HealthConnectAvailability.NotInstalled -> {
                    Text(
                        text = "Health Connect недоступен на этом устройстве",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    hint: String?,
    value: ThemeMode,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (value == selected),
                onClick = { onSelect(value) },
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = (value == selected),
            onClick = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
