#requires -Version 5
# 生成星语应用图标各密度 PNG（与 res/drawable/ic_launcher_fg.xml 的路径一致）
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$res = '.\src-android\EchoFlow-Android\app\src\main\res'

function P([double]$x, [double]$y, [double]$s) {
  return (New-Object System.Drawing.PointF([float]($x * $s), [float]($y * $s)))
}

function New-Icon([int]$size) {
  $bmp = New-Object System.Drawing.Bitmap($size, $size)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  [double]$s = $size / 108.0

  # 背景：线性渐变（左上亮紫 -> 右下深紫）
  $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
  $c1 = [System.Drawing.Color]::FromArgb(255, 58, 37, 96)
  $c2 = [System.Drawing.Color]::FromArgb(255, 21, 14, 40)
  $p1 = New-Object System.Drawing.PointF(0, 0)
  $p2 = New-Object System.Drawing.PointF([float]$size, [float]$size)
  $bg = [System.Drawing.Drawing2D.LinearGradientBrush]::new($p1, $p2, $c1, $c2)
  $g.FillRectangle($bg, $rect)
  $bg.Dispose()

  # 后气泡（左下，淡紫）
  $b1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 199, 168, 255))
  $g.FillEllipse($b1, [float](24*$s), [float](34*$s), [float](28*$s), [float](25*$s))
  $tri1 = @( (P 33.4 58.1 $s), (P 27.6 61.2 $s), (P 28.9 55.6 $s) )
  $g.FillPolygon($b1, [System.Drawing.PointF[]]$tri1)
  $b1.Dispose()

  # 前气泡（右上，亮白）
  $b2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 244, 242, 255))
  $g.FillEllipse($b2, [float](44*$s), [float](26*$s), [float](38*$s), [float](35*$s))
  $tri2 = @( (P 57.4 60.0 $s), (P 49.1 64.4 $s), (P 50.9 56.4 $s) )
  $g.FillPolygon($b2, [System.Drawing.PointF[]]$tri2)
  $b2.Dispose()

  # 四角星（右上角，暖橙）
  $b3 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 166, 87))
  $star = @(
    (P 78.0 20.0 $s), (P 80.6 28.4 $s), (P 87.0 29.0 $s), (P 80.6 31.6 $s),
    (P 78.0 38.0 $s), (P 75.4 31.6 $s), (P 69.0 29.0 $s), (P 75.4 28.4 $s)
  )
  $g.FillPolygon($b3, [System.Drawing.PointF[]]$star)
  $b3.Dispose()

  $g.Dispose()
  return $bmp
}

$dens = @{ 'mipmap-mdpi' = 48; 'mipmap-hdpi' = 72; 'mipmap-xhdpi' = 96; 'mipmap-xxhdpi' = 144; 'mipmap-xxxhdpi' = 192 }
foreach ($k in $dens.Keys) {
  $dir = Join-Path $res $k
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $bmp = New-Icon $dens[$k]
  $bmp.Save((Join-Path $dir 'ic_launcher.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  Write-Output ("  {0} -> {1}px" -f $k, $dens[$k])
}

$big = New-Icon 192
$big.Save((Join-Path $res 'drawable\app_icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose()

$big2 = New-Icon 512
$big2.Save((Join-Path $res 'drawable\ic_launcher_large.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$big2.Dispose()

Write-Output '  app_icon.png / ic_launcher_large.png updated'
