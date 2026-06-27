package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemotePowerRecord(
    val timestamp: String, // String ISO or epoch string
    val voltage: Float,
    val current: Float,
    val power: Float,
    val energy: Float,
    val frequency: Float,
    val powerFactor: Float
)

@JsonClass(generateAdapter = true)
data class RemoteLatestResponse(
    val status: String,
    val timestamp: String,
    val voltage: Float,
    val current: Float,
    val power: Float,
    val energy: Float,
    val frequency: Float,
    val powerFactor: Float,
    val lastUpdated: String
)

@JsonClass(generateAdapter = true)
data class RemoteSummaryResponse(
    val maxDemand: Float,
    val avgVoltage: Float,
    val avgCurrent: Float,
    val avgPower: Float,
    val avgPowerFactor: Float,
    val todayEnergy: Float,
    val monthlyEnergy: Float,
    val yearlyEnergy: Float,
    val co2Emissions: Float,
    val runningCost: Float
)

@JsonClass(generateAdapter = true)
data class RemoteAnalyticsResponse(
    val status: String,
    val message: String,
    val systemHealth: String,
    val efficiencyScore: Float,
    val peakHours: List<String>,
    val harmonicsDistortion: Float
)

@JsonClass(generateAdapter = true)
data class RemoteDailyEnergy(
    val date: String,
    val energy: Float
)

@JsonClass(generateAdapter = true)
data class RemoteMonthlyEnergy(
    val month: String,
    val energy: Float
)
