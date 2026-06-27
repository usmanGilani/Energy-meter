package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "power_records")
data class PowerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long, // Epoch millis
    val voltage: Float, // V
    val current: Float, // A
    val power: Float, // W
    val energy: Float, // kWh
    val frequency: Float, // Hz
    val powerFactor: Float // PF
) {
    // Calculated Properties
    val apparentPower: Float
        get() = voltage * current

    val reactivePower: Float
        get() {
            val s = apparentPower
            val p = power
            return if (s > p) {
                kotlin.math.sqrt(s * s - p * p)
            } else {
                0f
            }
        }
}
