private fun createBluetoothSocket(
    device: BluetoothDevice,
    uuid: ParcelUuid
): BluetoothSocket {

    val type = 3 // L2CAP

    val constructorSpecs = listOf(
        arrayOf(device, type, true, true, 0x1001, uuid),
        arrayOf(device, type, 1, true, true, 0x1001, uuid),
        arrayOf(type, 1, true, true, device, 0x1001, uuid),
        arrayOf(type, true, true, device, 0x1001, uuid)
    )

    var lastException: Exception? = null

    for ((index, params) in constructorSpecs.withIndex()) {
        try {
            Log.d(TAG, "Trying BluetoothSocket constructor signature #${index + 1}")

            val instance = HiddenApiBypass.newInstance(
                BluetoothSocket::class.java,
                *params
            )

            if (instance is BluetoothSocket) {
                return instance
            } else {
                throw IllegalStateException("Constructor did not return BluetoothSocket")
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Constructor signature #${index + 1} failed: ${e.message}"
            )
            lastException = e
        }
    }

    val errorMessage =
        "Failed to create BluetoothSocket after trying ${constructorSpecs.size} constructor signatures"

    Log.e(TAG, errorMessage)

    showSocketConnectionFailureNotification(errorMessage)

    throw lastException ?: IllegalStateException(errorMessage)
}