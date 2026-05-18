package com.maxshpl.myfit.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maxshpl.myfit.diary.DiaryEntry
import com.maxshpl.myfit.diary.DiaryEntryDao
import com.maxshpl.myfit.products.Product
import com.maxshpl.myfit.products.ProductDao
import com.maxshpl.myfit.products.ProductSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, DiaryEntry::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun diaryEntryDao(): DiaryEntryDao

    companion object {
        private const val TAG = "MyFitDb"
        private const val DB_NAME = "myfit.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(appContext: Context): AppDatabase {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                .addCallback(SeedCallback(scope))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        private class SeedCallback(
            private val scope: CoroutineScope,
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i(TAG, "onCreate fired — first install or after data clear")
                val database = instance ?: run {
                    Log.w(TAG, "instance is null in onCreate, skipping seed")
                    return
                }
                scope.launch {
                    val dao = database.productDao()
                    val seeded = ProductSeed.items
                    dao.insertAll(seeded)
                    Log.i(TAG, "Seeded ${seeded.size} products (count in DB: ${dao.count()})")
                }
            }
        }
    }
}
