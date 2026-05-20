package com.maxshpl.myfit.reminders

/**
 * Категория приёма пищи для напоминаний. Дефолтное время храним прямо
 * в enum'е — это discoverable и не размазывает defaults по нескольким
 * файлам.
 *
 * displayTitle — короткий заголовок для notification ("Время завтракать").
 */
enum class MealKind(
    val defaultHour: Int,
    val defaultMinute: Int,
    val displayTitle: String,
    val settingsLabel: String,
) {
    BREAKFAST(8, 0, "Время завтракать", "Завтрак"),
    LUNCH(13, 0, "Время обедать", "Обед"),
    DINNER(19, 0, "Время ужинать", "Ужин"),
    SNACK(16, 0, "Время перекусить", "Перекус"),
}
