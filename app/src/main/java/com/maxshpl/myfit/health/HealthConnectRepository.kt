package com.maxshpl.myfit.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
 * Lifecycle.ON_RESUME в DiaryScreen. Mutex — потому что getOrRead suspend и может вызываться
 * из combine() с разных корутин одновременно.
 */
class HealthConnectRepository(private val context: Context) {

    private val cache = mutableMapOf<LocalDate, BurnedKcalSummary>()
    private val cacheMutex = Mutex()

    private val client: HealthConnectClient? by lazy {
        if (rawStatus() == HealthConnectClient.SDK_AVAILABLE) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (t: Throwable) {
                Log.e(TAG, "client init: getOrCreate threw", t)
                null
            }
        } else {
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
        val c = client ?: return false
        return try {
            c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        } catch (t: Throwable) {
            Log.e(TAG, "hasAllPermissions: getGrantedPermissions threw", t)
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
        // Будущие даты: HC aggregate за полностью future range возвращает latest
        // BMR snapshot (наблюдаемо на устройстве Lead'а — 25/26 мая = значение
        // последнего завершённого дня, 23 мая = 1564). Это семантически некорректно
        // показывать как "сожжено" — день ещё не наступил. Возвращаем Empty без
        // запроса в HC и без кеширования: завтра, когда сегодня станет вчера,
        // запрос должен пройти нормальным путём.
        if (date.isAfter(LocalDate.now(zone))) return BurnedKcalSummary.Empty

        cacheMutex.withLock {
            cache[date]?.let { return it }

            val c = client ?: return BurnedKcalSummary.Empty
            if (!hasAllPermissions()) return BurnedKcalSummary.Empty

            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()

            val summary = try {
                val response = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                            StepsRecord.COUNT_TOTAL,
                        ),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                )
                BurnedKcalSummary(
                    burnedKcal = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                        ?.inKilocalories ?: 0.0,
                    steps = response[StepsRecord.COUNT_TOTAL] ?: 0L,
                )
            } catch (t: Throwable) {
                Log.e(TAG, "getOrRead($date): aggregate threw", t)
                BurnedKcalSummary.Empty
            }

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

    /**
     * Debug helper: пишет в Logcat сводку HC данных за последние 7 дней.
     * Дёргается из Settings DEBUG-кнопки. Полезен при отладке: разногласие
     * UI vs HC app, проверка работы datasource'ов после смены permissions,
     * sanity check после изменений aggregate metric'и.
     *
     * Тег MyFit_HC — единый с error logging в catch блоках.
     */
    suspend fun debugReadAllToLogcat() {
        Log.d(TAG, "=== debugReadAllToLogcat START ===")
        Log.d(TAG, "availability=${availability()}, hasPermissions=${hasAllPermissions()}")
        val today = LocalDate.now()
        for (offset in 0..6L) {
            val date = today.minusDays(offset)
            val summary = getOrRead(date)
            Log.d(TAG, "$date: burnedKcal=${summary.burnedKcal}, steps=${summary.steps}")
        }
        Log.d(TAG, "=== debugReadAllToLogcat END ===")
    }

    companion object {
        private const val TAG = "MyFit_HC"

        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
        )
    }
}
