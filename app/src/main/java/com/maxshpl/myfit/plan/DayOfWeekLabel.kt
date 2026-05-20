package com.maxshpl.myfit.plan

import java.time.DayOfWeek

private val ShortLabels = mapOf(
    DayOfWeek.MONDAY to "Пн",
    DayOfWeek.TUESDAY to "Вт",
    DayOfWeek.WEDNESDAY to "Ср",
    DayOfWeek.THURSDAY to "Чт",
    DayOfWeek.FRIDAY to "Пт",
    DayOfWeek.SATURDAY to "Сб",
    DayOfWeek.SUNDAY to "Вс",
)

private val FullLabels = mapOf(
    DayOfWeek.MONDAY to "Понедельник",
    DayOfWeek.TUESDAY to "Вторник",
    DayOfWeek.WEDNESDAY to "Среда",
    DayOfWeek.THURSDAY to "Четверг",
    DayOfWeek.FRIDAY to "Пятница",
    DayOfWeek.SATURDAY to "Суббота",
    DayOfWeek.SUNDAY to "Воскресенье",
)

fun DayOfWeek.shortRu(): String = ShortLabels.getValue(this)

fun DayOfWeek.fullRu(): String = FullLabels.getValue(this)
