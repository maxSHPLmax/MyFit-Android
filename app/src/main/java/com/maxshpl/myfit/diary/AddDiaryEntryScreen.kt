package com.maxshpl.myfit.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.products.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    onBack: () -> Unit,
    viewModel: AddDiaryEntryViewModel = viewModel(factory = AddDiaryEntryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить продукт") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.selectedProduct != null) {
                                viewModel.clearSelection()
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
            val selected = state.selectedProduct
            if (selected == null) {
                ProductPicker(
                    query = state.query,
                    products = state.products,
                    onQueryChange = viewModel::setQuery,
                    onProductClick = viewModel::selectProduct,
                )
            } else {
                GramsEntry(
                    product = selected,
                    gramsText = state.gramsText,
                    gramsError = state.gramsError,
                    isSaving = state.isSaving,
                    onGramsChange = viewModel::setGramsText,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPicker(
    query: String,
    products: List<Product>,
    onQueryChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск по имени") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (query.isBlank()) "Список пуст" else "Ничего не найдено",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    ProductPickerRow(product = product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerRow(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatProductMacros(product),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GramsEntry(
    product: Product,
    gramsText: String,
    gramsError: String?,
    isSaving: Boolean,
    onGramsChange: (String) -> Unit,
    onSave: () -> Unit,
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
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatProductMacros(product),
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
            isError = gramsError != null,
            supportingText = { gramsError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        val preview = computePreview(product, gramsText)
        if (preview != null) {
            HorizontalDivider()
            Text(
                text = "Будет добавлено",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PreviewCell(value = preview.kcal.toInt().toString(), label = "ккал")
                PreviewCell(value = formatMacro(preview.protein), label = "Б, г")
                PreviewCell(value = formatMacro(preview.fat), label = "Ж, г")
                PreviewCell(value = formatMacro(preview.carbs), label = "У, г")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = !isSaving && gramsText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun PreviewCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class MacroPreview(val kcal: Double, val protein: Double, val fat: Double, val carbs: Double)

private fun computePreview(product: Product, gramsText: String): MacroPreview? {
    val grams = gramsText.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (grams <= 0.0) return null
    val factor = grams / 100.0
    return MacroPreview(
        kcal = product.kcalPer100g * factor,
        protein = product.proteinPer100g * factor,
        fat = product.fatPer100g * factor,
        carbs = product.carbsPer100g * factor,
    )
}

private fun formatProductMacros(product: Product): String {
    val p = formatFloat(product.proteinPer100g)
    val f = formatFloat(product.fatPer100g)
    val c = formatFloat(product.carbsPer100g)
    return "${product.kcalPer100g} ккал · Б $p · Ж $f · У $c (на 100 г)"
}

private fun formatFloat(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
