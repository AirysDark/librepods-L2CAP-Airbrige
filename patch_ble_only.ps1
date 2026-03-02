$File = "AirPodsService.kt"
$Backup = "AirPodsService.kt.bak"

Write-Host "Backing up file..."
Copy-Item $File $Backup -Force

$content = Get-Content $File -Raw

# 1?? Remove BluetoothSocket property
$content = $content -replace "private lateinit var socket: BluetoothSocket\s*", ""

# 2?? Remove createBluetoothSocket function completely
$content = [regex]::Replace(
    $content,
    "private fun createBluetoothSocket\([\s\S]*?\n\s*}\n",
    "",
    "Singleline"
)

# 3?? Replace connectToSocket with BLE stub
$content = [regex]::Replace(
    $content,
    "fun connectToSocket\([\s\S]*?\n\s*}\n",
@"
fun connectToSocket(device: BluetoothDevice, manual: Boolean = false) {
    Log.d(TAG, "BLE-only mode: Skipping L2CAP socket connection")
    this.device = device
    isConnectedLocally = true
    updateNotificationContent(
        true,
        config.deviceName,
        batteryNotification.getBattery()
    )
}
"@,
    "Singleline"
)

# 4?? Remove socket failure notification function
$content = [regex]::Replace(
    $content,
    "private fun showSocketConnectionFailureNotification\([\s\S]*?\n\s*}\n",
    "",
    "Singleline"
)

# 5?? Remove socket references in updateNotificationContent
$content = $content -replace "if \(!::socket\.isInitialized\) \{\s*return\s*\}", ""
$content = $content -replace "socket\.isConnected", "true"

# 6?? Remove disconnectAirPods socket usage
$content = $content -replace "if \(!this::socket\.isInitialized\) return", ""
$content = $content -replace "socket\.close\(\)", ""

# 7?? Remove disconnectForCD socket usage
$content = $content -replace "if \(!this::socket\.isInitialized\) return", ""

# 8?? Remove onDestroy socket close
$content = $content -replace "socket\.close\(\)", ""

Set-Content $File $content

Write-Host "========================================="
Write-Host "BLE-only patch applied successfully."
Write-Host "Original backed up as AirPodsService.kt.bak"
Write-Host "========================================="