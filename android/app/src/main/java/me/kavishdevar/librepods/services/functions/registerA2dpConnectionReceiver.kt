    private fun registerA2dpConnectionReceiver() {
        val a2dpConnectionStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    val previousState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    Log.d("MediaController", "A2DP state changed: $previousState -> $state for device: ${device?.address}")

                    if (state == BluetoothProfile.STATE_CONNECTED &&
                        previousState != BluetoothProfile.STATE_CONNECTED &&
                        device?.address == this@AirPodsService.device?.address) {

                        Log.d("MediaController", "A2DP connected, sending play command")
                        MediaController.sendPlay()
                        MediaController.iPausedTheMedia = false

                        context.unregisterReceiver(this)
                    }
                }
            }
        }

        val a2dpIntentFilter = IntentFilter("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(a2dpConnectionStateReceiver, a2dpIntentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(a2dpConnectionStateReceiver, a2dpIntentFilter)
        }
    }
