package com.example.tesisv3.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MedicationEntity)

    @Update
    suspend fun update(entity: MedicationEntity)

    @Delete
    suspend fun delete(entity: MedicationEntity)

    @Query("UPDATE medications SET status = :newStatus")
    suspend fun updateAllStatus(newStatus: String)
}
