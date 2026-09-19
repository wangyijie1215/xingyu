#requires -Version 5
# Convert ComfyUI PNG outputs to web-friendly JPG in the prototype assets dir.
# ASCII-only (Windows PowerShell 5.1 + UTF-8 .ps1 without BOM issue).
param(
  [string]$Src = '.\design\prototype\assets',
  [int]$MaxW = 860,
  [int]$Quality = 88
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$enc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$ep = New-Object System.Drawing.Imaging.EncoderParameters(1)
$ep.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, [int64]$Quality)

$converted = 0
foreach ($f in Get-ChildItem $Src -File -Filter *.png) {
  $img = [System.Drawing.Image]::FromFile($f.FullName)
  $scale = [Math]::Min(1.0, $MaxW / $img.Width)
  $w = [int]($img.Width * $scale); $h = [int]($img.Height * $scale)
  $bmp = New-Object System.Drawing.Bitmap($w, $h)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.Clear([System.Drawing.Color]::FromArgb(255, 11, 11, 20))
  $g.DrawImage($img, 0, 0, $w, $h)
  $out = [System.IO.Path]::ChangeExtension($f.FullName, '.jpg')
  $bmp.Save($out, $enc, $ep)
  $g.Dispose(); $bmp.Dispose(); $img.Dispose()
  Remove-Item $f.FullName -Force
  $converted++
  Write-Output ("  converted: {0} -> {1}x{2}" -f $f.Name, $w, $h)
}
Write-Output ("Done. {0} file(s) converted." -f $converted)
Get-ChildItem $Src -File | Sort-Object Name | Select-Object Name, @{n='KB';e={[math]::Round($_.Length/1KB,0)}} | Format-Table -AutoSize
