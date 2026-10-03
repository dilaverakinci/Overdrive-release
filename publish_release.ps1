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
if (-not (Test-Path $apkPath) -and -not $SkipBuild) {
    Write-Host ">>> APK bulunamadi, derleme baslatiliyor (assembleDebug)..." -ForegroundColor Yellow
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

# 4. Çift Dilli (Türkçe & İngilizce) Release Notları
$releaseBody = @"
## 🚗 BetterOverdrive $versionTag

[TR] Moredrive projesi kapsamındaki en güncel geliştirmeleri ve optimizasyonları içeren **Debug** derlemesidir.  
[EN] **Debug** build containing the latest enhancements and optimizations from the Moredrive project.

---

### 🇹🇷 Türkçe Sürüm Notları

> ⚠️ **Kritik Mimari Not:** Kör nokta (Blind Spot) kamera akışı ve arka plan daemon servislerinin (UID 2000 / SurfaceControl) ADB \`run-as\` üzerinden sorunsuz çalışabilmesi için uygulama daima **Debug** modunda derlenir.

#### 🌟 Son Yenilikler ve İyileştirmeler:
- ⚡ **BYD Doğrudan Kablosuz ADB Motoru:** Araç multimedya ekranı üzerinden (ts-framework IPC) tek tıkla kablosuz ADB etkinleştirme ve port 5555 yönetimi.
- 🩺 **Gelişmiş CAN-Bus Teşhisleri:** 9-ECU canlı telemetri monitörü, BMS hücre voltaj dengesizliği (Δ) takibi ve OBD-II DTC arıza kodları tarayıcısı/silicisi.
- 📐 **HUD & Ekran Optimizasyonu:** Teşhisler sayfasında Ağ, Depolama, Kamera ve Batarya kartlarının kenar boşlukları otomotiv ekranlarına özel olarak sıkılaştırıldı.
- 🌐 **Gösterge Paneli Uzaktan Erişim (PWA) Düzeltmesi:** Uzaktan erişim kartı genişletildiğinde sayfanın en alta kaydırılabilmesi için alt boşluk payı ve otomatik kaydırma eklendi.
- 🗺️ **Türkiye EV Şarj Rehberi & Haritası:** 22.000+ şarj istasyonu desteği ve Navion akıllı oturum düzenleyicisi.
- 🔄 **Akıllı Otomatik Güncelleyici (AppUpdater):** Doğrudan \`dilaverakinci/Overdrive-release\` reposuna bağlandı; eski \`51.8\` sürümünden geçiş için tolerans kuralları eklendi.

---

### 🇬🇧 English Release Notes

> ⚠️ **Critical Architectural Note:** To ensure background daemon services (\`fast_cam_capture\` / UID 2000 / SurfaceControl) and Blind Spot camera streaming function correctly via ADB \`run-as\`, the application is always compiled in **Debug** mode.

#### 🌟 Recent Features & Improvements:
- ⚡ **BYD Direct Wireless ADB Engine:** One-click wireless ADB activation and port 5555 management directly from the vehicle infotainment display (ts-framework IPC).
- 🩺 **Advanced CAN-Bus Diagnostics:** 9-ECU live telemetry monitor, BMS cell voltage imbalance (Δ) tracking, and OBD-II DTC fault code scanner/clearing.
- 📐 **HUD & Display Optimization:** Margins and paddings for Network, Storage, Camera, and Battery cards on the Diagnostics page tightened specifically for automotive displays.
- 🌐 **Dashboard Remote Access (PWA) Fix:** Added bottom padding and auto-scroll handling so the page scrolls completely to the bottom when the remote access card is expanded.
- 🗺️ **Turkey EV Charging Guide & Map:** Support for 22,000+ EV charging stations and Navion intelligent session organizer.
- 🔄 **Smart In-App Updater (AppUpdater):** Directed updates to \`dilaverakinci/Overdrive-release\` with backwards compatibility tolerance for upgrading from upstream \`51.8\`.

---
**APK Dosyası / File:** \`betteroverdrive.apk\` ($apkSizeMB MB)  
**Derleme Tipi / Build Type:** Debug (arm64-v8a - BYD DiLink Android 10/12)
"@

# 5. GitHub Release Oluşturma / Güncelleme
$owner = "dilaverakinci"
$repo = "Overdrive-release"
$currentBranch = (git -C $repoRoot rev-parse --abbrev-ref HEAD).Trim()

$headers = @{
    "Authorization" = "token $Token"
    "User-Agent" = "BetterOverdrive-Publisher"
    "Accept" = "application/vnd.github.v3+json"
}

Write-Host ">>> GitHub Release olusturuluyor ($owner/$repo)..." -ForegroundColor Cyan

# Mevcut release var mı kontrol et
$existingRelease = $null
try {
    $existingRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/$versionTag" -Headers $headers -Method Get -ErrorAction SilentlyContinue
} catch {}

$release = $null
if ($existingRelease) {
    Write-Host ">>> $versionTag surumu zaten mevcut, baslik ve notlar guncelleniyor..." -ForegroundColor Yellow
    $updatePayload = @{
        name = $releaseTitle
        body = $releaseBody
    } | ConvertTo-Json
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($updatePayload)
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/$($existingRelease.id)" -Headers $headers -Method Patch -Body $bodyBytes -ContentType "application/json; charset=utf-8"
} else {
    $createPayloadJson = @{
        tag_name = $versionTag
        target_commitish = $currentBranch
        name = $releaseTitle
        body = $releaseBody
        draft = $false
        prerelease = $false
    } | ConvertTo-Json
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($createPayloadJson)

    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases" -Headers $headers -Method Post -Body $bodyBytes -ContentType "application/json; charset=utf-8"
    Write-Host ">>> Release olusturuldu: $($release.html_url)" -ForegroundColor Green
}

# 6. APK Varlık (Asset) Yükleme (betteroverdrive.apk)
$uploadUrl = $release.upload_url -replace '\{\?name,label\}', '?name=betteroverdrive.apk'

# Varsa eski betteroverdrive.apk asset'ini sil
if ($release.assets) {
    foreach ($asset in $release.assets) {
        if ($asset.name -eq "betteroverdrive.apk") {
            Write-Host ">>> Eski betteroverdrive.apk asset'i kaldiriliyor (ID: $($asset.id))..." -ForegroundColor Yellow
            Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($asset.id)" -Headers $headers -Method Delete
        }
    }
}

Write-Host ">>> betteroverdrive.apk yukleniyor ($apkSizeMB MB)..." -ForegroundColor Cyan

# HttpClient ile streaming upload (Bellek tasarrufu ve buyuk dosya guvenligi)
Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromMinutes(10)
$httpClient.DefaultRequestHeaders.Add("User-Agent", "BetterOverdrive-Publisher")
$httpClient.DefaultRequestHeaders.Add("Authorization", "token $Token")
$httpClient.DefaultRequestHeaders.Add("Accept", "application/vnd.github.v3+json")

$fileStream = [System.IO.File]::OpenRead($apkPath)
$streamContent = [System.Net.Http.StreamContent]::new($fileStream)
$streamContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/vnd.android.package-archive")

try {
    $response = $httpClient.PostAsync($uploadUrl, $streamContent).Result
    if (-not $response.IsSuccessStatusCode) {
        $errorBody = $response.Content.ReadAsStringAsync().Result
        Write-Error "Asset yukleme basarisiz ($($response.StatusCode)): $errorBody"
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
Write-Host "🎉 YAYINLAMA TAMAMLANDI!" -ForegroundColor Green
Write-Host "Sürüm: $versionTag" -ForegroundColor White
Write-Host "APK: betteroverdrive.apk ($apkSizeMB MB)" -ForegroundColor White
Write-Host "Release Sayfası: $($release.html_url)" -ForegroundColor Cyan
Write-Host "Doğrudan İndirme Linki: https://github.com/$owner/$repo/releases/download/$versionTag/betteroverdrive.apk" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
