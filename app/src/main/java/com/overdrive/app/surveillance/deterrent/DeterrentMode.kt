package com.overdrive.app.surveillance.deterrent

/**
 * Operating modes for Sentry Deterrent 2.0.
 */
enum class DeterrentMode(val titleTr: String, val titleEn: String) {
    SCREEN_ONLY("Sadece Ekran", "Screen Only"),
    LIGHTS_ONLY("Sadece Flaşör", "Lights Only"),
    SOUND_ONLY("Sadece Sesli İkaz", "Sound Only"),
    FULL_DETERRENT("Tam Caydırıcılık (Ekran + Flaşör + Ses)", "Full Deterrent")
}
