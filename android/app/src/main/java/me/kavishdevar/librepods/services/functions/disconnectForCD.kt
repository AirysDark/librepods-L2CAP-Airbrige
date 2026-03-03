package me.kavishdevar.librepods.services

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.util.Log
import me.kavishdevar.librepods.constants.IslandType
import me.kavishdevar.librepods.constants.BatteryComponent
import me.kavishdevar.librepods.utils.MediaController

fun AirPodsService.disconnectForCD() {

    if (!this::socket.isInitialized) return

    try {
        if (socket.isConnected) {
            socket.close()
        }
    } catch (e: Exception) {
        Log.e("AirPodsService", "Error closing socket: ${e.message}", e)
    }

    MediaController.pausedWhileTakingOver = false

    val batteryList = batteryNotification.getBattery()

    val unifiedBattery = minOf(
        batteryList.find { it.component == BatteryComponent.LEFT }?.level ?: 100,
        batteryList.find { it.component == BatteryComponent.RIGHT }?.level ?: 100
    )

    Log.d("AirPodsService", "Disconnected from AirPods, showing island.")

    showIsland(
        this,
        unifiedBattery,
        IslandType.MOVED_TO_REMOTE
    )

    val bluetoothAdapter =
        getSystemService(BluetoothManager::class.java)?.adapter

    bluetoothAdapter?.getProfileProxy(
        this,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                try {
                    if (profile == BluetoothProfile.A2DP) {

                        val a2dp = proxy as? BluetoothA2dp
                        val connectedDevices = a2dp?.connectedDevices ?: emptyList()

                        if (connectedDevices.isNotEmpty()) {
                            MediaController.sendPause()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AirPodsService", "A2DP pause check failed: ${e.message}", e)
                } finally {
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.A2DP
    )

    isConnectedLocally = false
}