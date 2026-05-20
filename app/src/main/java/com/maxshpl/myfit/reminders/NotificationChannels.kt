package com.maxshpl.myfit.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

internal const val CHANNEL_ID_MEAL_REMINDERS = "meal_reminders"

/**
 * Idempotent создание notification-channel для напоминаний о приёмах.
 * Безопасно вызывать многократно — getNotificationChannel проверяет existence.
 *
 * IMPORTANCE_DEFAULT — звук + статус-бар, без heads-up на lock screen
 * (см. ответ Lead на вопрос 8: не такое срочное событие).
 */
fun ensureMealRemindersChannel(context: Context) {
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(CHANNEL_ID_MEAL_REMINDERS) != null) return
    val channel = NotificationChannel(
        CHANNEL_ID_MEAL_REMINDERS,
        "Напоминания о приёмах пищи",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Уведомления о завтраке, обеде, ужине и перекусе"
    }
    manager.createNotificationChannel(channel)
}
