#requires -Version 5
# Robust Android UI driver: fresh remote dump file every call + retry + ASCII-only source.
# CJK is passed as base64 (-EncodedText) because PowerShell 5.1 mis-decodes UTF-8 .ps1.
param(
  [string]$EncodedText,
  [string]$Selector,          # text=xxx | id=xxx | idlike=xxx
  [int]$Index = 0,
  [switch]$Dump,
  [switch]$Clear,             # force-stop the app
  [string]$Start,             # activity to start after clear, e.g. .MainActivity
  [switch]$Key,               # send keyevent, use -KeyCode
  [int]$KeyCode = 4,
  [int]$Settle = 3,
  [switch]$Shot,              # screencap to -ShotPath
  [string]$ShotPath
)
$ErrorActionPreference = 'Continue'
$adb = 'D:\android-toolchain\sdk\platform-tools\adb.exe'
$script:seq = [DateTime]::Now.Ticks % 100000

function Fresh-Dump {
  for ($i = 0; $i -lt 4; $i++) {
    $script:seq++
    $remote = "/sdcard/ef_$($script:seq).xml"
    $local = Join-Path $env:TEMP "ef_$($script:seq).xml"
    & $adb shell rm -f $remote *> $null
    Start-Sleep -Milliseconds 500
    & $adb shell uiautomator dump $remote *> $null
    Start-Sleep -Milliseconds 400
    & $adb pull $remote $local *> $null
    if ((Test-Path $local) -and ((Get-Item $local).Length -gt 200)) {
      try {
        return [xml](Get-Content $local -Encoding utf8)
      } catch {
        # 解析失败（文件被截断）→ 重试
      }
    }
    Start-Sleep -Milliseconds 900
  }
  return $null
}

if ($Clear) {
  & $adb shell am force-stop com.echoflow.chat *> $null
  Start-Sleep -Seconds 1
  if ($Start) {
    & $adb shell am start -n "com.echoflow.chat/$Start" *> $null
    Start-Sleep -Seconds $Settle
  }
}

if ($Key) {
  & $adb shell input keyevent $KeyCode
  Start-Sleep -Seconds $Settle
}

$x = Fresh-Dump
if ($x -eq $null) { exit 1 }

if ($Dump -or (-not $Selector -and -not $EncodedText -and -not $Key -and -not $Clear)) {
  $x.SelectNodes("//node[@text!='']") | ForEach-Object { Write-Output ("{0}`t{1}" -f $_.text, $_.bounds) }
  exit 0
}

$nodes = $null
if ($EncodedText) {
  $t = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($EncodedText))
  $nodes = $x.SelectNodes("//node[@text='$t']")
} elseif ($Selector) {
  if ($Selector.StartsWith('id=')) {
    $nodes = $x.SelectNodes("//node[@resource-id='com.echoflow.chat:id/$($Selector.Substring(3))']")
  } elseif ($Selector.StartsWith('idlike=')) {
    $nodes = $x.SelectNodes("//node[contains(@resource-id,'$($Selector.Substring(7))')]")
  } elseif ($Selector.StartsWith('text~')) {
    $nodes = $x.SelectNodes("//node[contains(@text,'$($Selector.Substring(5))')]")
  }
}

if ($nodes -eq $null -or $nodes.Count -eq 0) {
  Write-Output 'NOT-FOUND'
  exit 2
}

$nd = $nodes[[Math]::Min($Index, $nodes.Count - 1)]
if ($nd.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
  $cx = [int](([int]$matches[1] + [int]$matches[3]) / 2)
  $cy = [int](([int]$matches[2] + [int]$matches[4]) / 2)
  & $adb shell input tap $cx $cy
  Write-Output "TAPPED $cx $cy"
  Start-Sleep -Seconds $Settle
} else {
  Write-Output 'BAD-BOUNDS'
  exit 3
}

if ($Shot -and $ShotPath) {
  & $adb shell screencap -p /sdcard/efshot.png *> $null
  & $adb pull /sdcard/efshot.png $ShotPath *> $null
  Write-Output "SHOT $ShotPath"
}
