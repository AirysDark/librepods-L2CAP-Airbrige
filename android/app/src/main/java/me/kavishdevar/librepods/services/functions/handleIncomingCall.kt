fun handleIncomingCall() {

    if (isInCall) {
        Log.d(TAG, "Already in call, ignoring incoming call handler")
        return
    }

    if (!config.headGestures) {
        Log.d(TAG, "Head gestures disabled, not handling call via gesture")
        return
    }

    val detector = try {
        initGestureDetector()
        startHeadTracking()
        gestureDetector
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize head tracking: ${e.message}", e)
        handleIncomingCallOnceConnected = false
        stopHeadTrackingSafely()
        return
    }

    if (detector == null) {
        Log.e(TAG, "Gesture detector not initialized")
        handleIncomingCallOnceConnected = false
        stopHeadTrackingSafely()
        return
    }

    var handled = false

    detector.startDetection { accepted ->

        if (handled) return@startDetection
        handled = true

        try {
            if (accepted) {
                Log.d(TAG, "Head gesture accepted — answering call")
                answerCall()
            } else {
                Log.d(TAG, "Head gesture rejected — rejecting call")
                rejectCall()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling gesture result: ${e.message}", e)
        } finally {
            handleIncomingCallOnceConnected = false
            stopHeadTrackingSafely()
        }
    }
}