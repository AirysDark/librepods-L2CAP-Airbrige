package me.kavishdevar.librepods.services

fun AirPodsService.clearLogs() {
    clearPacketLogs()
    packetLogsFlow.value = emptyList()
}