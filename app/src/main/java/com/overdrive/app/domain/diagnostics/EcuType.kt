package com.overdrive.app.domain.diagnostics

/**
 * 9 primary Electronic Control Units (ECUs) monitored for vehicle health and diagnostics.
 */
enum class EcuType(
    val titleTr: String,
    val titleEn: String,
    val dtcPrefix: Char
) {
    BMS("Batarya Yönetim Sistemi", "Battery Management System", 'P'),
    MCU("Motor Kontrol Ünitesi", "Motor Control Unit", 'P'),
    VCU("Araç Kontrol Ünitesi", "Vehicle Control Unit", 'P'),
    ESP("Elektronik Denge & Fren Sistemi", "Electronic Stability Program", 'C'),
    BCM("Gövde Kontrol Modülü", "Body Control Module", 'B'),
    HVAC("Termal & İklimlendirme", "HVAC & Thermal Management", 'B'),
    TPMS("Lastik Basınç İzleme Sistemi", "Tire Pressure Monitoring System", 'C'),
    EPS("Elektrikli Direksiyon Sistemi", "Electric Power Steering", 'C'),
    EPB("Elektronik Park Freni", "Electric Parking Brake", 'C');

    val displayName: String
        get() = "$name - $titleTr"
}
