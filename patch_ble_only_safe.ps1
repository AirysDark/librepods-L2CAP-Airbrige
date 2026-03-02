$File = Get-ChildItem -Recurse -Filter AirPodsService.kt |
Where-Object { $_.FullName -like "*services*" } |
Select-Object -First 1 -ExpandProperty FullName

if (-not $File) {
    Write-Host "AirPodsService.kt not found."
    exit
}

Write-Host "Found file at: $File"

$Backup = "$File.bak"
Copy-Item $File $Backup -Force
Write-Host "Backup created: $Backup"

$content = Get-Content $File -Raw

# Replace connectToSocket with BLE-only stub
$content = [regex]::Replace(
    $content,
    "fun connectToSocket\([\s\S]*?\n\s*}\n",
@"
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
"@,
    "Singleline"
)

# Prevent socket crash checks
$content = $content -replace "if \(!::socket\.isInitialized\) \{\s*return\s*\}", ""
$content = $content -replace "socket\.isConnected", "true"

Set-Content $File $content

Write-Host "========================================="
Write-Host "BLE-only SAFE patch applied successfully."
Write-Host "========================================="