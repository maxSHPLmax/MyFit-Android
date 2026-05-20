package com.maxshpl.myfit.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maxshpl.myfit.MainActivity
import com.maxshpl.myfit.R

/**
 * Получает alarm от AlarmManager, показывает notification и перерасписывает
 * себя на +24 часа (self-perpetuating chain — см. план B-4 коммит 3).
 *
 * Extras intent'а:
 * - EXTRA_MEAL_KIND (String, MealKind.name) — какой приём напоминаем
 * - EXTRA_IS_TEST (Boolean, optional) — тестовый алярм из debug-кнопки,
 *   не перерасписывается, логирует drift (коммит 7)
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mealKindName = intent.getStringExtra(EXTRA_MEAL_KIND) ?: return
        val mealKind = runCatching { MealKind.valueOf(mealKindName) }.getOrNull() ?: return
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        ensureMealRemindersChannel(context)
        showNotification(context, mealKind)

        // Self-perpetuating chain: реальные слоты перерасписываются на +24h
        // через тот же scheduleSlot — nextTriggerMillis вернёт завтрашний момент.
        // Тестовые алярмы (debug-кнопка в коммите 7) — one-shot, без перерасписания.
        if (!isTest && hour in 0..23 && minute in 0..59) {
            ReminderScheduler(context).scheduleSlot(mealKind, hour, minute)
        }
    }

    private fun showNotification(context: Context, kind: MealKind) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_DIARY_TODAY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            kind.ordinal,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEAL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_meal_24)
            .setContentTitle(kind.displayTitle)
            .setContentText("Не забудьте записать в дневник")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // POST_NOTIFICATIONS проверка — на Android 13+ без runtime grant ничего не
        // покажется (system silently drop'нет). Permission запрашиваем в UI при
        // включении главного switch (коммит 5).
        runCatching {
            NotificationManagerCompat.from(context).notify(kind.ordinal, notification)
        }
    }

    companion object {
        const val EXTRA_MEAL_KIND = "meal_kind"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_IS_TEST = "is_test"
        const val EXTRA_OPEN_DIARY_TODAY = "open_diary_today"
    }
}
