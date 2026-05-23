package com.maxshpl.myfit.health

/**
 * Состояние доступности Health Connect на устройстве.
 * Маппится из HealthConnectClient.getSdkStatus().
 */
sealed class HealthConnectAvailability {
    /** SDK_AVAILABLE — провайдер установлен и совместим. */
    data object Available : HealthConnectAvailability()

    /** SDK_UNAVAILABLE — на устройстве нет HC (например, Android <9 или provider не установлен). */
    data object NotInstalled : HealthConnectAvailability()

    /** SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED — нужно обновить HC через Play Store. */
    data object ProviderUpdateRequired : HealthConnectAvailability()
}
