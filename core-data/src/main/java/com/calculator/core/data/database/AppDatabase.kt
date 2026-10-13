package com.calculator.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CalculationEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(`calculations`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                }
                if (columns.isEmpty()) {
                    db.execSQL("""CREATE TABLE IF NOT EXISTS `calculations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `formulaId` TEXT NOT NULL, `formulaName` TEXT NOT NULL, `solvedVariable` TEXT NOT NULL, `resultValue` REAL NOT NULL, `resultUnit` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `serializedSteps` TEXT NOT NULL, `expression` TEXT NOT NULL, `result` TEXT NOT NULL, `subject` TEXT NOT NULL)""")
                } else {
                    val additions = mapOf(
                        "formulaId" to "TEXT NOT NULL DEFAULT 'general'", "formulaName" to "TEXT NOT NULL DEFAULT 'Вычисление'",
                        "solvedVariable" to "TEXT NOT NULL DEFAULT 'x'", "resultValue" to "REAL NOT NULL DEFAULT 0",
                        "resultUnit" to "TEXT NOT NULL DEFAULT ''", "timestamp" to "INTEGER NOT NULL DEFAULT 0",
                        "serializedSteps" to "TEXT NOT NULL DEFAULT ''", "expression" to "TEXT NOT NULL DEFAULT ''",
                        "result" to "TEXT NOT NULL DEFAULT ''", "subject" to "TEXT NOT NULL DEFAULT 'MATH'"
                    )
                    additions.forEach { (name, definition) ->
                        if (name !in columns) db.execSQL("ALTER TABLE `calculations` ADD COLUMN `$name` $definition")
                    }
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnicalc_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
