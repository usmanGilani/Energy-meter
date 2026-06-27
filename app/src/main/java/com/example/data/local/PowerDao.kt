package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PowerDao {
    @Query("SELECT * FROM power_records ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<PowerRecord>>

    @Query("SELECT * FROM power_records ORDER BY timestamp DESC")
    suspend fun getAll(): List<PowerRecord>

    @Query("SELECT * FROM power_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): PowerRecord?

    @Query("SELECT * FROM power_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestFlow(): Flow<PowerRecord?>

    @Query("SELECT * FROM power_records ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaginated(limit: Int, offset: Int): List<PowerRecord>

    @Query("SELECT * FROM power_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getRecordsInRange(startTime: Long, endTime: Long): List<PowerRecord>

    @Query("SELECT COUNT(*) FROM power_records")
    suspend fun getRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PowerRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PowerRecord>)

    @Query("DELETE FROM power_records")
    suspend fun deleteAll()
}
