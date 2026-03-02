$File = "android\app\src\main\java\me\kavishdevar\librepods\services\AirPodsService.kt"

if (-not (Test-Path $File)) {
    Write-Host "File not found."
    exit
}

$Backup = "$File.bak2"
Copy-Item $File $Backup -Force
Write-Host "Backup created: $Backup"

$content = Get-Content $File -Raw

# Regex to match annotated connectToSocket function
$pattern = "@SuppressLint\([\s\S]*?\)\s*fun connectToSocket\([\s\S]*?\n\s*}\n"

$replacement = @"
@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
fun connectToSocket(device: BluetoothDevice, manual: Boolean = false) {
    Log.d(TAG, "BLE-only mode active ? skipping L2CAP socket connection")
    this.device = device
    isConnectedLocally = true
    updateNotificationContent(
        true,
        config.deviceName,
        batteryNotification.getBattery()
    )
}
"@

$content = [regex]::Replace($content, $pattern, $replacement, "Singleline")

Set-Content $File $content

Write-Host "========================================="
Write-Host "FORCED BLE-only patch applied."
Write-Host "========================================="