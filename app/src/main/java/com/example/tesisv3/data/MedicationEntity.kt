package com.example.tesisv3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: String,
    val scheduleType: String,
    val hourOfDay: Int?,
    val minute: Int?,
    val intervalHours: Int?,
    val status: String,
    val enabled: Boolean,
    val medType: String,
    val createdAt: Long
)
