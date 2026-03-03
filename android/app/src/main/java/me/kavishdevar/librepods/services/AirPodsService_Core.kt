package me.kavishdevar.librepods.services

import android.content.SharedPreferences
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.kavishdevar.librepods.constants.StemAction
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.AACPManager.Companion.StemPressType
import me.kavishdevar.librepods.utils.AirPodsInstance
import me.kavishdevar.librepods.utils.ATTManager
import me.kavishdevar.librepods.utils.BLEManager

/* =====================================================
   CORE STATE EXTENSIONS
   ===================================================== */

data class ServiceConfig(
    var deviceName: String = "AirPods",
    var earDetectionEnabled: Boolean = true,
    var conversationalAwarenessPauseMusic: Boolean = false,
    var showPhoneBatteryInWidget: Boolean = true,
    var relativeConversationalAwarenessVolume: Boolean = true,
    var headGestures: Boolean = true,
    var disconnectWhenNotWearing: Boolean = false,
    var conversationalAwarenessVolume: Int = 43,
    var qsClickBehavior: String = "cycle",
    var bleOnlyMode: Boolean = false,
    var takeoverWhenDisconnected: Boolean = true,
    var takeoverWhenIdle: Boolean = true,
    var takeoverWhenMusic: Boolean = false,
    var takeoverWhenCall: Boolean = true,
    var takeoverWhenRingingCall: Boolean = true,
    var takeoverWhenMediaStart: Boolean = true,
    var leftSinglePressAction: StemAction = StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,
    var rightSinglePressAction: StemAction = StemAction.defaultActions[StemPressType.SINGLE_PRESS]!!,
    var leftDoublePressAction: StemAction = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,
    var rightDoublePressAction: StemAction = StemAction.defaultActions[StemPressType.DOUBLE_PRESS]!!,
    var leftTriplePressAction: StemAction = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,
    var rightTriplePressAction: StemAction = StemAction.defaultActions[StemPressType.TRIPLE_PRESS]!!,
    var leftLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,
    var rightLongPressAction: StemAction = StemAction.defaultActions[StemPressType.LONG_PRESS]!!,
    var cameraAction: StemPressType? = null,
    var airpodsName: String = "",
    var airpodsModelNumber: String = "",
    var airpodsManufacturer: String = "",
    var airpodsSerialNumber: String = "",
    var airpodsLeftSerialNumber: String = "",
    var airpodsRightSerialNumber: String = "",
    var airpodsVersion1: String = "",
    var airpodsVersion2: String = "",
    var airpodsVersion3: String = "",
    var airpodsHardwareRevision: String = "",
    var airpodsUpdaterIdentifier: String = "",
    var selfMacAddress: String = ""
)

/* =====================================================
   CORE INITIALIZATION
   ===================================================== */

fun AirPodsService.initializeCore() {

    config = ServiceConfig()

    sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
    sharedPreferencesLogs = getSharedPreferences("logs", MODE_PRIVATE)

    bleManager = BLEManager(this)
    airpodsInstance = AirPodsInstance()
    aacpManager = AACPManager(this)
    attManager = ATTManager(this)

    telephonyManager = getSystemService(TelephonyManager::class.java)
    phoneStateListener = object : PhoneStateListener() {}

}

/* =====================================================
   CORE BACKING FIELDS (MOVED TO BASE SERVICE)
   ===================================================== */

lateinit var AirPodsService.config: ServiceConfig
lateinit var AirPodsService.sharedPreferencesLogs: SharedPreferences
lateinit var AirPodsService.sharedPreferences: SharedPreferences
lateinit var AirPodsService.telephonyManager: TelephonyManager
lateinit var AirPodsService.phoneStateListener: PhoneStateListener

private val AirPodsService.packetLogKey: String
    get() = "packet_log"

private val AirPodsService.maxLogEntries: Int
    get() = 1000

val AirPodsService._packetLogsFlow: MutableStateFlow<Set<String>>
    get() = MutableStateFlow(emptySet())

val AirPodsService.packetLogsFlowCore: StateFlow<Set<String>>
    get() = _packetLogsFlow