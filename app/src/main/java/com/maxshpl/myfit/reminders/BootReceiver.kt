package com.maxshpl.myfit.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maxshpl.myfit.settings.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * При перезагрузке устройства AlarmManager стирает все алярмы. Этот
 * receiver перерасписывает enabled-слоты из RemindersRepository.
 *
 * goAsync обязателен: DataStore read — suspend, sync context.onReceive
 * не справится. pendingResult.finish() в finally — иначе система убьёт
 * процесс receiver'а раньше времени, и scheduleAll может не успеть.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RemindersRepository(appContext.settingsDataStore)
                val config = repository.currentConfig.first()
                if (config.enabled) {
                    ReminderScheduler(appContext).scheduleAll(config)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
