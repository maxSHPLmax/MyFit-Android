package com.maxshpl.myfit.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Material 3 TimePicker внутри AlertDialog. Возвращает строку "HH:mm"
 * (24-часовой формат). Используется и в Plan (время приёма), и в
 * Settings/Reminders (время напоминаний).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String?,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val (hour, minute) = parseHm(initialTime)
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
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

/**
 * "HH:mm" → (hour, minute). null или некорректный формат → (12, 0).
 */
fun parseHm(time: String?): Pair<Int, Int> {
    if (time == null) return 12 to 0
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}
