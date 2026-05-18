package com.maxshpl.myfit.activities

/**
 * Seed-набор активностей для свежей установки и для миграции 3→4.
 * 5 первых — из PWA (`MyFit/products.js:56-62`), 2 дополнительных по
 * ориентировочным MET-значениям, см. KAN-16 Description.
 */
object ActivitySeed {
    val items: List<Activity> = listOf(
        Activity(name = "Бег", kcalPerMin = 8.0),
        Activity(name = "Ходьба", kcalPerMin = 4.0),
        Activity(name = "Велосипед", kcalPerMin = 7.0),
        Activity(name = "Тренажёрный зал", kcalPerMin = 6.0),
        Activity(name = "Плавание", kcalPerMin = 10.0),
        Activity(name = "Йога", kcalPerMin = 3.0),
        Activity(name = "Степпер", kcalPerMin = 7.0),
    )
}
