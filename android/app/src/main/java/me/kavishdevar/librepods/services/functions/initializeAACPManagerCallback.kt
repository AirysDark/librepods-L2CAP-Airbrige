private fun initializeAACPManagerCallback() {

    aacpManager.setPacketCallback(object : AACPManager.PacketCallback {

        @SuppressLint("MissingPermission")
        override fun onBatteryInfoReceived(batteryInfo: ByteArray) {

            batteryNotification.setBattery(batteryInfo)

            val batteries = batteryNotification.getBattery()
            val left = batteries.find { it.component == BatteryComponent.LEFT }
            val right = batteries.find { it.component == BatteryComponent.RIGHT }

            sendBroadcast(
                Intent(AirPodsNotifications.BATTERY_DATA)
                    .putParcelableArrayListExtra("data", ArrayList(batteries))
            )

            updateBattery()

            updateNotificationContent(
                true,
                getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("name", device?.name),
                batteries
            )

            batteries.forEach {
                Log.d(
                    "AirPodsParser",
                    "${it.getComponentName()}: ${it.getStatusName()} at ${it.level}%"
                )
            }

            val currentDevice = device ?: return

            if (left?.status == BatteryStatus.CHARGING &&
                right?.status == BatteryStatus.CHARGING
            ) {
                disconnectAudio(this@AirPodsService, currentDevice)
            } else {
                connectAudio(this@AirPodsService, currentDevice)
            }
        }

        override fun onEarDetectionReceived(earDetection: ByteArray) {
            sendBroadcast(
                Intent(AirPodsNotifications.EAR_DETECTION_DATA).apply {
                    val status = earDetectionNotification.status
                    putExtra("data", byteArrayOf(status[0], status[1]))
                }
            )

            Log.d(
                "AirPodsParser",
                "Ear Detection: ${earDetectionNotification.status[0]} ${earDetectionNotification.status[1]}"
            )

            processEarDetectionChange(earDetection)
        }

        override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) {

            conversationAwarenessNotification.setData(conversationAwareness)

            sendBroadcast(
                Intent(AirPodsNotifications.CA_DATA)
                    .putExtra("data", conversationAwarenessNotification.status)
            )

            when (conversationAwarenessNotification.status) {
                1.toByte(), 2.toByte() -> MediaController.startSpeaking()
                8.toByte(), 9.toByte() -> MediaController.stopSpeaking()
            }

            Log.d(
                "AirPodsParser",
                "Conversation Awareness: ${conversationAwarenessNotification.status}"
            )
        }

        override fun onControlCommandReceived(controlCommand: ByteArray) {

            val command = AACPManager.ControlCommand.fromByteArray(controlCommand)

            if (command.identifier ==
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value
            ) {
                val mode = command.value.firstOrNull() ?: 0x00
                ancNotification.setStatus(byteArrayOf(mode))
                sendANCBroadcast()
                updateNoiseControlWidget()
            }
        }

        override fun onOwnershipChangeReceived(owns: Boolean) {

            if (owns) return

            MediaController.recentlyLostOwnership = true
            Handler(Looper.getMainLooper()).postDelayed({
                MediaController.recentlyLostOwnership = false
            }, 3000)

            Log.d(TAG, "Ownership lost")

            MediaController.sendPause()
            MediaController.pausedForOtherDevice = true
            otherDeviceTookOver = true

            device?.let {
                disconnectAudio(this@AirPodsService, it)
            }
        }

        override fun onOwnershipToFalseRequest(
            sender: String,
            reasonReverseTapped: Boolean
        ) {

            val senderName =
                aacpManager.connectedDevices
                    .find { it.mac == sender }
                    ?.type ?: "Other device"

            Log.d(TAG, "Connection hijacked. Reverse: $reasonReverseTapped")

            aacpManager.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.OWNS_CONNECTION.value,
                byteArrayOf(0x00)
            )

            otherDeviceTookOver = true

            if (reasonReverseTapped) {
                disconnectedBecauseReversed = true
            }

            device?.let {
                disconnectAudio(this@AirPodsService, it)
            }

            val batteries = batteryNotification.getBattery()
            val minBattery = minOf(
                batteries.find { it.component == BatteryComponent.LEFT }?.level ?: 0,
                batteries.find { it.component == BatteryComponent.RIGHT }?.level ?: 0
            )

            if (!aacpManager.owns) {
                showIsland(
                    this@AirPodsService,
                    minBattery,
                    IslandType.MOVED_TO_OTHER_DEVICE,
                    reversed = reasonReverseTapped,
                    otherDeviceName = senderName
                )
            }

            MediaController.sendPause()
        }

        override fun onShowNearbyUI(sender: String) {

            val senderName =
                aacpManager.connectedDevices
                    .find { it.mac == sender }
                    ?.type ?: "Other device"

            val batteries = batteryNotification.getBattery()

            val minBattery = minOf(
                batteries.find { it.component == BatteryComponent.LEFT }?.level ?: 0,
                batteries.find { it.component == BatteryComponent.RIGHT }?.level ?: 0
            )

            showIsland(
                this@AirPodsService,
                minBattery,
                IslandType.MOVED_TO_OTHER_DEVICE,
                otherDeviceName = senderName
            )
        }

        // ---- The rest of your callbacks can remain unchanged ----
        // They are structurally fine.
    })
}