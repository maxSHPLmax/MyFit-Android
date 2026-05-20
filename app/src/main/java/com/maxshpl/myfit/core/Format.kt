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

// Сейчас алиас на formatMacro — единица "г" добавляется в call site.
// В B-tech-1 коммите 2 переедет на формат "X г" со встроенной единицей
// + семантически разделится с formatMinutes.
fun formatGrams(value: Double): String = formatMacro(value)
