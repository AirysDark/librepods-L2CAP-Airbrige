    fun stopHeadTracking() {
        val useAlternatePackets = sharedPreferences.getBoolean("use_alternate_head_tracking_packets", false)
        if (useAlternatePackets) {
            aacpManager.sendDataPacket(aacpManager.createAlternateStopHeadTrackingPacket())
        } else {
            aacpManager.sendStopHeadTracking()
        }
        isHeadTrackingActive = false
    }
