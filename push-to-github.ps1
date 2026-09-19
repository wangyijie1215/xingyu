# 推送到 GitHub
#
# 用法：
#   .\push-to-github.ps1 -Repo https://github.com/你的用户名/星语.git
#
# 为什么要用 Token 而不是密码：
#   GitHub 从 2021 年 8 月起就禁用了密码推送（会直接报 403）。
#   必须用 Personal Access Token（PAT）。
#
# 怎么拿 Token：
#   1. https://github.com/settings/tokens  →  Generate new token (classic)
#   2. Note 随便填，Expiration 建议 90 天
#   3. 勾选 scope：**只勾 repo**（私有仓库读写够用）
#   4. 生成后复制那串 ghp_xxx（**只显示一次**）
#
# 推送时用户名填你的 GitHub 用户名，密码位置粘 Token。
#
# 为什么建议先建私有仓库：
#   这个项目个人向内容不少。确认没问题再转公开（仓库 Settings 里一键切换）。

param(
    [Parameter(Mandatory = $true)]
    [string]$Repo,

    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host ""
Write-Host "== 推送前检查 ==" -ForegroundColor Cyan

# 1) 有没有敏感文件被跟踪
$dangerous = git ls-files | Where-Object { $_ -match '\.jks$|keystore\.properties$|\.apk$|^be.*\.txt$' }
if ($dangerous) {
    Write-Host "  [X] 发现不该提交的文件：" -ForegroundColor Red
    $dangerous | ForEach-Object { Write-Host "      $_" }
    Write-Host "      先修 .gitignore 并 git rm --cached，再重跑。" -ForegroundColor Yellow
    exit 1
}
Write-Host "  [OK] 无密钥库 / APK / 编译日志" -ForegroundColor Green

# 2) 有没有密钥泄漏
$leak = git ls-files |
    Where-Object { $_ -match '\.(java|md|gradle|properties|xml|py|ps1|json|yml|txt)$' } |
    ForEach-Object { Select-String -Path $_ -Pattern 'sk-[a-zA-Z0-9]{12,}','gh[pous]_[a-zA-Z0-9]{20,}' -ErrorAction SilentlyContinue }
if ($leak) {
    Write-Host "  [X] 发现疑似 API key：" -ForegroundColor Red
    $leak | Select-Object -First 5 | ForEach-Object { Write-Host "      $($_.Filename):$($_.LineNumber)" }
    exit 1
}
Write-Host "  [OK] 无 API key" -ForegroundColor Green

# 3) 工作区是否干净
$dirty = git status --porcelain
if ($dirty) {
    Write-Host "  [!] 有未提交的改动，先提交或 stash：" -ForegroundColor Yellow
    $dirty | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" }
    exit 1
}
Write-Host "  [OK] 工作区干净" -ForegroundColor Green

# 4) 设置 remote
$existing = git remote 2>$null
if ($existing -contains "origin") {
    Write-Host "  [i] 已有 origin，更新为：$Repo" -ForegroundColor Yellow
    git remote set-url origin $Repo
} else {
    git remote add origin $Repo
}

Write-Host ""
Write-Host "== 开始推送 ==" -ForegroundColor Cyan
Write-Host "  仓库: $Repo"
Write-Host "  分支: $Branch"
Write-Host ""
Write-Host "  提示：弹出验证时，用户名填 GitHub 用户名，密码位置粘 **Token**" -ForegroundColor Yellow
Write-Host ""

git push -u origin $Branch

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "  推送成功。" -ForegroundColor Green
} else {
    Write-Host "  推送失败。常见原因：" -ForegroundColor Red
    Write-Host "    · 用了账号密码而不是 Token（GitHub 已禁用密码推送）"
    Write-Host "    · Token 没有 repo 权限"
    Write-Host "    · 远程仓库非空（建仓库时别勾 README/gitignore）"
}
