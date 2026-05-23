package com.maxshpl.myfit.health

import android.content.Context

/**
 * Application-scoped singleton для HealthConnectRepository.
 *
 * Зачем: in-memory cache в repository должен переживать пересоздание ViewModel
 * (rotate, navigation). ViewModel.Factory вызывается на каждое создание VM —
 * без holder'а каждый раз был бы новый repository с пустым cache.
 */
object HealthConnectHolder {
    @Volatile
    private var instance: HealthConnectRepository? = null

    fun get(context: Context): HealthConnectRepository =
        instance ?: synchronized(this) {
            instance ?: HealthConnectRepository(context.applicationContext).also { instance = it }
        }
}
