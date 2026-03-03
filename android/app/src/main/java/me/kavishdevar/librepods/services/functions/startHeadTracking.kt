    fun startHeadTracking() {
        isHeadTrackingActive = true
        val useAlternatePackets = sharedPreferences.getBoolean("use_alternate_head_tracking_packets", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && aacpManager.getControlCommandStatus(AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION)?.value?.get(0)?.toInt() != 1) {
            takeOver("call", startHeadTrackingAgain = true)
            Log.d(TAG, "Taking over for head tracking")
        } else {
            Log.w(TAG, "Will not be taking over for head tracking, might not work.")
        }
        if (useAlternatePackets) {
            aacpManager.sendDataPacket(aacpManager.createAlternateStartHeadTrackingPacket())
        } else {
            aacpManager.sendStartHeadTracking()
        }
        HeadTracking.reset()
    }
