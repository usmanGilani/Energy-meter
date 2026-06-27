package com.example.data.repository

import com.example.data.local.PowerDao
import com.example.data.local.PowerRecord
import com.example.data.remote.EnergyApiService
import com.example.data.remote.RemoteLatestResponse
import com.example.data.remote.RemotePowerRecord
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class EnergyRepository(
    private val powerDao: PowerDao,
    private val apiService: EnergyApiService = RetrofitClient.apiService
) {
    val allRecordsFlow: Flow<List<PowerRecord>> = powerDao.getAllFlow()
    val latestRecordFlow: Flow<PowerRecord?> = powerDao.getLatestFlow()

    suspend fun getLatestRecord(): PowerRecord? {
        return powerDao.getLatest()
    }

    suspend fun getRecordCount(): Int {
        return powerDao.getRecordCount()
    }

    suspend fun clearAllData() {
        powerDao.deleteAll()
    }

    suspend fun insertRecord(record: PowerRecord) {
        powerDao.insert(record)
    }

    suspend fun fetchAndSyncLatest(webAppUrl: String): PowerRecord? {
        if (webAppUrl.isBlank()) return null
        
        if (webAppUrl.contains("docs.google.com/spreadsheets/d/")) {
            val records = fetchGoogleSheetCsv(webAppUrl)
            if (records.isNotEmpty()) {
                val existingTimestamps = powerDao.getAll().map { it.timestamp }.toSet()
                val newRecords = records.filter { it.timestamp !in existingTimestamps }
                if (newRecords.isNotEmpty()) {
                    powerDao.insertAll(newRecords)
                }
                return records.maxByOrNull { it.timestamp }
            }
            return null
        }
        
        return try {
            val remoteLatest = apiService.getLatest(webAppUrl)
            val parsedTime = parseIsoOrCurrent(remoteLatest.timestamp)
            
            val record = PowerRecord(
                timestamp = parsedTime,
                voltage = remoteLatest.voltage,
                current = remoteLatest.current,
                power = remoteLatest.power,
                energy = remoteLatest.energy,
                frequency = remoteLatest.frequency,
                powerFactor = remoteLatest.powerFactor
            )
            
            powerDao.insert(record)
            record
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchAndSyncHistory(webAppUrl: String, limit: Int = 100, offset: Int = 0): Boolean {
        if (webAppUrl.isBlank()) return false
        
        if (webAppUrl.contains("docs.google.com/spreadsheets/d/")) {
            val records = fetchGoogleSheetCsv(webAppUrl)
            if (records.isNotEmpty()) {
                val existingTimestamps = powerDao.getAll().map { it.timestamp }.toSet()
                val newRecords = records.filter { it.timestamp !in existingTimestamps }
                if (newRecords.isNotEmpty()) {
                    powerDao.insertAll(newRecords)
                }
                return true
            }
            return false
        }
        
        return try {
            val remoteList = apiService.getHistory(webAppUrl, limit = limit, offset = offset)
            if (remoteList.isNotEmpty()) {
                val localRecords = remoteList.map { remote ->
                    PowerRecord(
                        timestamp = parseIsoOrCurrent(remote.timestamp),
                        voltage = remote.voltage,
                        current = remote.current,
                        power = remote.power,
                        energy = remote.energy,
                        frequency = remote.frequency,
                        powerFactor = remote.powerFactor
                    )
                }
                powerDao.insertAll(localRecords)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPaginatedHistory(limit: Int, offset: Int): List<PowerRecord> {
        return powerDao.getPaginated(limit, offset)
    }

    suspend fun getRecordsInRange(start: Long, end: Long): List<PowerRecord> {
        return powerDao.getRecordsInRange(start, end)
    }

    private fun parseIsoOrCurrent(isoString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val format2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format2.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private suspend fun fetchGoogleSheetCsv(url: String): List<PowerRecord> = withContext(Dispatchers.IO) {
        val spreadsheetId = extractSpreadsheetId(url) ?: return@withContext emptyList()
        val csvUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&cache_buster=${System.currentTimeMillis()}"
        
        val request = Request.Builder()
            .url(csvUrl)
            .build()
            
        try {
            RetrofitClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val lines = bodyString.lines().filter { it.isNotBlank() }
                if (lines.size <= 1) return@withContext emptyList()
                
                // Dynamic header row detection
                var headerIndex = 0
                for (i in lines.indices) {
                    val line = lines[i]
                    val lower = line.lowercase(Locale.US)
                    if (lower.contains("timestamp") || lower.contains("voltage") || lower.contains("volt")) {
                        headerIndex = i
                        break
                    }
                }
                
                val headerLine = lines[headerIndex]
                val headers = parseCsvLine(headerLine).map { it.lowercase(Locale.US).replace(" ", "") }
                
                val tsIndex = headers.indexOfFirst { it.contains("time") }
                val vIndex = headers.indexOfFirst { it.contains("volt") }
                val cIndex = headers.indexOfFirst { it.contains("curr") }
                val pIndex = headers.indexOfFirst { it.contains("power") && !it.contains("factor") }
                val eIndex = headers.indexOfFirst { it.contains("energy") }
                val fIndex = headers.indexOfFirst { it.contains("freq") }
                val pfIndex = headers.indexOfFirst { it.contains("factor") || it == "pf" }
                
                val records = mutableListOf<PowerRecord>()
                for (i in (headerIndex + 1) until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue
                    val cols = parseCsvLine(line)
                    if (cols.size < 2) continue
                    
                    try {
                        val timestamp = if (tsIndex != -1 && tsIndex < cols.size) parseDateOrCurrent(cols[tsIndex]) else System.currentTimeMillis()
                        val voltage = if (vIndex != -1 && vIndex < cols.size) cols[vIndex].toFloatOrNull() ?: 0f else 0f
                        val current = if (cIndex != -1 && cIndex < cols.size) cols[cIndex].toFloatOrNull() ?: 0f else 0f
                        val power = if (pIndex != -1 && pIndex < cols.size) cols[pIndex].toFloatOrNull() ?: 0f else 0f
                        val energy = if (eIndex != -1 && eIndex < cols.size) cols[eIndex].toFloatOrNull() ?: 0f else 0f
                        val frequency = if (fIndex != -1 && fIndex < cols.size) cols[fIndex].toFloatOrNull() ?: 0f else 0f
                        val powerFactor = if (pfIndex != -1 && pfIndex < cols.size) cols[pfIndex].toFloatOrNull() ?: 0f else 0f
                        
                        // Only add record if it has valid telemetry values (avoid empty placeholder lines)
                        if (voltage == 0f && current == 0f && power == 0f) {
                            continue
                        }
                        
                        records.add(
                            PowerRecord(
                                id = 0, // autoGenerate id
                                timestamp = timestamp,
                                voltage = voltage,
                                current = current,
                                power = power,
                                energy = energy,
                                frequency = frequency,
                                powerFactor = powerFactor
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                records
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun extractSpreadsheetId(url: String): String? {
        val delimiter = "/d/"
        val index = url.indexOf(delimiter)
        if (index == -1) return null
        val afterD = url.substring(index + delimiter.length)
        val nextSlash = afterD.indexOf("/")
        return if (nextSlash == -1) {
            afterD
        } else {
            afterD.substring(0, nextSlash)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '\"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(currentToken.toString().trim())
                    currentToken.setLength(0)
                }
                else -> {
                    currentToken.append(char)
                }
            }
        }
        result.add(currentToken.toString().trim())
        return result
    }

    private fun parseDateOrCurrent(dateString: String): Long {
        val clean = dateString.replace("\"", "").trim()
        if (clean.isBlank()) return System.currentTimeMillis()
        
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return format.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {}
        
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return format.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {}
        
        try {
            val format = SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.US)
            return format.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {}
        
        try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            return format.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {}

        try {
            val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
            return format.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {}

        return System.currentTimeMillis()
    }
}
