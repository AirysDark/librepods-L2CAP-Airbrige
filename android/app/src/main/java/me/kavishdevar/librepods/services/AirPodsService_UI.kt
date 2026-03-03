package me.kavishdevar.librepods.services

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import me.kavishdevar.librepods.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.constants.Battery
import me.kavishdevar.librepods.constants.IslandType
import me.kavishdevar.librepods.utils.IslandWindow
import me.kavishdevar.librepods.utils.PopupWindow

class AirPodsService : Service() {

    /* ------------------------------------------------------- */
    /* ---------------- POPUP ------------------------------- */
    /* ------------------------------------------------------- */

    fun showPopup(service: Service, name: String) {
        if (!Settings.canDrawOverlays(service)) {
            Log.d(TAG, "No permission for SYSTEM_ALERT_WINDOW")
            return
        }

        if (popupShown) return

        val popupWindow = PopupWindow(service.applicationContext)
        popupWindow.open(name, batteryNotification)
        popupShown = true
    }

    /* ------------------------------------------------------- */
    /* ---------------- ISLAND ------------------------------ */
    /* ------------------------------------------------------- */

    fun showIsland(
        service: Service,
        batteryPercentage: Int,
        type: IslandType = IslandType.CONNECTED,
        reversed: Boolean = false,
        otherDeviceName: String? = null
    ) {

        if (!Settings.canDrawOverlays(service)) {
            Log.d(TAG, "No permission for SYSTEM_ALERT_WINDOW")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            islandWindow = IslandWindow(service.applicationContext)
            islandWindow?.show(
                sharedPreferences.getString("name", "AirPods") ?: "AirPods",
                batteryPercentage,
                this@AirPodsService,
                type,
                reversed,
                otherDeviceName
            )
        }
    }

    /* ------------------------------------------------------- */
    /* ---------------- MAIN ACTIVITY ----------------------- */
    /* ------------------------------------------------------- */

    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /* ------------------------------------------------------- */
    /* ---------------- FOREGROUND NOTIFICATION ------------- */
    /* ------------------------------------------------------- */

    fun startForegroundNotification() {

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        val backgroundChannel = NotificationChannel(
            "background_service_status",
            "Background Service Status",
            NotificationManager.IMPORTANCE_LOW
        )

        val socketFailureChannel = NotificationChannel(
            "socket_connection_failure",
            "AirPods Socket Connection Issues",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Problems connecting to AirPods protocol"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(backgroundChannel)
        notificationManager.createNotificationChannel(socketFailureChannel)

        val notification = NotificationCompat.Builder(
            this,
            "background_service_status"
        )
            .setSmallIcon(R.drawable.airpods)
            .setContentTitle("LibrePods Running")
            .setContentText("Background service active")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    /* ------------------------------------------------------- */
    /* ---------------- FIXED FUNCTION ---------------------- */
    /* ------------------------------------------------------- */

    fun updateNotificationContent(
        connected: Boolean,
        name: String? = null,
        batteryList: List<Battery>? = null
    ) {

        val channelId =
            if (connected) "airpods_connection_status"
            else "background_service_status"

        val title =
            if (connected) "$name Connected"
            else "AirPods Disconnected"

        val contentText =
            if (connected && batteryList != null && batteryList.isNotEmpty()) {
                val left = batteryList.firstOrNull()?.level ?: 0
                "Battery: $left%"
            } else {
                "Waiting for connection"
            }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.airpods)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(connected)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    /* ------------------------------------------------------- */
    /* ---------------- TOAST ------------------------------- */
    /* ------------------------------------------------------- */

    fun sendToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}