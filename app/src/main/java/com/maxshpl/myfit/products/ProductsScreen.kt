package com.maxshpl.myfit.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed interface PendingAction {
    val product: Product
    data class Hide(override val product: Product) : PendingAction
    data class Delete(override val product: Product) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: ProductsViewModel = viewModel(factory = ProductsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Продукты") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить продукт")
            }
        },
    ) { padding ->
        when (val current = state) {
            ProductsUiState.Loading -> LoadingView(Modifier.padding(padding))
            is ProductsUiState.Error -> ErrorView(current.message, Modifier.padding(padding))
            is ProductsUiState.Success -> ProductList(
                products = current.products,
                onCardClick = { product ->
                    if (product.isCustom) onEditClick(product.id)
                },
                onSwipe = { product ->
                    pendingAction = if (product.isCustom) {
                        PendingAction.Delete(product)
                    } else {
                        PendingAction.Hide(product)
                    }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }

    pendingAction?.let { action ->
        ConfirmActionDialog(
            action = action,
            onConfirm = {
                when (action) {
                    is PendingAction.Hide -> viewModel.hide(action.product)
                    is PendingAction.Delete -> viewModel.delete(action.product)
                }
                pendingAction = null
            },
            onDismiss = { pendingAction = null },
        )
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Ошибка: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ProductList(
    products: List<Product>,
    onCardClick: (Product) -> Unit,
    onSwipe: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Пока нет продуктов",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(products, key = { it.id }) { product ->
            SwipeableProductCard(
                product = product,
                onClick = { onCardClick(product) },
                onSwipe = { onSwipe(product) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableProductCard(
    product: Product,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onSwipe()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(isCustom = product.isCustom) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        ProductCard(product = product, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground(isCustom: Boolean) {
    val (color, label) = if (isCustom) {
        MaterialTheme.colorScheme.errorContainer to "Удалить"
    } else {
        MaterialTheme.colorScheme.tertiaryContainer to "Скрыть"
    }
    val textColor = if (isCustom) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(color),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = product.isCustom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMacros(product),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    action: PendingAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, message, confirmLabel) = when (action) {
        is PendingAction.Hide -> Triple(
            "Скрыть продукт?",
            "«${action.product.name}» перестанет показываться. Базовые продукты можно " +
                "только скрыть — удалить их нельзя.",
            "Скрыть",
        )
        is PendingAction.Delete -> Triple(
            "Удалить продукт?",
            "«${action.product.name}» будет удалён без возможности восстановления.",
            "Удалить",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private fun formatMacros(product: Product): String {
    val protein = formatGrams(product.proteinPer100g)
    val fat = formatGrams(product.fatPer100g)
    val carbs = formatGrams(product.carbsPer100g)
    return "${product.kcalPer100g} ккал · Б $protein · Ж $fat · У $carbs (на 100 г)"
}

private fun formatGrams(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
