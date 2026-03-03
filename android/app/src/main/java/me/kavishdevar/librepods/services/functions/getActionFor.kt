private fun getActionFor(
    bud: AACPManager.Companion.StemPressBudType,
    type: StemPressType
): StemAction? {

    val isLeft =
        bud == AACPManager.Companion.StemPressBudType.LEFT

    return when (type) {

        StemPressType.SINGLE_PRESS ->
            if (isLeft)
                config.leftSinglePressAction
            else
                config.rightSinglePressAction

        StemPressType.DOUBLE_PRESS ->
            if (isLeft)
                config.leftDoublePressAction
            else
                config.rightDoublePressAction

        StemPressType.TRIPLE_PRESS ->
            if (isLeft)
                config.leftTriplePressAction
            else
                config.rightTriplePressAction

        StemPressType.LONG_PRESS ->
            if (isLeft)
                config.leftLongPressAction
            else
                config.rightLongPressAction
    }
}