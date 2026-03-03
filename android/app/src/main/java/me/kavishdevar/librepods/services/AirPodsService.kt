package me.kavishdevar.librepods.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.constants.AirPodsNotifications
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.AirPodsInstance
import me.kavishdevar.librepods.utils.ATTManager
import kotlin.io.encoding.ExperimentalEncodingApi

class AirPodsService : Service(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    /* ==========================
       Transport / Bluetooth State
       ========================== */

    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var device: BluetoothDevice
    lateinit var socket: BluetoothSocket

    var macAddress: String = ""
    var localMac: String = ""

    var attManager: ATTManager? = null
    lateinit var aacpManager: AACPManager
    lateinit var airpodsInstance: AirPodsInstance

    var cameraActive = false
    var disconnectedBecauseReversed = false
    var otherDeviceTookOver = false
    var airpodsConnectedRemotely = false

    private val TAG = "AirPodsService"

    /* ==========================
       Shared State
       ========================== */

    val controlCommandStatusList =
        MutableStateFlow<Map<Int, ByteArray>>(emptyMap())

    val packetLogsFlow =
        MutableStateFlow<List<String>>(emptyList())

    var isConnectedLocally = false

    /* ==========================
       Notifications (ORIGINAL)
       ========================== */

    val earDetectionNotification = AirPodsNotifications.EarDetection()
    val ancNotification = AirPodsNotifications.ANC()
    val batteryNotification = AirPodsNotifications.BatteryNotification()
    val conversationAwarenessNotification =
        AirPodsNotifications.ConversationalAwarenessNotification()

    /* ==========================
       Binder
       ========================== */

    inner class LocalBinder : Binder() {
        fun getService(): AirPodsService = this@AirPodsService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    /* ==========================
       Lifecycle
       ========================== */

    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate() {
        super.onCreate()
        ServiceManager.setService(this)
        initializeCore()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.setService(null)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        // handled in split modules
    }

    /* ============================================================
       Broadcast Helpers
       ============================================================ */

    fun sendANCBroadcast() {
        sendBroadcast(
            Intent(AirPodsNotifications.ANC_DATA).apply {
                putExtra("data", ancNotification.status)
            }
        )
    }

    fun sendBatteryBroadcast() {
        sendBroadcast(
            Intent(AirPodsNotifications.BATTERY_DATA).apply {
                putParcelableArrayListExtra(
                    "data",
                    ArrayList(batteryNotification.getBattery())
                )
            }
        )
    }

    fun sendEarDetectionBroadcast() {
        sendBroadcast(
            Intent(AirPodsNotifications.EAR_DETECTION_DATA).apply {
                putExtra("data", earDetectionNotification.status)
            }
        )
    }

    fun sendConversationAwarenessBroadcast() {
        sendBroadcast(
            Intent(AirPodsNotifications.CA_DATA).apply {
                putExtra("data", conversationAwarenessNotification.status)
            }
        )
    }

    /* ============================================================
       Foreground Service Notification
       ============================================================ */

    fun startForegroundNotification() {

        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val disconnectedChannel = NotificationChannel(
                "background_service_status",
                "Background Service Status",
                NotificationManager.IMPORTANCE_LOW
            )

            val connectedChannel = NotificationChannel(
                "airpods_connection_status",
                "AirPods Connection Status",
                NotificationManager.IMPORTANCE_LOW
            )

            val socketFailureChannel = NotificationChannel(
                "socket_connection_failure",
                "Socket Connection Failure",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Notifications about problems connecting to AirPods protocol"
            }

            manager.createNotificationChannel(disconnectedChannel)
            manager.createNotificationChannel(connectedChannel)
            manager.createNotificationChannel(socketFailureChannel)
        }

        val notification = NotificationCompat.Builder(
            this,
            "background_service_status"
        )
            .setContentTitle("LibrePods Running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    fun updateNotificationContent(
        connected: Boolean,
        name: String? = null
    ) {
        val manager = getSystemService(NotificationManager::class.java)

        val builder = NotificationCompat.Builder(
            this,
            "airpods_connection_status"
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (connected) {
            builder.setContentTitle("Connected to $name")
        } else {
            builder.setContentTitle("AirPods Disconnected")
        }

        manager.notify(2, builder.build())
    }

    fun showSocketConnectionFailureNotification(
        errorMessage: String
    ) {
        val manager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(
            this,
            "socket_connection_failure"
        )
            .setContentTitle("Socket Connection Failure")
            .setContentText(errorMessage)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(errorMessage)
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(Notification.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(3, notification)
    }

    fun sendBatteryNotification() {
        updateNotificationContent(true)
        sendBatteryBroadcast()
    }
}

/* ==========================
   Global Service Facade
   ========================== */

object ServiceManager {

    private var service: AirPodsService? = null

    @Synchronized
    fun getService(): AirPodsService? = service

    @Synchronized
    fun setService(service: AirPodsService?) {
        this.service = service
    }

    val airpodsInstance: AirPodsInstance?
        get() = service?.airpodsInstance

    val aacpManager: AACPManager?
        get() = service?.aacpManager

    val controlCommandStatusList
        get() = service?.controlCommandStatusList

    val packetLogsFlow
        get() = service?.packetLogsFlow

    val isConnectedLocally: Boolean
        get() = service?.isConnectedLocally ?: false

    val device: BluetoothDevice?
        get() = service?.device
}