        override fun onBatteryChanged(device: BLEManager.AirPodsStatus) {
            if (isConnectedLocally) return
            val leftLevel = bleManager.getMostRecentStatus()?.leftBattery?: 0
            val rightLevel = bleManager.getMostRecentStatus()?.rightBattery?: 0
            val caseLevel = bleManager.getMostRecentStatus()?.caseBattery?: 0
            val leftCharging = bleManager.getMostRecentStatus()?.isLeftCharging
            val rightCharging = bleManager.getMostRecentStatus()?.isRightCharging
            val caseCharging = bleManager.getMostRecentStatus()?.isCaseCharging

            batteryNotification.setBatteryDirect(
                leftLevel = leftLevel,
                leftCharging = leftCharging == true,
                rightLevel = rightLevel,
                rightCharging = rightCharging == true,
                caseLevel = caseLevel,
                caseCharging = caseCharging == true
            )
            updateBattery()
            Log.d(TAG, "Battery changed")
        }
