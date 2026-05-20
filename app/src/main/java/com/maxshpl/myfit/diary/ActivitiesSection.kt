package com.maxshpl.myfit.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMinutes

@Composable
fun ActivitiesSection(
    logs: List<ActivityLogRow>,
    onAddClick: () -> Unit,
    onRowClick: (ActivityLogRow) -> Unit,
    onSwipeLog: (ActivityLogRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SectionHeader(
                title = "Активности",
                actionDescription = "Добавить активность",
                onActionClick = onAddClick,
            )
            HorizontalDivider()
            if (logs.isEmpty()) {
                Text(
                    text = "Активностей нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                Column {
                    logs.forEach { log ->
                        key(log.logId) {
                            SwipeableActivityLogRow(
                                log = log,
                                onClick = { onRowClick(log) },
                                onSwipe = { onSwipeLog(log) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableActivityLogRow(
    log: ActivityLogRow,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
) {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnClick by rememberUpdatedState(onClick)
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
        ActivityLogRowItem(log = log, onClick = { currentOnClick() })
    }
}

@Composable
private fun SwipeBackground() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
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

@Composable
private fun ActivityLogRowItem(log: ActivityLogRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = log.activityName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMinutes(log.durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatKcal(log.kcalBurned)} ккал",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

