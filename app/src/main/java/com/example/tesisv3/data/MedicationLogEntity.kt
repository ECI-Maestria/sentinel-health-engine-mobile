package com.example.tesisv3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val medicationName: String,
    val status: String,
    val takenAt: Long
)
