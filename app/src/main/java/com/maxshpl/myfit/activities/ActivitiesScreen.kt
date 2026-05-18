package com.maxshpl.myfit.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ActivitiesViewModel = viewModel(factory = ActivitiesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val deleteBlocked by viewModel.deleteBlocked.collectAsStateWithLifecycle()

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Activity?>(null) }

    Scaffold(
        topBar = {
            ActivitiesTopBar(
                isSearchActive = isSearchActive,
                query = query,
                onSearchToggle = {
                    isSearchActive = it
                    if (!it) viewModel.setQuery("")
                },
                onQueryChange = viewModel::setQuery,
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить активность")
            }
        },
    ) { padding ->
        when (val current = state) {
            ActivitiesUiState.Loading -> LoadingView(Modifier.padding(padding))
            is ActivitiesUiState.Error -> ErrorView(current.message, Modifier.padding(padding))
            is ActivitiesUiState.Success -> ActivityList(
                activities = current.activities,
                isQueryEmpty = query.isBlank(),
                onCardClick = { onEditClick(it.id) },
                onSwipe = { pendingDelete = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    pendingDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить активность?") },
            text = {
                Text(
                    "«${activity.name}» (${formatKcalPerMin(activity.kcalPerMin)} ккал/мин) " +
                        "будет удалена без возможности восстановления.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(activity)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }

    deleteBlocked?.let { blocked ->
        val count = blocked.logCount
        val recordsText = pluralStringResource(R.plurals.diary_records_count, count, count)
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlocked,
            title = { Text("Нельзя удалить") },
            text = {
                Text(
                    "«${blocked.activity.name}» используется в $recordsText. " +
                        "Удаление невозможно.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlocked) { Text("Понятно") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitiesTopBar(
    isSearchActive: Boolean,
    query: String,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Поиск по названию") },
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Закрыть поиск")
                }
            },
        )
    } else {
        TopAppBar(
            title = { Text("Активности") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            },
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
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
private fun ActivityList(
    activities: List<Activity>,
    isQueryEmpty: Boolean,
    onCardClick: (Activity) -> Unit,
    onSwipe: (Activity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activities.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isQueryEmpty) "Пока нет активностей" else "Ничего не найдено",
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
        items(activities, key = { it.id }) { activity ->
            SwipeableActivityCard(
                activity = activity,
                onClick = { onCardClick(activity) },
                onSwipe = { onSwipe(activity) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableActivityCard(
    activity: Activity,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
) {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                currentOnSwipe()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground() },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        ActivityCard(activity = activity, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(CardDefaults.shape)
            .background(scheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = "Удалить",
            color = scheme.onErrorContainer,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCard(activity: Activity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${formatKcalPerMin(activity.kcalPerMin)} ккал/мин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun formatKcalPerMin(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}
