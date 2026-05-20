package com.maxshpl.myfit.core

/**
 * Общий util форматирования числовых значений UI. Использовать ВЕЗДЕ
 * вместо локальных копий в feature-папках.
 */

fun formatKcal(value: Double): String = value.toInt().toString()

fun formatMacro(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

// formatGrams и formatMinutes — намеренно разные функции, хотя
// математика одинаковая. Семантика важнее DRY: легче читать call site и
// легче ловить ошибки "формат граммов на минутах" статически. Единица
// встроена в результат, не дублировать в строках.
fun formatGrams(value: Double): String = "${formatMacro(value)} г"

fun formatMinutes(value: Double): String = "${formatMacro(value)} мин"
