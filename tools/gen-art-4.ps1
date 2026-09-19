#requires -Version 5
# EchoFlow art batch 4: one more attempt at Rin, matching the soft rendering of the rest.
$ErrorActionPreference = 'Stop'

$api      = 'http://127.0.0.1:8188'
$outDir   = '.\design\prototype\assets'
$comfyOut = 'D:\ComfyUI\ComfyUI_windows_portable\ComfyUI\output'
$ckpt     = 'ponyDiffusionV6XL_v6.safetensors'
$DETAIL   = 'AddMicroDetails_pony.safetensors'

$NEG = 'score_6, score_5, score_4, worst quality, low quality, source_pony, source_furry, blurry, jpeg artifacts, text, watermark, signature, extra digits, bad hands, deformed, missing limbs, swimsuit, bikini, lingerie, cleavage, navel, bare legs, revealing clothes, fan service, flat color, poster, graphic design, duotone'

$POS = 'score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, soft cel shading, detailed rendering, upper body portrait, long black hair in a high ponytail, crimson eyes, composed confident expression with a small smirk, wearing a white kendo gi jacket with a dark indigo hakama and a chest protector, both hands resting on a bamboo shinai held in front of her, wooden kendo dojo interior, paper sliding doors, warm afternoon sunlight streaming in, dust motes in the light, soft warm color palette, cinematic depth of field'

$wf = [ordered]@{
  '4'  = @{ class_type = 'CheckpointLoaderSimple'; inputs = @{ ckpt_name = $ckpt } }
  '10' = @{ class_type = 'LoraLoader'; inputs = @{
              model = @('4', 0); clip = @('4', 1)
              lora_name = $DETAIL; strength_model = 0.55; strength_clip = 0.55 } }
  '6'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $POS; clip = @('10', 1) } }
  '7'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $NEG; clip = @('10', 1) } }
  '5'  = @{ class_type = 'EmptyLatentImage'; inputs = @{ width = 768; height = 1120; batch_size = 1 } }
  '3'  = @{ class_type = 'KSampler'; inputs = @{
              model = @('10', 0); seed = 101004; steps = 30; cfg = 7
              sampler_name = 'euler_ancestral'; scheduler = 'normal'
              positive = @('6', 0); negative = @('7', 0); latent_image = @('5', 0); denoise = 1.0 } }
  '8'  = @{ class_type = 'VAEDecode'; inputs = @{ samples = @('3', 0); vae = @('4', 2) } }
  '9'  = @{ class_type = 'SaveImage'; inputs = @{ filename_prefix = 'ef4/rin-portrait'; images = @('8', 0) } }
}

$body = @{ prompt = $wf; client_id = 'echoflow-batch4' } | ConvertTo-Json -Depth 24 -Compress
$r = Invoke-RestMethod -Uri "$api/prompt" -Method Post -Body $body -ContentType 'application/json'
$id = $r.prompt_id
Write-Output ("queued rin-portrait: {0}" -f $id)

$deadline = (Get-Date).AddMinutes(15)
while ((Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 5
  $hist = Invoke-RestMethod -Uri "$api/history/$id" -TimeoutSec 20
  if ($hist.PSObject.Properties.Name -contains $id) {
    $im = $hist.$id.outputs.'9'.images[0]
    $src = Join-Path (Join-Path $comfyOut $im.subfolder) $im.filename
    if (Test-Path $src) {
      Copy-Item $src (Join-Path $outDir 'rin-portrait.png') -Force
      Write-Output "OK rin-portrait"
    } else { Write-Output "MISSING $src" }
    break
  }
  Write-Output "  ... waiting"
}
