/* ==========================
   Binder
   ========================== */

inner class LocalBinder : Binder() {
    fun getService(): AirPodsService = this@AirPodsService
}

/* ==========================
   Logging
   ========================== */

private lateinit var sharedPreferencesLogs: SharedPreferences
private lateinit var sharedPreferences: SharedPreferences

private val packetLogKey = "packet_log"

private val _packetLogsFlow =
    MutableStateFlow<Set<String>>(emptySet())

val packetLogsFlow: StateFlow<Set<String>>
    get() = _packetLogsFlow

private val maxLogEntries = 1000

private val inMemoryLogs =
    Collections.synchronizedSet(mutableSetOf<String>())

/* ==========================
   Telephony
   ========================== */

private lateinit var telephonyManager: TelephonyManager
private lateinit var phoneStateListener: PhoneStateListener

private var handleIncomingCallOnceConnected = false

/* ==========================
   BLE
   ========================== */

lateinit var bleManager: BLEManager

/* ==========================
   BLE Status Listener
   ========================== */

private val bleStatusListener =
    object : BLEManager.AirPodsStatusListener {

        override fun onConnected() {
            Log.d(TAG, "BLE connected")
        }

        override fun onDisconnected() {
            Log.d(TAG, "BLE disconnected")
        }
    }