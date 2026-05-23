package com.maxshpl.myfit.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Обёртка над Health Connect SDK.
 *
 * Зачем нужна: SDK работает только при наличии provider'а (на Android <9, или если HC не
 * установлен — getSdkStatus вернёт UNAVAILABLE). Этот класс изолирует все проверки availability
 * и пермишенов, чтобы вызывающий код не падал в SecurityException и не зависел напрямую от SDK.
 *
 * Кеш: aggregate() — сетевой/IPC вызов в provider, latency 50-300мс. При свайпе дат на Дневнике
 * это заметно. Держим Map<LocalDate, Summary> в памяти; invalidate(today) дёргается из
 * Lifecycle.ON_RESUME в DiaryScreen (см. коммит 6). Mutex — потому что getOrRead suspend и
 * может вызываться из combine() с разных корутин одновременно.
 *
 * ВРЕМЕННО: логи безусловные (не под BuildConfig.DEBUG) для диагностики бага "0 ккал на всех
 * датах". После решения убрать — это коммит-маркер.
 */
class HealthConnectRepository(private val context: Context) {

    private val cache = mutableMapOf<LocalDate, BurnedKcalSummary>()
    private val cacheMutex = Mutex()

    /** Lazy: getOrCreate бросает на устройствах без HC, поэтому проверяем status сначала. */
    private val client: HealthConnectClient? by lazy {
        val status = rawStatus()
        Log.d(TAG, "client lazy init: rawStatus=$status (AVAILABLE=${HealthConnectClient.SDK_AVAILABLE})")
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            try {
                HealthConnectClient.getOrCreate(context).also {
                    Log.d(TAG, "client lazy init: getOrCreate OK")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "client lazy init: getOrCreate THREW", t)
                null
            }
        } else {
            Log.w(TAG, "client lazy init: status != AVAILABLE → null")
            null
        }
    }

    fun availability(): HealthConnectAvailability = when (rawStatus()) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            HealthConnectAvailability.ProviderUpdateRequired
        else -> HealthConnectAvailability.NotInstalled
    }

    private fun rawStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasAllPermissions(): Boolean {
        val c = client
        if (c == null) {
            Log.w(TAG, "hasAllPermissions: client is null → false")
            return false
        }
        return try {
            val granted = c.permissionController.getGrantedPermissions()
            val hasAll = granted.containsAll(PERMISSIONS)
            Log.d(TAG, "hasAllPermissions: granted=$granted")
            Log.d(TAG, "hasAllPermissions: required=$PERMISSIONS")
            Log.d(TAG, "hasAllPermissions: containsAll=$hasAll")
            hasAll
        } catch (t: Throwable) {
            Log.e(TAG, "hasAllPermissions: THREW", t)
            false
        }
    }

    /**
     * Возвращает агрегированную сводку за календарный день в указанной зоне.
     * Result.Empty в случаях: HC недоступен / нет permissions / любой Throwable на чтении.
     *
     * Mutex держим вокруг проверки-и-чтения, чтобы одновременные вызовы getOrRead(same date)
     * не сделали два aggregate() запроса.
     */
    suspend fun getOrRead(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): BurnedKcalSummary {
        Log.d(TAG, "getOrRead START date=$date")
        cacheMutex.withLock {
            cache[date]?.let {
                Log.d(TAG, "getOrRead($date): cache HIT → $it")
                return it
            }

            val avail = availability()
            val c = client
            Log.d(TAG, "getOrRead($date): availability=$avail, client=${c != null}")
            if (c == null) {
                Log.w(TAG, "getOrRead($date): client is null → Empty (NOT cached)")
                return BurnedKcalSummary.Empty
            }
            if (!hasAllPermissions()) {
                Log.w(TAG, "getOrRead($date): missing permissions → Empty (NOT cached)")
                return BurnedKcalSummary.Empty
            }

            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            Log.d(TAG, "getOrRead($date): TimeRange start=$start end=$end zone=$zone")

            val summary = try {
                val response = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            StepsRecord.COUNT_TOTAL,
                        ),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                )
                val activeEnergy = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                val stepsCount = response[StepsRecord.COUNT_TOTAL]
                val hasActive = response.contains(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                val hasSteps = response.contains(StepsRecord.COUNT_TOTAL)
                Log.d(TAG, "getOrRead($date): aggregate OK")
                Log.d(TAG, "getOrRead($date): hasActive=$hasActive, hasSteps=$hasSteps")
                Log.d(TAG, "getOrRead($date): ActiveCalories raw=$activeEnergy (kcal=${activeEnergy?.inKilocalories})")
                Log.d(TAG, "getOrRead($date): Steps raw=$stepsCount")
                Log.d(TAG, "getOrRead($date): dataOrigins=${response.dataOrigins}")
                BurnedKcalSummary(
                    activeKcal = activeEnergy?.inKilocalories ?: 0.0,
                    steps = stepsCount ?: 0L,
                )
            } catch (t: Throwable) {
                Log.e(TAG, "getOrRead($date): aggregate THREW", t)
                BurnedKcalSummary.Empty
            }

            Log.d(TAG, "getOrRead($date): final summary=$summary")
            cache[date] = summary
            return summary
        }
    }

    suspend fun invalidate(date: LocalDate) {
        cacheMutex.withLock { cache.remove(date) }
    }

    suspend fun invalidateAll() {
        cacheMutex.withLock { cache.clear() }
    }

    companion object {
        private const val TAG = "MyFit_HC"

        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
        )
    }
}
