package com.overdrive.app.domain.diagnostics.dtc

import com.overdrive.app.domain.diagnostics.EcuType

/**
 * Diagnostic Trouble Code severity levels.
 */
enum class DtcSeverity(val labelTr: String, val labelEn: String) {
    INFO("Bilgilendirme", "Info"),
    WARNING("Uyarı", "Warning"),
    CRITICAL("Kritik", "Critical")
}

/**
 * Standard automotive DTC categories based on the code's first letter.
 */
enum class DtcCategory(val prefix: Char, val titleTr: String, val titleEn: String) {
    POWERTRAIN('P', "Güç Aktarım Sistemi", "Powertrain"),
    CHASSIS('C', "Şasi ve Güvenlik", "Chassis"),
    BODY('B', "Gövde ve Konfor", "Body"),
    NETWORK('U', "Ağ ve İletişim", "Network & Communication");

    companion object {
        fun fromCode(code: String): DtcCategory {
            val first = code.trim().firstOrNull()?.uppercaseChar() ?: 'P'
            return values().firstOrNull { it.prefix == first } ?: POWERTRAIN
        }
    }
}

/**
 * Strongly-typed OBD-II Diagnostic Trouble Code representation.
 */
data class DtcCode(
    val code: String,
    val ecu: EcuType,
    val category: DtcCategory,
    val severity: DtcSeverity,
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val isActive: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = "$code - $titleTr"
}
