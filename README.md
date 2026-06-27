# Energy Monitor Pro
### Industrial-Grade SCADA Power Grid Monitoring & Analytics Engine

Energy Monitor Pro is a read-only, high-performance SCADA-style mobile application designed for electrical engineers and grid operations managers. It securely monitors electrical parameters in real time by connecting to a Google Sheets database backed by a NodeMCU or ESP32 hardware telemetry stream.

---

## 🛠️ System Architecture

```
  +-------------------+        Telemetry        +------------------+
  |  Power Analyzer   |------------------------>|  NodeMCU/ESP32   |
  |  (Sensors/Energy) |                         |  (WiFi Buffer)   |
  +-------------------+                         +------------------+
                                                         |
                                                         v HTTPS POST
+---------------------+      JSON REST API      +------------------+
| Energy Monitor Pro  |<------------------------| Google Apps      |
| (Native Android App)|                         | Script (GAS)     |
+---------+-----------+                         +--------+---------+
          |                                              |
          | Offline Cache                                | SQL-like CRUD
          v                                              v
+---------------------+                         +------------------+
|    Local Room DB    |                         |   Google Sheet   |
|   (SQLite Engine)   |                         | (data database)  |
+---------------------+                         +------------------+
```

---

## 📊 Google Sheet Configuration

Create a Google Spreadsheet with a sheet named exactly: `data`

### 📋 Columns Matrix (Row 1 Header Names)

| Column | Name | Type | Description |
| :--- | :--- | :--- | :--- |
| **A** | `Timestamp` | DateTime | Sample collection epoch time or date string |
| **B** | `Voltage` | Float (V) | RMS AC line voltage |
| **C** | `Current` | Float (A) | RMS AC load current |
| **D** | `Power` | Float (W) | Active power demand ($P = V \times I \times \cos \phi$) |
| **E** | `Energy` | Float (kWh) | Cumulative electrical consumption |
| **F** | `Frequency` | Float (Hz) | Grid frequency |
| **G** | `Power Factor`| Float ($\cos \phi$)| Power quality angle index ($0.0 \rightarrow 1.0$) |

---

## ☁️ Google Apps Script (GAS) Deployment

To enable secure, real-time HTTPS JSON synchronization, you must deploy the Google Apps Script included in this repository (`google_apps_script.js`) as a Web App:

1. Open your configured Google Sheet.
2. Click **Extensions** -> **Apps Script** from the top menu bar.
3. Delete any default code in `Code.gs` and paste the exact contents of `google_apps_script.js` from this project's root folder.
4. Click **Deploy** -> **New Deployment**.
5. Configure the deployment:
   - **Type:** Web App
   - **Description:** Energy Monitor Pro SCADA Connector API
   - **Execute As:** Me (Your Account)
   - **Who Has Access:** Anyone
6. Click **Deploy** and authorize the script access permissions.
7. Copy the generated **Web App URL** (ends with `/exec`).
8. Paste this URL into the **GAS Cloud Configuration** section inside the App's **Settings** tab.

---

## 📱 Mobile App Features

*   **SCADA Industrial Dashboard:**
    *   Circular analog dials for real-time Voltage, Current, Active Power, and Power Factor tracking.
    *   Color-coded flashing alarm indicator panel responding to voltage dips/surges, load overrides, and poor power factors.
    *   Dual-mode live telemetry vs. historical review toggles.
*   **Intelligent History Screen:**
    *   Full-screen diagnostics and event logger displaying 200,000+ cached Room records.
    *   Search and filter records by active anomalies (voltage violations, current overloads, or low power factors).
    *   Advanced CSV/Diagnostics logger export utilities.
*   **Load Analytics & Harmonics:**
    *   Interactive Zoom & Pinch charts representing Active Power, Voltage deviations, Current profiles, and System Frequency stability.
    *   Weekly/monthly aggregate bar charts displaying power demand and accumulative consumption logs.
*   **Alarms & System Configuration:**
    *   Min/Max Voltage, Max Current, Max Power, and Minimum Power Factor alarm threshold sliders.
    *   Custom electricity tariff multiplier parameter ($/kWh) for real-time electrical billing forecasting.
    *   Local SQLite database buffer cache clearing utilities.

---

## 🏗️ Build & Compiling Instructions

This is a native Android application built with **Kotlin** and **Jetpack Compose** following modern clean architecture MVVM guidelines.

### Command to Compile Release APK
To generate a fully signed, optimized release APK, execute the following Gradle command in the applet's working directory:

```bash
gradle assembleDebug
```
The compiled APK will be outputted to:
`/app/build/outputs/apk/debug/app-debug.apk`

---

## ⚙️ Development Environment Technical Spec

*   **Language:** Kotlin 1.9.x
*   **UI System:** Jetpack Compose (Material Design 3 Theme Engine with SCADA Custom Overrides)
*   **Database:** Room SQL Persistence Layer with full KSP compiler optimization
*   **API Client:** Retrofit 2 REST Engine with full offline cache interceptors
*   **Dependency Injection:** Clean constructor injection via ViewModel factories

---
*Designed for corporate grid control and industrial automation integrations.*
