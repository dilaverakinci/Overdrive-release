## 🚗 BetterOverdrive {VERSION_TAG}

[TR] Moredrive projesi kapsamındaki en güncel geliştirmeleri ve optimizasyonları içeren **Debug** derlemesidir.  
[EN] **Debug** build containing the latest enhancements and optimizations from the Moredrive project.

---

### 🇹🇷 Türkçe Sürüm Notları

> ⚠️ **Kritik Mimari Not:** Kör nokta (Blind Spot) kamera akışı ve arka plan daemon servislerinin (UID 2000 / SurfaceControl) ADB `run-as` üzerinden sorunsuz çalışabilmesi için uygulama daima **Debug** modunda derlenir.

#### 🌟 Son Yenilikler ve İyileştirmeler:
- 🔋 **Tüketim & Gerçekçi Menzil Revizyonu:**
  - **Son 50 km Tüketimi:** Doğrudan BYD araç donanımından (`getLast50KmPowerConsume`) orijinal fabrika verisi olarak okunur ve gösterilir.
  - **Son Şarjdan İtibaren:** Araç şarjdan ayrıldığı anda referansı otomatik kilitler ve kalıcı hafızaya alır (`SinceChargeManager`); kat edilen km ve net tüketilen kWh/100km oranını hesaplar.
  - **Aktif Seyahat:** Araç hareket ettiği andan itibaren başlayan sürüşün süresini (dk) ve mesafesini (km) gerçek zamanlı canlı takip eder.
  - **Dinamik Gerçekçi Menzil:** Son şarj harcama trendine göre anlık kalan menzili hesaplar; eklenen "Sıfırla" butonu ile istenildiği anda referans sıfırlanabilir.
- 📹 **Canlı Kamera Akış İyileştirmesi:** Canlı araç kamerası açıldığında kullanıcının seçim yapmasını beklemeden otomatik olarak ilk kamerayı anında yayına başlatır.
- 📊 **Telemetri & Gösterge Kararlılığı:** Hız, tüketim ve regen/güç barındaki tutarsız dalgalanmalar filtrelendi; sürüş esnasında Trip kaydının kesintisiz çalışması güvenceye alındı.
- 🎨 **Otomotiv Tasarım Uyumu:** Araç kontrol ekranındaki tüm kartlar, paneller ve butonlar `OverdriveTheme` tasarım sistemine uyarlandı.
- ⚡ **BYD Doğrudan Kablosuz ADB Motoru:** Araç multimedya ekranı üzerinden (ts-framework IPC) tek tıkla kablosuz ADB etkinleştirme ve port 5555 yönetimi.
- 🩺 **Gelişmiş CAN-Bus Teşhisleri:** 9-ECU canlı telemetri monitörü, BMS hücre voltaj dengesizliği (Δ) takibi ve OBD-II DTC arıza kodları tarayıcısı/silicisi.
- 🔄 **Akıllı Otomatik Güncelleyici (AppUpdater):** Doğrudan `dilaverakinci/Overdrive-release` reposuna bağlandı; eski `51.8` sürümünden geçiş toleransı ile donatıldı.

---

### 🇬🇧 English Release Notes

> ⚠️ **Critical Architectural Note:** To ensure background daemon services (`fast_cam_capture` / UID 2000 / SurfaceControl) and Blind Spot camera streaming function correctly via ADB `run-as`, the application is always compiled in **Debug** mode.

#### 🌟 Recent Features & Improvements:
- 🔋 **Consumption & Dynamic Range Overhaul:**
  - **Hardware Last 50 km:** Direct retrieval of authentic factory metrics via BYD vehicle HAL (`getLast50KmPowerConsume`).
  - **Since Last Charge Tracking:** Automatically anchors baseline when charging session ends (`SinceChargeManager`), tracking precise distance driven and kWh/100km rate with persistent storage.
  - **Live Active Trip:** Real-time elapsed drive duration (min) and trip distance (km) tracked from the moment movement begins.
  - **Realistic Dynamic Range:** Calculated continuously from since-charge consumption, with an instant "Reset" button for on-demand baseline recalibration.
- 📹 **Instant Live Camera Streaming:** Automatically initializes and streams the primary camera upon opening the live view without requiring manual camera toggling.
- 📊 **Telemetry & Gauge Stabilization:** Filtered erratic fluctuations on speed, power bar, and consumption gauges; ensured 100% trip recording reliability during driving.
- 🎨 **Automotive UI Theme Alignment:** Completely restyled all cards, stats, and dialogs on the Vehicle page to match `OverdriveTheme`.
- ⚡ **BYD Direct Wireless ADB Engine:** One-click wireless ADB activation and port 5555 management directly from the vehicle infotainment display (ts-framework IPC).
- 🩺 **Advanced CAN-Bus Diagnostics:** 9-ECU live telemetry monitor, BMS cell voltage imbalance (Δ) tracking, and OBD-II DTC fault code scanner/clearing.
- 🔄 **Smart In-App Updater (AppUpdater):** Directed updates to `dilaverakinci/Overdrive-release` with backwards compatibility tolerance for upgrading from upstream `51.8`.

---
**APK Dosyası / File:** `betteroverdrive.apk` ({APK_SIZE_MB} MB)  
**Derleme Tipi / Build Type:** Debug (arm64-v8a - BYD DiLink Android 10/12)
