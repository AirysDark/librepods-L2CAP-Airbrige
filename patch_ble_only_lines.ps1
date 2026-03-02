$File = "android\app\src\main\java\me\kavishdevar\librepods\services\AirPodsService.kt"

if (-not (Test-Path $File)) {
    Write-Host "File not found."
    exit
}

$Backup = "$File.bak3"
Copy-Item $File $Backup -Force
Write-Host "Backup created: $Backup"

$lines = Get-Content $File

$start = 2391 - 1   # zero-based index
$end   = 2556 - 2   # stop right before disconnectForCD

$before = $lines[0..($start-1)]
$after  = $lines[($end+1)..($lines.Length-1)]

$replacement = @(
"    @SuppressLint(""MissingPermission"", ""UnspecifiedRegisterReceiverFlag"")"
"    fun connectToSocket(device: BluetoothDevice, manual: Boolean = false) {"
"        Log.d(TAG, ""BLE-only mode active ? skipping L2CAP socket connection"")"
"        this.device = device"
"        isConnectedLocally = true"
"        updateNotificationContent("
"            true,"
"            config.deviceName,"
"            batteryNotification.getBattery()"
"        )"
"    }"
)

$newContent = $before + $replacement + $after

Set-Content $File $newContent

Write-Host "========================================="
Write-Host "connectToSocket() successfully replaced."
Write-Host "========================================="