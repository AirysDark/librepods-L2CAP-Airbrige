fun connectToSocket(device: BluetoothDevice, manual: Boolean = false) {

    Log.d(TAG, "<LogCollector:Start> Connecting to socket")

    HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/BluetoothSocket;")

    val uuid: ParcelUuid =
        ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

    if (isConnectedLocally) {
        Log.d(
            TAG,
            "Already connected locally, skipping socket connection (isConnectedLocally = $isConnectedLocally, socket.isConnected = ${this::socket.isInitialized && socket.isConnected})"
        )
        return
    }

    socket = try {
        createBluetoothSocket(device, uuid)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create BluetoothSocket: ${e.message}")
        showSocketConnectionFailureNotification(
            "Failed to create Bluetooth socket: ${e.localizedMessage}"
        )
        return
    }

    try {
        runBlocking {
            withTimeout(5000L) {
                try {
                    socket.connect()

                    isConnectedLocally = true
                    this@AirPodsService.device = device

                    BluetoothConnectionManager.setCurrentConnection(socket, device)

                    attManager = ATTManager(device)
                    attManager?.connect()

                    if (!::airpodsInstance.isInitialized &&
                        config.airpodsModelNumber.isNotEmpty()
                    ) {
                        AirPodsModels.getModelByModelNumber(
                            config.airpodsModelNumber
                        )?.let { model ->
                            airpodsInstance = AirPodsInstance(
                                name = config.airpodsName,
                                model = model,
                                actualModelNumber = config.airpodsModelNumber,
                                serialNumber = config.airpodsSerialNumber,
                                leftSerialNumber = config.airpodsLeftSerialNumber,
                                rightSerialNumber = config.airpodsRightSerialNumber,
                                version1 = config.airpodsVersion1,
                                version2 = config.airpodsVersion2,
                                version3 = config.airpodsVersion3,
                                aacpManager = aacpManager,
                                attManager = attManager
                            )
                        }
                    }

                    updateNotificationContent(
                        true,
                        config.deviceName,
                        batteryNotification.getBattery()
                    )

                    Log.d(TAG, "<LogCollector:Complete:Success> Socket connected")

                } catch (e: Exception) {

                    Log.d(
                        TAG,
                        "<LogCollector:Complete:Failed> Socket not connected, ${e.message}"
                    )

                    if (manual) {
                        sendToast("Couldn't connect to socket: ${e.localizedMessage}")
                    } else {
                        showSocketConnectionFailureNotification(
                            "Couldn't connect to socket: ${e.localizedMessage}"
                        )
                    }

                    return@withTimeout
                }
            }
        }

        if (!socket.isConnected) {
            if (manual) {
                sendToast("Couldn't connect to socket: timeout.")
            } else {
                showSocketConnectionFailureNotification(
                    "Couldn't connect to socket: Timeout"
                )
            }
            return
        }

        aacpManager.sendPacket(aacpManager.createHandshakePacket())
        aacpManager.sendSetFeatureFlagsPacket()
        aacpManager.sendNotificationRequest()

        CoroutineScope(Dispatchers.IO).launch {

            sendBroadcast(
                Intent(AirPodsNotifications.AIRPODS_CONNECTED)
                    .putExtra("device", device)
            )

            setupStemActions()

            val buffer = ByteArray(1024)

            while (socket.isConnected) {
                try {
                    val bytesRead = socket.inputStream.read(buffer)

                    if (bytesRead > 0) {

                        val data = buffer.copyOfRange(0, bytesRead)

                        sendBroadcast(
                            Intent(AirPodsNotifications.AIRPODS_DATA).apply {
                                putExtra("data", data)
                            }
                        )

                        updateNotificationContent(
                            true,
                            sharedPreferences.getString("name", device.name),
                            batteryNotification.getBattery()
                        )

                        aacpManager.receivePacket(data)

                        if (!isHeadTrackingData(data)) {
                            logPacket(data, "AirPods")
                        }

                    } else if (bytesRead == -1) {
                        break
                    }

                } catch (e: Exception) {
                    break
                }
            }

            Log.d("AirPods Service", "Socket closed")

            isConnectedLocally = false

            try {
                socket.close()
            } catch (_: Exception) {}

            aacpManager.disconnected()

            updateNotificationContent(false)

            sendBroadcast(
                Intent(AirPodsNotifications.AIRPODS_DISCONNECTED)
            )
        }

    } catch (e: Exception) {

        Log.e(TAG, "Failed to connect to socket: ${e.message}")

        showSocketConnectionFailureNotification(
            "Failed to establish connection: ${e.localizedMessage}"
        )

        isConnectedLocally = false
        updateNotificationContent(false)
    }
}