package com.aerocalc.smartcalculator.ui.main

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.calculator.core.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationTest {
    @Test fun oldCalculationSurvivesUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test-${System.nanoTime()}"
        val oldDb = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        oldDb.execSQL("""CREATE TABLE calculations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, formulaId TEXT NOT NULL, formulaName TEXT NOT NULL, solvedVariable TEXT NOT NULL, resultValue REAL NOT NULL, resultUnit TEXT NOT NULL, timestamp INTEGER NOT NULL, serializedSteps TEXT NOT NULL)""")
        oldDb.execSQL("""INSERT INTO calculations (formulaId, formulaName, solvedVariable, resultValue, resultUnit, timestamp, serializedSteps) VALUES ('calc', 'Математика', 'x', 42, '', 1, '')""")
        oldDb.version = 1
        oldDb.close()

        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val entries = upgraded.calculationDao().getAllCalculations().first()
            assertEquals(1, entries.size)
            assertEquals(42.0, entries.single().resultValue, 0.0)
            assertEquals("MATH", entries.single().subject)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
