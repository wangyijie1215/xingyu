#requires -Version 5
# EchoFlow art asset batch (ComfyUI / Pony V6 XL + AddMicroDetails LoRA)
# ASCII-only on purpose: Windows PowerShell 5.1 mis-parses UTF-8 .ps1 without BOM.
$ErrorActionPreference = 'Stop'

$api      = 'http://127.0.0.1:8188'
$outDir   = 'C:\Users\30828\.qclaw\dsh-next\_echoflow\design\prototype\assets'
$comfyOut = 'D:\ComfyUI\ComfyUI_windows_portable\ComfyUI\output'
$ckpt     = 'ponyDiffusionV6XL_v6.safetensors'

$NEG = 'score_6, score_5, score_4, worst quality, low quality, source_pony, source_furry, blurry, jpeg artifacts, text, watermark, signature, extra digits, bad hands, deformed, missing limbs'

$ALICE = 'score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, long silver-white hair with soft blue highlights, amber eyes, light silver-blue ceremonial plate armor, white cape with gold trim, small pauldron, upper body, looking at viewer, night background with deep indigo sky and faint stars, soft rim light, cinematic'

function New-Workflow {
  param($Pos, $W, $H, $Seed, $Prefix, $Lora, $LoraStrength)
  return [ordered]@{
    '4'  = @{ class_type = 'CheckpointLoaderSimple'; inputs = @{ ckpt_name = $ckpt } }
    '10' = @{ class_type = 'LoraLoader'; inputs = @{
                model = @('4', 0); clip = @('4', 1)
                lora_name = $Lora; strength_model = $LoraStrength; strength_clip = $LoraStrength } }
    '6'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $Pos; clip = @('10', 1) } }
    '7'  = @{ class_type = 'CLIPTextEncode'; inputs = @{ text = $NEG; clip = @('10', 1) } }
    '5'  = @{ class_type = 'EmptyLatentImage'; inputs = @{ width = $W; height = $H; batch_size = 1 } }
    '3'  = @{ class_type = 'KSampler'; inputs = @{
                model = @('10', 0); seed = $Seed; steps = 32; cfg = 7
                sampler_name = 'euler_ancestral'; scheduler = 'normal'
                positive = @('6', 0); negative = @('7', 0); latent_image = @('5', 0); denoise = 1.0 } }
    '8'  = @{ class_type = 'VAEDecode'; inputs = @{ samples = @('3', 0); vae = @('4', 2) } }
    '9'  = @{ class_type = 'SaveImage'; inputs = @{ filename_prefix = "ef/$Prefix"; images = @('8', 0) } }
  }
}

$DETAIL = 'AddMicroDetails_pony.safetensors'
$ANIME  = 'RealisticAnimeStyle_pony.safetensors'

$assets = @(
  @{ n='shrine-water'; w=832; h=1216; s=778202; lora=$DETAIL; ls=0.6;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, scenery, japanese shrine water pavilion, chozuya, stone water basin with bamboo ladle, falling water, moss, stone lanterns, cherry blossom petals on water, soft morning light, mist, highly detailed anime background art, atmospheric' },

  @{ n='omikuji'; w=1024; h=1024; s=778203; lora=$DETAIL; ls=0.6;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, object focus, japanese omikuji fortune box, wooden hexagonal cylinder with red and gold ornaments, white paper fortune slips sticking out, on a wooden shrine counter, warm lantern light, dark background, still life, highly detailed' },

  @{ n='omamori'; w=1024; h=1024; s=778204; lora=$DETAIL; ls=0.6;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, object focus, japanese omamori charm, small brocade pouch in deep violet silk with gold woven pattern, braided cord and tassel, lavender and gold color scheme, floating against dark night background, soft glow, product still life, highly detailed fabric texture' },

  @{ n='ema'; w=1024; h=1024; s=778205; lora=$DETAIL; ls=0.45;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, object focus, japanese ema wooden wishing plaque, small pentagonal cypress wood board with painted star and moon motif, red silk cord through top hole, blank surface, warm lantern light, dark wooden shrine rack behind, still life, highly detailed wood grain' },

  @{ n='fortune-scroll'; w=1216; h=832; s=778206; lora=$DETAIL; ls=0.3;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, object focus, unrolled japanese washi paper fortune slip, blank aged cream paper with subtle fibers and red seals on the edges, lying on dark wooden planks, cherry blossom petal resting on the paper, warm lamp light from the side, top down view, highly detailed paper texture, no writing' },

  @{ n='ema-rack'; w=1216; h=832; s=778207; lora=$DETAIL; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, scenery, japanese shrine ema rack, hundreds of small wooden wishing plaques hanging in rows, red torii and lanterns in the background, soft evening light, cherry blossom petals falling, shallow depth of field, highly detailed anime background art' },

  @{ n='alice-happy'; w=832; h=1216; s=900001; lora=$DETAIL; ls=0.55;
     p="$ALICE, bright happy smile, open mouth, sparkling eyes, joyful, warm expression" },
  @{ n='alice-shy'; w=832; h=1216; s=900001; lora=$DETAIL; ls=0.55;
     p="$ALICE, heavy blush, embarrassed, looking away, one hand raised near her cheek, shy expression, closed mouth, flustered" },
  @{ n='alice-sad'; w=832; h=1216; s=900001; lora=$DETAIL; ls=0.55;
     p="$ALICE, sad expression, downcast eyes, slightly trembling, tear at the corner of one eye, quiet and lonely mood, soft melancholy lighting" },
  @{ n='alice-tired'; w=832; h=1216; s=900001; lora=$DETAIL; ls=0.55;
     p="$ALICE, tired expression, half closed sleepy eyes, faint exhale, one hand rubbing her neck, exhausted but calm, late night mood" },

  @{ n='rin-portrait'; w=832; h=1216; s=910001; lora=$DETAIL; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, long black hair in a high ponytail, sharp crimson eyes, white kendo gi and dark indigo hakama, bamboo shinai resting on her shoulder, confident smirk, school dojo interior at dusk, dramatic side light, cinematic' },
  @{ n='hakuyo-portrait'; w=832; h=1216; s=910002; lora=$DETAIL; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, short dark teal bob hair, round glasses, calm silver-grey eyes, oversized beige cardigan over a school uniform, holding a star chart, rooftop observatory at night, starry sky, cool moonlight, serene expression, cinematic' },
  @{ n='rocco-portrait'; w=832; h=1216; s=910003; lora=$DETAIL; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, 1girl, solo, masterpiece, best quality, very aesthetic, absurdres, ash blonde hair in twin braids, green eyes, brass goggles pushed up on her forehead, dark canvas mechanic coveralls with rolled sleeves, grease smudge on one cheek, tiny smile, cluttered workshop with warm lamps, brass machinery, cinematic' },

  @{ n='campus-autumn'; w=1216; h=832; s=920001; lora=$ANIME; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, scenery, japanese university campus in late autumn, ginkgo trees turned golden, long pathway of fallen leaves, old brick building, soft afternoon sunlight through branches, warm nostalgic atmosphere, highly detailed anime background art' },
  @{ n='chat-bg-alice'; w=1216; h=832; s=930001; lora=$DETAIL; ls=0.5;
     p='score_9, score_8_up, score_7_up, score_6_up, source_anime, no humans, scenery, silver moon knight city at night, white marble castle towers with silver banners, rain wet cobblestone plaza, warm lantern glow reflecting on the ground, deep indigo sky with a large moon, distant mountain silhouettes, melancholic and beautiful, highly detailed anime background art, cinematic' }
)

Write-Output ("Queueing {0} jobs..." -f $assets.Count)
$jobs = New-Object System.Collections.ArrayList
foreach ($a in $assets) {
  $wf = New-Workflow -Pos $a.p -W $a.w -H $a.h -Seed $a.s -Prefix $a.n -Lora $a.lora -LoraStrength $a.ls
  $body = @{ prompt = $wf; client_id = 'echoflow-batch' } | ConvertTo-Json -Depth 24 -Compress
  try {
    $r = Invoke-RestMethod -Uri "$api/prompt" -Method Post -Body $body -ContentType 'application/json'
    [void]$jobs.Add(@{ id = $r.prompt_id; name = $a.n })
    Write-Output ("  queued: {0}" -f $a.n)
  } catch {
    Write-Output ("  QUEUE FAILED: {0} -> {1}" -f $a.n, $_.Exception.Message)
  }
  Start-Sleep -Milliseconds 250
}

Write-Output ("All queued: {0}. Waiting..." -f $jobs.Count)
$done = 0
$pending = $jobs
$deadline = (Get-Date).AddMinutes(70)
while ($pending.Count -gt 0 -and (Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 6
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

Write-Output ("Finished: {0}/{1}" -f $done, $jobs.Count)
Get-ChildItem $outDir -File | Sort-Object Name | Select-Object Name, @{n='KB';e={[math]::Round($_.Length/1KB,0)}} | Format-Table -AutoSize
