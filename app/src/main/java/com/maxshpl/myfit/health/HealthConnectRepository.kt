package com.maxshpl.myfit.health

import android.content.Context
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
 */
class HealthConnectRepository(private val context: Context) {

    private val cache = mutableMapOf<LocalDate, BurnedKcalSummary>()
    private val cacheMutex = Mutex()

    /** Lazy: getOrCreate бросает на устройствах без HC, поэтому проверяем status сначала. */
    private val client: HealthConnectClient? by lazy {
        if (rawStatus() == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
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
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Возвращает агрегированную сводку за календарный день в указанной зоне.
     * Result.Empty в случаях: HC недоступен / нет permissions / SecurityException на чтении.
     *
     * Mutex держим вокруг проверки-и-чтения, чтобы одновременные вызовы getOrRead(same date)
     * не сделали два aggregate() запроса.
     */
    suspend fun getOrRead(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): BurnedKcalSummary {
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
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            StepsRecord.COUNT_TOTAL,
                        ),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                )
                BurnedKcalSummary(
                    activeKcal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                        ?.inKilocalories ?: 0.0,
                    steps = response[StepsRecord.COUNT_TOTAL] ?: 0L,
                )
            } catch (e: SecurityException) {
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

    companion object {
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
        )
    }
}
