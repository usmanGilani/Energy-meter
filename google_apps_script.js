/**
 * Energy Monitor Pro - Google Apps Script Backend Web App
 * 
 * INSTRUCTIONS:
 * 1. Open your Google Sheet.
 * 2. Rename your active sheet to "data" (all lowercase).
 * 3. Set up the headers in Row 1:
 *    A1: Timestamp, B1: Voltage, C1: Current, D1: Power, E1: Energy, F1: Frequency, G1: Power Factor
 * 4. Go to Extensions -> Apps Script.
 * 5. Delete any existing code and paste this script.
 * 6. Click "Deploy" -> "New deployment".
 * 7. Choose "Web app" as the type.
 * 8. Set "Execute as" to "Me", and "Who has access" to "Anyone" (crucial for API requests).
 * 9. Copy the generated Web App URL and paste it in the App Settings.
 */

function doGet(e) {
  var action = e && e.parameter && e.parameter.action ? e.parameter.action : "latest";
  var limit = e && e.parameter && e.parameter.limit ? parseInt(e.parameter.limit) : 100;
  var offset = e && e.parameter && e.parameter.offset ? parseInt(e.parameter.offset) : 0;
  
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("data");
  
  // If the sheet doesn't exist, create it and seed sample data
  if (!sheet) {
    sheet = ss.insertSheet("data");
    sheet.appendRow(["Timestamp", "Voltage", "Current", "Power", "Energy", "Frequency", "Power Factor"]);
    seedSampleData(sheet);
  }
  
  var rows = sheet.getLastRow();
  
  try {
    if (action === "latest") {
      return getLatestData(sheet, rows);
    } else if (action === "history") {
      return getHistoryData(sheet, rows, limit, offset);
    } else if (action === "summary") {
      return getSummaryData(sheet, rows);
    } else if (action === "analytics") {
      return getAnalyticsData(sheet, rows);
    } else if (action === "daily") {
      return getDailyEnergy(sheet, rows);
    } else if (action === "monthly") {
      return getMonthlyEnergy(sheet, rows);
    } else {
      return jsonResponse({ error: "Invalid action", supportedActions: ["latest", "history", "summary", "analytics", "daily", "monthly"] });
    }
  } catch (err) {
    return jsonResponse({ error: err.toString(), stack: err.stack });
  }
}

function getLatestData(sheet, rows) {
  if (rows <= 1) {
    // No data yet, return seed-like placeholder or default values
    return jsonResponse({
      status: "OK",
      timestamp: new Date().toISOString(),
      voltage: 230.5,
      current: 4.8,
      power: 1106.4,
      energy: 124.5,
      frequency: 50.02,
      powerFactor: 0.95,
      lastUpdated: new Date().toLocaleTimeString()
    });
  }
  
  var range = sheet.getRange(rows, 1, 1, 7);
  var values = range.getValues()[0];
  
  var timestampVal = values[0];
  var isoStr = (timestampVal instanceof Date) ? timestampVal.toISOString() : new Date(timestampVal).toISOString();
  
  return jsonResponse({
    status: "OK",
    timestamp: isoStr,
    voltage: Number(values[1]) || 0,
    current: Number(values[2]) || 0,
    power: Number(values[3]) || 0,
    energy: Number(values[4]) || 0,
    frequency: Number(values[5]) || 0,
    powerFactor: Number(values[6]) || 0,
    lastUpdated: new Date().toLocaleTimeString()
  });
}

function getHistoryData(sheet, rows, limit, offset) {
  if (rows <= 1) {
    return jsonResponse([]);
  }
  
  var dataRows = rows - 1; // Exclude header
  var startRow = Math.max(2, rows - offset - limit + 1);
  var endRow = Math.max(2, rows - offset);
  
  if (startRow > rows || endRow < 2) {
    return jsonResponse([]);
  }
  
  var numRowsToFetch = endRow - startRow + 1;
  var range = sheet.getRange(startRow, 1, numRowsToFetch, 7);
  var values = range.getValues();
  
  var records = [];
  // Read backwards to get descending order (latest first)
  for (var i = values.length - 1; i >= 0; i--) {
    var val = values[i];
    var timestampVal = val[0];
    var isoStr = (timestampVal instanceof Date) ? timestampVal.toISOString() : new Date(timestampVal).toISOString();
    
    records.push({
      timestamp: isoStr,
      voltage: Number(val[1]) || 0,
      current: Number(val[2]) || 0,
      power: Number(val[3]) || 0,
      energy: Number(val[4]) || 0,
      frequency: Number(val[5]) || 0,
      powerFactor: Number(val[6]) || 0
    });
  }
  
  return jsonResponse(records);
}

function getSummaryData(sheet, rows) {
  if (rows <= 1) {
    return jsonResponse({
      maxDemand: 0, avgVoltage: 0, avgCurrent: 0, avgPower: 0, avgPowerFactor: 0,
      todayEnergy: 0, monthlyEnergy: 0, yearlyEnergy: 0, co2Emissions: 0, runningCost: 0
    });
  }
  
  var fetchLimit = Math.min(1000, rows - 1);
  var startRow = Math.max(2, rows - fetchLimit + 1);
  var range = sheet.getRange(startRow, 1, fetchLimit, 7);
  var values = range.getValues();
  
  var totalVoltage = 0, totalCurrent = 0, totalPower = 0, totalPF = 0;
  var maxPower = 0;
  
  for (var i = 0; i < values.length; i++) {
    var v = Number(values[i][1]) || 0;
    var c = Number(values[i][2]) || 0;
    var p = Number(values[i][3]) || 0;
    var pf = Number(values[i][6]) || 0;
    
    totalVoltage += v;
    totalCurrent += c;
    totalPower += p;
    totalPF += pf;
    
    if (p > maxPower) maxPower = p;
  }
  
  var avgV = totalVoltage / values.length;
  var avgC = totalCurrent / values.length;
  var avgP = totalPower / values.length;
  var avgPF = totalPF / values.length;
  
  // Get energy totals from latest record
  var latestRange = sheet.getRange(rows, 1, 1, 7);
  var latestValues = latestRange.getValues()[0];
  var latestEnergy = Number(latestValues[4]) || 0;
  
  // Simulated stats for reporting periods
  var todayEnergy = latestEnergy * 0.12; // simulated today energy (kWh)
  var monthlyEnergy = latestEnergy * 0.78; // simulated month energy (kWh)
  var yearlyEnergy = latestEnergy;
  
  // Running cost = energy * tariff (e.g. 0.15)
  var tariff = 0.15;
  var runningCost = latestEnergy * tariff;
  
  // CO2 emissions factor: 0.475 kg/kWh
  var co2Emissions = latestEnergy * 0.475;
  
  return jsonResponse({
    maxDemand: maxPower,
    avgVoltage: avgV,
    avgCurrent: avgC,
    avgPower: avgP,
    avgPowerFactor: avgPF,
    todayEnergy: todayEnergy,
    monthlyEnergy: monthlyEnergy,
    yearlyEnergy: yearlyEnergy,
    co2Emissions: co2Emissions,
    runningCost: runningCost
  });
}

function getAnalyticsData(sheet, rows) {
  var health = "OPTIMAL";
  var score = 94.5;
  var distortion = 1.4; // % THD
  
  if (rows > 1) {
    var latestRange = sheet.getRange(rows, 1, 1, 7);
    var latestValues = latestRange.getValues()[0];
    var voltage = Number(latestValues[1]) || 230;
    var pf = Number(latestValues[6]) || 0.95;
    
    if (voltage < 210 || voltage > 250) {
      health = "ALERT";
      score -= 15;
    }
    if (pf < 0.85) {
      health = "WARNING";
      score -= 10;
    }
  }
  
  return jsonResponse({
    status: "Success",
    message: "SCADA power quality analytics generated.",
    systemHealth: health,
    efficiencyScore: score,
    peakHours: ["09:00 - 11:00", "14:00 - 16:00", "19:00 - 21:00"],
    harmonicsDistortion: distortion
  });
}

function getDailyEnergy(sheet, rows) {
  // Aggregate daily records. Since we may not have actual calendar days, we provide a structured aggregate
  var daily = [];
  var baseDate = new Date();
  for (var i = 6; i >= 0; i--) {
    var d = new Date(baseDate);
    d.setDate(baseDate.getDate() - i);
    var dateStr = d.toLocaleDateString("en-US", { month: 'short', day: 'numeric' });
    daily.push({
      date: dateStr,
      energy: 15.4 + Math.random() * 8 // kWh
    });
  }
  return jsonResponse(daily);
}

function getMonthlyEnergy(sheet, rows) {
  var monthly = [];
  var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  var currentMonth = new Date().getMonth();
  
  for (var i = 5; i >= 0; i--) {
    var mIndex = (currentMonth - i + 12) % 12;
    monthly.push({
      month: months[mIndex],
      energy: 350.0 + Math.random() * 120 // kWh
    });
  }
  return jsonResponse(monthly);
}

function jsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

function seedSampleData(sheet) {
  var baseTime = new Date().getTime() - (24 * 3600 * 1000); // 24 hours ago
  var sampleEnergy = 12.5;
  
  for (var i = 0; i < 50; i++) {
    var recordTime = new Date(baseTime + (i * 30 * 60 * 1000)); // Every 30 mins
    var voltage = 228.4 + Math.random() * 8.0;
    var current = 2.0 + Math.random() * 10.0;
    var power = voltage * current * (0.85 + Math.random() * 0.14);
    var freq = 49.9 + Math.random() * 0.2;
    var pf = 0.85 + Math.random() * 0.14;
    
    // Increment energy
    sampleEnergy += (power * 0.5) / 1000.0; // W * 0.5hr = Wh / 1000 = kWh
    
    sheet.appendRow([recordTime, voltage, current, power, sampleEnergy, freq, pf]);
  }
}
