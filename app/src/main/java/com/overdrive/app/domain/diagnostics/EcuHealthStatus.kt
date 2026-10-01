package com.overdrive.app.domain.diagnostics

/**
 * Diagnostic health severity status for automotive ECUs.
 */
enum class EcuHealthStatus(val labelTr: String, val labelEn: String) {
    NORMAL("Normal", "Normal"),
    WARNING("Uyarı", "Warning"),
    FAULT("Arıza", "Fault"),
    OFFLINE("Çevrimdışı", "Offline");

    val isHealthy: Boolean
        get() = this == NORMAL

    val isCritical: Boolean
        get() = this == FAULT
}
