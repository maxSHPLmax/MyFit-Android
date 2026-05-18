package com.maxshpl.myfit.diary

internal fun formatKcal(value: Double): String = value.toInt().toString()

internal fun formatMacro(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

internal fun formatGrams(value: Double): String = formatMacro(value)
