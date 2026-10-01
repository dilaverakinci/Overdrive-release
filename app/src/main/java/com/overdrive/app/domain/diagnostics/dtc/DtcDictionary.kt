package com.overdrive.app.domain.diagnostics.dtc

import com.overdrive.app.domain.diagnostics.EcuType

/**
 * Built-in dictionary and translator for standard OBD-II and BYD EV/PHEV specific trouble codes.
 */
object DtcDictionary {

    private val DICTIONARY = mapOf(
        // BMS / High Voltage Pack
        "P0A1F" to DtcCode(
            code = "P0A1F",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Batarya Kontrol Modülü Arızası",
            titleEn = "Battery Energy Control Module Internal Fault",
            descriptionTr = "BMS mikroişlemcisinde veya dahili ölçüm donanımında kritik arıza tespit edildi.",
            descriptionEn = "Internal microcontroller or analog hardware failure detected within the BMS."
        ),
        "P0A7F" to DtcCode(
            code = "P0A7F",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "Batarya Paketi Bozulması",
            titleEn = "Hybrid / EV Battery Pack Degradation",
            descriptionTr = "Yüksek voltaj batarya kapasitesi veya SOH değeri üretici eşiğinin altına indi.",
            descriptionEn = "High-voltage battery pack capacity or SOH has degraded below threshold."
        ),
        "P0A80" to DtcCode(
            code = "P0A80",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Batarya Paketini Değiştirin",
            titleEn = "Replace Hybrid / EV Battery Pack",
            descriptionTr = "Batarya hücreleri güvenli çalışma sınırlarının dışına çıktı, servis değişimi gereklidir.",
            descriptionEn = "Battery pack cells operate outside safe tolerances, replacement required."
        ),
        "P0B24" to DtcCode(
            code = "P0B24",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "Batarya Hücre Voltaj Dengesizliği",
            titleEn = "Hybrid / EV Battery Cell Voltage Imbalance",
            descriptionTr = "En yüksek ve en düşük hücre arasındaki voltaj farkı (Δ > 50mV) kabul edilebilir eşiği aştı.",
            descriptionEn = "Voltage spread between highest and lowest cell exceeds 50mV tolerance."
        ),
        "P0562" to DtcCode(
            code = "P0562",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "12V Sistem Voltajı Düşük",
            titleEn = "System Voltage Low (12V Auxiliary Battery)",
            descriptionTr = "12V yardımcı akü voltajı 11.8V altına düştü, DC-DC şarjı veya akü kontrolü gerekebilir.",
            descriptionEn = "Auxiliary 12V battery voltage dropped below 11.8V threshold."
        ),
        "P0563" to DtcCode(
            code = "P0563",
            ecu = EcuType.BMS,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "12V Sistem Voltajı Yüksek",
            titleEn = "System Voltage High (12V Auxiliary Battery)",
            descriptionTr = "12V akü gerilimi güvenli şarj eşiğinin üzerinde (15.2V+).",
            descriptionEn = "Auxiliary 12V bus voltage exceeded 15.2V."
        ),

        // MCU / Drivetrain
        "P0A93" to DtcCode(
            code = "P0A93",
            ecu = EcuType.MCU,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "İnverter Soğutma Sistemi Performansı",
            titleEn = "Inverter Cooling System Performance",
            descriptionTr = "Motor sürücü inverteri soğutma sıvısı akışı yetersiz veya sıcaklık yüksek.",
            descriptionEn = "Inverter cooling flow insufficient or temperature elevated."
        ),
        "P0C73" to DtcCode(
            code = "P0C73",
            ecu = EcuType.MCU,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "Motor Soğutma Pompası Performansı",
            titleEn = "Motor Electronics Coolant Pump Performance",
            descriptionTr = "Elektrik motoru elektrikli su pompası devir sapması veya tıkanıklık.",
            descriptionEn = "Motor cooling auxiliary pump speed feedback mismatch."
        ),

        // VCU
        "P0A12" to DtcCode(
            code = "P0A12",
            ecu = EcuType.VCU,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.CRITICAL,
            titleTr = "DC-DC Dönüştürücü Devre Arızası",
            titleEn = "DC-DC Converter Circuit Malfunction",
            descriptionTr = "Yüksek voltajdan 12V üreten dahili DC-DC dönüştürücü devresinde hata.",
            descriptionEn = "HV to 12V onboard DC-DC converter circuit fault."
        ),
        "P0700" to DtcCode(
            code = "P0700",
            ecu = EcuType.VCU,
            category = DtcCategory.POWERTRAIN,
            severity = DtcSeverity.WARNING,
            titleTr = "Şanzıman / Sürüş Kontrol İstek Hatası",
            titleEn = "Transmission / Drive Control System Malfunction",
            descriptionTr = "Vites seçici veya şanzıman aktüatör mekanizmasında durum uyumsuzluğu.",
            descriptionEn = "Transmission range sensor or shift actuator position disagreement."
        ),

        // ESP / Chassis
        "C0035" to DtcCode(
            code = "C0035",
            ecu = EcuType.ESP,
            category = DtcCategory.CHASSIS,
            severity = DtcSeverity.WARNING,
            titleTr = "Sol Ön Tekerlek Hız Sensörü Devresi",
            titleEn = "Left Front Wheel Speed Sensor Circuit",
            descriptionTr = "Sol ön tekerlek hız sensöründen sinyal alınamıyor veya dalgalı.",
            descriptionEn = "Left front wheel speed sensor erratic or missing signal."
        ),
        "C0040" to DtcCode(
            code = "C0040",
            ecu = EcuType.ESP,
            category = DtcCategory.CHASSIS,
            severity = DtcSeverity.WARNING,
            titleTr = "Sağ Ön Tekerlek Hız Sensörü Devresi",
            titleEn = "Right Front Wheel Speed Sensor Circuit",
            descriptionTr = "Sağ ön tekerlek hız sensöründen sinyal alınamıyor veya dalgalı.",
            descriptionEn = "Right front wheel speed sensor erratic or missing signal."
        ),
        "C1234" to DtcCode(
            code = "C1234",
            ecu = EcuType.ESP,
            category = DtcCategory.CHASSIS,
            severity = DtcSeverity.WARNING,
            titleTr = "Denge Kontrol (ESP) Müdahale Hatası",
            titleEn = "Electronic Stability Program Intervention Fault",
            descriptionTr = "ESP fren basınç valfleri veya ivmeölçer kalibrasyonunda sapma.",
            descriptionEn = "ESP hydraulic valve pressure control or yaw rate sensor calibration drift."
        ),

        // TPMS
        "C1500" to DtcCode(
            code = "C1500",
            ecu = EcuType.TPMS,
            category = DtcCategory.CHASSIS,
            severity = DtcSeverity.INFO,
            titleTr = "Lastik Basınç Düşüklüğü veya Sensör Uyarısı",
            titleEn = "Tire Pressure Low or Sensor Warning",
            descriptionTr = "Bir veya daha fazla lastikte basınç 2.0 Bar altına düştü.",
            descriptionEn = "One or more tires dropped below recommended 2.0 Bar pressure."
        ),

        // BCM / Body
        "B1000" to DtcCode(
            code = "B1000",
            ecu = EcuType.BCM,
            category = DtcCategory.BODY,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Gövde Kontrol Modülü (BCM) Donanım Arızası",
            titleEn = "Body Control Module (BCM) Internal Fault",
            descriptionTr = "Gövde kontrol modülü mikrodenetleyicisinde donanım hatası.",
            descriptionEn = "BCM microcontroller internal watchdog or memory checksum fault."
        ),
        "B1200" to DtcCode(
            code = "B1200",
            ecu = EcuType.HVAC,
            category = DtcCategory.BODY,
            severity = DtcSeverity.WARNING,
            titleTr = "Klima Kompresörü Çalışma Hatası",
            titleEn = "Climate Compressor Circuit Fault",
            descriptionTr = "Yüksek voltajlı elektrikli klima kompresörü iletişim veya güç hatası.",
            descriptionEn = "HV electric A/C scroll compressor communication or inverter issue."
        ),

        // Network / UDS CAN-Bus
        "U0100" to DtcCode(
            code = "U0100",
            ecu = EcuType.VCU,
            category = DtcCategory.NETWORK,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Araç Kontrol Ünitesi (VCU) ile İletişim Kaybı",
            titleEn = "Lost Communication with ECM / VCU",
            descriptionTr = "CAN-Bus üzerinde VCU periyodik mesajları zaman aşımına uğradı.",
            descriptionEn = "CAN bus heartbeat timeout with main vehicle control unit."
        ),
        "U0110" to DtcCode(
            code = "U0110",
            ecu = EcuType.MCU,
            category = DtcCategory.NETWORK,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Motor Kontrol Ünitesi (MCU) ile İletişim Kaybı",
            titleEn = "Lost Communication with Motor Control Module",
            descriptionTr = "CAN-Bus üzerinde elektrik motoru invertör kontrolörü ile iletişim koptu.",
            descriptionEn = "Lost communication with traction motor inverter drive unit."
        ),
        "U0111" to DtcCode(
            code = "U0111",
            ecu = EcuType.BMS,
            category = DtcCategory.NETWORK,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Batarya Yönetim Sistemi (BMS) ile İletişim Kaybı",
            titleEn = "Lost Communication with Battery Energy Control Module",
            descriptionTr = "CAN-Bus üzerinde yüksek voltaj batarya yönetim ünitesinden sinyal alınamıyor.",
            descriptionEn = "Lost communication with high voltage battery management system."
        ),
        "U0121" to DtcCode(
            code = "U0121",
            ecu = EcuType.ESP,
            category = DtcCategory.NETWORK,
            severity = DtcSeverity.CRITICAL,
            titleTr = "ABS / ESP Fren Modülü ile İletişim Kaybı",
            titleEn = "Lost Communication with Anti-Lock Brake (ABS) Module",
            descriptionTr = "ESP / ABS hidrolik modülatöründen CAN veri akışı kesildi.",
            descriptionEn = "Lost communication with chassis stability control module."
        ),
        "U0140" to DtcCode(
            code = "U0140",
            ecu = EcuType.BCM,
            category = DtcCategory.NETWORK,
            severity = DtcSeverity.CRITICAL,
            titleTr = "Gövde Kontrol Modülü (BCM) ile İletişim Kaybı",
            titleEn = "Lost Communication with Body Control Module",
            descriptionTr = "Gövde aydınlatma, kilit ve kapı kontrol ünitesi CAN zaman aşımı.",
            descriptionEn = "Lost communication with central body electronics module."
        )
    )

    /**
     * Resolves a DTC code string into a detailed DtcCode object.
     * Uses lookup table when available, or provides a smart fallback conforming to OBD-II standards.
     */
    fun lookup(rawCode: String): DtcCode {
        val sanitized = rawCode.trim().uppercase()
        val found = DICTIONARY[sanitized]
        if (found != null) return found

        val category = DtcCategory.fromCode(sanitized)
        val defaultEcu = when (category) {
            DtcCategory.POWERTRAIN -> EcuType.VCU
            DtcCategory.CHASSIS -> EcuType.ESP
            DtcCategory.BODY -> EcuType.BCM
            DtcCategory.NETWORK -> EcuType.VCU
        }

        return DtcCode(
            code = sanitized,
            ecu = defaultEcu,
            category = category,
            severity = DtcSeverity.WARNING,
            titleTr = "Genel Diyagnostik Arıza Kodu ($sanitized)",
            titleEn = "Generic Diagnostic Trouble Code ($sanitized)",
            descriptionTr = "${category.titleTr} sisteminde $sanitized kodlu arıza tespit edildi.",
            descriptionEn = "Fault code $sanitized detected in ${category.titleEn} system."
        )
    }

    /**
     * Returns all known codes in the dictionary.
     */
    fun allKnownCodes(): List<DtcCode> = DICTIONARY.values.toList()
}
