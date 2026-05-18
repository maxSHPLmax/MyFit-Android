package com.maxshpl.myfit.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val KCAL_RANGE_MIN = 1.0
private const val KCAL_RANGE_MAX = 20.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityScreen(
    onBack: () -> Unit,
    viewModel: AddEditActivityViewModel = viewModel(factory = AddEditActivityViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onBack()
    }
    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditMode) "Редактировать активность" else "Новая активность")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Название") },
                singleLine = true,
                isError = state.errors.name != null,
                supportingText = { state.errors.name?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            val softWarning = computeKcalWarning(state.kcalPerMin)
            OutlinedTextField(
                value = state.kcalPerMin,
                onValueChange = viewModel::onKcalPerMinChange,
                label = { Text("Ккал/мин") },
                singleLine = true,
                isError = state.errors.kcalPerMin != null,
                supportingText = {
                    val err = state.errors.kcalPerMin
                    when {
                        err != null -> Text(err)
                        softWarning != null -> Text(
                            text = softWarning,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditMode) "Сохранить" else "Добавить")
            }
        }
    }
}

private fun computeKcalWarning(raw: String): String? {
    val parsed = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (parsed <= 0.0) return null
    if (parsed < KCAL_RANGE_MIN) {
        return "Низкое значение — обычно активности от ${KCAL_RANGE_MIN.toInt()} ккал/мин"
    }
    if (parsed > KCAL_RANGE_MAX) {
        return "Высокое значение — обычно до ${KCAL_RANGE_MAX.toInt()} ккал/мин (интенсивные нагрузки)"
    }
    return null
}
