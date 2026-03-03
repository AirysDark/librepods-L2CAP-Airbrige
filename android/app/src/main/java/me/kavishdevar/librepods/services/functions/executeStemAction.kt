private fun executeStemAction(action: StemAction) {

    val defaultSinglePress =
        StemAction.defaultActions[StemPressType.SINGLE_PRESS]

    when (action) {

        defaultSinglePress -> {
            Log.d(
                TAG,
                "Default single press action: Play/Pause handled by system."
            )
        }

        StemAction.PLAY_PAUSE -> {
            MediaController.sendPlayPause()
        }

        StemAction.PREVIOUS_TRACK -> {
            MediaController.sendPreviousTrack()
        }

        StemAction.NEXT_TRACK -> {
            MediaController.sendNextTrack()
        }

        StemAction.DIGITAL_ASSISTANT -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch digital assistant: ${e.message}")
                }
            } else {
                Log.w(
                    TAG,
                    "Digital Assistant action not supported on this Android version."
                )
            }
        }

        StemAction.CYCLE_NOISE_CONTROL_MODES -> {
            Log.d(TAG, "Cycling noise control modes")
            sendBroadcast(
                Intent("me.kavishdevar.librepods.SET_ANC_MODE")
            )
        }

        else -> {
            Log.w(TAG, "Unhandled StemAction: $action")
        }
    }
}