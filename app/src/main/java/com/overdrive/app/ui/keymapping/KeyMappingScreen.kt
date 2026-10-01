package com.overdrive.app.ui.keymapping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveColors
import com.overdrive.app.ui.theme.OverdriveDimensions
import com.overdrive.app.ui.theme.OverdriveTheme
import org.json.JSONObject

private val OverdriveColors.textPrimary: Color get() = onSurface
private val OverdriveColors.textSecondary: Color get() = onSurfaceVariant
private val OverdriveColors.cardBackground: Color get() = surfaceContainer
private val OverdriveColors.cardBorder: Color get() = outlineVariant
private val OverdriveColors.accentGreen: Color get() = statusSuccess
private val OverdriveColors.accentAmber: Color get() = statusWarning
private val OverdriveColors.accentRed: Color get() = statusDanger

enum class KeyMappingTab(val label: String) {
    BINDINGS("Mevcut Eşlemeler"),
    ADD_EDIT("Eşleme Ekle / Düzenle"),
    SETTINGS("Ayarlar & Hassasiyet")
}

data class KeyBindingItem(
    val index: Int,
    val keyCode: Int,
    val buttonName: String,
    val pressType: String, // "single", "double", "long"
    val isEnabled: Boolean,
    val blockNativeSingle: Boolean,
    val actionDescription: String,
    val actionRaw: JSONObject
)

data class KnownButtonDef(
    val code: Int,
    val name: String,
    val allowedPressTypes: List<String> = listOf("single", "double"),
    val isFixedSingle: Boolean = false
)

data class CuratedActionDef(
    val id: String,
    val category: String,
    val title: String,
    val kind: String, // "catalog", "vehicle", "api", "radio"
    val key: String,
    val sub: String? = null,
    val method: String = "POST",
    val path: String = "",
    val body: String = "",
    val payloadOptions: List<Pair<String, String>> = emptyList()
)

data class AppOption(
    val packageName: String,
    val label: String
)

data class KeyMappingUiState(
    val selectedTab: KeyMappingTab = KeyMappingTab.BINDINGS,
    val isMasterEnabled: Boolean = false,
    val allowAdvanced: Boolean = false,
    val doubleTapWindowMs: Long = 450L,
    val isA11yBound: Boolean = false,
    val bindings: List<KeyBindingItem> = emptyList(),
    val launchableApps: List<AppOption> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val deleteConfirmIndex: Int? = null,

    // Form State (Add / Edit)
    val editIndex: Int? = null,
    val selectedButtonCode: Int = 87,
    val isCustomButton: Boolean = false,
    val manualKeyCodeText: String = "",
    val isCapturing: Boolean = false,
    val capturedFeedback: String? = null,
    val selectedPressType: String = "single",
    val blockNativeSingle: Boolean = true,
    val actionKind: String = "curated", // "curated", "manualClip", "openApp", "shell"
    val selectedCuratedId: String = "lock",
    val selectedPayloadValue: String = "",
    val clipBeforeSeconds: Int = 30,
    val clipAfterSeconds: Int = 0,
    val selectedAppPackage: String = "",
    val appSplitScreen: Boolean = false,
    val shellCommandText: String = ""
)

object KeyMappingCatalog {
    val KNOWN_BUTTONS = listOf(
        KnownButtonDef(87, "Sonraki Parça / İleri (Next)", listOf("single", "double")),
        KnownButtonDef(88, "Önceki Parça / Geri (Previous)", listOf("single", "double")),
        KnownButtonDef(289, "Mod Tuşu (Mode)", listOf("single", "double")),
        KnownButtonDef(291, "Ses Artır (+)", listOf("single")),
        KnownButtonDef(292, "Ses Azalt (-)", listOf("single")),
        KnownButtonDef(293, "Sessiz Tuşu (Mute)", listOf("single", "double")),
        KnownButtonDef(294, "360 Çevre Görüş / Kamera", listOf("single", "double")),
        KnownButtonDef(304, "Sesli Komut (Voice)", listOf("single", "double")),
        KnownButtonDef(305, "Ekran Döndür (Rotate)", listOf("single", "double")),
        KnownButtonDef(313, "Telefon / Çağrı (Phone)", listOf("single", "double")),
        KnownButtonDef(317, "Güç Tuşu (Power)", listOf("single", "double")),
        KnownButtonDef(302, "Sonraki Tuşuna Uzun Basış", listOf("single"), isFixedSingle = true),
        KnownButtonDef(303, "Önceki Tuşuna Uzun Basış", listOf("single"), isFixedSingle = true),
        KnownButtonDef(306, "Ekran Döndürmeye Uzun Basış", listOf("single"), isFixedSingle = true),
        KnownButtonDef(312, "Sesli Komuta Uzun Basış", listOf("single"), isFixedSingle = true)
    )

    fun getButtonName(code: Int): String {
        return KNOWN_BUTTONS.find { it.code == code }?.name ?: "Tuş Kodu: $code"
    }

    val CURATED_ACTIONS = listOf(
        // Kilit & Güvenlik
        CuratedActionDef(
            id = "lock",
            category = "Kilit & Güvenlik",
            title = "Kapıları Kilitle",
            kind = "vehicle",
            key = "lock"
        ),
        CuratedActionDef(
            id = "unlock",
            category = "Kilit & Güvenlik",
            title = "Kapı Kilitlerini Aç",
            kind = "vehicle",
            key = "unlock"
        ),
        CuratedActionDef(
            id = "flash",
            category = "Kilit & Güvenlik",
            title = "Flaşörleri Yak (Selektör)",
            kind = "vehicle",
            key = "flash"
        ),
        CuratedActionDef(
            id = "find_car",
            category = "Kilit & Güvenlik",
            title = "Aracımı Bul (Işık & Korna)",
            kind = "vehicle",
            key = "find_car"
        ),
        CuratedActionDef(
            id = "child_lock",
            category = "Kilit & Güvenlik",
            title = "Çocuk Güvenlik Kilidi",
            kind = "catalog",
            key = "child_lock",
            payloadOptions = listOf("1" to "Etkinleştir (Açık)", "0" to "Devre Dışı (Kapalı)")
        ),

        // Cam & Tavan & Bagaj
        CuratedActionDef(
            id = "windows_all",
            category = "Cam & Tavan",
            title = "Tüm Camlar",
            kind = "catalog",
            key = "windows_all",
            payloadOptions = listOf("OPEN" to "Tümünü Aç", "CLOSE" to "Tümünü Kapat", "STOP" to "Durdur")
        ),
        CuratedActionDef(
            id = "window_lf",
            category = "Cam & Tavan",
            title = "Ön Sol Cam",
            kind = "api",
            key = "window_lf",
            path = "/api/vehicle/window",
            body = "{\"area\":1,\"targetPercent\":\${v}}",
            payloadOptions = listOf("0" to "Kapat", "15" to "Havalandırma (%15)", "50" to "Yarı Açık (%50)", "100" to "Tam Açık")
        ),
        CuratedActionDef(
            id = "window_rf",
            category = "Cam & Tavan",
            title = "Ön Sağ Cam",
            kind = "api",
            key = "window_rf",
            path = "/api/vehicle/window",
            body = "{\"area\":2,\"targetPercent\":\${v}}",
            payloadOptions = listOf("0" to "Kapat", "15" to "Havalandırma (%15)", "50" to "Yarı Açık (%50)", "100" to "Tam Açık")
        ),
        CuratedActionDef(
            id = "tailgate",
            category = "Cam & Tavan",
            title = "Elektrikli Bagaj Kapağı",
            kind = "catalog",
            key = "tailgate",
            payloadOptions = listOf("OPEN" to "Aç", "CLOSE" to "Kapat", "STOP" to "Durdur")
        ),
        CuratedActionDef(
            id = "sunroof",
            category = "Cam & Tavan",
            title = "Panoramik Sunroof",
            kind = "catalog",
            key = "sunroof",
            payloadOptions = listOf("OPEN" to "Aç", "CLOSE" to "Kapat", "STOP" to "Durdur")
        ),
        CuratedActionDef(
            id = "sunshade",
            category = "Cam & Tavan",
            title = "Tavan Perdesi / Güneşlik",
            kind = "catalog",
            key = "sunshade",
            payloadOptions = listOf("OPEN" to "Aç", "CLOSE" to "Kapat", "STOP" to "Durdur")
        ),
        CuratedActionDef(
            id = "mirror_fold",
            category = "Cam & Tavan",
            title = "Yan Aynalar",
            kind = "catalog",
            key = "mirror_fold",
            payloadOptions = listOf("on" to "Katla", "off" to "Aç", "toggle" to "Katla / Aç Değiştir")
        ),

        // İklimlendirme (Klima)
        CuratedActionDef(
            id = "climate",
            category = "İklimlendirme",
            title = "Klima Aç/Kapat",
            kind = "catalog",
            key = "climate",
            sub = "mode",
            payloadOptions = listOf("auto" to "Otomatik Aç", "off" to "Kapat")
        ),
        CuratedActionDef(
            id = "ac_fan",
            category = "İklimlendirme",
            title = "Klima Fan Hızı",
            kind = "api",
            key = "ac_fan",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"set_fan\",\"fan\":\${v}}",
            payloadOptions = (1..7).map { it.toString() to "Fan Kademesi $it" }
        ),
        CuratedActionDef(
            id = "ac_temp_step",
            category = "İklimlendirme",
            title = "Kabin Sıcaklığı Adımı",
            kind = "api",
            key = "ac_temp_step",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"step_temp\",\"delta\":\${v}}",
            payloadOptions = listOf("1" to "+1°C Isıt", "-1" to "-1°C Soğut")
        ),
        CuratedActionDef(
            id = "defrost_front",
            category = "İklimlendirme",
            title = "Ön Cam Buğu Çözücü",
            kind = "api",
            key = "defrost_front",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"defrost_front_\${v}\"}",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "defrost_rear",
            category = "İklimlendirme",
            title = "Arka Cam Buğu Çözücü",
            kind = "api",
            key = "defrost_rear",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"defrost_rear_\${v}\"}",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "recirculation",
            category = "İklimlendirme",
            title = "Hava Sirkülasyonu",
            kind = "api",
            key = "recirculation",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"recirculate_\${v}\"}",
            payloadOptions = listOf("on" to "İç Hava Sirkülasyonu", "off" to "Temiz Dış Hava")
        ),
        CuratedActionDef(
            id = "steering_heat",
            category = "İklimlendirme",
            title = "Direksiyon Isıtma",
            kind = "api",
            key = "steering_heat",
            path = "/api/vehicle/climate",
            body = "{\"action\":\"steering_heat_\${v}\"}",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "seat_heat_driver",
            category = "İklimlendirme",
            title = "Sürücü Koltuk Isıtma",
            kind = "catalog",
            key = "seat_heat_driver",
            payloadOptions = listOf("high" to "Yüksek", "low" to "Düşük", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "seat_heat_passenger",
            category = "İklimlendirme",
            title = "Yolcu Koltuk Isıtma",
            kind = "catalog",
            key = "seat_heat_passenger",
            payloadOptions = listOf("high" to "Yüksek", "low" to "Düşük", "off" to "Kapalı")
        ),

        // Aydınlatma
        CuratedActionDef(
            id = "drl",
            category = "Aydınlatma",
            title = "Gündüz Farları (DRL)",
            kind = "catalog",
            key = "drl",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı", "toggle" to "Değiştir")
        ),
        CuratedActionDef(
            id = "headlight_mode",
            category = "Aydınlatma",
            title = "Far Çalışma Modu",
            kind = "catalog",
            key = "headlight_mode",
            payloadOptions = listOf("auto" to "Otomatik", "low_beam" to "Kısa Farlar", "parking" to "Park Işıkları", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "hazard",
            category = "Aydınlatma",
            title = "Dörtlü Flaşörler",
            kind = "catalog",
            key = "hazard",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı", "toggle" to "Değiştir")
        ),
        CuratedActionDef(
            id = "welcome_light",
            category = "Aydınlatma",
            title = "Karşılama Işıkları",
            kind = "api",
            key = "welcome_light",
            path = "/api/vehicle/lights",
            body = "{\"target\":\"welcomeLight\",\"enable\":\${v}}",
            payloadOptions = listOf("true" to "Açık", "false" to "Kapalı")
        ),

        // Sürüş & Güç
        CuratedActionDef(
            id = "drive_mode",
            category = "Sürüş & Güç",
            title = "Sürüş Modu Seçimi",
            kind = "catalog",
            key = "drive_mode",
            payloadOptions = listOf("normal" to "Normal", "eco" to "Eko", "sport" to "Spor")
        ),
        CuratedActionDef(
            id = "powertrain_mode",
            category = "Sürüş & Güç",
            title = "Güç Aktarımı (EV / HEV)",
            kind = "catalog",
            key = "powertrain_mode",
            payloadOptions = listOf("ev" to "Saf Elektrik (EV)", "hev" to "Hibrit (HEV)")
        ),
        CuratedActionDef(
            id = "battery_hold",
            category = "Sürüş & Güç",
            title = "Batarya Koruma (SOC Hold)",
            kind = "catalog",
            key = "battery_hold",
            payloadOptions = listOf("at_current" to "Mevcut Şarjı Koru", "at_floor" to "Tüketime İzin Ver", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "regen_level",
            category = "Sürüş & Güç",
            title = "Rejenerasyon Hissi",
            kind = "catalog",
            key = "regen_level",
            payloadOptions = listOf("standard" to "Standart", "high" to "Yüksek", "toggle" to "Değiştir")
        ),
        CuratedActionDef(
            id = "steering_mode",
            category = "Sürüş & Güç",
            title = "Direksiyon Hissi",
            kind = "catalog",
            key = "steering_mode",
            payloadOptions = listOf("comfort" to "Konfor", "sport" to "Spor", "toggle" to "Değiştir")
        ),
        CuratedActionDef(
            id = "brake_feel",
            category = "Sürüş & Güç",
            title = "Fren Hissi",
            kind = "catalog",
            key = "brake_feel",
            payloadOptions = listOf("comfort" to "Konfor", "sport" to "Spor", "toggle" to "Değiştir")
        ),
        CuratedActionDef(
            id = "esp_control",
            category = "Sürüş & Güç",
            title = "ESP Çekiş Kontrolü",
            kind = "catalog",
            key = "esp_control",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        ),

        // Kameralar & Gözetim
        CuratedActionDef(
            id = "camview_all",
            category = "Kamera & Gözetim",
            title = "360 Çevre Görünüm",
            kind = "api",
            key = "camview_all",
            path = "/api/camview/show?cam=all&target=head_unit&preset=\${v}/center",
            payloadOptions = listOf("45" to "Orta Boyut", "70" to "Geniş Boyut", "90" to "Tam Ekran", "25" to "Küçük Boyut")
        ),
        CuratedActionDef(
            id = "camview_front",
            category = "Kamera & Gözetim",
            title = "Ön Kamera Görünümü",
            kind = "api",
            key = "camview_front",
            path = "/api/camview/show?cam=front&target=head_unit&preset=\${v}/center",
            payloadOptions = listOf("45" to "Orta Boyut", "70" to "Geniş Boyut", "90" to "Tam Ekran", "25" to "Küçük Boyut")
        ),
        CuratedActionDef(
            id = "camview_rear",
            category = "Kamera & Gözetim",
            title = "Arka Kamera Görünümü",
            kind = "api",
            key = "camview_rear",
            path = "/api/camview/show?cam=rear&target=head_unit&preset=\${v}/center",
            payloadOptions = listOf("45" to "Orta Boyut", "70" to "Geniş Boyut", "90" to "Tam Ekran", "25" to "Küçük Boyut")
        ),
        CuratedActionDef(
            id = "camview_hide",
            category = "Kamera & Gözetim",
            title = "Kamera Görünümünü Kapat",
            kind = "api",
            key = "camview_hide",
            path = "/api/camview/hide"
        ),
        CuratedActionDef(
            id = "bs_enable",
            category = "Kamera & Gözetim",
            title = "Kör Nokta Kartı",
            kind = "api",
            key = "bs_enable",
            path = "/api/bs/tweak?enabled=\${v}",
            payloadOptions = listOf("true" to "Etkinleştir", "false" to "Devre Dışı")
        ),
        CuratedActionDef(
            id = "surveillance",
            category = "Kamera & Gözetim",
            title = "Nöbetçi / Gözetim Modu",
            kind = "api",
            key = "surveillance",
            path = "/api/surveillance/\${v}",
            payloadOptions = listOf("enable" to "Etkinleştir", "disable" to "Kapat")
        ),
        CuratedActionDef(
            id = "recording",
            category = "Kamera & Gözetim",
            title = "Dashcam Kayıt Modu",
            kind = "api",
            key = "recording",
            path = "/api/recording/mode",
            body = "{\"mode\":\"\${v}\"}",
            payloadOptions = listOf(
                "CONTINUOUS" to "Sürekli Kayıt",
                "DRIVE_MODE" to "Sürüşte Kayıt",
                "PROXIMITY_GUARD" to "Yakınlık Koruması",
                "NONE" to "Kayıt Kapalı"
            )
        ),

        // Multimedya & Sistem
        CuratedActionDef(
            id = "volume_step",
            category = "Multimedya & Sistem",
            title = "Ses Seviyesi Adımı",
            kind = "api",
            key = "volume_step",
            path = "/api/vehicle/media",
            body = "{\"target\":\"volume_step\",\"channel\":\"media\",\"value\":\${v}}",
            payloadOptions = listOf("1" to "Sesi Artır (+1)", "-1" to "Sesi Azalt (-1)")
        ),
        CuratedActionDef(
            id = "media_control",
            category = "Multimedya & Sistem",
            title = "Medya Oynatma Kontrolü",
            kind = "api",
            key = "media_control",
            path = "/api/vehicle/media",
            body = "{\"target\":\"media_key\",\"key\":\"\${v}\"}",
            payloadOptions = listOf(
                "play_pause" to "Oynat / Duraklat",
                "next" to "Sonraki Parça",
                "previous" to "Önceki Parça"
            )
        ),
        CuratedActionDef(
            id = "infotainment_rotation",
            category = "Multimedya & Sistem",
            title = "Orta Ekran Döndürme",
            kind = "catalog",
            key = "infotainment_rotation",
            payloadOptions = listOf(
                "horizontal" to "Yatay Konum",
                "vertical" to "Dikey Konum",
                "toggle" to "Yatay / Dikey Değiştir"
            )
        ),
        CuratedActionDef(
            id = "screen_power",
            category = "Multimedya & Sistem",
            title = "Orta Ekranı Kapat / Aç",
            kind = "api",
            key = "screen_power",
            path = "/api/vehicle/media",
            body = "{\"target\":\"screen_power\",\"value\":\${v}}",
            payloadOptions = listOf("0" to "Ekranı Karart (Kapat)", "1" to "Ekranı Aç")
        ),
        CuratedActionDef(
            id = "screenshot",
            category = "Multimedya & Sistem",
            title = "Ekran Görüntüsü Al",
            kind = "api",
            key = "screenshot",
            path = "/api/vehicle/system",
            body = "{\"target\":\"screenshot\",\"display\":\${v}}",
            payloadOptions = listOf("0" to "Orta Bilgi Ekranı", "1" to "Gösterge Ekranı")
        ),
        CuratedActionDef(
            id = "ui_nav",
            category = "Multimedya & Sistem",
            title = "Sistem Navigasyon Tuşu",
            kind = "api",
            key = "ui_nav",
            path = "/api/vehicle/system",
            body = "{\"target\":\"\${v}\"}",
            payloadOptions = listOf("home" to "Ana Sayfa", "back" to "Geri", "recents" to "Son Uygulamalar")
        ),
        CuratedActionDef(
            id = "radio_wifi",
            category = "Multimedya & Sistem",
            title = "Wi-Fi Bağlantısı",
            kind = "radio",
            key = "wifi",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        ),
        CuratedActionDef(
            id = "radio_bluetooth",
            category = "Multimedya & Sistem",
            title = "Bluetooth Bağlantısı",
            kind = "radio",
            key = "bluetooth",
            payloadOptions = listOf("on" to "Açık", "off" to "Kapalı")
        )
    )
}

@Composable
fun KeyMappingScreen(
    state: KeyMappingUiState,
    onTabSelected: (KeyMappingTab) -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleAllowAdvanced: (Boolean) -> Unit,
    onDoubleTapWindowChange: (Long) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onTestRunBinding: (KeyBindingItem) -> Unit,
    onToggleBinding: (index: Int, enabled: Boolean) -> Unit,
    onEditBinding: (index: Int) -> Unit,
    onDeleteRequest: (index: Int) -> Unit,
    onDeleteConfirm: (index: Int) -> Unit,
    onDeleteDismiss: () -> Unit,
    onSelectKnownButton: (code: Int) -> Unit,
    onSelectCustomButton: () -> Unit,
    onManualKeyCodeChange: (String) -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onSelectPressType: (String) -> Unit,
    onToggleBlockNativeSingle: (Boolean) -> Unit,
    onSelectActionKind: (String) -> Unit,
    onSelectCuratedAction: (String) -> Unit,
    onSelectPayloadValue: (String) -> Unit,
    onClipBeforeChange: (Int) -> Unit,
    onClipAfterChange: (Int) -> Unit,
    onSelectAppPackage: (String) -> Unit,
    onToggleAppSplitScreen: (Boolean) -> Unit,
    onShellCommandChange: (String) -> Unit,
    onSaveBinding: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OverdriveTheme.colors.background)
    ) {
        // Top Header
        KeyMappingHeader(
            state = state,
            onTabSelected = onTabSelected
        )

        // Accessibility Service Status Banner (if mapping enabled but a11y not bound)
        if (state.isMasterEnabled && !state.isA11yBound) {
            AccessibilityWarningBanner(
                onOpenSettings = onOpenAccessibilitySettings
            )
        }

        // Body Content based on Tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                KeyMappingTab.BINDINGS -> {
                    BindingsListTab(
                        state = state,
                        onTestRunBinding = onTestRunBinding,
                        onToggleBinding = onToggleBinding,
                        onEditBinding = onEditBinding,
                        onDeleteRequest = onDeleteRequest,
                        onNavigateToAdd = { onTabSelected(KeyMappingTab.ADD_EDIT) }
                    )
                }
                KeyMappingTab.ADD_EDIT -> {
                    AddEditBindingTab(
                        state = state,
                        onSelectKnownButton = onSelectKnownButton,
                        onSelectCustomButton = onSelectCustomButton,
                        onManualKeyCodeChange = onManualKeyCodeChange,
                        onStartCapture = onStartCapture,
                        onStopCapture = onStopCapture,
                        onSelectPressType = onSelectPressType,
                        onToggleBlockNativeSingle = onToggleBlockNativeSingle,
                        onSelectActionKind = onSelectActionKind,
                        onSelectCuratedAction = onSelectCuratedAction,
                        onSelectPayloadValue = onSelectPayloadValue,
                        onClipBeforeChange = onClipBeforeChange,
                        onClipAfterChange = onClipAfterChange,
                        onSelectAppPackage = onSelectAppPackage,
                        onToggleAppSplitScreen = onToggleAppSplitScreen,
                        onShellCommandChange = onShellCommandChange,
                        onSaveBinding = onSaveBinding,
                        onCancelEdit = onCancelEdit
                    )
                }
                KeyMappingTab.SETTINGS -> {
                    SettingsTab(
                        state = state,
                        onToggleMaster = onToggleMaster,
                        onToggleAllowAdvanced = onToggleAllowAdvanced,
                        onDoubleTapWindowChange = onDoubleTapWindowChange,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (state.deleteConfirmIndex != null) {
        val targetIndex = state.deleteConfirmIndex
        val targetBinding = state.bindings.find { it.index == targetIndex }
        OverdriveDialog(
            title = "Eşlemeyi Sil",
            onDismissRequest = onDeleteDismiss,
            positiveButtonText = "Sil",
            onPositiveClick = { onDeleteConfirm(targetIndex) },
            negativeButtonText = "İptal",
            onNegativeClick = onDeleteDismiss
        ) {
            Text(
                text = "${targetBinding?.buttonName ?: "Bu tuş"} için tanımlanmış eylem eşlemesini silmek istediğinizden emin misiniz?",
                style = MaterialTheme.typography.bodyMedium,
                color = OverdriveTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun KeyMappingHeader(
    state: KeyMappingUiState,
    onTabSelected: (KeyMappingTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = OverdriveTheme.colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OverdriveTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_key_mapping),
                            contentDescription = null,
                            tint = OverdriveTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Tuş Eşleme",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = OverdriveTheme.colors.textPrimary
                            )

                            OverdriveStatusPill(
                                status = if (state.isMasterEnabled) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                                label = if (state.isMasterEnabled) "ETKİN" else "DEVRE DIŞI"
                            )
                        }

                        Text(
                            text = "Direksiyon ve konsol tuşlarına özel eylemler, anlık klip ve uygulamalar atayın",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                }

                // Eşleme Sayısı Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "${state.bindings.size} Eşleme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KeyMappingTab.values().forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    val bgColor by animateColorAsState(
                        if (isSelected) OverdriveTheme.colors.primary else Color.Transparent,
                        label = "tabBg"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary,
                        label = "tabText"
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTabSelected(tab) },
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (tab == KeyMappingTab.ADD_EDIT && state.editIndex != null) "Eşlemeyi Düzenle" else tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityWarningBanner(
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color(0xFF332005),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "Erişilebilirlik Hizmeti Gerekli",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "Direksiyon ve panel tuşlarının algılanabilmesi için Overdrive Erişilebilirlik Hizmeti'nin açık olması gerekir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            OverdriveButton(
                text = "Ayarları Aç",
                onClick = onOpenSettings,
                variant = OverdriveButtonVariant.PRIMARY,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_outward),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun BindingsListTab(
    state: KeyMappingUiState,
    onTestRunBinding: (KeyBindingItem) -> Unit,
    onToggleBinding: (Int, Boolean) -> Unit,
    onEditBinding: (Int) -> Unit,
    onDeleteRequest: (Int) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    if (state.bindings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(OverdriveTheme.colors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_key_mapping),
                        contentDescription = null,
                        tint = OverdriveTheme.colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "Henüz Tanımlı Tuş Eşlemesi Yok",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Text(
                    text = "Direksiyon üzerindeki tuşlara ve konsol butonlarına dilediğiniz araç eylemlerini bağlayabilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OverdriveTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OverdriveButton(
                    text = "İlk Eşlemeyi Ekle",
                    onClick = onNavigateToAdd,
                    variant = OverdriveButtonVariant.PRIMARY,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.bindings, key = { it.index }) { item ->
                BindingRowCard(
                    item = item,
                    onTestRun = { onTestRunBinding(item) },
                    onToggle = { enabled -> onToggleBinding(item.index, enabled) },
                    onEdit = { onEditBinding(item.index) },
                    onDelete = { onDeleteRequest(item.index) }
                )
            }
        }
    }
}

@Composable
private fun BindingRowCard(
    item: KeyBindingItem,
    onTestRun: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    OverdriveCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol Taraf: Tuş Kodu ve Eylem Bilgisi
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key Code Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isEnabled) OverdriveTheme.colors.primary.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (item.isEnabled) OverdriveTheme.colors.primary.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = "${item.keyCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (item.isEnabled) OverdriveTheme.colors.primary else OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.buttonName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isEnabled) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                        )

                        // Press Type Pill
                        val pressLabel = when (item.pressType) {
                            "double" -> "Çift Basış"
                            "long" -> "Uzun Basış"
                            else -> "Tek Basış"
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = pressLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = OverdriveTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (item.pressType == "double" && item.blockNativeSingle) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E3A8A).copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "Tek Basışı Engeller",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = item.actionDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.isEnabled) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textSecondary.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sağ Taraf: Eylemler (Test Run, Switch, Edit, Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Run Button
                IconButton(
                    onClick = onTestRun,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_circle),
                        contentDescription = "Test Et",
                        tint = OverdriveTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Enable/Disable Switch
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Düzenle",
                        tint = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Sil",
                        tint = OverdriveTheme.colors.accentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditBindingTab(
    state: KeyMappingUiState,
    onSelectKnownButton: (Int) -> Unit,
    onSelectCustomButton: () -> Unit,
    onManualKeyCodeChange: (String) -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onSelectPressType: (String) -> Unit,
    onToggleBlockNativeSingle: (Boolean) -> Unit,
    onSelectActionKind: (String) -> Unit,
    onSelectCuratedAction: (String) -> Unit,
    onSelectPayloadValue: (String) -> Unit,
    onClipBeforeChange: (Int) -> Unit,
    onClipAfterChange: (Int) -> Unit,
    onSelectAppPackage: (String) -> Unit,
    onToggleAppSplitScreen: (Boolean) -> Unit,
    onShellCommandChange: (String) -> Unit,
    onSaveBinding: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section 1: Tuş Seçimi & Canlı Yakalama
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Donanım Tuşunu Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                // Known Button Dropdown
                var buttonDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = buttonDropdownExpanded,
                    onExpandedChange = { buttonDropdownExpanded = !buttonDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentText = if (state.isCustomButton) {
                        "Özel Tuş Kodu veya Canlı Yakalama"
                    } else {
                        KeyMappingCatalog.getButtonName(state.selectedButtonCode)
                    }

                    OutlinedTextField(
                        value = currentText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buttonDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OverdriveTheme.colors.primary,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        ),
                        label = { Text("Fiziksel Tuş / Buton", color = OverdriveTheme.colors.textSecondary) }
                    )

                    ExposedDropdownMenu(
                        expanded = buttonDropdownExpanded,
                        onDismissRequest = { buttonDropdownExpanded = false },
                        modifier = Modifier.background(OverdriveTheme.colors.cardBackground)
                    ) {
                        KeyMappingCatalog.KNOWN_BUTTONS.forEach { btn ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${btn.code}: ${btn.name}",
                                        color = OverdriveTheme.colors.textPrimary
                                    )
                                },
                                onClick = {
                                    onSelectKnownButton(btn.code)
                                    buttonDropdownExpanded = false
                                }
                            )
                        }

                        HorizontalDivider(color = OverdriveTheme.colors.cardBorder)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "🔍 Özel Tuş Kodu / Canlı Yakalama",
                                    color = OverdriveTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                onSelectCustomButton()
                                buttonDropdownExpanded = false
                            }
                        )
                    }
                }

                // Custom Key / Capture Mode Area
                AnimatedVisibility(visible = state.isCustomButton) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                OverdriveTheme.colors.background.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Canlı Tuş Yakalama & Özel Kod",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )

                        Text(
                            text = "Direksiyon tuşuna basarak kodunu otomatik yakalayabilir veya tuş kodunu doğrudan yazabilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = state.manualKeyCodeText,
                                onValueChange = onManualKeyCodeChange,
                                label = { Text("Tuş Kodu (Örn: 87)", color = OverdriveTheme.colors.textSecondary) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OverdriveTheme.colors.primary,
                                    unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                                    focusedTextColor = OverdriveTheme.colors.textPrimary,
                                    unfocusedTextColor = OverdriveTheme.colors.textPrimary
                                )
                            )

                            OverdriveButton(
                                text = if (state.isCapturing) "Durdur" else "Tuşa Basarak Yakala",
                                onClick = if (state.isCapturing) onStopCapture else onStartCapture,
                                variant = if (state.isCapturing) OverdriveButtonVariant.DANGER else OverdriveButtonVariant.PRIMARY,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(if (state.isCapturing) R.drawable.ic_clear else R.drawable.ic_crop_free),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // Capture Active Feedback
                        if (state.isCapturing) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = OverdriveTheme.colors.primary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OverdriveTheme.colors.primary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = OverdriveTheme.colors.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Tuş dinleniyor... Direksiyon veya konsoldaki herhangi bir tuşa basın.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OverdriveTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (state.capturedFeedback != null) {
                            Text(
                                text = state.capturedFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = OverdriveTheme.colors.accentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Basış Tipi (Tek / Çift / Uzun)
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. Basış Türünü Belirleyin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pressOptions = listOf(
                        "single" to "Tek Basış",
                        "double" to "Çift Basış",
                        "long" to "Uzun Basış"
                    )

                    pressOptions.forEach { (typeKey, typeLabel) ->
                        val isSelected = state.selectedPressType == typeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectPressType(typeKey) },
                            color = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Çift basışta tek basışı engelleme seçeneği
                AnimatedVisibility(visible = state.selectedPressType == "double") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                OverdriveTheme.colors.background.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tek Basış Varsayılan Eylemini Engelle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OverdriveTheme.colors.textPrimary
                            )
                            Text(
                                text = "Çift basış yaparken aracın orijinal tek basış fonksiyonunun (şarkı değiştirme vb.) devreye girmesini önler.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OverdriveTheme.colors.textSecondary
                            )
                        }

                        Switch(
                            checked = state.blockNativeSingle,
                            onCheckedChange = onToggleBlockNativeSingle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OverdriveTheme.colors.primary,
                                checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                                uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                                uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                            )
                        )
                    }
                }
            }
        }

        // Section 3: Eylem Türü ve Parametreleri
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "3. Tetiklenecek Eylemi Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                // Action Kind Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val kindList = mutableListOf(
                        "curated" to "Araç Eylemi",
                        "manualClip" to "Anlık Klip",
                        "openApp" to "Uygulama Başlat"
                    )
                    if (state.allowAdvanced) {
                        kindList.add("shell" to "Kabuk (Shell)")
                    }

                    kindList.forEach { (kindKey, kindLabel) ->
                        val isSelected = state.actionKind == kindKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectActionKind(kindKey) },
                            color = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = kindLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.4f))

                // Kind Specific Config
                when (state.actionKind) {
                    "curated" -> {
                        CuratedActionPicker(
                            selectedId = state.selectedCuratedId,
                            selectedPayload = state.selectedPayloadValue,
                            onSelectCurated = onSelectCuratedAction,
                            onSelectPayload = onSelectPayloadValue
                        )
                    }
                    "manualClip" -> {
                        ManualClipConfig(
                            beforeSec = state.clipBeforeSeconds,
                            afterSec = state.clipAfterSeconds,
                            onBeforeChange = onClipBeforeChange,
                            onAfterChange = onClipAfterChange
                        )
                    }
                    "openApp" -> {
                        OpenAppConfig(
                            apps = state.launchableApps,
                            selectedPackage = state.selectedAppPackage,
                            splitScreen = state.appSplitScreen,
                            onSelectApp = onSelectAppPackage,
                            onToggleSplit = onToggleAppSplitScreen
                        )
                    }
                    "shell" -> {
                        ShellConfig(
                            cmd = state.shellCommandText,
                            onCmdChange = onShellCommandChange
                        )
                    }
                }
            }
        }

        // Action Buttons: Save & Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.editIndex != null) {
                OverdriveButton(
                    text = "İptal",
                    onClick = onCancelEdit,
                    variant = OverdriveButtonVariant.OUTLINED,
                    modifier = Modifier.weight(1f)
                )
            }

            OverdriveButton(
                text = if (state.editIndex != null) "Değişiklikleri Kaydet" else "Eşlemeyi Ekle",
                onClick = onSaveBinding,
                variant = OverdriveButtonVariant.PRIMARY,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuratedActionPicker(
    selectedId: String,
    selectedPayload: String,
    onSelectCurated: (String) -> Unit,
    onSelectPayload: (String) -> Unit
) {
    val currentAction = KeyMappingCatalog.CURATED_ACTIONS.find { it.id == selectedId }
        ?: KeyMappingCatalog.CURATED_ACTIONS.first()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Curated Action Dropdown
        var actionExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = actionExpanded,
            onExpandedChange = { actionExpanded = !actionExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "${currentAction.category}: ${currentAction.title}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OverdriveTheme.colors.primary,
                    unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                    focusedTextColor = OverdriveTheme.colors.textPrimary,
                    unfocusedTextColor = OverdriveTheme.colors.textPrimary
                ),
                label = { Text("Eylem Seçin", color = OverdriveTheme.colors.textSecondary) }
            )

            ExposedDropdownMenu(
                expanded = actionExpanded,
                onDismissRequest = { actionExpanded = false },
                modifier = Modifier.background(OverdriveTheme.colors.cardBackground)
            ) {
                val grouped = KeyMappingCatalog.CURATED_ACTIONS.groupBy { it.category }
                grouped.forEach { (category, actions) ->
                    Text(
                        text = category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    actions.forEach { act ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = act.title,
                                    color = OverdriveTheme.colors.textPrimary
                                )
                            },
                            onClick = {
                                onSelectCurated(act.id)
                                actionExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.3f))
                }
            }
        }

        // Action Payload Dropdown (if curated action has payload options)
        if (currentAction.payloadOptions.isNotEmpty()) {
            var payloadExpanded by remember { mutableStateOf(false) }
            val currentPayloadOpt = currentAction.payloadOptions.find { it.first == selectedPayload }
                ?: currentAction.payloadOptions.first()

            ExposedDropdownMenuBox(
                expanded = payloadExpanded,
                onExpandedChange = { payloadExpanded = !payloadExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentPayloadOpt.second,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payloadExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.primary,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    ),
                    label = { Text("Eylem Değeri / Parametre", color = OverdriveTheme.colors.textSecondary) }
                )

                ExposedDropdownMenu(
                    expanded = payloadExpanded,
                    onDismissRequest = { payloadExpanded = false },
                    modifier = Modifier.background(OverdriveTheme.colors.cardBackground)
                ) {
                    currentAction.payloadOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt.second,
                                    color = OverdriveTheme.colors.textPrimary
                                )
                            },
                            onClick = {
                                onSelectPayload(opt.first)
                                payloadExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualClipConfig(
    beforeSec: Int,
    afterSec: Int,
    onBeforeChange: (Int) -> Unit,
    onAfterChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tuşa basıldığında anlık kamera kaydı (Instant Replay) oluşturulur.",
            style = MaterialTheme.typography.bodySmall,
            color = OverdriveTheme.colors.textSecondary
        )

        // Before Seconds Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tuşa Basmadan Önceki Süre",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "$beforeSec saniye",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.primary
                )
            }
            Slider(
                value = beforeSec.toFloat(),
                onValueChange = { onBeforeChange(it.toInt()) },
                valueRange = 0f..60f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = OverdriveTheme.colors.primary,
                    activeTrackColor = OverdriveTheme.colors.primary
                )
            )
        }

        // After Seconds Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tuşa Bastıktan Sonraki Süre",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "$afterSec saniye",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.primary
                )
            }
            Slider(
                value = afterSec.toFloat(),
                onValueChange = { onAfterChange(it.toInt()) },
                valueRange = 0f..60f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = OverdriveTheme.colors.primary,
                    activeTrackColor = OverdriveTheme.colors.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenAppConfig(
    apps: List<AppOption>,
    selectedPackage: String,
    splitScreen: Boolean,
    onSelectApp: (String) -> Unit,
    onToggleSplit: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        var appExpanded by remember { mutableStateOf(false) }
        val currentApp = apps.find { it.packageName == selectedPackage }
        val appTitle = currentApp?.label ?: if (selectedPackage.isNotBlank()) selectedPackage else "Bir Uygulama Seçin"

        ExposedDropdownMenuBox(
            expanded = appExpanded,
            onExpandedChange = { appExpanded = !appExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = appTitle,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OverdriveTheme.colors.primary,
                    unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                    focusedTextColor = OverdriveTheme.colors.textPrimary,
                    unfocusedTextColor = OverdriveTheme.colors.textPrimary
                ),
                label = { Text("Başlatılacak Uygulama", color = OverdriveTheme.colors.textSecondary) }
            )

            ExposedDropdownMenu(
                expanded = appExpanded,
                onDismissRequest = { appExpanded = false },
                modifier = Modifier.background(OverdriveTheme.colors.cardBackground)
            ) {
                apps.forEach { app ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = app.label,
                                color = OverdriveTheme.colors.textPrimary
                            )
                        },
                        onClick = {
                            onSelectApp(app.packageName)
                            appExpanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    OverdriveTheme.colors.background.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bölünmüş Ekranda Başlat (Split Screen)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Uygulamayı mevcut ekranı ikiye bölerek yan tarafta açar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OverdriveTheme.colors.textSecondary
                )
            }

            Switch(
                checked = splitScreen,
                onCheckedChange = onToggleSplit,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OverdriveTheme.colors.primary,
                    checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                    uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                )
            )
        }
    }
}

@Composable
private fun ShellConfig(
    cmd: String,
    onCmdChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Gelişmiş Kabuk (Shell) Komutu",
            style = MaterialTheme.typography.bodySmall,
            color = OverdriveTheme.colors.textSecondary
        )

        OutlinedTextField(
            value = cmd,
            onValueChange = onCmdChange,
            label = { Text("sh Komutu (Örn: am start -n ...)", color = OverdriveTheme.colors.textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OverdriveTheme.colors.primary,
                unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                focusedTextColor = OverdriveTheme.colors.textPrimary,
                unfocusedTextColor = OverdriveTheme.colors.textPrimary
            )
        )
    }
}

@Composable
private fun SettingsTab(
    state: KeyMappingUiState,
    onToggleMaster: (Boolean) -> Unit,
    onToggleAllowAdvanced: (Boolean) -> Unit,
    onDoubleTapWindowChange: (Long) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Enable Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tuş Eşleme Sistemini Etkinleştir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Fiziksel donanım tuşlarının Overdrive tarafından yakalanmasını ve eşlenen eylemlerin tetiklenmesini sağlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.isMasterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Double Tap Window Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Çift Basış Algılama Süresi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "İki tuş basışı arasında çift tıklama olarak sayılacak maksimum zaman penceresi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    Text(
                        text = "${state.doubleTapWindowMs} ms (${String.format("%.1f", state.doubleTapWindowMs / 1000f)} sn)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.primary
                    )
                }

                Slider(
                    value = state.doubleTapWindowMs.toFloat(),
                    onValueChange = { onDoubleTapWindowChange(it.toLong()) },
                    valueRange = 250f..1500f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.primary,
                        activeTrackColor = OverdriveTheme.colors.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "250 ms (Hızlı)", style = MaterialTheme.typography.labelSmall, color = OverdriveTheme.colors.textSecondary)
                    Text(text = "450 ms (Varsayılan)", style = MaterialTheme.typography.labelSmall, color = OverdriveTheme.colors.textSecondary)
                    Text(text = "1500 ms (Yavaş)", style = MaterialTheme.typography.labelSmall, color = OverdriveTheme.colors.textSecondary)
                }
            }
        }

        // Allow Advanced Actions Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gelişmiş Kabuk (Shell) Komutları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Tuşlara özel Linux/Android shell scriptleri ve komutları atamaya izin verir. Yalnızca deneyimli kullanıcılar içindir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.allowAdvanced,
                    onCheckedChange = onToggleAllowAdvanced,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.accentRed,
                        checkedTrackColor = OverdriveTheme.colors.accentRed.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Accessibility Service Info Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Erişilebilirlik Hizmeti Durumu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = if (state.isA11yBound) "Overdrive Erişilebilirlik Hizmeti aktif ve tuşları dinliyor." else "Hizmet bağlı değil. Tuş eşlemenin çalışabilmesi için açılmalıdır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isA11yBound) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.accentAmber
                    )
                }

                OverdriveButton(
                    text = "Sistem Ayarları",
                    onClick = onOpenAccessibilitySettings,
                    variant = OverdriveButtonVariant.OUTLINED,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}
