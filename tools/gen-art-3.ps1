#requires -Version 5
# EchoFlow art batch 3: fix the three assets that drifted from their brief.
# ASCII-only on purpose.
$ErrorActionPreference = 'Stop'

$api      = 'http://127.0.0.1:8188'
$outDir   = 'C:\Users\30828\.qclaw\dsh-next\_echoflow\design\prototype\assets'
$comfyOut = 'D:\ComfyUI\ComfyUI_windows_portable\ComfyUI\output'
$ckpt     = 'ponyDiffusionV6XL_v6.safetensors'

$NEG_COMMON = 'score_6, score_5, score_4, worst quality, low quality, source_pony, source_furry, blurry, jpeg artifacts, text, watermark, signature, extra digits, bad hands, deformed, missing limbs'
$NEG_MODEST = "$NEG_COMMON, swimsuit, bikini, lingerie, cleavage, navel, bare legs, revealing clothes, fan service, beach"

$ALICE = 'score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, long silver-white hair with soft blue highlights, amber eyes, light silver-blue ceremonial plate armor, white cape with gold trim, small pauldron, upper body, looking at viewer, night background with deep indigo sky and faint stars, soft rim light, cinematic'

function New-Workflow {
  param($Pos, $Neg, $W, $H, $Seed, $Prefix, $Lora, $LoraStrength, $Steps)
  return [ordered]@{
    '4'  = @{ class_type = 'CheckpointLoaderSimple'; inputs = @{ ckpt_name = $ckpt } }
    '10' = @{ class_type = 'LoraLoader'; inputs = @{
                model = @('4', 0); clip = @('4', 1)
                lora_name = $Lora; strength_model = $LoraStrength; strength_clip = $LoraStrength } }
    '6'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $Pos; clip = @('10', 1) } }
    '7'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $Neg; clip = @('10', 1) } }
    '5'  = @{ class_type = 'EmptyLatentImage'; inputs = @{ width = $W; height = $H; batch_size = 1 } }
    '3'  = @{ class_type = 'KSampler'; inputs = @{
                model = @('10', 0); seed = $Seed; steps = $Steps; cfg = 7
                sampler_name = 'euler_ancestral'; scheduler = 'normal'
                positive = @('6', 0); negative = @('7', 0); latent_image = @('5', 0); denoise = 1.0 } }
    '8'  = @{ class_type = 'VAEDecode'; inputs = @{ samples = @('3', 0); vae = @('4', 2) } }
    '9'  = @{ class_type = 'SaveImage'; inputs = @{ filename_prefix = "ef3/$Prefix"; images = @('8', 0) } }
  }
}

$DETAIL = 'AddMicroDetails_pony.safetensors'

$assets = @(
  # 1) Rin: must read as a kendo club captain, fully clothed, dojo interior
  @{ n='rin-portrait'; w=768; h=1120; s=101001; lora=$DETAIL; ls=0.5; st=28; neg=$NEG_MODEST;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, upper body portrait, long black hair tied in a high ponytail, sharp crimson eyes, serious confident expression, wearing a thick white kendo gi jacket and dark indigo hakama with a chest protector, both hands holding a bamboo shinai in front of her, wooden kendo dojo interior, dusty floor, paper sliding doors, warm lantern light from the side, dramatic side lighting, cinematic, cool and composed' },

  # 2) Hakuyo: must read as an astronomy senior on a rooftop with a telescope
  @{ n='hakuyo-portrait'; w=768; h=1120; s=101002; lora=$DETAIL; ls=0.5; st=28; neg=$NEG_COMMON;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, upper body portrait, short dark teal bob hair, round wire glasses, calm silver grey eyes, gentle neutral expression, oversized beige knit cardigan over a white shirt, holding an old folded star chart against her chest, standing on a rooftop observatory at night, large telescope behind her, deep starry sky and milky way, cool moonlight rim light, cinematic, quiet and intellectual' },

  # 3) Alice sad: the expression must actually read as sad
  @{ n='alice-sad'; w=768; h=1120; s=101003; lora=$DETAIL; ls=0.55; st=28; neg=$NEG_COMMON;
     p="$ALICE, very sad expression, eyebrows drawn together and raised inward, downcast wet eyes, looking down, downturned mouth, a single tear rolling on her cheek, quiet lonely melancholic mood, dim cold lighting, slumped shoulders" }
)

Write-Output ("Batch3: queueing {0} jobs..." -f $assets.Count)
$jobs = New-Object System.Collections.ArrayList
foreach ($a in $assets) {
  $wf = New-Workflow -Pos $a.p -Neg $a.neg -W $a.w -H $a.h -Seed $a.s -Prefix $a.n -Lora $a.lora -LoraStrength $a.ls -Steps $a.st
  $body = @{ prompt = $wf; client_id = 'echoflow-batch3' } | ConvertTo-Json -Depth 24 -Compress
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
Write-Output ("Batch3 finished: {0}/{1}" -f $done, $jobs.Count)
