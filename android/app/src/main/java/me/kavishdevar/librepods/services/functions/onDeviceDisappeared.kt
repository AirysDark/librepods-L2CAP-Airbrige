        override fun onDeviceDisappeared() {
            Log.d(TAG, "All disappeared")
            updateNotificationContent(
                false
            )
        }
