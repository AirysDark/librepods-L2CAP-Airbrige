package me.kavishdevar.librepods.services

import android.Manifest
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.ComponentName
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.UserHandle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.app.PendingIntent
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.constants.AirPodsNotifications
import me.kavishdevar.librepods.constants.Battery
import me.kavishdevar.librepods.constants.BatteryComponent
import me.kavishdevar.librepods.constants.BatteryStatus
import me.kavishdevar.librepods.utils.SystemApisUtils
import me.kavishdevar.librepods.widgets.BatteryWidget

/* ------------------------------------------------------- */
/* ---------------- BATTERY EXTENSIONS ------------------- */
/* ------------------------------------------------------- */

override fun AirPodsService.onBatteryChanged(device: BLEManager.AirPodsStatus) {
    if (isConnectedLocally) return

    val status = bleManager.getMostRecentStatus()

    val leftLevel = status?.leftBattery ?: 0
    val rightLevel = status?.rightBattery ?: 0
    val caseLevel = status?.caseBattery ?: 0

    val leftCharging = status?.isLeftCharging == true
    val rightCharging = status?.isRightCharging == true
    val caseCharging = status?.isCaseCharging == true

    batteryNotification.setBatteryDirect(
        leftLevel = leftLevel,
        leftCharging = leftCharging,
        rightLevel = rightLevel,
        rightCharging = rightCharging,
        caseLevel = caseLevel,
        caseCharging = caseCharging
    )

    updateBattery()
    Log.d(TAG, "Battery changed")
}

fun AirPodsService.sendBatteryBroadcast() {
    sendBroadcast(
        Intent(AirPodsNotifications.BATTERY_DATA).apply {
            putParcelableArrayListExtra(
                "data",
                ArrayList(batteryNotification.getBattery())
            )
        }
    )
}

fun AirPodsService.sendBatteryNotification() {
    updateNotificationContent(
        true,
        getSharedPreferences("settings", MODE_PRIVATE)
            .getString("name", device?.name),
        batteryNotification.getBattery()
    )
}

fun AirPodsService.setBatteryMetadata() {
    device?.let { bluetoothDevice ->
        val batteryList = batteryNotification.getBattery()

        fun level(component: BatteryComponent) =
            batteryList.find { it.component == component }?.level ?: 0

        fun charging(component: BatteryComponent) =
            batteryList.find { it.component == component }?.status == BatteryStatus.CHARGING

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY,
            level(BatteryComponent.CASE).toString().toByteArray()
        )

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING,
            if (charging(BatteryComponent.CASE)) "1".toByteArray() else "0".toByteArray()
        )

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY,
            level(BatteryComponent.LEFT).toString().toByteArray()
        )

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING,
            if (charging(BatteryComponent.LEFT)) "1".toByteArray() else "0".toByteArray()
        )

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY,
            level(BatteryComponent.RIGHT).toString().toByteArray()
        )

        SystemApisUtils.setMetadata(
            bluetoothDevice,
            bluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING,
            if (charging(BatteryComponent.RIGHT)) "1".toByteArray() else "0".toByteArray()
        )
    }
}

fun AirPodsService.updateBatteryWidget() {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val componentName = ComponentName(this, BatteryWidget::class.java)
    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

    val remoteViews = RemoteViews(packageName, R.layout.battery_widget).also { views ->

        val openActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.battery_widget, openActivityIntent)

        val batteryList = batteryNotification.getBattery()

        fun update(component: BatteryComponent, textId: Int, progressId: Int, chargingId: Int) {
            val battery = batteryList.find { it.component == component }
            views.setTextViewText(textId, battery?.let { "${it.level}%" } ?: "")
            views.setProgressBar(progressId, 100, battery?.level ?: 0, false)
            views.setViewVisibility(
                chargingId,
                if (battery?.status == BatteryStatus.CHARGING) View.VISIBLE else View.GONE
            )
        }

        update(
            BatteryComponent.LEFT,
            R.id.left_battery_widget,
            R.id.left_battery_progress,
            R.id.left_charging_icon
        )

        update(
            BatteryComponent.RIGHT,
            R.id.right_battery_widget,
            R.id.right_battery_progress,
            R.id.right_charging_icon
        )

        update(
            BatteryComponent.CASE,
            R.id.case_battery_widget,
            R.id.case_battery_progress,
            R.id.case_charging_icon
        )

        if (widgetMobileBatteryEnabled) {
            val batteryManager = getSystemService(BatteryManager::class.java)
            val batteryLevel =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            views.setTextViewText(R.id.phone_battery_widget, "$batteryLevel%")
            views.setProgressBar(
                R.id.phone_battery_progress,
                100,
                batteryLevel,
                false
            )
        }
    }

    appWidgetManager.updateAppWidget(widgetIds, remoteViews)
}

fun AirPodsService.updateBattery() {
    setBatteryMetadata()
    updateBatteryWidget()
    sendBatteryBroadcast()
    sendBatteryNotification()
}

fun AirPodsService.getBattery(): List<Battery> {
    return batteryNotification.getBattery()
}