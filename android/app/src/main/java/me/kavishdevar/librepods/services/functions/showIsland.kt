    fun showIsland(service: Service, batteryPercentage: Int, type: IslandType = IslandType.CONNECTED, reversed: Boolean = false, otherDeviceName: String? = null) {
        Log.d(TAG, "Showing island window")
        if (!Settings.canDrawOverlays(service)) {
            Log.d(TAG, "No permission for SYSTEM_ALERT_WINDOW")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            islandWindow = IslandWindow(service.applicationContext)
            islandWindow!!.show(sharedPreferences.getString("name", "AirPods Pro").toString(), batteryPercentage, this@AirPodsService, type, reversed, otherDeviceName)
        }
    }
