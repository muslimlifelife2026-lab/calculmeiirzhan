package com.calculator.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<CalculationEntity>>

    @Query("SELECT * FROM calculations WHERE subject = :subject ORDER BY timestamp DESC")
    fun getCalculationsBySubject(subject: String): Flow<List<CalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCalculation(calculation: CalculationEntity)

    @Query("DELETE FROM calculations WHERE id = :id")
    fun deleteCalculation(id: Int)

    @Query("DELETE FROM calculations")
    fun clearHistory()
}
