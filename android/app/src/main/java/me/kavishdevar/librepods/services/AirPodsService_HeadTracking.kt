package me.kavishdevar.librepods.services

import android.os.Build
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.GestureDetector
import me.kavishdevar.librepods.utils.HeadTracking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

/* ------------------------------------------------------- */
/* ---------------- GESTURE INITIALIZATION --------------- */
/* ------------------------------------------------------- */

private fun AirPodsService.initGestureDetector() {
    if (gestureDetector == null) {
        gestureDetector = GestureDetector(this)
    }
}

/* ------------------------------------------------------- */
/* ---------------- TEST HEAD GESTURES ------------------- */
/* ------------------------------------------------------- */

suspend fun AirPodsService.testHeadGestures(): Boolean {
    return suspendCancellableCoroutine { continuation ->

        gestureDetector?.startDetection(doNotStop = true) { accepted ->

            if (continuation.isActive) {
                continuation.resume(accepted)
            }

            gestureDetector?.stopDetection()
        }
    }
}

/* ------------------------------------------------------- */
/* ---------------- PROCESS HEAD TRACKING ---------------- */
/* ------------------------------------------------------- */

fun AirPodsService.processHeadTrackingData(data: ByteArray) {

    if (data.size < 55) return

    val horizontal = ByteBuffer
        .wrap(data, 51, 2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .short
        .toInt()

    val vertical = ByteBuffer
        .wrap(data, 53, 2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .short
        .toInt()

    gestureDetector?.processHeadOrientation(horizontal, vertical)
}

/* ------------------------------------------------------- */
/* ---------------- START HEAD TRACKING ------------------ */
/* ------------------------------------------------------- */

fun AirPodsService.startHeadTracking() {

    isHeadTrackingActive = true

    val useAlternatePackets =
        sharedPreferences.getBoolean("use_alternate_head_tracking_packets", false)

    val ownsConnection =
        aacpManager
            .getControlCommandStatus(
                AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION
            )
            ?.value
            ?.getOrNull(0)
            ?.toInt() == 1

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !ownsConnection) {
        takeOver("call", startHeadTrackingAgain = true)
        Log.d(TAG, "Taking over for head tracking")
    } else {
        Log.w(TAG, "Not taking over for head tracking")
    }

    if (useAlternatePackets) {
        aacpManager.sendDataPacket(
            aacpManager.createAlternateStartHeadTrackingPacket()
        )
    } else {
        aacpManager.sendStartHeadTracking()
    }

    HeadTracking.reset()
}

/* ------------------------------------------------------- */
/* ---------------- STOP HEAD TRACKING ------------------- */
/* ------------------------------------------------------- */

fun AirPodsService.stopHeadTracking() {

    val useAlternatePackets =
        sharedPreferences.getBoolean("use_alternate_head_tracking_packets", false)

    if (useAlternatePackets) {
        aacpManager.sendDataPacket(
            aacpManager.createAlternateStopHeadTrackingPacket()
        )
    } else {
        aacpManager.sendStopHeadTracking()
    }

    isHeadTrackingActive = false
}