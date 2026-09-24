# Overdrive Projesi Geliştirme ve Derleme Kuralları

## 🔒 Kritik Güvenlik / Lisans Kuralı: `libod.so` & Derleme Türü

- **Kör Nokta Lens Projeksiyonu (`libod.so`):**
  - `app/src/main/jniLibs/arm64-v8a/libod.so` kütüphanesi kapalı kaynaklıdır ve çalışma zamanında resmi imza sertifikasını doğrular.
  - Özel veya farklı bir `release.jks` ile `assembleRelease` derlemesi yapılırsa imza uyuşmazlığı nedeniyle kör nokta kamera kartı (**blind-spot card**) yetki vermez ve **siyah ekranda** kalır.

- **Zorunlu Derleme Komutu:**
  - Geliştirme ve test süreçlerinde her zaman **Debug** modu kullanılmalıdır:
    ```bash
    ./gradlew assembleDebug
    # veya cihaza doğrudan kurulum için:
    ./gradlew installDebug
    ```
  - **Debug** modunda `libod.so` imza denetimini doğrudan **bypass eder / atlar**. Bu sayede kör nokta projeksiyonu dahil tüm özellikler araçta ve emülatörde sorunsuz çalışır.
  - Kullanıcı açıkça talep etmedikçe `assembleRelease` derlemesi çalıştırılmamalıdır.
