    fun setupStemActions() {
        val singlePressDefault = StemAction.defaultActions[StemPressType.SINGLE_PRESS]
        val doublePressDefault = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]
        val triplePressDefault = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]
        val longPressDefault   = StemAction.defaultActions[StemPressType.LONG_PRESS]

        val singlePressCustomized = isCustomAction(config.leftSinglePressAction, singlePressDefault) ||
            isCustomAction(config.rightSinglePressAction, singlePressDefault) ||
            (cameraActive && config.cameraAction == StemPressType.SINGLE_PRESS)
        val doublePressCustomized = isCustomAction(config.leftDoublePressAction, doublePressDefault) ||
            isCustomAction(config.rightDoublePressAction, doublePressDefault)
        val triplePressCustomized = isCustomAction(config.leftTriplePressAction, triplePressDefault) ||
            isCustomAction(config.rightTriplePressAction, triplePressDefault)
        val longPressCustomized = isCustomAction(config.leftLongPressAction, longPressDefault) ||
            isCustomAction(config.rightLongPressAction, longPressDefault) ||
            (cameraActive && config.cameraAction == StemPressType.LONG_PRESS)
        Log.d(TAG, "Setting up stem actions: " +
            "Single Press Customized: $singlePressCustomized, " +
            "Double Press Customized: $doublePressCustomized, " +
            "Triple Press Customized: $triplePressCustomized, " +
            "Long Press Customized: $longPressCustomized")
        aacpManager.sendStemConfigPacket(
            singlePressCustomized,
            doublePressCustomized,
            triplePressCustomized,
            longPressCustomized,
        )
    }
