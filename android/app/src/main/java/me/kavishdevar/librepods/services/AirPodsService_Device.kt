package me.kavishdevar.librepods.services

import android.util.Log
import androidx.core.content.edit
import me.kavishdevar.librepods.constants.BatteryComponent
import me.kavishdevar.librepods.utils.BLEManager
import me.kavishdevar.librepods.utils.MediaController

/* ------------------------------------------------------- */
/* ---------------- BLE CALLBACKS ------------------------ */
/* ------------------------------------------------------- */

override fun onDeviceStatusChanged(device: BLEManager.AirPodsStatus) {
    Log.d(TAG, "Device status changed")
}

override fun onLidStateChanged(lidOpen: Boolean) {
    Log.d(TAG, "Lid state changed: $lidOpen")
}

override fun onEarStateChanged(device: BLEManager.AirPodsStatus) {
    Log.d(TAG, "Ear state changed")
}

/* ------------------------------------------------------- */
/* ---------------- DEVICE DISAPPEARED ------------------- */
/* ------------------------------------------------------- */

override fun onDeviceDisappeared() {
    Log.d(TAG, "All disappeared")
    updateNotificationContent(false)
}

/* ------------------------------------------------------- */
/* ---------------- EAR DETECTION LOGIC ------------------ */
/* ------------------------------------------------------- */

private fun processEarDetectionChange(earDetection: ByteArray) {

    val previousState = listOf(
        earDetectionNotification.status[0] == 0x00.toByte(),
        earDetectionNotification.status[1] == 0x00.toByte()
    )

    earDetectionNotification.setStatus(earDetection)

    if (!config.earDetectionEnabled) return

    val data = earDetection.copyOfRange(earDetection.size - 2, earDetection.size)

    val newState = listOf(
        data[0] == 0x00.toByte(),
        data[1] == 0x00.toByte()
    )

    val inEar = newState[0] && newState[1]

    Log.d("AirPodsParser", "Old: ${previousState.sorted()} New: ${newState.sorted()}")

    if (previousState.sorted() == listOf(false, false) &&
        newState.sorted() != listOf(false, false)
    ) {
        showIsland(
            this,
            (batteryNotification.getBattery()
                .find { it.component == BatteryComponent.LEFT }?.level ?: 0)
                .coerceAtMost(
                    batteryNotification.getBattery()
                        .find { it.component == BatteryComponent.RIGHT }?.level ?: 0
                )
        )
    }

    if (newState == listOf(false, false)) {
        MediaController.sendPause(force = true)
        if (config.disconnectWhenNotWearing) {
            disconnectAudio(this, device)
        }
    } else {
        connectAudio(this, device)
        MediaController.sendPlay()
    }
}

/* ------------------------------------------------------- */
/* ---------------- LOG MANAGEMENT ----------------------- */
/* ------------------------------------------------------- */

private fun clearPacketLogs() {
    synchronized(inMemoryLogs) {
        inMemoryLogs.clear()
        _packetLogsFlow.value = emptySet()
    }
    sharedPreferencesLogs.edit { remove(packetLogKey) }
}

fun clearLogs() {
    clearPacketLogs()
}

/* ------------------------------------------------------- */
/* ---------------- SETTINGS ----------------------------- */
/* ------------------------------------------------------- */

fun setEarDetection(enabled: Boolean) {
    if (config.earDetectionEnabled != enabled) {
        config.earDetectionEnabled = enabled
        sharedPreferences.edit {
            putBoolean("automatic_ear_detection", enabled)
        }
    }
}

fun setName(name: String) {
    aacpManager.sendRename(name)

    if (config.deviceName != name) {
        config.deviceName = name
        sharedPreferences.edit { putString("name", name) }
    }

    updateNotificationContent(true, name, batteryNotification.getBattery())
    Log.d(TAG, "setName: $name")
}