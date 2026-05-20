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

        ensureMealRemindersChannel(context)
        showNotification(context, mealKind)

        // TODO commit 3: после появления ReminderScheduler — здесь self-perpetuate:
        // вызвать scheduler.scheduleSlot(kind, hour, minute) для алярма на +24 часа.
        // Hour/minute прокидываем через extras или читаем из RemindersRepository.
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
        const val EXTRA_IS_TEST = "is_test"
        const val EXTRA_OPEN_DIARY_TODAY = "open_diary_today"
    }
}
