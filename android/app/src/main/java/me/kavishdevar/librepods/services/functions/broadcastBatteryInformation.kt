package me.kavishdevar.librepods.services

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import android.util.Log
import me.kavishdevar.librepods.models.BatteryComponent
import me.kavishdevar.librepods.models.BatteryStatus

fun AirPodsService.broadcastBatteryInformation() {

    if (device == null) return

    val batteryList = batteryNotification.getBattery()

    val leftBattery = batteryList.find {
        it.component == BatteryComponent.LEFT
    }

    val rightBattery = batteryList.find {
        it.component == BatteryComponent.RIGHT
    }

    // Unified battery level (minimum of L/R)
    val batteryUnified = minOf(
        leftBattery?.level ?: 100,
        rightBattery?.level ?: 100
    )

    val isLeftCharging =
        leftBattery?.status == BatteryStatus.CHARGING

    val isRightCharging =
        rightBattery?.status == BatteryStatus.CHARGING

    isLeftCharging && isRightCharging

    val arguments = arrayOf<Any>(
        1,
        VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,
        batteryUnified
    )

    val intent =
        Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
            putExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD,
                VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV
            )
            putExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                BluetoothHeadset.AT_CMD_TYPE_SET
            )
            putExtra(
                BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS,
                arguments
            )
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            putExtra(BluetoothDevice.EXTRA_NAME, device?.name)
            addCategory(
                "${BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY}.$APPLE"
            )
        }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sendBroadcastAsUser(
                intent,
                UserHandle.getUserHandleForUid(-1),
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            sendBroadcastAsUser(
                intent,
                UserHandle.getUserHandleForUid(-1)
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send vendor-specific event: ${e.message}")
    }

    val batteryIntent = Intent(ACTION_BATTERY_LEVEL_CHANGED).apply {
        putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        putExtra(EXTRA_BATTERY_LEVEL, batteryUnified)
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sendBroadcast(
                batteryIntent,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            sendBroadcastAsUser(
                batteryIntent,
                UserHandle.getUserHandleForUid(-1)
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send battery level broadcast: ${e.message}")
    }

    val statusIntent = Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).apply {
        setPackage(PACKAGE_ASI)
        putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent)
    }

    try {
        sendBroadcastAsUser(
            statusIntent,
            UserHandle.getUserHandleForUid(-1)
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send ASI battery level broadcast: ${e.message}")
    }

    Log.d(TAG, "Broadcast battery level $batteryUnified% to system")
}