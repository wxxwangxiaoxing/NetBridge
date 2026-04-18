param(
    [string]$InstallDir = "C:\Program Files\NetBridgeAgent",
    [string]$ServiceName = "NetBridgeAgent"
)

$ErrorActionPreference = "Stop"

try {
    sc.exe stop $ServiceName | Out-Null
} catch {
}

try {
    sc.exe delete $ServiceName | Out-Null
} catch {
}

if (Test-Path $InstallDir) {
    Remove-Item -LiteralPath $InstallDir -Recurse -Force
}

Write-Host "Uninstalled $ServiceName"
