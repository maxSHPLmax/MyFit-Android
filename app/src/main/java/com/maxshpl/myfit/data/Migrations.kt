package com.maxshpl.myfit.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maxshpl.myfit.activities.ActivitySeed

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `diary_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `meal_type` TEXT NOT NULL,
                `product_id` INTEGER NOT NULL,
                `grams` REAL NOT NULL,
                FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_product_id` ON `diary_entries` (`product_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_date` ON `diary_entries` (`date`)")
    }
}

/**
 * Аддитивная миграция: добавляет справочник активностей с UNIQUE-индексом
 * по name. Сразу после CREATE — seed через INSERT OR IGNORE, чтобы
 * пользователи, обновляющиеся с v3, не получили пустой экран Activities
 * (для них Callback.onCreate не вызывается, только Migration).
 * Для свежих установок тот же seed применяется через
 * AppDatabase.SeedCallback.onCreate.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activities` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `kcal_per_min` REAL NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_activities_name` ON `activities` (`name`)",
        )
        ActivitySeed.items.forEach { activity ->
            db.execSQL(
                "INSERT OR IGNORE INTO `activities` (`name`, `kcal_per_min`) VALUES (?, ?)",
                arrayOf<Any>(activity.name, activity.kcalPerMin),
            )
        }
    }
}

/**
 * Аддитивная миграция: добавляет таблицу activity_log с FK на activities
 * (ON DELETE RESTRICT) и индексами по activity_id и date. Существующие
 * таблицы products, diary_entries, activities не трогаются.
 * Без seed — пользовательские данные.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activity_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `activity_id` INTEGER NOT NULL,
                `duration_minutes` REAL NOT NULL,
                FOREIGN KEY(`activity_id`) REFERENCES `activities`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_log_activity_id` ON `activity_log` (`activity_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_log_date` ON `activity_log` (`date`)",
        )
    }
}

/**
 * Аддитивная миграция для B-2a (План питания):
 * - Создаёт planned_meals (приёмы на день недели) и planned_meal_items (продукты в приёме).
 * - Пересоздаёт diary_entries, чтобы добавить колонку from_meal_id с FK на planned_meals
 *   ON DELETE SET NULL. SQLite не умеет ALTER ... ADD FOREIGN KEY, поэтому classic
 *   table-recreate dance (как в MIGRATION_2_3).
 *
 * ON DELETE SET NULL для from_meal_id — намеренное отклонение от ADR-004 (RESTRICT):
 * план не справочник, его удаление не должно блокироваться имеющимися записями
 * в дневнике и не должно их каскадно стирать.
 *
 * Существующие diary_entries сохраняются: from_meal_id выставляется в NULL для
 * всех старых записей (они не были созданы из плана).
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `planned_meals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `day_of_week` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `time` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_planned_meals_day_of_week` " +
                "ON `planned_meals` (`day_of_week`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `planned_meal_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `meal_id` INTEGER NOT NULL,
                `product_id` INTEGER NOT NULL,
                `grams` REAL NOT NULL,
                FOREIGN KEY(`meal_id`) REFERENCES `planned_meals`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`product_id`) REFERENCES `products`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_planned_meal_items_meal_id` " +
                "ON `planned_meal_items` (`meal_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_planned_meal_items_product_id` " +
                "ON `planned_meal_items` (`product_id`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `diary_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `meal_type` TEXT,
                `product_id` INTEGER NOT NULL,
                `grams` REAL NOT NULL,
                `from_meal_id` INTEGER,
                FOREIGN KEY(`product_id`) REFERENCES `products`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`from_meal_id`) REFERENCES `planned_meals`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `diary_entries_new`
                (`id`, `date`, `meal_type`, `product_id`, `grams`, `from_meal_id`)
            SELECT `id`, `date`, `meal_type`, `product_id`, `grams`, NULL
            FROM `diary_entries`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `diary_entries`")
        db.execSQL("ALTER TABLE `diary_entries_new` RENAME TO `diary_entries`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_entries_product_id` " +
                "ON `diary_entries` (`product_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_entries_date` " +
                "ON `diary_entries` (`date`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_entries_from_meal_id` " +
                "ON `diary_entries` (`from_meal_id`)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `diary_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `meal_type` TEXT,
                `product_id` INTEGER NOT NULL,
                `grams` REAL NOT NULL,
                FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `diary_entries_new` (`id`, `date`, `meal_type`, `product_id`, `grams`)
            SELECT `id`, `date`, `meal_type`, `product_id`, `grams` FROM `diary_entries`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `diary_entries`")
        db.execSQL("ALTER TABLE `diary_entries_new` RENAME TO `diary_entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_product_id` ON `diary_entries` (`product_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_date` ON `diary_entries` (`date`)")
    }
}
