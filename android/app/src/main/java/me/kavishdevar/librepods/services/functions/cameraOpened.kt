package me.kavishdevar.librepods.services

import android.util.Log

fun AirPodsService.cameraOpened() {
    Log.d(TAG, "Camera opened, gonna handle stem presses and take action if enabled")
    cameraActive = true
    setupStemActions()
}