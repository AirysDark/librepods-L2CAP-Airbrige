fun disconnectAirPods() {

    if (!this::socket.isInitialized) return

    try {
        if (socket.isConnected) {
            socket.close()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error closing socket: ${e.message}")
    }

    isConnectedLocally = false

    try {
        aacpManager.disconnected()
    } catch (e: Exception) {
        Log.e(TAG, "Error notifying AACP disconnect: ${e.message}")
    }

    try {
        attManager?.disconnect()
    } catch (e: Exception) {
        Log.e(TAG, "Error disconnecting ATT: ${e.message}")
    }

    updateNotificationContent(false)

    sendBroadcast(Intent(AirPodsNotifications.AIRPODS_DISCONNECTED))

    val bluetoothAdapter =
        getSystemService(BluetoothManager::class.java)?.adapter

    bluetoothAdapter?.getProfileProxy(
        this,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                try {
                    if (profile == BluetoothProfile.A2DP) {
                        val connectedDevices = proxy.connectedDevices
                        if (!connectedDevices.isNullOrEmpty()) {
                            MediaController.sendPause()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "A2DP pause check failed: ${e.message}")
                } finally {
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.A2DP
    )

    Log.d(TAG, "Disconnected AirPods upon user request")
}