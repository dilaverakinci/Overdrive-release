<#
.SYNOPSIS
    BetterOverdrive GitHub Release Publisher
    Compiles (or uses existing) debug APK, computes version from commit count,
    renames the APK to betteroverdrive.apk, and publishes it as a GitHub Release.
#>
param(
    [string]$Token = "",
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 1. Credentials
if (-not $Token) {
    if ($env:GITHUB_TOKEN) {
        $Token = $env:GITHUB_TOKEN
    } else {
        try {
            $gitCred = ("protocol=https`nhost=github.com`n" | git credential fill 2>$null)
            foreach ($line in $gitCred) {
                if ($line -like "password=*") {
                    $Token = $line.Substring(9).Trim()
                    break
                }
            }
        } catch {}

        if (-not $Token) {
            Add-Type -TypeDefinition @"
            using System;
            using System.Runtime.InteropServices;
            public class CredReader {
                [DllImport("advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
                public static extern bool CredRead(string target, int type, int reservedFlag, out IntPtr credentialPtr);
                [DllImport("advapi32.dll", EntryPoint = "CredFree", SetLastError = true)]
                public static extern void CredFree(IntPtr cred);

                [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
                public struct CREDENTIAL {
                    public int Flags;
                    public int Type;
                    public string TargetName;
                    public string Comment;
                    public long LastWritten;
                    public int CredentialBlobSize;
                    public IntPtr CredentialBlob;
                    public int Persist;
                    public int AttributeCount;
                    public IntPtr Attributes;
                    public string TargetAlias;
                    public string UserName;
                }

                public static string Read(string target) {
                    IntPtr credPtr;
                    if (CredRead(target, 1, 0, out credPtr)) {
                        CREDENTIAL cred = (CREDENTIAL)Marshal.PtrToStructure(credPtr, typeof(CREDENTIAL));
                        string pass = Marshal.PtrToStringUni(cred.CredentialBlob, cred.CredentialBlobSize / 2);
                        CredFree(credPtr);
                        return pass;
                    }
                    return null;
                }
            }
"@ -ErrorAction SilentlyContinue
            $Token = [CredReader]::Read("git:https://github.com")
        }
    }
}

if (-not $Token) {
    Write-Error "GitHub token bulunamadi! Lutfen -Token parametresi veya GITHUB_TOKEN ortam degiskeni ile token saglayin."
    exit 1
}

# 2. Commit Sayısı & Versiyon
$repoRoot = $PSScriptRoot
if (-not $repoRoot) { $repoRoot = Get-Location }

$commitCount = 0
try {
    $commitCount = (git -C $repoRoot rev-list --count upstream/main..HEAD 2>$null).Trim()
} catch {}

if (-not $commitCount -or $commitCount -eq 0) {
    $commitCount = (git -C $repoRoot rev-list --count HEAD).Trim()
}

$major = [math]::Floor([int]$commitCount / 10)
$minor = [int]$commitCount % 10
$versionName = "$major.$minor"
$versionTag = "v$versionName"
$releaseTitle = "BetterOverdrive v$versionName (Build $commitCount)"
Write-Host ">>> Belirlenen Versiyon: $versionTag ($versionName / Build $commitCount)" -ForegroundColor Cyan

# 3. APK Kontrolü veya Derleme (KRİTİK: Kör nokta / BSD ve ADB daemon servisleri için DAİMA assembleDebug kullanılmalıdır!)
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-arm64-v8a-debug.apk"
if (-not $SkipBuild) {
    Write-Host ">>> APK derleme baslatiliyor (assembleDebug - v$versionName / Build $commitCount)..." -ForegroundColor Yellow
    & "$repoRoot\gradlew.bat" assembleDebug -PoverdriveVersionName="$versionName" -PoverdriveVersionCode=$commitCount
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Derleme basarisiz oldu!"
        exit 1
    }
}

if (-not (Test-Path $apkPath)) {
    Write-Error "APK dosyasi bulunamadi: $apkPath"
    exit 1
}

$apkFileSize = (Get-Item $apkPath).Length
$apkSizeMB = [math]::Round($apkFileSize / 1MB, 2)
Write-Host ">>> Derlenmis APK: $apkPath ($apkSizeMB MB - Debug / Blind Spot Ready)" -ForegroundColor Green

# 4. Çift Dilli (Türkçe & İngilizce) Release Notları - UTF-8 Dosyasından Okuma
$templatePath = Join-Path $repoRoot "RELEASE_TEMPLATE.md"
if (Test-Path $templatePath) {
    $rawTemplate = [System.IO.File]::ReadAllText($templatePath, [System.Text.Encoding]::UTF8)
    $releaseBody = $rawTemplate.Replace('{VERSION_TAG}', $versionTag).Replace('{APK_SIZE_MB}', "$apkSizeMB")
} else {
    $releaseBody = "## BetterOverdrive $versionTag`n`n[TR] Debug derlemesidir.`n[EN] Debug build."
}

# 5. GitHub API İstemcisi Yapılandırması (.NET HttpClient ile tam UTF-8 garantisi)
$owner = "dilaverakinci"
$repo = "Overdrive-release"
$currentBranch = (git -C $repoRoot rev-parse --abbrev-ref HEAD).Trim()

Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromMinutes(10)
$httpClient.DefaultRequestHeaders.Add("User-Agent", "BetterOverdrive-Publisher")
$httpClient.DefaultRequestHeaders.Add("Authorization", "token $Token")
$httpClient.DefaultRequestHeaders.Add("Accept", "application/vnd.github.v3+json")

function Send-GitHubJson {
    param(
        [string]$Uri,
        [string]$Method,
        [object]$Payload
    )
    $httpMethod = [System.Net.Http.HttpMethod]::new($Method)
    $req = [System.Net.Http.HttpRequestMessage]::new($httpMethod, $Uri)
    if ($Payload) {
        $jsonStr = $Payload | ConvertTo-Json -Depth 10
        $utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($jsonStr)
        $req.Content = [System.Net.Http.ByteArrayContent]::new($utf8Bytes)
        $req.Content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/json; charset=utf-8")
    }
    $res = $httpClient.SendAsync($req).Result
    $respBody = $res.Content.ReadAsStringAsync().Result
    if (-not $res.IsSuccessStatusCode) {
        throw "GitHub API Hatasi ($($res.StatusCode)): $respBody"
    }
    if ($respBody) {
        return ($respBody | ConvertFrom-Json)
    }
    return $null
}

Write-Host ">>> GitHub Release kontrol ediliyor ($owner/$repo)..." -ForegroundColor Cyan

# Mevcut release var mı kontrol et
$existingRelease = $null
try {
    $getReq = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, "https://api.github.com/repos/$owner/$repo/releases/tags/$versionTag")
    $getRes = $httpClient.SendAsync($getReq).Result
    if ($getRes.IsSuccessStatusCode) {
        $existingRelease = ($getRes.Content.ReadAsStringAsync().Result | ConvertFrom-Json)
    }
} catch {}

$release = $null
if ($existingRelease) {
    Write-Host ">>> $versionTag surumu zaten mevcut, baslik ve Turkce/Ingilizce notlar UTF-8 ile guncelleniyor..." -ForegroundColor Yellow
    $patchPayload = @{
        name = $releaseTitle
        body = $releaseBody
    }
    $release = Send-GitHubJson -Uri "https://api.github.com/repos/$owner/$repo/releases/$($existingRelease.id)" -Method "PATCH" -Payload $patchPayload
    Write-Host ">>> Release basariyla guncellendi: $($release.html_url)" -ForegroundColor Green
} else {
    Write-Host ">>> Yeni $versionTag release olusturuluyor..." -ForegroundColor Cyan
    $createPayload = @{
        tag_name = $versionTag
        target_commitish = $currentBranch
        name = $releaseTitle
        body = $releaseBody
        draft = $false
        prerelease = $false
    }
    $release = Send-GitHubJson -Uri "https://api.github.com/repos/$owner/$repo/releases" -Method "POST" -Payload $createPayload
    Write-Host ">>> Release olusturuldu: $($release.html_url)" -ForegroundColor Green
}

# 6. APK Varlık (Asset) Yükleme (betteroverdrive.apk)
$uploadUrl = $release.upload_url -replace '\{\?name,label\}', '?name=betteroverdrive.apk'

# Varsa eski betteroverdrive.apk asset'ini sil
if ($release.assets) {
    foreach ($asset in $release.assets) {
        if ($asset.name -eq "betteroverdrive.apk") {
            Write-Host ">>> Eski betteroverdrive.apk kaldiriliyor (ID: $($asset.id))..." -ForegroundColor Yellow
            $delReq = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Delete, "https://api.github.com/repos/$owner/$repo/releases/assets/$($asset.id)")
            $null = $httpClient.SendAsync($delReq).Result
        }
    }
}

Write-Host ">>> betteroverdrive.apk yukleniyor ($apkSizeMB MB)..." -ForegroundColor Cyan

$fileStream = [System.IO.File]::OpenRead($apkPath)
$streamContent = [System.Net.Http.StreamContent]::new($fileStream)
$streamContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/vnd.android.package-archive")

try {
    $upRes = $httpClient.PostAsync($uploadUrl, $streamContent).Result
    if (-not $upRes.IsSuccessStatusCode) {
        $errBody = $upRes.Content.ReadAsStringAsync().Result
        Write-Error "Asset yukleme basarisiz ($($upRes.StatusCode)): $errBody"
        exit 1
    }
    Write-Host ">>> betteroverdrive.apk basariyla yuklendi!" -ForegroundColor Green
} finally {
    $streamContent.Dispose()
    $fileStream.Dispose()
    $httpClient.Dispose()
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "YAYINLAMA TAMAMLANDI!" -ForegroundColor Green
Write-Host "Surum: $versionTag" -ForegroundColor White
Write-Host "APK: betteroverdrive.apk ($apkSizeMB MB)" -ForegroundColor White
Write-Host "Release Sayfasi: $($release.html_url)" -ForegroundColor Cyan
Write-Host "Dogrudan Indirme Linki: https://github.com/$owner/$repo/releases/download/$versionTag/betteroverdrive.apk" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
