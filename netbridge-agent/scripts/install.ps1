param(
    [string]$InstallDir = "C:\Program Files\NetBridgeAgent",
    [string]$BinarySource = ".\netbridge-agent.exe",
    [string]$ConfigSource = ".\configs\agent.yaml",
    [string]$ServiceName = "NetBridgeAgent"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BinarySource)) {
    throw "Agent binary not found: $BinarySource"
}

if (-not (Test-Path $ConfigSource)) {
    throw "Agent config not found: $ConfigSource"
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $InstallDir "configs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $InstallDir "data") | Out-Null

Copy-Item $BinarySource (Join-Path $InstallDir "netbridge-agent.exe") -Force
Copy-Item $ConfigSource (Join-Path $InstallDir "configs\agent.yaml") -Force

$exePath = Join-Path $InstallDir "netbridge-agent.exe"
$configPath = Join-Path $InstallDir "configs\agent.yaml"
$serviceCommand = "`"$exePath`" -config `"$configPath`""

try {
    sc.exe stop $ServiceName | Out-Null
} catch {
}

try {
    sc.exe delete $ServiceName | Out-Null
} catch {
}

sc.exe create $ServiceName binPath= $serviceCommand start= auto DisplayName= "NetBridge Agent" | Out-Null
sc.exe start $ServiceName | Out-Null

Write-Host "Installed and started $ServiceName"
