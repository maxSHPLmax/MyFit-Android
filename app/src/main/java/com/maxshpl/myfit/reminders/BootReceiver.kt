package com.maxshpl.myfit.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Placeholder: реальная логика перерасписания алярмов после reboot
 * появится в коммите 4 (нужен ReminderScheduler, который добавляется
 * в коммите 3). AndroidManifest уже ссылается на этот класс, чтобы
 * intent-filter для BOOT_COMPLETED был объявлен.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO commit 4: на BOOT_COMPLETED перечитать RemindersRepository
        // и через scheduler.scheduleAll(config) восстановить все enabled-слоты.
    }
}
