package com.maxshpl.myfit.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMacro
import com.maxshpl.myfit.products.ProductPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    onBack: () -> Unit,
    viewModel: AddEditMealViewModel = viewModel(factory = AddEditMealViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditing) "Редактирование приёма" else "Новый приём")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.picker !is ItemPicker.Hidden) {
                                viewModel.closePicker()
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.picker is ItemPicker.ProductSearch -> {
                    val picker = state.picker as ItemPicker.ProductSearch
                    ProductPicker(
                        query = picker.query,
                        products = state.visibleProducts,
                        onQueryChange = viewModel::setProductQuery,
                        onProductClick = viewModel::selectProduct,
                        searchPlaceholder = "Поиск продукта",
                    )
                }

                state.picker is ItemPicker.GramsEntry -> {
                    val picker = state.picker as ItemPicker.GramsEntry
                    GramsForItem(
                        productName = picker.product.name,
                        productKcal = picker.product.kcalPer100g,
                        gramsText = picker.gramsText,
                        error = picker.error,
                        onGramsChange = viewModel::setGramsText,
                        onConfirm = viewModel::confirmItem,
                    )
                }

                else -> MealForm(
                    state = state,
                    onNameChange = viewModel::setName,
                    onTimeChange = viewModel::setTime,
                    onAddItemClick = viewModel::openProductPicker,
                    onRemoveItem = viewModel::removeItem,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealForm(
    state: AddEditMealUiState,
    onNameChange: (String) -> Unit,
    onTimeChange: (String?) -> Unit,
    onAddItemClick: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val totalKcal = state.items.sumOf { it.kcal }
    val totalProtein = state.items.sumOf { it.protein }
    val totalFat = state.items.sumOf { it.fat }
    val totalCarbs = state.items.sumOf { it.carbs }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.dayOfWeek.fullRu(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Название приёма") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            TimeRow(
                time = state.time,
                onPickClick = { showTimePicker = true },
                onClearClick = { onTimeChange(null) },
            )

            HorizontalDivider()
            Text(
                text = "Продукты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.items.isEmpty()) {
                Text(
                    text = "Пока ни одного продукта",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((state.items.size * 72).coerceAtMost(360)).dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.items, key = { _, it -> "${it.productId}-${it.grams}" }) { idx, item ->
                        MealItemRow(item = item, onRemove = { onRemoveItem(idx) })
                    }
                }
            }
            OutlinedButton(
                onClick = onAddItemClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Продукт")
            }

            if (state.items.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Итого: ${formatKcal(totalKcal)} ккал · " +
                        "Б ${formatMacro(totalProtein)} · " +
                        "Ж ${formatMacro(totalFat)} · " +
                        "У ${formatMacro(totalCarbs)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }
    }

    if (showTimePicker) {
        TimePickerDialogContent(
            initialTime = state.time,
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                onTimeChange(picked)
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(
    time: String?,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = onPickClick,
            label = { Text(time ?: "Время не указано") },
        )
        if (time != null) {
            IconButton(onClick = onClearClick) {
                Icon(Icons.Default.Clear, contentDescription = "Убрать время")
            }
        }
    }
}

@Composable
private fun MealItemRow(item: MealItemDraft, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${formatMacro(item.grams)} г · ${formatKcal(item.kcal)} ккал",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить продукт")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    initialTime: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val (hour, minute) = parseHm(initialTime)
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Время приёма",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        onConfirm("%02d:%02d".format(state.hour, state.minute))
                    }) { Text("Ок") }
                }
            }
        }
    }
}

@Composable
private fun GramsForItem(
    productName: String,
    productKcal: Int,
    gramsText: String,
    error: String?,
    onGramsChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$productKcal ккал на 100 г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = gramsText,
            onValueChange = onGramsChange,
            label = { Text("Вес, г") },
            singleLine = true,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onConfirm,
            enabled = gramsText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Добавить в приём")
        }
    }
}

private fun parseHm(time: String?): Pair<Int, Int> {
    if (time == null) return 12 to 0
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}
