    fun setBatteryMetadata() {
        device?.let { it ->
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_CASE_BATTERY,
                batteryNotification.getBattery().find { it.component == BatteryComponent.CASE }?.level.toString().toByteArray()
            )
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_CASE_CHARGING,
                (if (batteryNotification.getBattery().find { it.component == BatteryComponent.CASE}?.status == BatteryStatus.CHARGING) "1".toByteArray() else "0".toByteArray())
            )
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_LEFT_BATTERY,
                batteryNotification.getBattery().find { it.component == BatteryComponent.LEFT }?.level.toString().toByteArray()
            )
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_LEFT_CHARGING,
                (if (batteryNotification.getBattery().find { it.component == BatteryComponent.LEFT}?.status == BatteryStatus.CHARGING) "1".toByteArray() else "0".toByteArray())
            )
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_RIGHT_BATTERY,
                batteryNotification.getBattery().find { it.component == BatteryComponent.RIGHT }?.level.toString().toByteArray()
            )
            SystemApisUtils.setMetadata(
                it,
                it.METADATA_UNTETHERED_RIGHT_CHARGING,
                (if (batteryNotification.getBattery().find { it.component == BatteryComponent.RIGHT}?.status == BatteryStatus.CHARGING) "1".toByteArray() else "0".toByteArray())
            )
        }
    }
