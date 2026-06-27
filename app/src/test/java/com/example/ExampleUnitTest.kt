package com.example

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun fetchSpreadsheetAndPrint() {
    val client = OkHttpClient()
    val spreadsheetId = "10_5kvSRtBEp3vTJ3A-z-i5jBqHP3abA3Rx2jKHmQxkk"
    val url = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv"
    val request = Request.Builder().url(url).build()
    
    try {
        client.newCall(request).execute().use { response ->
            println("--- RESPONSE STATUS: ${response.code} ---")
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val lines = body.lines().filter { it.isNotBlank() }
                
                var headerIndex = 0
                for (i in lines.indices) {
                    val line = lines[i]
                    val lower = line.lowercase(java.util.Locale.US)
                    if (lower.contains("timestamp") || lower.contains("voltage") || lower.contains("volt")) {
                        headerIndex = i
                        break
                    }
                }
                
                println("Detected headers at index: $headerIndex")
                val headerLine = lines[headerIndex]
                val headers = headerLine.split(",").map { it.trim().lowercase(java.util.Locale.US).replace(" ", "") }
                println("Headers parsed: $headers")
                
                val tsIndex = headers.indexOfFirst { it.contains("time") }
                val vIndex = headers.indexOfFirst { it.contains("volt") }
                val cIndex = headers.indexOfFirst { it.contains("curr") }
                val pIndex = headers.indexOfFirst { it.contains("power") && !it.contains("factor") }
                val eIndex = headers.indexOfFirst { it.contains("energy") }
                val fIndex = headers.indexOfFirst { it.contains("freq") }
                val pfIndex = headers.indexOfFirst { it.contains("factor") || it == "pf" }
                
                println("Column Indices: ts=$tsIndex, v=$vIndex, c=$cIndex, p=$pIndex, e=$eIndex, f=$fIndex, pf=$pfIndex")
                
                var recordsParsed = 0
                for (i in (headerIndex + 1) until lines.size) {
                    val line = lines[i]
                    val cols = line.split(",")
                    if (cols.size < 2) continue
                    
                    val voltage = if (vIndex != -1 && vIndex < cols.size) cols[vIndex].toFloatOrNull() ?: 0f else 0f
                    val current = if (cIndex != -1 && cIndex < cols.size) cols[cIndex].toFloatOrNull() ?: 0f else 0f
                    val power = if (pIndex != -1 && pIndex < cols.size) cols[pIndex].toFloatOrNull() ?: 0f else 0f
                    val energy = if (eIndex != -1 && eIndex < cols.size) cols[eIndex].toFloatOrNull() ?: 0f else 0f
                    
                    if (voltage == 0f && current == 0f && power == 0f) {
                        continue
                    }
                    
                    recordsParsed++
                    if (recordsParsed <= 5) {
                        println("Parsed Record #$recordsParsed: Time=${cols.getOrNull(tsIndex)}, V=$voltage, C=$current, P=$power, E=$energy")
                    }
                }
                println("Total parsed valid records: $recordsParsed")
                assertTrue(recordsParsed > 0)
            } else {
                println("Response not successful: ${response.message}")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        fail(e.message)
    }
  }
}

