#requires -Version 5
# Tap an Android UI element by its text. ASCII-only source (PowerShell 5.1 mangles
# UTF-8 .ps1 without BOM), so the search text is passed via -EncodedText (base64 UTF-8).
param(
  [string]$EncodedText = '',
  [string]$PlainText = '',
  [int]$Index = 0
)
$ErrorActionPreference = 'Stop'
$adb = 'D:\android-toolchain\sdk\platform-tools\adb.exe'
$tmp = Join-Path $env:TEMP 'efui.xml'

if ($EncodedText -ne '') {
  $Text = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($EncodedText))
} else {
  $Text = $PlainText
}

& $adb shell uiautomator dump /sdcard/efui.xml 2>&1 | Out-Null
& $adb pull /sdcard/efui.xml $tmp 2>&1 | Out-Null

[xml]$x = Get-Content $tmp -Encoding utf8
$nodes = $x.SelectNodes("//node[@text='$Text']")
if ($nodes.Count -eq 0) {
  Write-Output "NOT-FOUND"
  $x.SelectNodes("//node[@text!='']") | ForEach-Object { $_.text } | Select-Object -First 25 | ForEach-Object { Write-Output ("  have: " + $_) }
  exit 1
}
$n = $nodes[[Math]::Min($Index, $nodes.Count - 1)]
$b = $n.bounds
if ($b -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
  $cx = [int](([int]$matches[1] + [int]$matches[3]) / 2)
  $cy = [int](([int]$matches[2] + [int]$matches[4]) / 2)
  & $adb shell input tap $cx $cy
  Write-Output "TAPPED $cx $cy"
} else {
  Write-Output "BAD-BOUNDS $b"
  exit 1
}
