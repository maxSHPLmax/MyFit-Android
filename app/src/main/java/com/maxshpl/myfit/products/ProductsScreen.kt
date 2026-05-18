package com.maxshpl.myfit.products

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed interface PendingAction {
    val product: Product
    data class Hide(override val product: Product) : PendingAction
    data class Delete(override val product: Product) : PendingAction
    data class Unhide(override val product: Product) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: ProductsViewModel = viewModel(factory = ProductsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val showHidden by viewModel.showHidden.collectAsStateWithLifecycle()

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    Scaffold(
        topBar = {
            ProductsTopBar(
                isSearchActive = isSearchActive,
                query = query,
                showHidden = showHidden,
                onSearchToggle = {
                    isSearchActive = it
                    if (!it) viewModel.setQuery("")
                },
                onQueryChange = viewModel::setQuery,
                onShowHiddenToggle = { viewModel.setShowHidden(!showHidden) },
            )
        },
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
                isQueryEmpty = query.isBlank(),
                onCardClick = { product ->
                    if (product.isCustom && !product.isHidden) onEditClick(product.id)
                },
                onSwipe = { product ->
                    pendingAction = when {
                        product.isHidden -> PendingAction.Unhide(product)
                        product.isCustom -> PendingAction.Delete(product)
                        else -> PendingAction.Hide(product)
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
                    is PendingAction.Unhide -> viewModel.unhide(action.product)
                }
                pendingAction = null
            },
            onDismiss = { pendingAction = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductsTopBar(
    isSearchActive: Boolean,
    query: String,
    showHidden: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onShowHiddenToggle: () -> Unit,
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Поиск по имени") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                )
            },
            navigationIcon = {
                IconButton(onClick = { onSearchToggle(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Закрыть поиск",
                    )
                }
            },
        )
    } else {
        TopAppBar(
            title = { Text("Продукты") },
            actions = {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
                OverflowMenu(
                    showHidden = showHidden,
                    onShowHiddenToggle = onShowHiddenToggle,
                )
            },
        )
    }
}

@Composable
private fun OverflowMenu(
    showHidden: Boolean,
    onShowHiddenToggle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(if (showHidden) "Скрыть скрытые" else "Показать скрытые")
                },
                onClick = {
                    onShowHiddenToggle()
                    expanded = false
                },
            )
        }
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
    isQueryEmpty: Boolean,
    onCardClick: (Product) -> Unit,
    onSwipe: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isQueryEmpty) "Пока нет продуктов" else "Ничего не найдено",
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
        backgroundContent = { SwipeBackground(product = product) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        ProductCard(product = product, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground(product: Product) {
    val scheme = MaterialTheme.colorScheme
    val (color, textColor, label) = when {
        product.isHidden -> Triple(
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
            "Показать",
        )
        product.isCustom -> Triple(
            scheme.errorContainer,
            scheme.onErrorContainer,
            "Удалить",
        )
        else -> Triple(
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
            "Скрыть",
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(CardDefaults.shape)
            .background(color),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val clickable = product.isCustom && !product.isHidden
    Card(
        onClick = onClick,
        enabled = clickable,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (product.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Скрыт",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
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
        is PendingAction.Unhide -> Triple(
            "Показать продукт?",
            "«${action.product.name}» снова появится в обычном списке.",
            "Показать",
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
