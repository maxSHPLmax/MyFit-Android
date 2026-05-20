package com.maxshpl.myfit.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import java.util.Calendar

/**
 * Wraps AlarmManager для daily-репитеров напоминаний.
 *
 * Approach: self-perpetuating chain. Один alarm на каждый kind, при
 * срабатывании ReminderReceiver показывает notification и снова
 * scheduleSlot на +24h (через эту же функцию — nextTriggerMillis
 * вернёт завтрашний момент).
 *
 * Гибрид (c) согласованный с Lead:
 * - пробуем setExactAndAllowWhileIdle (точно ±сек, требует
 *   SCHEDULE_EXACT_ALARM с Android 14+)
 * - на SecurityException fallback на setAndAllowWhileIdle (inexact,
 *   drift до ~15 мин в Doze, без permission)
 *
 * Permission UX: дефолтно работает в inexact-режиме. Hint про exact
 * alarms показывается в Settings (коммит 6), если canScheduleExact()=false.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService<AlarmManager>()
        ?: error("AlarmManager unavailable")

    fun scheduleSlot(kind: MealKind, hour: Int, minute: Int) {
        val triggerAt = nextTriggerMillis(hour, minute)
        val pending = buildPendingIntent(kind, hour, minute)
        scheduleAlarm(triggerAt, pending)
    }

    fun cancelSlot(kind: MealKind) {
        val pending = lookupPendingIntent(kind) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    fun scheduleAll(config: RemindersConfig) {
        if (!config.enabled) {
            cancelAll()
            return
        }
        MealKind.entries.forEach { kind ->
            val slot = config.slotFor(kind)
            if (slot.enabled) {
                scheduleSlot(kind, slot.hour, slot.minute)
            } else {
                cancelSlot(kind)
            }
        }
    }

    fun cancelAll() {
        MealKind.entries.forEach { cancelSlot(it) }
    }

    /**
     * canScheduleExact — есть ли разрешение на точные алярмы.
     * На Android < 12 — всегда true (permission concept не существовал).
     * На 12+ зависит от грантa в системных настройках.
     */
    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun scheduleAlarm(triggerAt: Long, pending: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending,
            )
        } catch (_: SecurityException) {
            // Fallback: на Android 14+ без SCHEDULE_EXACT_ALARM permission
            // setExactAndAllowWhileIdle бросает SecurityException. inexact-версия
            // не требует permission, но может drift'ить до 15 мин в Doze mode.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending,
            )
        }
    }

    private fun buildPendingIntent(kind: MealKind, hour: Int, minute: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEAL_KIND, kind.name)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * FLAG_NO_CREATE возвращает null если PendingIntent с этими (requestCode,
     * intent template) не зарегистрирован — это значит alarm и так нет, отменять
     * нечего.
     */
    private fun lookupPendingIntent(kind: MealKind): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Если целевое время уже прошло сегодня (или ровно сейчас) — на завтра.
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }
}
