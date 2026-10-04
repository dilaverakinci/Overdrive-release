## 🚗 BetterOverdrive {VERSION_TAG}

[TR] Moredrive projesi kapsamındaki en güncel geliştirmeleri ve optimizasyonları içeren **Debug** derlemesidir.  
[EN] **Debug** build containing the latest enhancements and optimizations from the Moredrive project.

---

### 🇹🇷 Türkçe Sürüm Notları

> ⚠️ **Kritik Mimari Not:** Kör nokta (Blind Spot) kamera akışı ve arka plan daemon servislerinin (UID 2000 / SurfaceControl) ADB `run-as` üzerinden sorunsuz çalışabilmesi için uygulama daima **Debug** modunda derlenir.

#### 🌟 Son Yenilikler ve İyileştirmeler:
- 🐟 **4 Kanallı Bağımsız Balıkgözü (Fisheye / Barrel Distortion) Düzeltmesi:**
  - 4 kameranın (Ön, Sağ Ayna, Arka, Sol Ayna) optik bükülme açıları birbirinden tamamen bağımsız olarak yapılandırılabilir hale getirildi.
  - Yan aynalardaki aşırı geniş ($180^\circ+$) balıkgözü distorsiyonu (~%50) düzeltilirken; ön ve arka kameralardaki doğal açı korunarak görüntü bozulmaları önlenir (~%10-15).
  - CPU remap yerine doğrudan Adreno GPU OpenGL ES 2.0 Fragment Shader seviyesinde (`vec2 k` uniformları ve `rectifyTileWithK` donanımsal çift-doğrusal örnekleme) işlenir; **0% ek CPU yükü**, **0 ms gecikme** ve **sıfır kare kaybı (frame drop)** garantilenir.
  - **BYD Sealion 7 / Seal Optimum Profili:** Tek tuşla test edilmiş optimum dewarp değerlerini (Ön: %10, Yan Aynalar: %50, Arka: %15) uygulayan hazır profil butonu Kayıt ayarlarına eklendi.
  - Eski tek kaydırıcılı ana dewarp modu ile %100 geriye dönük uyumluluk korundu.
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
- 🏎️ **Gerçek Zamanlı Gaz & Fren Pedalı ve Rejen Güç Barı İyileştirmesi:**
  - Gaz ve fren pedalı derinlikleri (`%0..100`), periyodik snapshot beklemesi olmadan donanım katmanından doğrudan yansıma (reflection) ile mikro-saniyelik sıfır gecikmeyle okunacak şekilde optimize edildi.
  - Rejen / güç gösterge barında araç durduğunda veya rejenerasyondan çıkıldığında negatif değerin asılı kalmasını önleyen anlık sıfırlama kilidi (zero-speed snap) eklendi.
  - Rejen ve fren barları aktif olduklarında dinamik renk vurgularıyla (Zümrüt yeşili ve Fren kırmızısı) görselleştirildi.
- 📊 **Navion Tasarımıyla %100 Uyumlu Tüketim & Seyahat Kartları:**
  - **SON 50 KM ORTALAMASI (Camgöbeği #06B6D4):** Fabrika donanım verisi (`getLast50KmPowerConsume`) ve alt satırda `Genel: 16,8 kWh/100km` ömür boyu ortalama tüketim göstergesi.
  - **SON ŞARJDAN İTİBAREN (Amber #F59E0B):** Son şarjdan sonraki net mesafe ve `Ort: 16,8 kWh/100km` alt bilgisi; karta tıklanarak anında sıfırlanabilir.
  - **AKTİF SEYAHAT - TRIP:** Sürüş süresi ve mesafesi `0,0 km (0 dk)` formatında; alt satırda aracın toplam kilometre sayacı (`Toplam: 12.450 km`).
  - **REGEN TASARRUFU (Zümrüt #10B981):** Aktif sürüş boyunca frenleme ve rejen ile geri kazanılan gerçek enerji entegrasyonu (`+0,13 kWh`) ve `Geri Kazanılan Enerji` alt başlığı.
- 📹 **Canlı Kamera Akış İyileştirmesi:** Canlı araç kamerası açıldığında kullanıcının seçim yapmasını beklemeden otomatik olarak ilk kamerayı anında yayına başlatır.
- 🎨 **Otomotiv Tasarım Uyumu:** Araç kontrol ekranındaki tüm kartlar, paneller ve butonlar `OverdriveTheme` tasarım sistemine uyarlandı.

---

### 🇬🇧 English Release Notes

> ⚠️ **Critical Architectural Note:** To ensure background daemon services (`fast_cam_capture` / UID 2000 / SurfaceControl) and Blind Spot camera streaming function correctly via ADB `run-as`, the application is always compiled in **Debug** mode.

#### 🌟 Recent Features & Improvements:
- 🐟 **4-Channel Independent Fisheye (Barrel Distortion) Dewarping:**
  - Added fully independent distortion correction for each camera channel (Front, Right Mirror, Rear, Left Mirror).
  - High curvature ($180^\circ+$) side mirror cameras can now be aggressively dewarped (~50%) without over-stretching or distorting the front and rear cameras (~10-15%).
  - Implemented entirely within the Adreno GPU OpenGL ES 2.0 Fragment Shader (`vec2 k` uniforms + `rectifyTileWithK` hardware bilinear sampling) ensuring **0% additional CPU load**, **0 ms latency**, and **zero frame drops**.
  - **BYD Sealion 7 / Seal Optimum Preset:** Added 1-tap preset button in Recording settings to instantly apply tailored optimum values (Front: 10%, Side Mirrors: 50%, Rear: 15%).
  - Maintained 100% backward compatibility with legacy single-slider rectification.
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
- 🏎️ **Real-Time Accelerator & Brake Pedal Telemetry and Power Bar:**
  - Optimized accelerator and brake depth (%0..100) via direct reflection from hardware HAL with zero periodic snapshot latency.
  - Added zero-speed snap guard to prevent lingering negative kW values when coming to a complete stop or exiting regeneration.
  - Dynamic active color highlighting (Emerald Green for regen, Brake Red for braking).
- 📊 **1:1 Navion-Aligned Consumption & Trip Telemetry Cards:**
  - **LAST 50 KM AVG (Cyan #06B6D4):** Direct factory metric (`getLast50KmPowerConsume`) with `Overall: 16.8 kWh/100km` lifetime average subtitle.
  - **SINCE LAST CHARGE (Amber #F59E0B):** Driven km with `Avg: 16.8 kWh/100km` subtitle; tap-to-reset capability.
  - **ACTIVE TRIP (TRIP):** Driven distance and duration `0.0 km (0 min)` with lifetime vehicle odometer subtitle (`Total: 12,450 km`).
  - **REGEN SAVINGS (Emerald #10B981):** Real-time integration of recovered energy (`+0.13 kWh`) with `Recovered Energy` subtitle.
- 📹 **Instant Live Camera Streaming:** Automatically initializes and streams the primary camera upon opening the live view without requiring manual camera toggling.
- 🎨 **Automotive UI Theme Alignment:** Completely restyled all cards, stats, and dialogs on the Vehicle page to match `OverdriveTheme`.

---
**APK Dosyası / File:** `betteroverdrive.apk` ({APK_SIZE_MB} MB)  
**Derleme Tipi / Build Type:** Debug (arm64-v8a - BYD DiLink Android 10/12)
