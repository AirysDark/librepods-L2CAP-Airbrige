fun disconnectAudio(
    context: Context,
    device: BluetoothDevice?
) {

    if (device == null) {
        Log.d(TAG, "disconnectAudio called with null device")
        return
    }

    val bluetoothAdapter =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    if (bluetoothAdapter == null) {
        Log.e(TAG, "BluetoothAdapter is null")
        return
    }

    // A2DP
    bluetoothAdapter.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                try {
                    if (profile == BluetoothProfile.A2DP) {

                        val state =
                            proxy.getConnectionState(device)

                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(TAG, "Already disconnected from A2DP")
                            return
                        }

                        val method =
                            proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )

                        method.invoke(proxy, device, 0)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "A2DP disconnect failed: ${e.message}")
                } finally {
                    bluetoothAdapter.closeProfileProxy(
                        profile,
                        proxy
                    )
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.A2DP
    )

    // HEADSET
    bluetoothAdapter.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                try {
                    if (profile == BluetoothProfile.HEADSET) {

                        val method =
                            proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )

                        method.invoke(proxy, device, 0)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "HEADSET disconnect failed: ${e.message}")
                } finally {
                    bluetoothAdapter.closeProfileProxy(
                        profile,
                        proxy
                    )
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        },
        BluetoothProfile.HEADSET
    )
}