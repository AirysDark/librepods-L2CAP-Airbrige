    private fun initializeConfig() {
        config = ServiceConfig(
            deviceName = sharedPreferences.getString("name", "AirPods") ?: "AirPods",
            earDetectionEnabled = sharedPreferences.getBoolean("automatic_ear_detection", true),
            conversationalAwarenessPauseMusic = sharedPreferences.getBoolean("conversational_awareness_pause_music", false),
            showPhoneBatteryInWidget = sharedPreferences.getBoolean("show_phone_battery_in_widget", true),
            relativeConversationalAwarenessVolume = sharedPreferences.getBoolean("relative_conversational_awareness_volume", true),
            headGestures = sharedPreferences.getBoolean("head_gestures", true),
            disconnectWhenNotWearing = sharedPreferences.getBoolean("disconnect_when_not_wearing", false),
            conversationalAwarenessVolume = sharedPreferences.getInt("conversational_awareness_volume", 43),
            qsClickBehavior = sharedPreferences.getString("qs_click_behavior", "cycle") ?: "cycle",

            // AirPods state-based takeover
            takeoverWhenDisconnected = sharedPreferences.getBoolean("takeover_when_disconnected", true),
            takeoverWhenIdle = sharedPreferences.getBoolean("takeover_when_idle", true),
            takeoverWhenMusic = sharedPreferences.getBoolean("takeover_when_music", false),
            takeoverWhenCall = sharedPreferences.getBoolean("takeover_when_call", true),

            // Phone state-based takeover
            takeoverWhenRingingCall = sharedPreferences.getBoolean("takeover_when_ringing_call", true),
            takeoverWhenMediaStart = sharedPreferences.getBoolean("takeover_when_media_start", true),

            // Stem actions
            leftSinglePressAction = StemAction.fromString(sharedPreferences.getString("left_single_press_action", "PLAY_PAUSE") ?: "PLAY_PAUSE")!!,
            rightSinglePressAction = StemAction.fromString(sharedPreferences.getString("right_single_press_action", "PLAY_PAUSE") ?: "PLAY_PAUSE")!!,

            leftDoublePressAction = StemAction.fromString(sharedPreferences.getString("left_double_press_action", "PREVIOUS_TRACK") ?: "NEXT_TRACK")!!,
            rightDoublePressAction = StemAction.fromString(sharedPreferences.getString("right_double_press_action", "NEXT_TRACK") ?: "NEXT_TRACK")!!,

            leftTriplePressAction = StemAction.fromString(sharedPreferences.getString("left_triple_press_action", "PREVIOUS_TRACK") ?: "PREVIOUS_TRACK")!!,
            rightTriplePressAction = StemAction.fromString(sharedPreferences.getString("right_triple_press_action", "PREVIOUS_TRACK") ?: "PREVIOUS_TRACK")!!,

            leftLongPressAction = StemAction.fromString(sharedPreferences.getString("left_long_press_action", "CYCLE_NOISE_CONTROL_MODES") ?: "CYCLE_NOISE_CONTROL_MODES")!!,
            rightLongPressAction = StemAction.fromString(sharedPreferences.getString("right_long_press_action", "DIGITAL_ASSISTANT") ?: "DIGITAL_ASSISTANT")!!,

            cameraAction = sharedPreferences.getString("camera_action", null)?.let { StemPressType.valueOf(it) },

            // AirPods device information
            airpodsName = sharedPreferences.getString("airpods_name", "") ?: "",
            airpodsModelNumber = sharedPreferences.getString("airpods_model_number", "") ?: "",
            airpodsManufacturer = sharedPreferences.getString("airpods_manufacturer", "") ?: "",
            airpodsSerialNumber = sharedPreferences.getString("airpods_serial_number", "") ?: "",
            airpodsLeftSerialNumber = sharedPreferences.getString("airpods_left_serial_number", "") ?: "",
            airpodsRightSerialNumber = sharedPreferences.getString("airpods_right_serial_number", "") ?: "",
            airpodsVersion1 = sharedPreferences.getString("airpods_version1", "") ?: "",
            airpodsVersion2 = sharedPreferences.getString("airpods_version2", "") ?: "",
            airpodsVersion3 = sharedPreferences.getString("airpods_version3", "") ?: "",
            airpodsHardwareRevision = sharedPreferences.getString("airpods_hardware_revision", "") ?: "",
            airpodsUpdaterIdentifier = sharedPreferences.getString("airpods_updater_identifier", "") ?: "",

            selfMacAddress = sharedPreferences.getString("self_mac_address", "") ?: ""
        )
    }
