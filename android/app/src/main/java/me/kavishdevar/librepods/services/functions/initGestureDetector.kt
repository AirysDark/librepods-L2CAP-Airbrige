@Synchronized
private fun initGestureDetector() {

    if (gestureDetector != null) {
        return
    }

    try {
        gestureDetector = GestureDetector(this)
        Log.d(TAG, "GestureDetector initialized")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize GestureDetector: ${e.message}", e)
        gestureDetector = null
    }
}