package com.muz.ztemodem

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.muz.ztemodem.data.LiveSpeed
import com.muz.ztemodem.data.ModemConnectionState
import com.muz.ztemodem.network.ZteApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val api = ZteApiClient()
    private var autoRefreshJob: Job? = null
    private var signalHunterJob: Job? = null

    // Pembolehubah Pengiraan Speedometer
    private var lastRxBytes: Long = 0L
    private var lastTxBytes: Long = 0L
    private var lastSpeedCalcTime: Long = 0L

    // Views
    private lateinit var cardLogin: MaterialCardView
    private lateinit var layoutDashboard: View
    private lateinit var etGatewayIp: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnReboot: Button
    private lateinit var tvLogs: TextView

    // Live Badge Views
    private lateinit var layoutLiveStatus: View
    private lateinit var viewLiveDot: View
    private lateinit var tvLiveText: TextView
    private var livePulseAnim: AlphaAnimation? = null

    // Signal Views
    private lateinit var tvTelcoName: TextView
    private lateinit var tvNetworkType: TextView
    private lateinit var pbSignal: ProgressBar
    private lateinit var tvRsrp: TextView
    private lateinit var tvRsrq: TextView
    private lateinit var tvRssi: TextView
    private lateinit var tvSinr: TextView
    private lateinit var tvCellId: TextView
    private lateinit var tvWanIp: TextView
    private lateinit var btnSignalHunter: Button

    // Speedometer Views
    private lateinit var tvDlSpeed: TextView
    private lateinit var tvUlSpeed: TextView

    // Battery & Temp Views
    private lateinit var tvBatteryState: TextView
    private lateinit var tvChargingNote: TextView
    private lateinit var tvModemTemp: TextView

    // Data Switch & Repair
    private lateinit var btnDataOn: Button
    private lateinit var btnDataOff: Button
    private lateinit var btnResetIp: Button
    private lateinit var btnRepairNetwork: Button

    // Traffic Views
    private lateinit var tvMonthlyData: TextView
    private lateinit var tvLiveTraffic: TextView
    private lateinit var tvUpTime: TextView

    // Toolbox Buttons
    private lateinit var btnLockBand: Button
    private lateinit var btnLock4G: Button
    private lateinit var btnLockAuto: Button
    private lateinit var btnClients: Button
    private lateinit var btnSms: Button
    private lateinit var btnUssd: Button
    private lateinit var btnSendSms: Button
    private lateinit var btnWifiSettings: Button
    private lateinit var btnPing: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        loadSavedCredentialsAndAutoLogin()
    }

    private fun initViews() {
        cardLogin = findViewById(R.id.cardLogin)
        layoutDashboard = findViewById(R.id.layoutDashboard)
        etGatewayIp = findViewById(R.id.etGatewayIp)
        etPassword = findViewById(R.id.etPassword)
        cbAutoLogin = findViewById(R.id.cbAutoLogin)
        btnLogin = findViewById(R.id.btnLogin)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnReboot = findViewById(R.id.btnReboot)
        tvLogs = findViewById(R.id.tvLogs)

        layoutLiveStatus = findViewById(R.id.layoutLiveStatus)
        viewLiveDot = findViewById(R.id.viewLiveDot)
        tvLiveText = findViewById(R.id.tvLiveText)

        tvTelcoName = findViewById(R.id.tvTelcoName)
        tvNetworkType = findViewById(R.id.tvNetworkType)
        pbSignal = findViewById(R.id.pbSignal)
        tvRsrp = findViewById(R.id.tvRsrp)
        tvRsrq = findViewById(R.id.tvRsrq)
        tvRssi = findViewById(R.id.tvRssi)
        tvSinr = findViewById(R.id.tvSinr)
        tvCellId = findViewById(R.id.tvCellId)
        tvWanIp = findViewById(R.id.tvWanIp)
        btnSignalHunter = findViewById(R.id.btnSignalHunter)

        tvDlSpeed = findViewById(R.id.tvDlSpeed)
        tvUlSpeed = findViewById(R.id.tvUlSpeed)

        tvBatteryState = findViewById(R.id.tvBatteryState)
        tvChargingNote = findViewById(R.id.tvChargingNote)
        tvModemTemp = findViewById(R.id.tvModemTemp)

        btnDataOn = findViewById(R.id.btnDataOn)
        btnDataOff = findViewById(R.id.btnDataOff)
        btnResetIp = findViewById(R.id.btnResetIp)
        btnRepairNetwork = findViewById(R.id.btnRepairNetwork)

        tvMonthlyData = findViewById(R.id.tvMonthlyData)
        tvLiveTraffic = findViewById(R.id.tvLiveTraffic)
        tvUpTime = findViewById(R.id.tvUpTime)

        btnLockBand = findViewById(R.id.btnLockBand)
        btnLock4G = findViewById(R.id.btnLock4G)
        btnLockAuto = findViewById(R.id.btnLockAuto)
        btnClients = findViewById(R.id.btnClients)
        btnSms = findViewById(R.id.btnSms)
        btnUssd = findViewById(R.id.btnUssd)
        btnSendSms = findViewById(R.id.btnSendSms)
        btnWifiSettings = findViewById(R.id.btnWifiSettings)
        btnPing = findViewById(R.id.btnPing)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val ip = etGatewayIp.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            saveCredentials(ip, pass, cbAutoLogin.isChecked)
            triggerLoginProcess(ip, pass)
        }

        btnRefresh.setOnClickListener {
            refreshAllData(isAuto = false)
        }

        // Tombol 1-Click Network Repair
        btnRepairNetwork.setOnClickListener {
            lifecycleScope.launch {
                log("Memulakan proses 1-Click Network Repair...")
                Toast.makeText(this@MainActivity, "Sedang memulihkan modem, sila tunggu sebentar...", Toast.LENGTH_SHORT).show()
                btnRepairNetwork.isEnabled = false

                val success = api.repairNetworkStack()
                btnRepairNetwork.isEnabled = true
                if (success) {
                    log(">> Rangkaian berjaya dipulihkan ke CelcomDigi!")
                    Toast.makeText(this@MainActivity, "Talian berjaya dipulihkan ke CelcomDigi Auto!", Toast.LENGTH_LONG).show()
                    delay(2000)
                    refreshAllData(isAuto = false)
                } else {
                    log(">> Gagal menghantar arahan pemulihan.")
                    Toast.makeText(this@MainActivity, "Gagal menghubungi modem.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Kunci Band LTE Spesifik
        btnLockBand.setOnClickListener {
            showBandLockDialog()
        }

        // Suis Data ON
        btnDataOn.setOnClickListener {
            lifecycleScope.launch {
                log("Menyambungkan talian data selular...")
                val ok = api.setMobileDataState(true)
                log(if (ok) "Data selular disambungkan." else "Gagal menyambung data.")
                refreshAllData(isAuto = false)
            }
        }

        // Suis Data OFF
        btnDataOff.setOnClickListener {
            lifecycleScope.launch {
                log("Memutuskan data selular...")
                val ok = api.setMobileDataState(false)
                log(if (ok) "Data selular dimatikan." else "Gagal mematikan data.")
                refreshAllData(isAuto = false)
            }
        }

        // Reset WAN IP Pantas
        btnResetIp.setOnClickListener {
            lifecycleScope.launch {
                log("Menjalankan kitaran WAN IP...")
                api.setMobileDataState(false)
                delay(2000)
                api.setMobileDataState(true)
                delay(1500)
                refreshAllData(isAuto = false)
                log("IP baru sedang diperoleh.")
            }
        }

        // Signal Hunter Live
        btnSignalHunter.setOnClickListener {
            if (signalHunterJob?.isActive == true) {
                signalHunterJob?.cancel()
                btnSignalHunter.text = "Mula Signal Hunter (Live)"
                btnSignalHunter.setTextColor(0xFF3FB950.toInt())
                log("Signal Hunter ditamatkan.")
                startAutoRefresh()
            } else {
                stopAutoRefresh()
                btnSignalHunter.text = "Berhenti Signal Hunter"
                btnSignalHunter.setTextColor(0xFFDA3633.toInt())
                log("Signal Hunter aktif! Gerakkan modem untuk cari isyarat terbaik.")
                signalHunterJob = lifecycleScope.launch {
                    while (isActive) {
                        val sig = api.getSignalDiagnostics()
                        tvTelcoName.text = sig.provider
                        tvNetworkType.text = sig.networkType
                        pbSignal.progress = sig.signalBar
                        tvRsrp.text = "RSRP: ${sig.rsrp} dBm"
                        tvRsrq.text = "RSRQ: ${sig.rsrq} dB"
                        tvRssi.text = "RSSI: ${sig.rssi} dBm"
                        tvSinr.text = "SINR: ${sig.sinr} dB"
                        tvCellId.text = "Cell ID: ${sig.cellId}"
                        tvWanIp.text = "WAN: ${sig.wanIp}"
                        delay(2000)
                    }
                }
            }
        }

        // Kunci 4G Only
        btnLock4G.setOnClickListener {
            lifecycleScope.launch {
                log("Mengunci modem ke mod 4G Only...")
                val ok = api.setNetworkMode("Only_LTE")
                log(if (ok) "Berjaya dikunci ke 4G Only." else "Gagal menukar mod.")
                refreshAllData(isAuto = false)
            }
        }

        // Kunci Mod Auto
        btnLockAuto.setOnClickListener {
            lifecycleScope.launch {
                log("Menukar ke mod Auto (4G/3G)...")
                val ok = api.setNetworkMode("NETWORK_auto")
                log(if (ok) "Mod auto diaktifkan." else "Gagal menukar mod.")
                refreshAllData(isAuto = false)
            }
        }

        // Peranti Bersambung
        btnClients.setOnClickListener {
            lifecycleScope.launch {
                val clients = api.getConnectedClients()
                val msg = if (clients.isEmpty()) {
                    "Tiada peranti lain dikesan."
                } else {
                    clients.joinToString("\n\n") { "Nama: ${it.hostName}\nIP: ${it.ipAddress}\nMAC: ${it.macAddress}" }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Peranti Bersambung (${clients.size})")
                    .setMessage(msg)
                    .setPositiveButton("Tutup", null)
                    .show()
            }
        }

        // Peti Masuk SMS
        btnSms.setOnClickListener {
            lifecycleScope.launch {
                log("Memuat turun senarai SMS...")
                val messages = api.getSmsMessages()
                val msg = if (messages.isEmpty()) {
                    "Peti masuk SMS kosong."
                } else {
                    messages.joinToString("\n----------------\n") { "Dari: ${it.sender} (${it.timestamp})\n${it.content}" }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Peti Masuk SMS")
                    .setMessage(msg)
                    .setPositiveButton("Tutup", null)
                    .show()
            }
        }

        // Kirim SMS Keluar
        btnSendSms.setOnClickListener {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 10)
            }
            val etNum = EditText(this).apply { hint = "Nombor (cth: 28882)" }
            val etMsg = EditText(this).apply { hint = "Mesej SMS" }
            layout.addView(etNum)
            layout.addView(etMsg)

            AlertDialog.Builder(this)
                .setTitle("Kirim SMS Telco")
                .setView(layout)
                .setPositiveButton("Kirim") { _, _ ->
                    val num = etNum.text.toString().trim()
                    val msg = etMsg.text.toString().trim()
                    if (num.isNotEmpty() && msg.isNotEmpty()) {
                        lifecycleScope.launch {
                            log("Menghantar SMS ke $num...")
                            val ok = api.sendSms(num, msg)
                            log(if (ok) "SMS berjaya dihantar!" else "Gagal menghantar SMS.")
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Konfigurasi Wi-Fi
        btnWifiSettings.setOnClickListener {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 10)
            }
            val etSsid = EditText(this).apply { hint = "Nama SSID Baru" }
            val etKey = EditText(this).apply { hint = "Kata Laluan Wi-Fi Baru (min 8 aksara)" }
            layout.addView(etSsid)
            layout.addView(etKey)

            AlertDialog.Builder(this)
                .setTitle("Konfigurasi Wi-Fi Modem")
                .setView(layout)
                .setPositiveButton("Simpan") { _, _ ->
                    val ssid = etSsid.text.toString().trim()
                    val key = etKey.text.toString().trim()
                    if (ssid.isNotEmpty() && key.length >= 8) {
                        lifecycleScope.launch {
                            log("Mengemas kini tetapan Wi-Fi...")
                            val ok = api.updateWifiConfig(ssid, key)
                            log(if (ok) "Wi-Fi dikemaskini. Sila sambung semula telefon ke Wi-Fi baru." else "Gagal mengemas kini Wi-Fi.")
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Ujian Ping Latency
        btnPing.setOnClickListener {
            lifecycleScope.launch {
                log("Menguji latency DNS ke 8.8.8.8...")
                val res = api.pingHost("8.8.8.8")
                val msg = if (res.isSuccess) "Latency: ${res.latencyMs} ms" else "Ping Gagal (Tiada Talian)"
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Keputusan Ping (8.8.8.8)")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
                log("Ujian ping: $msg")
            }
        }

        // Dail USSD
        btnUssd.setOnClickListener {
            val input = EditText(this).apply { hint = "*128#" }
            AlertDialog.Builder(this)
                .setTitle("Dail USSD Telco")
                .setMessage("Masukkan kod USSD:")
                .setView(input)
                .setPositiveButton("Hantar") { _, _ ->
                    val code = input.text.toString().trim()
                    if (code.isNotEmpty()) {
                        lifecycleScope.launch {
                            log("Menghantar kod USSD $code...")
                            val reply = api.sendUssd(code)
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Balasan USSD")
                                .setMessage(reply)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Reboot Modem
        btnReboot.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reboot Modem")
                .setMessage("Adakah anda pasti mahu memulakan semula modem ZTE?")
                .setPositiveButton("Reboot") { _, _ ->
                    lifecycleScope.launch {
                        stopAutoRefresh()
                        signalHunterJob?.cancel()
                        log("Menghantar isyarat reboot...")
                        api.reboot()
                        log(">> MODEM SEDANG REBOOT. Wi-Fi akan terputus.")
                        layoutDashboard.visibility = View.GONE
                        cardLogin.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun showBandLockDialog() {
        val bands = arrayOf(
            "Semua Band (Auto Default)",
            "Band 3 (1800 MHz - CelcomDigi Utama)",
            "Band 7 (2600 MHz - Bandar / Laju)",
            "Band 8 (900 MHz - Liputan Jauh)",
            "Band 1 (2100 MHz)"
        )
        val bandCodes = arrayOf("0", "3", "7", "8", "1")

        AlertDialog.Builder(this)
            .setTitle("Kunci Band LTE")
            .setItems(bands) { _, which ->
                val selectedName = bands[which]
                val selectedCode = bandCodes[which]

                lifecycleScope.launch {
                    log("Mengunci modem ke $selectedName...")
                    val ok = api.lockLteBand(selectedCode)

                    if (ok) {
                        Toast.makeText(
                            this@MainActivity,
                            "BUKTI: Berjaya kunci ke $selectedName!",
                            Toast.LENGTH_LONG
                        ).show()
                        log(">> ARAHAN DITERIMA: Modem berjaya dikunci ke Band $selectedCode.")
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Gagal: Menara sekitar tiada frekuensi $selectedName.",
                            Toast.LENGTH_SHORT
                        ).show()
                        log(">> GAGAL: Modem menolak tetapan Band $selectedCode.")
                    }

                    delay(1500)
                    refreshAllData(isAuto = false)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun triggerLoginProcess(ip: String, pass: String) {
        api.gatewayIp = ip
        log("Menghubungi modem di $ip...")

        lifecycleScope.launch {
            btnLogin.isEnabled = false
            val success = api.login(pass)
            btnLogin.isEnabled = true
            if (success) {
                log("Login Berjaya! Membuka Dashboard...")
                cardLogin.visibility = View.GONE
                layoutDashboard.visibility = View.VISIBLE
                refreshAllData(isAuto = false)
                startAutoRefresh()
            } else {
                log("Login Gagal. Sila semak kata laluan atau IP.")
            }
        }
    }

    private fun saveCredentials(ip: String, pass: String, auto: Boolean) {
        val prefs = getSharedPreferences("zte_moder_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("ip", ip)
            .putString("pass", pass)
            .putBoolean("auto_login", auto)
            .apply()
    }

    private fun loadSavedCredentialsAndAutoLogin() {
        val prefs = getSharedPreferences("zte_moder_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "192.168.0.1") ?: "192.168.0.1"
        val savedPass = prefs.getString("pass", "admin") ?: "admin"
        val isAuto = prefs.getBoolean("auto_login", false)

        etGatewayIp.setText(savedIp)
        etPassword.setText(savedPass)
        cbAutoLogin.isChecked = isAuto

        if (isAuto) {
            triggerLoginProcess(savedIp, savedPass)
        }
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = lifecycleScope.launch {
            while (isActive) {
                refreshAllData(isAuto = true)
                delay(3000)
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        runOnUiThread {
            viewLiveDot.clearAnimation()
            layoutLiveStatus.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (::layoutDashboard.isInitialized && layoutDashboard.visibility == View.VISIBLE && signalHunterJob?.isActive != true) {
            startAutoRefresh()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }

    private fun updateSyncStatusUI(state: ModemConnectionState) {
        runOnUiThread {
            layoutLiveStatus.visibility = View.VISIBLE
            when (state) {
                ModemConnectionState.LIVE -> {
                    viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3FB950"))
                    tvLiveText.text = "LIVE"
                    tvLiveText.setTextColor(Color.parseColor("#3FB950"))
                    if (viewLiveDot.animation == null) {
                        livePulseAnim = AlphaAnimation(0.2f, 1.0f).apply {
                            duration = 650
                            repeatMode = Animation.REVERSE
                            repeatCount = Animation.INFINITE
                        }
                        viewLiveDot.startAnimation(livePulseAnim)
                    }
                }
                ModemConnectionState.NO_SERVICE -> {
                    viewLiveDot.clearAnimation()
                    viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D29922"))
                    tvLiveText.text = "NO SERVICE"
                    tvLiveText.setTextColor(Color.parseColor("#D29922"))
                    pbSignal.progress = 0
                    pbSignal.progressTintList = ColorStateList.valueOf(Color.parseColor("#D29922"))
                }
                ModemConnectionState.OFFLINE -> {
                    viewLiveDot.clearAnimation()
                    viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DA3633"))
                    tvLiveText.text = "OFFLINE"
                    tvLiveText.setTextColor(Color.parseColor("#DA3633"))
                    pbSignal.progress = 0
                    pbSignal.progressTintList = ColorStateList.valueOf(Color.parseColor("#DA3633"))
                }
            }
        }
    }

    private fun refreshAllData(isAuto: Boolean = false) {
        lifecycleScope.launch {
            val sig = api.getSignalDiagnostics()

            if (!sig.isReachable) {
                updateSyncStatusUI(ModemConnectionState.OFFLINE)
                tvTelcoName.text = "Modem Terputus"
                tvNetworkType.text = "OFFLINE"
                tvWanIp.text = "WAN: -"
                tvDlSpeed.text = "0.0 KB/s"
                tvUlSpeed.text = "0.0 KB/s"
                if (!isAuto) log("Peringatan: Sambungan ke gateway modem terputus.")
                return@launch
            }

            val hasNoCellular = sig.signalBar == 0 ||
                    sig.provider.contains("Tiada", ignoreCase = true) ||
                    sig.networkType.contains("NONE", ignoreCase = true) ||
                    sig.networkType.contains("NO_SERVICE", ignoreCase = true)

            if (hasNoCellular) {
                updateSyncStatusUI(ModemConnectionState.NO_SERVICE)
                tvTelcoName.text = "Tiada Servis"
                tvNetworkType.text = "NO SERVICE"
            } else {
                updateSyncStatusUI(ModemConnectionState.LIVE)
                tvTelcoName.text = sig.provider
                pbSignal.progressTintList = ColorStateList.valueOf(Color.parseColor("#3FB950"))

                // Tampilkan label LTE beserta nomor band aktif jika tersedia
                if (sig.networkType.equals("LTE", ignoreCase = true) && sig.lteBand.isNotBlank()) {
                    tvNetworkType.text = "LTE (B${sig.lteBand})"
                } else {
                    tvNetworkType.text = sig.networkType
                }
            }

            pbSignal.progress = sig.signalBar
            tvRsrp.text = "RSRP: ${sig.rsrp} dBm"
            tvRsrq.text = "RSRQ: ${sig.rsrq} dB"
            tvRssi.text = "RSSI: ${sig.rssi} dBm"
            tvSinr.text = "SINR: ${sig.sinr} dB"
            tvCellId.text = "Cell ID: ${sig.cellId}"
            tvWanIp.text = "WAN: ${sig.wanIp}"

            // Hitung kecepatan unduh dan unggah seketika
            val traffic = api.getTrafficStats()
            val currentTime = System.currentTimeMillis()
            if (lastSpeedCalcTime > 0L) {
                val timeDeltaSec = (currentTime - lastSpeedCalcTime) / 1000.0
                if (timeDeltaSec > 0.5) {
                    val rxDelta = if (traffic.realtimeRxBytes >= lastRxBytes) traffic.realtimeRxBytes - lastRxBytes else 0L
                    val txDelta = if (traffic.realtimeTxBytes >= lastTxBytes) traffic.realtimeTxBytes - lastTxBytes else 0L

                    val dlSpeedBytes = (rxDelta / timeDeltaSec).toLong()
                    val ulSpeedBytes = (txDelta / timeDeltaSec).toLong()

                    val speed = LiveSpeed()
                    tvDlSpeed.text = speed.formatSpeed(dlSpeedBytes)
                    tvUlSpeed.text = speed.formatSpeed(ulSpeedBytes)
                }
            }
            lastRxBytes = traffic.realtimeRxBytes
            lastTxBytes = traffic.realtimeTxBytes
            lastSpeedCalcTime = currentTime

            tvMonthlyData.text = traffic.formatBytes(traffic.monthlyTotalBytes)
            tvLiveTraffic.text = "Sesi ini: DL ${traffic.formatBytes(traffic.realtimeRxBytes)} | UL ${traffic.formatBytes(traffic.realtimeTxBytes)}"
            tvUpTime.text = "Masa: ${traffic.formatDuration()}"

            // Tarik suhu dan persentase baterai
            val battery = api.getBatteryHealth()
            tvBatteryState.text = "Bateri: ${battery.percent}"
            tvChargingNote.text = if (battery.isCharging) "Sedang Dicas (Charging)" else "Guna Bateri"
            tvModemTemp.text = "Suhu: ${battery.temperatureCelsius}"

            if (!isAuto) {
                log("Data modem berjaya dikemaskini.")
            }
        }
    }

    private fun log(text: String) {
        runOnUiThread { tvLogs.append("\n$text") }
    }
}
