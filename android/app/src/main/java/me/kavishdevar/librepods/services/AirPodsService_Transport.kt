package me.kavishdevar.librepods.services

import android.app.*
import android.bluetooth.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.constants.*
import me.kavishdevar.librepods.utils.*
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AirPodsService : Service() {

    /* ------------------------------------------------------- */
    /* ---------------- SAFE BATTERY HELPER ------------------ */
    /* ------------------------------------------------------- */

    private fun safeBatteryLevel(): Int {
        val left = batteryNotification.getBattery()
            .find { it.component == BatteryComponent.LEFT }?.level ?: 0
        val right = batteryNotification.getBattery()
            .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
        return left.coerceAtMost(right)
    }

    /* ------------------------------------------------------- */
    /* ---------------- A2DP RECEIVER ------------------------ */
    /* ------------------------------------------------------- */

    private fun registerA2dpConnectionReceiver() {

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                if (intent.action != BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) return

                val state = intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED
                )

                val deviceExtra =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (state == BluetoothProfile.STATE_CONNECTED &&
                    deviceExtra?.address == device?.address
                ) {
                    MediaController.sendPlay()
                    MediaController.iPausedTheMedia = false
                    context.unregisterReceiver(this)
                }
            }
        }

        registerReceiver(
            receiver,
            IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        )
    }

    /* ------------------------------------------------------- */
    /* ---------------- SOCKET CREATION ---------------------- */
    /* ------------------------------------------------------- */

    private fun createBluetoothSocket(
        device: BluetoothDevice,
        uuid: ParcelUuid
    ): BluetoothSocket {

        HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/BluetoothSocket;")

        return try {
            HiddenApiBypass.newInstance(
                BluetoothSocket::class.java,
                device,
                3,
                true,
                true,
                0x1001,
                uuid
            ) as BluetoothSocket
        } catch (e: Exception) {
            showSocketConnectionFailureNotification(e.localizedMessage ?: "Unknown error")
            throw e
        }
    }

    /* ------------------------------------------------------- */
    /* ---------------- CONNECT TO SOCKET -------------------- */
    /* ------------------------------------------------------- */

    fun connectToSocket(device: BluetoothDevice, manual: Boolean = false) {

        if (isConnectedLocally) return

        val uuid =
            ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        try {
            socket = createBluetoothSocket(device, uuid)

            runBlocking {
                withTimeout(5000) {
                    socket.connect()
                }
            }

            isConnectedLocally = true
            this.device = device

            BluetoothConnectionManager.setCurrentConnection(socket, device)

            attManager = ATTManager(device)
            attManager?.connect()

            updateNotificationContent(
                true,
                config.deviceName,
                batteryNotification.getBattery()
            )

            startSocketListener()

        } catch (e: Exception) {

            isConnectedLocally = false

            if (manual)
                sendToast("Socket connection failed: ${e.localizedMessage}")
            else
                showSocketConnectionFailureNotification(e.localizedMessage ?: "Timeout")
        }
    }

    /* ------------------------------------------------------- */
    /* ---------------- SOCKET LISTENER ---------------------- */
    /* ------------------------------------------------------- */

    private fun startSocketListener() {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                while (socket.isConnected) {

                    val buffer = ByteArray(1024)
                    val read = socket.inputStream.read(buffer)

                    if (read <= 0) break

                    val data = buffer.copyOfRange(0, read)

                    aacpManager.receivePacket(data)

                    if (!isHeadTrackingData(data)) {
                        logPacket(data, "AirPods")
                    }
                }

            } catch (_: Exception) {
            }

            disconnectAirPods()
        }
    }

    /* ------------------------------------------------------- */
    /* ---------------- TAKEOVER (SAFE) ---------------------- */
    /* ------------------------------------------------------- */

    fun takeOver(reason: String) {

        if (bleManager.getMostRecentStatus()?.isLeftInEar == false &&
            bleManager.getMostRecentStatus()?.isRightInEar == false
        ) return

        MediaController.sendPause(true)

        device = getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices
            .find { it.address == macAddress }

        device?.let {
            connectToSocket(it)
            connectAudio(this, it)
        }

        showIsland(this, safeBatteryLevel(), IslandType.TAKING_OVER)
    }

    /* ------------------------------------------------------- */
    /* ---------------- DISCONNECT --------------------------- */
    /* ------------------------------------------------------- */

    fun disconnectAirPods() {

        if (::socket.isInitialized && socket.isConnected) {
            try { socket.close() } catch (_: Exception) {}
        }

        isConnectedLocally = false
        aacpManager.disconnected()
        attManager?.disconnect()

        updateNotificationContent(false)

        sendBroadcast(Intent(AirPodsNotifications.AIRPODS_DISCONNECTED))
    }

    /* ------------------------------------------------------- */
    /* ---------------- AUDIO CONNECT/DISCONNECT ------------ */
    /* ------------------------------------------------------- */

    fun connectAudio(context: Context, device: BluetoothDevice?) {

        if (device == null) return

        val adapter =
            context.getSystemService(BluetoothManager::class.java).adapter

        adapter.getProfileProxy(context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {

                    try {
                        val method = proxy.javaClass.getMethod(
                            "setConnectionPolicy",
                            BluetoothDevice::class.java,
                            Int::class.java
                        )
                        method.invoke(proxy, device, 100)
                    } catch (_: Exception) {
                    } finally {
                        adapter.closeProfileProxy(profile, proxy)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP
        )
    }

    fun disconnectAudio(context: Context, device: BluetoothDevice?) {

        if (device == null) return

        val adapter =
            context.getSystemService(BluetoothManager::class.java).adapter

        adapter.getProfileProxy(context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {

                    try {
                        val method = proxy.javaClass.getMethod(
                            "setConnectionPolicy",
                            BluetoothDevice::class.java,
                            Int::class.java
                        )
                        method.invoke(proxy, device, 0)
                    } catch (_: Exception) {
                    } finally {
                        adapter.closeProfileProxy(profile, proxy)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP
        )
    }
}