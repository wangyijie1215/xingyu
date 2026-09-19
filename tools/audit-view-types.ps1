#requires -Version 5
# 审计布局声明类型与 Java findViewById 类型是否冲突（曾出过一次 ClassCastException）
$ErrorActionPreference = 'Stop'
$root = '.\src-android\EchoFlow-Android\app\src\main'

$map = @{
  'ImageView' = 'ImageView'; 'TextView' = 'TextView'; 'MaterialTextView' = 'TextView';
  'Button' = 'Button'; 'MaterialButton' = 'Button'; 'EditText' = 'EditText';
  'TextInputEditText' = 'EditText'; 'ProgressBar' = 'ProgressBar'; 'RecyclerView' = 'RecyclerView';
  'View' = 'View'; 'LinearLayout' = 'LinearLayout'; 'FrameLayout' = 'FrameLayout'
}

function Normalize($t) {
  if ($map.ContainsKey($t)) { return $map[$t] }
  if ($t -like '*TextView') { return 'TextView' }
  if ($t -like '*ImageView') { return 'ImageView' }
  if ($t -like '*EditText') { return 'EditText' }
  if ($t -like '*Button') { return 'Button' }
  return $t
}

# 1) 收集每个 id 的真实控件类型（正则，避开 XML 命名空间问题）
$decl = @{}
foreach ($l in Get-ChildItem "$root\res\layout" -File -Filter *.xml) {
  $txt = Get-Content $l.FullName -Raw -Encoding utf8
  foreach ($m in [regex]::Matches($txt, '<(\w+(?:\.\w+)*)[^>]*?android:id="@\+id/(\w+)"')) {
    $decl[$m.Groups[2].Value] = @{ type = $m.Groups[1].Value; file = $l.Name }
  }
}
Write-Output ("扫描到 " + $decl.Count + " 个布局 id")

# 2) 逐个 Java 文件对比
$bad = 0
foreach ($j in Get-ChildItem "$root\java\com\echoflow\chat" -File -Filter *.java) {
  $lines = Get-Content $j.FullName -Encoding utf8

  $vars = @{}
  foreach ($ln in $lines) {
    if ($ln -match '^\s*(?:private|protected|public)?\s*(?:final\s+)?([A-Z]\w+)\s+([^;=]+);') {
      $t = $matches[1]
      foreach ($v in ($matches[2] -split ',')) {
        $name = ($v -replace '[\s=].*$', '').Trim()
        if ($name -match '^\w+$') { $vars[$name] = $t }
      }
    }
  }

  for ($i = 0; $i -lt $lines.Count; $i++) {
    $m = [regex]::Match($lines[$i], '(\w+)\s*=\s*(?:\((\w+)\)\s*)?findViewById\(R\.id\.(\w+)\)')
    if (-not $m.Success) { continue }
    $var = $m.Groups[1].Value
    $cast = $m.Groups[2].Value
    $id = $m.Groups[3].Value
    if (-not $decl.ContainsKey($id)) { continue }
    $actual = Normalize $decl[$id].type
    $code = $null
    if ($cast) { $code = Normalize $cast }
    elseif ($vars.ContainsKey($var)) { $code = Normalize $vars[$var] }
    if (-not $code) { continue }
    if ($actual -ne $code) {
      Write-Output ("冲突: {0}:{1}  id={2}  布局={3}  代码={4}" -f $j.Name, ($i + 1), $id, $actual, $code)
      $bad++
    }
  }
}
if ($bad -eq 0) { Write-Output '未发现布局/代码类型冲突' }
