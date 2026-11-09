package com.muz.ztemodem.data

import java.util.Locale

// Status sambungan rangkaian dashboard
enum class ModemConnectionState {
    LIVE,
    NO_SERVICE,
    OFFLINE
}

// Model diagnostik signal radio frequency (RF)
data class SignalDiagnostics(
    val isReachable: Boolean = false,
    val provider: String = "Tiada Rangkaian",
    val networkType: String = "N/A",
    val lteBand: String = "", // Bukti band aktif (cth: 3, 7, 8)
    val signalBar: Int = 0,
    val rsrp: String = "-",
    val rsrq: String = "-",
    val rssi: String = "-",
    val sinr: String = "-",
    val cellId: String = "-",
    val wanIp: String = "-"
)

// Model statistik penggunaan data
data class TrafficStats(
    val monthlyTotalBytes: Long = 0L,
    val realtimeRxBytes: Long = 0L,
    val realtimeTxBytes: Long = 0L,
    val connectedTimeSeconds: Long = 0L
) {
    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    fun formatDuration(): String {
        val hours = connectedTimeSeconds / 3600
        val minutes = (connectedTimeSeconds % 3600) / 60
        val seconds = connectedTimeSeconds % 60
        return String.format(Locale.US, "%02dj %02dm %02ds", hours, minutes, seconds)
    }
}

// Model kiraan speedometer masa nyata
data class LiveSpeed(
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L
) {
    fun formatSpeed(bytesPerSec: Long): String {
        val kb = bytesPerSec / 1024.0
        return if (kb >= 1024.0) {
            String.format(Locale.US, "%.2f MB/s", kb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f KB/s", kb)
        }
    }
}

// Model info bateri & suhu
data class BatteryHealth(
    val percent: String = "--%",
    val isCharging: Boolean = false,
    val temperatureCelsius: String = "--°C",
    val voltage: String = "-- V"
)

// Model peranti hotspot
data class ConnectedClient(
    val ipAddress: String,
    val macAddress: String,
    val hostName: String
)

// Model SMS
data class SmsMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: String
)

// Model ujian ping
data class PingResult(
    val target: String,
    val latencyMs: Long,
    val isSuccess: Boolean
)
