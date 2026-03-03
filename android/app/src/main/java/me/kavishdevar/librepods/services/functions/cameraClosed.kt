package me.kavishdevar.librepods.services

fun AirPodsService.cameraClosed() {
    cameraActive = false
    setupStemActions()
}