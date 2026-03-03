package me.kavishdevar.librepods.services

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

fun AirPodsService.answerCall() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val telecomManager =
                getSystemService(TELECOM_SERVICE) as TelecomManager

            if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager.acceptRingingCall()
            }
        } else {
            val telephonyService =
                getSystemService(TELEPHONY_SERVICE) as TelephonyManager

            val telephonyClass =
                Class.forName(telephonyService.javaClass.name)

            val method =
                telephonyClass.getDeclaredMethod("getITelephony")

            method.isAccessible = true

            val telephonyInterface =
                method.invoke(telephonyService)

            val answerCallMethod =
                telephonyInterface.javaClass
                    .getDeclaredMethod("answerRingingCall")

            answerCallMethod.invoke(telephonyInterface)
        }

        sendToast("Call answered via head gesture")

    } catch (e: Exception) {
        e.printStackTrace()
        sendToast("Failed to answer call: ${e.message}")
    } finally {
        islandWindow?.close()
    }
}