package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PowerDatabase
import com.example.data.local.PowerRecord
import com.example.data.repository.EnergyRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EnergyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PowerDatabase.getDatabase(application)
    private val energyRepository = EnergyRepository(database.powerDao)
    private val settingsRepository = SettingsRepository(application)

    // Settings flows
    val googleAppsScriptUrl = settingsRepository.googleAppsScriptUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "https://docs.google.com/spreadsheets/d/10_5kvSRtBEp3vTJ3A-z-i5jBqHP3abA3Rx2jKHmQxkk/edit?usp=drivesdk")
    val electricityTariff = settingsRepository.electricityTariff.stateIn(viewModelScope, SharingStarted.Eagerly, 0.15f)
    val refreshIntervalSec = settingsRepository.refreshIntervalSec.stateIn(viewModelScope, SharingStarted.Eagerly, 5)
    val voltageMin = settingsRepository.voltageMin.stateIn(viewModelScope, SharingStarted.Eagerly, 210f)
    val voltageMax = settingsRepository.voltageMax.stateIn(viewModelScope, SharingStarted.Eagerly, 250f)
    val currentMax = settingsRepository.currentMax.stateIn(viewModelScope, SharingStarted.Eagerly, 15.0f)
    val powerMax = settingsRepository.powerMax.stateIn(viewModelScope, SharingStarted.Eagerly, 3000f)
    val pfMin = settingsRepository.pfMin.stateIn(viewModelScope, SharingStarted.Eagerly, 0.85f)
    val themeMode = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "LIGHT")

    // UI Telemetry State
    private val _latestRecord = MutableStateFlow<PowerRecord?>(null)
    val latestRecord: StateFlow<PowerRecord?> = _latestRecord.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    // Alarm Status
    val hasHighVoltageAlarm = latestRecord.combine(voltageMax) { record, max ->
        record != null && record.voltage > max
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hasLowVoltageAlarm = latestRecord.combine(voltageMin) { record, min ->
        record != null && record.voltage < min
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hasOverloadAlarm = latestRecord.combine(powerMax) { record, max ->
        record != null && record.power > max
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hasPoorPfAlarm = latestRecord.combine(pfMin) { record, min ->
        record != null && record.powerFactor < min
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // History and analytics
    private val _historyRecords = MutableStateFlow<List<PowerRecord>>(emptyList())
    val historyRecords: StateFlow<List<PowerRecord>> = _historyRecords.asStateFlow()

    // Sorting & Filtering parameters
    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow("DESC") // "DESC" or "ASC"
    val filterType = MutableStateFlow("ALL") // "ALL", "ALARM", "VOLTAGE", "OVERLOAD"

    // Paginated / Sorted History list based on search/filters
    val filteredHistory: StateFlow<List<PowerRecord>> = combine(
        _historyRecords,
        searchQuery,
        sortOrder,
        filterType
    ) { records, query, order, filter ->
        val vMin = voltageMin.value
        val vMax = voltageMax.value
        val pMax = powerMax.value
        val pfM = pfMin.value

        var list = records.filter { record ->
            // Date matching or general text match
            query.isBlank() || 
            String.format("%.1f", record.voltage).contains(query) ||
            String.format("%.1f", record.power).contains(query)
        }

        list = when (filter) {
            "ALARM" -> list.filter { it.voltage < vMin || it.voltage > vMax || it.power > pMax || it.powerFactor < pfM }
            "VOLTAGE" -> list.filter { it.voltage < vMin || it.voltage > vMax }
            "OVERLOAD" -> list.filter { it.power > pMax }
            else -> list
        }

        if (order == "ASC") {
            list.sortedBy { it.timestamp }
        } else {
            list.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Aggregates for Charts & Analytics
    val chartDataPoints = _historyRecords.map { list ->
        list.map { Pair(it.timestamp, it.power) }.take(100).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val voltageDataPoints = _historyRecords.map { list ->
        list.map { Pair(it.timestamp, it.voltage) }.take(100).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val currentDataPoints = _historyRecords.map { list ->
        list.map { Pair(it.timestamp, it.current) }.take(100).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val freqDataPoints = _historyRecords.map { list ->
        list.map { Pair(it.timestamp, it.frequency) }.take(100).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val pfDataPoints = _historyRecords.map { list ->
        list.map { Pair(it.timestamp, it.powerFactor) }.take(100).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Real Daily/Monthly aggregates calculated from Google Sheet data
    private val _dailyEnergy = MutableStateFlow<List<Pair<Long, Float>>>(emptyList())
    val dailyEnergy: StateFlow<List<Pair<Long, Float>>> = _dailyEnergy.asStateFlow()

    private val _monthlyEnergy = MutableStateFlow<List<Pair<Long, Float>>>(emptyList())
    val monthlyEnergy: StateFlow<List<Pair<Long, Float>>> = _monthlyEnergy.asStateFlow()

    // Calculated Statistics
    val maxPowerDemand = _historyRecords.map { list ->
        if (list.isEmpty()) 0f else list.maxOf { it.power }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val averageVoltage = _historyRecords.map { list ->
        if (list.isEmpty()) 0f else list.map { it.voltage }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val averageCurrent = _historyRecords.map { list ->
        if (list.isEmpty()) 0f else list.map { it.current }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val averagePower = _historyRecords.map { list ->
        if (list.isEmpty()) 0f else list.map { it.power }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val averagePowerFactor = _historyRecords.map { list ->
        if (list.isEmpty()) 0f else list.map { it.powerFactor }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    // Cost and CO2 calculation
    val todayEnergyKWh: StateFlow<Float> = _historyRecords.map { records ->
        if (records.isEmpty()) return@map 0f
        val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        
        val todayStr = sdfDay.format(Date())
        var todayRecords = records.filter { sdfDay.format(Date(it.timestamp)) == todayStr }
        
        if (todayRecords.isEmpty()) {
            val latestRecordTime = records.maxOfOrNull { it.timestamp }
            if (latestRecordTime != null) {
                val latestDayStr = sdfDay.format(Date(latestRecordTime))
                todayRecords = records.filter { sdfDay.format(Date(it.timestamp)) == latestDayStr }
            }
        }
        
        if (todayRecords.isEmpty()) return@map 0f
        
        val maxEnergy = todayRecords.maxOf { it.energy }
        val minEnergy = todayRecords.minOf { it.energy }
        val consumption = maxEnergy - minEnergy
        if (consumption > 0.01f) {
            consumption
        } else {
            maxEnergy.coerceAtLeast(0f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val monthlyEnergyKWh: StateFlow<Float> = _historyRecords.map { records ->
        if (records.isEmpty()) return@map 0f
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = TimeZone.getDefault() }
        
        val currentMonthStr = sdfMonth.format(Date())
        var monthRecords = records.filter { sdfMonth.format(Date(it.timestamp)) == currentMonthStr }
        
        if (monthRecords.isEmpty()) {
            val latestRecordTime = records.maxOfOrNull { it.timestamp }
            if (latestRecordTime != null) {
                val latestMonthStr = sdfMonth.format(Date(latestRecordTime))
                monthRecords = records.filter { sdfMonth.format(Date(it.timestamp)) == latestMonthStr }
            }
        }
        
        if (monthRecords.isEmpty()) return@map 0f
        
        val maxEnergy = monthRecords.maxOf { it.energy }
        val minEnergy = monthRecords.minOf { it.energy }
        val consumption = maxEnergy - minEnergy
        if (consumption > 0.01f) {
            consumption
        } else {
            maxEnergy.coerceAtLeast(0f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    val runningCostVal = monthlyEnergyKWh.combine(electricityTariff) { energy, tariff -> energy * tariff }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)
    val co2EmissionsVal = monthlyEnergyKWh.map { it * 0.475f } // 0.475 kg/kWh CO2
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    private var refreshJob: Job? = null
    private var simulatedEnergyAccumulator = 124.5f

    init {
        // Initial Startup Google Sheet Sync
        viewModelScope.launch {
            _isSyncing.value = true
            _connectionError.value = null
            try {
                val url = googleAppsScriptUrl.value
                if (url.isNotBlank()) {
                    // Sync the fresh Google Sheet historical data
                    energyRepository.fetchAndSyncHistory(url, limit = 300)
                    // Fetch the latest data point
                    val record = energyRepository.fetchAndSyncLatest(url)
                    if (record != null) {
                        _latestRecord.value = record
                        simulatedEnergyAccumulator = record.energy
                    }
                }
            } catch (e: Exception) {
                _connectionError.value = e.localizedMessage
            } finally {
                _isSyncing.value = false
            }
        }

        // Load initial cache from Room and update aggregates
        viewModelScope.launch {
            energyRepository.allRecordsFlow.collect { records ->
                _historyRecords.value = records
                if (records.isNotEmpty()) {
                    _latestRecord.value = records.first()
                    simulatedEnergyAccumulator = records.first().energy
                }
                // Update daily and monthly aggregates using real Google Sheet data
                updateRealTimeAggregates(records)
            }
        }

        // Monitor webapp URL to toggle Demo Mode and trigger full sync
        viewModelScope.launch {
            googleAppsScriptUrl.collect { url ->
                _isDemoMode.value = url.isBlank()
                if (url.isNotBlank()) {
                    triggerManualRefresh()
                }
                restartRefreshJob()
            }
        }

        // Monitor refresh interval setting
        viewModelScope.launch {
            refreshIntervalSec.collect {
                restartRefreshJob()
            }
        }
    }

    fun restartRefreshJob() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                if (_isDemoMode.value) {
                    generateSimulatedDataPoint()
                } else {
                    fetchLatestFromUrl()
                }
                delay(refreshIntervalSec.value * 1000L)
            }
        }
    }

    private suspend fun fetchLatestFromUrl() {
        val url = googleAppsScriptUrl.value
        if (url.isBlank()) return
        _isSyncing.value = true
        _connectionError.value = null
        try {
            val record = energyRepository.fetchAndSyncLatest(url)
            if (record != null) {
                _latestRecord.value = record
                _connectionError.value = null
            } else {
                _connectionError.value = "Unable to process telemetry payload"
            }
        } catch (e: Exception) {
            _connectionError.value = e.localizedMessage ?: "Connection Timeout"
        } finally {
            _isSyncing.value = false
        }
    }

    fun triggerManualRefresh() {
        viewModelScope.launch {
            if (_isDemoMode.value) {
                generateSimulatedDataPoint()
            } else {
                fetchLatestFromUrl()
                // Sync some history too!
                _isSyncing.value = true
                energyRepository.fetchAndSyncHistory(googleAppsScriptUrl.value, limit = 300)
                _isSyncing.value = false
            }
        }
    }

    private suspend fun generateSimulatedDataPoint() {
        val baseV = 230f
        val randomVOffset = (Math.random() - 0.5) * 8.0 // +/- 4V fluctuation
        val voltage = (baseV + randomVOffset).toFloat()

        // Heavy machines turning on/off
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val baseA = if (hour in 9..17) 6.5f else 2.5f
        val current = (baseA + Math.random() * 5.0).toFloat()

        val pf = (0.90 + (Math.random() - 0.5) * 0.1).toFloat().coerceIn(0.6f, 1.0f)
        val power = voltage * current * pf
        val freq = (50.0 + (Math.random() - 0.5) * 0.15).toFloat()

        // Increment Energy: Power (W) * Delay / 3,600,000 = kWh
        val intervalSec = refreshIntervalSec.value
        val deltaKWh = (power * intervalSec) / 3600000f
        simulatedEnergyAccumulator += deltaKWh

        val record = PowerRecord(
            timestamp = System.currentTimeMillis(),
            voltage = voltage,
            current = current,
            power = power,
            energy = simulatedEnergyAccumulator,
            frequency = freq,
            powerFactor = pf
        )

        // Keep local cache clean to prevent memory issues, but supports massive records conceptually
        _latestRecord.value = record
        energyRepository.insertRecord(record)
    }

    private fun updateRealTimeAggregates(records: List<PowerRecord>) {
        if (records.isEmpty()) {
            _dailyEnergy.value = emptyList()
            _monthlyEnergy.value = emptyList()
            return
        }

        val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = TimeZone.getDefault() }

        // Group by day
        val recordsByDay = records.groupBy { sdfDay.format(Date(it.timestamp)) }
        val dailyList = recordsByDay.map { (dayStr, dayRecords) ->
            val timestamp = sdfDay.parse(dayStr)?.time ?: dayRecords.first().timestamp
            val maxEnergy = dayRecords.maxOf { it.energy }
            val minEnergy = dayRecords.minOf { it.energy }
            var consumption = maxEnergy - minEnergy
            if (consumption <= 0.01f) {
                consumption = maxEnergy
            }
            if (consumption <= 0f) {
                val avgPower = dayRecords.map { it.power }.average().toFloat()
                val hours = dayRecords.size * 5f / 3600f
                consumption = (avgPower * hours) / 1000f
            }
            Pair(timestamp, consumption)
        }.sortedBy { it.first }

        _dailyEnergy.value = dailyList

        // Group by month
        val recordsByMonth = records.groupBy { sdfMonth.format(Date(it.timestamp)) }
        val monthlyList = recordsByMonth.map { (monthStr, monthRecords) ->
            val timestamp = sdfMonth.parse(monthStr)?.time ?: monthRecords.first().timestamp
            val maxEnergy = monthRecords.maxOf { it.energy }
            val minEnergy = monthRecords.minOf { it.energy }
            var consumption = maxEnergy - minEnergy
            if (consumption <= 0.01f) {
                consumption = maxEnergy
            }
            if (consumption <= 0f) {
                val avgPower = monthRecords.map { it.power }.average().toFloat()
                val hours = monthRecords.size * 5f / 3600f
                consumption = (avgPower * hours) / 1000f
            }
            Pair(timestamp, consumption)
        }.sortedBy { it.first }

        _monthlyEnergy.value = monthlyList
    }

    // Settings actions
    fun setGoogleAppsScriptUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setGoogleAppsScriptUrl(url)
        }
    }

    fun setElectricityTariff(tariff: Float) {
        viewModelScope.launch {
            settingsRepository.setElectricityTariff(tariff)
        }
    }

    fun setRefreshIntervalSec(interval: Int) {
        viewModelScope.launch {
            settingsRepository.setRefreshIntervalSec(interval)
        }
    }

    fun setVoltageLimits(min: Float, max: Float) {
        viewModelScope.launch {
            settingsRepository.setVoltageLimits(min, max)
        }
    }

    fun setCurrentMax(max: Float) {
        viewModelScope.launch {
            settingsRepository.setCurrentMax(max)
        }
    }

    fun setPowerMax(max: Float) {
        viewModelScope.launch {
            settingsRepository.setPowerMax(max)
        }
    }

    fun setPfMin(min: Float) {
        viewModelScope.launch {
            settingsRepository.setPfMin(min)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            energyRepository.clearAllData()
            _latestRecord.value = null
            simulatedEnergyAccumulator = 124.5f
        }
    }
}
