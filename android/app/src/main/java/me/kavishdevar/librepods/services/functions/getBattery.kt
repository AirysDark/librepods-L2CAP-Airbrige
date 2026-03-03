fun getBattery(): List<Battery> {

    return try {
        batteryNotification.getBattery()
    } catch (e: Exception) {
        Log.e(TAG, "Battery data not available: ${e.message}")
        emptyList()
    }
}