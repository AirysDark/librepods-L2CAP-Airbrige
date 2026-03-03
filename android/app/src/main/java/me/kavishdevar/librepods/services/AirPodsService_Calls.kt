package me.kavishdevar.librepods.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.edit
import me.kavishdevar.librepods.constants.*
import me.kavishdevar.librepods.utils.*
import kotlin.io.encoding.Base64

/* ------------------------------------------------------- */
/* ---------------- AACP CALLBACK SETUP ------------------ */
/* ------------------------------------------------------- */

fun AirPodsService.initializeAACPManagerCallback() {

    aacpManager.setPacketCallback(object : AACPManager.PacketCallback {

        @SuppressLint("MissingPermission")
        override fun onBatteryInfoReceived(batteryInfo: ByteArray) {
            batteryNotification.setBattery(batteryInfo)

            sendBatteryBroadcast()
            updateBattery()

            updateNotificationContent(
                true,
                getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("name", device?.name),
                batteryNotification.getBattery()
            )

            batteryNotification.getBattery().forEach {
                Log.d(
                    "AirPodsParser",
                    "${it.getComponentName()}: ${it.getStatusName()} at ${it.level}%"
                )
            }

            if (batteryNotification.getBattery()[0].status == BatteryStatus.CHARGING &&
                batteryNotification.getBattery()[1].status == BatteryStatus.CHARGING
            ) {
                disconnectAudio(this@AirPodsService, device)
            } else {
                connectAudio(this@AirPodsService, device)
            }
        }

        override fun onEarDetectionReceived(earDetection: ByteArray) {
            sendBroadcast(Intent(AirPodsNotifications.EAR_DETECTION_DATA).apply {
                val list = earDetectionNotification.status
                putExtra("data", byteArrayOf(list[0], list[1]))
            })
            processEarDetectionChange(earDetection)
        }

        override fun onConversationAwarenessReceived(data: ByteArray) {
            conversationAwarenessNotification.setData(data)

            sendBroadcast(Intent(AirPodsNotifications.CA_DATA).apply {
                putExtra("data", conversationAwarenessNotification.status)
            })

            when (conversationAwarenessNotification.status) {
                1.toByte(), 2.toByte() -> MediaController.startSpeaking()
                8.toByte(), 9.toByte() -> MediaController.stopSpeaking()
            }
        }

        override fun onControlCommandReceived(controlCommand: ByteArray) {
            val command = AACPManager.ControlCommand.fromByteArray(controlCommand)

            if (command.identifier ==
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value
            ) {
                ancNotification.setStatus(
                    byteArrayOf(command.value.firstOrNull() ?: 0x00)
                )
                sendANCBroadcast()
                updateNoiseControlWidget()
            }
        }

        override fun onOwnershipChangeReceived(owns: Boolean) {
            if (!owns) {
                MediaController.recentlyLostOwnership = true
                Handler(Looper.getMainLooper()).postDelayed({
                    MediaController.recentlyLostOwnership = false
                }, 3000)

                MediaController.sendPause()
                MediaController.pausedForOtherDevice = true
                otherDeviceTookOver = true

                disconnectAudio(this@AirPodsService, device)
            }
        }

        override fun onDeviceInformationReceived(info: AACPManager.Companion.AirPodsInformation) {

            sharedPreferences.edit {
                putString("airpods_name", info.name)
                putString("airpods_model_number", info.modelNumber)
                putString("airpods_manufacturer", info.manufacturer)
                putString("airpods_serial_number", info.serialNumber)
            }

            config.airpodsName = info.name
            config.airpodsModelNumber = info.modelNumber
            config.airpodsManufacturer = info.manufacturer
            config.airpodsSerialNumber = info.serialNumber

            val model = AirPodsModels.getModelByModelNumber(config.airpodsModelNumber)

            model?.let {
                airpodsInstance = AirPodsInstance(
                    name = config.airpodsName,
                    model = it,
                    actualModelNumber = config.airpodsModelNumber,
                    serialNumber = config.airpodsSerialNumber,
                    leftSerialNumber = info.leftSerialNumber,
                    rightSerialNumber = info.rightSerialNumber,
                    version1 = info.version1,
                    version2 = info.version2,
                    version3 = info.version3,
                    aacpManager = aacpManager,
                    attManager = attManager
                )
            }
        }

        override fun onUnknownPacketReceived(packet: ByteArray) {
            Log.d(
                "AACPManager",
                "Unknown packet: ${packet.joinToString(" ") { "%02X".format(it) }}"
            )
        }
    })
}

/* ------------------------------------------------------- */
/* ---------------- CALL HANDLING ------------------------ */
/* ------------------------------------------------------- */

fun AirPodsService.handleIncomingCall() {
    if (isInCall) return

    if (config.headGestures) {
        initGestureDetector()
        startHeadTracking()

        gestureDetector?.startDetection { accepted ->
            if (accepted) answerCall() else rejectCall()
            handleIncomingCallOnceConnected = false
        }
    }
}

private fun AirPodsService.answerCall() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager.acceptRingingCall()
            }
        }
        sendToast("Call answered via head gesture")
    } catch (e: Exception) {
        sendToast("Failed to answer call: ${e.message}")
    } finally {
        islandWindow?.close()
    }
}

private fun AirPodsService.rejectCall() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager.endCall()
            }
        }
        sendToast("Call rejected via head gesture")
    } catch (e: Exception) {
        sendToast("Failed to reject call: ${e.message}")
    } finally {
        islandWindow?.close()
    }
}