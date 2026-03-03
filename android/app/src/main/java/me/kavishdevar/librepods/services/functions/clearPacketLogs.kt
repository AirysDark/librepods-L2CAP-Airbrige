package me.kavishdevar.librepods.services

import androidx.core.content.edit

fun AirPodsService.clearPacketLogs() {

    synchronized(inMemoryLogs) {
        inMemoryLogs.clear()
        packetLogsFlow.value = emptyList()
    }

    sharedPreferencesLogs.edit {
        remove(packetLogKey)
    }
}