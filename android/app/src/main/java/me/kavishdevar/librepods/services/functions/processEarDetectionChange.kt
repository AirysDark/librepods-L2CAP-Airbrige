    private fun processEarDetectionChange(earDetection: ByteArray) {
        var inEar: Boolean
        val inEarData = listOf(earDetectionNotification.status[0] == 0x00.toByte(), earDetectionNotification.status[1] == 0x00.toByte())
        var justEnabledA2dp = false
        earDetectionNotification.setStatus(earDetection)
        if (config.earDetectionEnabled) {
            val data = earDetection.copyOfRange(earDetection.size - 2, earDetection.size)
            inEar = data[0] == 0x00.toByte() && data[1] == 0x00.toByte()

            val newInEarData = listOf(
                data[0] == 0x00.toByte(),
                data[1] == 0x00.toByte()
            )

            if (inEarData.sorted() == listOf(false, false) && newInEarData.sorted() != listOf(false, false) && islandWindow?.isVisible != true) {
                showIsland(
                    this@AirPodsService,
                    (batteryNotification.getBattery().find { it.component == BatteryComponent.LEFT}?.level?: 0).coerceAtMost(batteryNotification.getBattery().find { it.component == BatteryComponent.RIGHT}?.level?: 0))
            }

            if (newInEarData == listOf(false, false) && islandWindow?.isVisible == true) {
                islandWindow?.close()
            }

            if (newInEarData.contains(true) && inEarData == listOf(false, false)) {
                connectAudio(this@AirPodsService, device)
                justEnabledA2dp = true
                registerA2dpConnectionReceiver()
                if (MediaController.getMusicActive()) {
                    MediaController.userPlayedTheMedia = true
                }
            } else if (newInEarData == listOf(false, false)) {
                MediaController.sendPause(force = true)
                if (config.disconnectWhenNotWearing) {
                    disconnectAudio(this@AirPodsService, device)
                }
            }

            if (inEarData.contains(false) && newInEarData == listOf(true, true)) {
                Log.d("AirPodsParser", "User put in both AirPods from just one.")
                MediaController.userPlayedTheMedia = false
            }

            if (newInEarData.contains(false) && inEarData == listOf(true, true)) {
                Log.d("AirPodsParser", "User took one of two out.")
                MediaController.userPlayedTheMedia = false
            }

            Log.d("AirPodsParser", "inEarData: ${inEarData.sorted()}, newInEarData: ${newInEarData.sorted()}")

            if (newInEarData.sorted() != inEarData.sorted()) {
                if (inEar) {
                    if (!justEnabledA2dp) {
                        MediaController.sendPlay()
                        MediaController.iPausedTheMedia = false
                    }
                } else {
                    MediaController.sendPause()
                }
            }
        }
    }
