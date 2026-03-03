fun getANC(): Int {

    return try {
        ancNotification.status
    } catch (e: Exception) {
        Log.e(TAG, "ANC status not available: ${e.message}")
        0
    }
}