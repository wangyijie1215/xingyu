#requires -Version 5
# Batch 5: the two remaining built-in character portraits (Elian, Yueling).
$ErrorActionPreference = 'Stop'

$api      = 'http://127.0.0.1:8188'
$outDir   = '.\design\prototype\assets'
$comfyOut = 'D:\ComfyUI\ComfyUI_windows_portable\ComfyUI\output'
$ckpt     = 'ponyDiffusionV6XL_v6.safetensors'
$DETAIL   = 'AddMicroDetails_pony.safetensors'

$NEG = 'score_6, score_5, score_4, worst quality, low quality, source_pony, source_furry, blurry, jpeg artifacts, text, watermark, signature, extra digits, bad hands, deformed, missing limbs, swimsuit, bikini, revealing clothes, child'

function New-Workflow {
  param($Pos, $W, $H, $Seed, $Prefix, $Steps)
  return [ordered]@{
    '4'  = @{ class_type = 'CheckpointLoaderSimple'; inputs = @{ ckpt_name = $ckpt } }
    '10' = @{ class_type = 'LoraLoader'; inputs = @{
                model = @('4', 0); clip = @('4', 1)
                lora_name = $DETAIL; strength_model = 0.5; strength_clip = 0.5 } }
    '6'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $Pos; clip = @('10', 1) } }
    '7'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $NEG; clip = @('10', 1) } }
    '5'  = @{ class_type = 'EmptyLatentImage'; inputs = @{ width = $W; height = $H; batch_size = 1 } }
    '3'  = @{ class_type = 'KSampler'; inputs = @{
                model = @('10', 0); seed = $Seed; steps = $Steps; cfg = 7
                sampler_name = 'euler_ancestral'; scheduler = 'normal'
                positive = @('6', 0); negative = @('7', 0); latent_image = @('5', 0); denoise = 1.0 } }
    '8'  = @{ class_type = 'VAEDecode'; inputs = @{ samples = @('3', 0); vae = @('4', 2) } }
    '9'  = @{ class_type = 'SaveImage'; inputs = @{ filename_prefix = "ef5/$Prefix"; images = @('8', 0) } }
  }
}

$assets = @(
  @{ n='elian-portrait'; w=768; h=1120; s=950001; st=28;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, upper body portrait, a woman in her early thirties, chestnut brown long hair loosely gathered at the back with a wooden hairpin, calm gentle hazel eyes, warm knowing half smile, dark knit sweater and long skirt, standing behind an old wooden bookshop counter, warm amber lamp light from the side, tall bookshelves blurred behind, dust motes in the light, serene mature atmosphere, soft warm color palette, cinematic depth of field' },

  @{ n='yueling-portrait'; w=768; h=1120; s=950002; st=28;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, upper body portrait, a mysterious young woman, very long straight black hair falling past her waist, deep violet eyes that faintly glow in the dark, pale skin, solemn quiet expression, wearing a slightly oversized black uniform jacket with embroidered pale patterns on the cuffs, holding up a small old lantern with a dim pale light, standing in a narrow old town alley at night, faint moonlit mist, cool violet and indigo palette, ethereal and melancholic, cinematic' }
)

Write-Output ("Batch5: {0} jobs" -f $assets.Count)
$jobs = New-Object System.Collections.ArrayList
foreach ($a in $assets) {
  $wf = New-Workflow -Pos $a.p -W $a.w -H $a.h -Seed $a.s -Prefix $a.n -Steps $a.st
  $body = @{ prompt = $wf; client_id = 'echoflow-batch5' } | ConvertTo-Json -Depth 24 -Compress
  try {
    $r = Invoke-RestMethod -Uri "$api/prompt" -Method Post -Body $body -ContentType 'application/json'
    [void]$jobs.Add(@{ id = $r.prompt_id; name = $a.n })
    Write-Output ("  queued: {0}" -f $a.n)
  } catch {
    Write-Output ("  QUEUE FAILED: {0} -> {1}" -f $a.n, $_.Exception.Message)
  }
  Start-Sleep -Milliseconds 250
}

$done = 0
$pending = $jobs
$deadline = (Get-Date).AddMinutes(25)
while ($pending.Count -gt 0 -and (Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 5
  $still = New-Object System.Collections.ArrayList
  foreach ($p in $pending) {
    try {
      $hist = Invoke-RestMethod -Uri "$api/history/$($p.id)" -TimeoutSec 20
      if ($hist.PSObject.Properties.Name -contains $p.id) {
        $imgs = $hist.$($p.id).outputs.'9'.images
        if ($imgs -and $imgs.Count -gt 0) {
          $im = $imgs[0]
          $src = Join-Path (Join-Path $comfyOut $im.subfolder) $im.filename
          $dst = Join-Path $outDir ($p.name + '.png')
          if (Test-Path $src) { Copy-Item $src $dst -Force; $done++; Write-Output ("  OK {0}" -f $p.name) }
          else { Write-Output ("  MISSING {0}" -f $src) }
        } else { Write-Output ("  NOOUT {0}" -f $p.name) }
      } else { [void]$still.Add($p) }
    } catch { [void]$still.Add($p) }
  }
  $pending = $still
  Write-Output ("  ... done {0}/{1}" -f $done, $jobs.Count)
}
Write-Output ("Batch5 finished: {0}/{1}" -f $done, $jobs.Count)
