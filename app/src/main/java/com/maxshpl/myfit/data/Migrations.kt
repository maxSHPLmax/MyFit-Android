package com.maxshpl.myfit.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
 * SQLite не поддерживает прямой `ALTER COLUMN ... DROP NOT NULL`,
 * поэтому делаем classic table-recreate dance: новая таблица → INSERT
 * существующих данных → DROP старой → RENAME. Индексы и FK
 * восстанавливаются вручную, потому что DROP уничтожает их вместе с
 * таблицей.
 */
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
