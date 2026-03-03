package me.kavishdevar.librepods.services

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.AirPodsInstance
import kotlin.io.encoding.ExperimentalEncodingApi

class AirPodsService : Service(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    /* ==========================
       Shared State (USED BY ALL MODULE FILES)
       ========================== */

    lateinit var aacpManager: AACPManager
    lateinit var airpodsInstance: AirPodsInstance

    // Control command state tracking
    val controlCommandStatusList =
        MutableStateFlow<Map<Int, ByteArray>>(emptyMap())

    // Packet logs
    val packetLogsFlow =
        MutableStateFlow<List<String>>(emptyList())

    // Connection state
    var isConnectedLocally: Boolean = false

    // Notifications (used in UI modules)
    var batteryNotification: Any? = null
    var ancNotification: Any? = null
    var earDetectionNotification: Any? = null
    var conversationAwarenessNotification: Any? = null

    /* ==========================
       Binder
       ========================== */

    inner class LocalBinder : Binder() {
        fun getService(): AirPodsService = this@AirPodsService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    /* ==========================
       Lifecycle
       ========================== */

    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate() {
        super.onCreate()

        // Register this service instance globally
        ServiceManager.setService(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear global reference
        ServiceManager.setService(null)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        // Handled in modules
    }
}

object ServiceManager {

    @OptIn(ExperimentalEncodingApi::class)
    private var service: AirPodsService? = null

    @OptIn(ExperimentalEncodingApi::class)
    @Synchronized
    fun getService(): AirPodsService? {
        return service
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Synchronized
    fun setService(service: AirPodsService?) {
        this.service = service
    }
}