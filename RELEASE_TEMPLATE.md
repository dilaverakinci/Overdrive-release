## 🚗 BetterOverdrive {VERSION_TAG}

[TR] Moredrive projesi kapsamındaki en güncel geliştirmeleri ve optimizasyonları içeren **Debug** derlemesidir.  
[EN] **Debug** build containing the latest enhancements and optimizations from the Moredrive project.

---

### 🇹🇷 Türkçe Sürüm Notları

> ⚠️ **Kritik Mimari Not:** Kör nokta (Blind Spot) kamera akışı ve arka plan daemon servislerinin (UID 2000 / SurfaceControl) ADB `run-as` üzerinden sorunsuz çalışabilmesi için uygulama daima **Debug** modunda derlenir.

#### 🌟 Son Yenilikler ve İyileştirmeler:
- ⚡ **Sıfır Gecikmeli Bellek İçi Telemetri (Veri Dalgalanması & Çakışma Çözümü):**
  - Yerel HTTP loopback (`/api/vehicle/state`) sorguları native Compose ekranında kaldırılarak 100% in-process paylaşımlı bellek mimarisine geçildi.
  - Ekranda gözlemlenen "LOCKED <-> UNLOCKED" ve "206 km <-> 96 km" arasındaki ani veri zıplamaları ve yarış durumları (race-condition) tamamen engellendi.
  - Hız için mikro titreşim deadband filtresi (< 1.8 km/s veya P vitesi -> 0.0 km/s) ve araç dururken sıfır rejen koruması devreye alındı.
- 🚗 **Şasi Kartı & Zümrüt Yeşili Batarya Dolgusu (Navion Mimarisi):**
  - Navion'daki `ChassisVehicleBatteryView` görselleştirme mimarisi Compose Canvas (`nativeCanvas.saveLayer` ve `PorterDuff.Mode.SRC_ATOP`) ile uyarlandı.
  - Araç silüetinin içine taşma yapmadan, batarya doluluğuna (%SoC) göre arkadan öne yükselen zümrüt yeşili degrade dolgu, su seviyesi (waterline) çizgisi ve merkezde batarya yüzdesi eklendi.
- 🛞 **Gerçek Lastik Basınç ve Sıcaklık Telemetrisi (TPMS):**
  - Statik/sabit sahte değerler tamamen kaldırıldı.
  - Araçtan gelen çok katmanlı TPMS donanım metodları (`readTyrePressureSafe`, `readTyreTemperatureSafe`) ile her 4 tekerleğin anlık kPa ve °C değerleri gerçek zamanlı okundu.
- 🚪 **Canlı Kaput, Kapı, Cam ve Bagaj Sensörleri:**
  - Ön kaput (`getFrontEngineCoverState`), bagaj kapağı (`getBackDoorCurState`), kapı kilitleri (`getDoorLockStatus 1..4`) ve cam açıklıkları araç donanımından canlı çekilerek alt durum çubuğuna aktarıldı.
- 🔋 **Tüketim & Gerçekçi Menzil Revizyonu:**
  - **Son 50 km Tüketimi:** Doğrudan BYD araç donanımından (`getLast50KmPowerConsume`) orijinal fabrika verisi olarak okunur.
  - **Son Şarjdan İtibaren:** Araç şarjdan ayrıldığı anda referansı otomatik kilitler (`SinceChargeManager`); kat edilen km ve net tüketim oranını hesaplar.
  - **Aktif Seyahat:** Hareket başladığı andan itibaren sürüş süresi (dk) ve mesafesini (km) takip eder.
  - **Dinamik Gerçekçi Menzil:** Son şarj harcama trendine göre gerçekçi menzili hesaplar; eklenen "Sıfırla" butonu ile referans sıfırlanabilir.
- 📹 **Canlı Kamera Akış İyileştirmesi:** Canlı araç kamerası açıldığında kullanıcının seçim yapmasını beklemeden otomatik olarak ilk kamerayı anında yayına başlatır.
- 🎨 **Otomotiv Tasarım Uyumu:** Araç kontrol ekranındaki tüm kartlar, paneller ve butonlar `OverdriveTheme` tasarım sistemine uyarlandı.

---

### 🇬🇧 English Release Notes

> ⚠️ **Critical Architectural Note:** To ensure background daemon services (`fast_cam_capture` / UID 2000 / SurfaceControl) and Blind Spot camera streaming function correctly via ADB `run-as`, the application is always compiled in **Debug** mode.

#### 🌟 Recent Features & Improvements:
- ⚡ **Zero-Latency In-Memory Telemetry Engine:**
  - Replaced localhost HTTP loopback polling with 100% in-process shared memory binding in Compose.
  - Eliminated data toggling and race-condition flickering between LOCKED <-> UNLOCKED and 206 km <-> 96 km.
  - Enforced speed deadband (< 1.8 km/h or Park gear -> 0.0 km/h) and stationary zero-regen filtering.
- 🚗 **Chassis Silhouette & Emerald Green Battery Fill (Navion Architecture):**
  - Integrated top-down vehicle silhouette with dynamic Compose Canvas `nativeCanvas.saveLayer` using `PorterDuff.Mode.SRC_ATOP`.
  - Battery fills upwards proportional to SoC% with emerald green gradient, crisp waterline indicator, and centered bold %SoC without overflowing the vehicle outline.
- 🛞 **Real TPMS Tyre Pressure & Temperature Telemetry:**
  - Removed all hardcoded static values.
  - Probes live multi-method vehicle TPMS sensors (`readTyrePressureSafe`, `readTyreTemperatureSafe`) to display authentic corner kPa and °C.
- 🚪 **Live Hood, Doors, Windows & Trunk Sensors:**
  - Synchronized front engine cover (`getFrontEngineCoverState`), tailgate (`getBackDoorCurState`), door locks (`getDoorLockStatus 1..4`), and window positions directly from hardware HAL.
- 🔋 **Consumption & Dynamic Range Engine:**
  - **Hardware Last 50 km:** Direct retrieval of authentic factory metrics via BYD vehicle HAL (`getLast50KmPowerConsume`).
  - **Since Last Charge Tracking:** Automatically anchors baseline when charging session ends (`SinceChargeManager`), tracking precise distance driven and kWh/100km rate with persistent storage.
  - **Live Active Trip:** Real-time elapsed drive duration (min) and trip distance (km) tracked from the moment movement begins.
  - **Realistic Dynamic Range:** Calculated continuously from since-charge consumption, with an instant "Reset" button for on-demand baseline recalibration.
- 📹 **Instant Live Camera Streaming:** Automatically initializes and streams the primary camera upon opening the live view without requiring manual camera toggling.
- 🎨 **Automotive UI Theme Alignment:** Completely restyled all cards, stats, and dialogs on the Vehicle page to match `OverdriveTheme`.

---
**APK Dosyası / File:** `betteroverdrive.apk` ({APK_SIZE_MB} MB)  
**Derleme Tipi / Build Type:** Debug (arm64-v8a - BYD DiLink Android 10/12)
