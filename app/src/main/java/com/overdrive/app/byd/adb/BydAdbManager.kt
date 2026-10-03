package com.overdrive.app.byd.adb

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.overdrive.app.logging.LogManager
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BYD'nin yerel araç çerçevesi (ts-framework.jar / SettingsAdapterManager) ile doğrudan
 * etkileşime girerek araç içi Kablosuz ADB'yi (TCP Port 5555) harici bir PC veya kablo
 * gerekmeksizin programatik olarak açar ve denetler.
 */
object BydAdbManager {

    private const val TAG = "BydAdbManager"
    const val DEFAULT_ADB_PORT = 5555

    private const val PROP_WIRESS_ENABLE = "persist.sys.adb.wiress.enable"
    private const val PROP_WIRESS_CONNECT = "sys.connect.adb.wiress"
    private const val PROP_USB_CONFIG = "persist.sys.usb.config"

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BydAdbWorker").apply { isDaemon = true }
    }

    private val isActivating = AtomicBoolean(false)
    private val logManager = LogManager.getInstance()

    sealed class AdbStatus {
        object Idle : AdbStatus()
        object Checking : AdbStatus()
        object Activating : AdbStatus()
        data class Connected(val port: Int) : AdbStatus()
        data class Error(val message: String) : AdbStatus()
    }

    private val _status = MutableStateFlow<AdbStatus>(AdbStatus.Idle)
    val status: StateFlow<AdbStatus> = _status.asStateFlow()

    @Volatile
    var lastActivePort: Int = DEFAULT_ADB_PORT
        private set

    /**
     * ADB'nin yerel 127.0.0.1:5555 (veya verilen portta) dinlemede olup olmadığını hızlıca kontrol eder.
     */
    fun isAdbListening(port: Int = DEFAULT_ADB_PORT): Boolean {
        return AdbPortScanner.isPortListening(port)
    }

    /**
     * Port kapalıysa otomatik olarak arka planda BYD framework'ü ile uyandırır.
     * Eğer port zaten açıksa hiçbir işlem yapmadan başarı döner.
     */
    fun ensureAdbEnabledAsync(context: Context, onResult: ((Boolean) -> Unit)? = null) {
        executor.execute {
            if (isAdbListening(DEFAULT_ADB_PORT)) {
                lastActivePort = DEFAULT_ADB_PORT
                _status.value = AdbStatus.Connected(DEFAULT_ADB_PORT)
                onResult?.invoke(true)
                return@execute
            }

            // Port kapalı, uyandırma sürecini başlat
            enableWirelessAdbInternal(context.applicationContext) { success, _ ->
                onResult?.invoke(success)
            }
        }
    }

    /**
     * Kullanıcı isteği veya servis tetiklemesiyle BYD araç içi Kablosuz ADB aktivasyonunu başlatır.
     */
    fun enableWirelessAdbAsync(context: Context, callback: ((Boolean, String) -> Unit)? = null) {
        executor.execute {
            enableWirelessAdbInternal(context.applicationContext, callback)
        }
    }

    private fun enableWirelessAdbInternal(
        context: Context,
        callback: ((Boolean, String) -> Unit)?
    ) {
        if (!isActivating.compareAndSet(false, true)) {
            val msg = "Aktivasyon işlemi zaten devam ediyor..."
            logWarn(msg)
            callback?.invoke(false, msg)
            return
        }

        _status.value = AdbStatus.Activating
        logInfo("⚡ BYD ts-framework üzerinden yerel ADB uyandırma süreci başlatıldı...")

        try {
            // 1. Zaten açık mı?
            if (AdbPortScanner.isPortListening(DEFAULT_ADB_PORT)) {
                lastActivePort = DEFAULT_ADB_PORT
                _status.value = AdbStatus.Connected(DEFAULT_ADB_PORT)
                val msg = "ADB 5555 zaten dinleme modunda aktif."
                logInfo("✓ $msg")
                callback?.invoke(true, msg)
                return
            }

            // 2. BYD SettingsService / Property IPC servisine bağlan
            val propertyService = connectToBydPropertyService(context, 5000L)
            if (propertyService == null) {
                // Servis bulunamadı - Belki sistemde port zaten başka bir aralıkta açıktır?
                logWarn("BYD SettingsService bağlanamadı, alternatif açık port taranıyor...")
                val openPort = AdbPortScanner.findOpenPort()
                if (openPort != null) {
                    lastActivePort = openPort
                    _status.value = AdbStatus.Connected(openPort)
                    val msg = "Açık ADB portu bulundu: $openPort"
                    logInfo("✓ $msg")
                    callback?.invoke(true, msg)
                    return
                }

                val err = "BYD sistem servisi (ts-framework) bulunamadı veya bağlanılamadı"
                _status.value = AdbStatus.Error(err)
                logError(err)
                callback?.invoke(false, err)
                return
            }

            // 3. Kablosuz ADB kalıcılığını aç
            logInfo("1. $PROP_WIRESS_ENABLE = true yazılıyor...")
            writeProperty(propertyService, PROP_WIRESS_ENABLE, "true")

            // 4. Mevcut durumu oku ve gerekirse tazele
            val currentState = readProperty(propertyService, PROP_WIRESS_CONNECT)
            logInfo("Mevcut $PROP_WIRESS_CONNECT değeri: $currentState")
            if ("1" == currentState) {
                logInfo("ADB servisini tazelemek için geçici '0' veriliyor...")
                writeProperty(propertyService, PROP_WIRESS_CONNECT, "0")
                SystemClock.sleep(150)
            }

            // 5. Bağlantı anahtarını 1 yap
            logInfo("2. $PROP_WIRESS_CONNECT = 1 yazılıyor...")
            writeProperty(propertyService, PROP_WIRESS_CONNECT, "1")

            // 6. USB yapılandırmasına da adb ekle
            try {
                writeProperty(propertyService, PROP_USB_CONFIG, "adb")
            } catch (ignored: Throwable) {}

            // 7. Doğrula
            val verified = readProperty(propertyService, PROP_WIRESS_CONNECT)
            logInfo("Yazma sonrası $PROP_WIRESS_CONNECT: $verified")

            // 8. 127.0.0.1:5555 soketinin dinlemeye geçmesini bekle (azami 4.5 sn)
            logInfo("Port 5555 soketinin açılması bekleniyor (4.5 sn polling)...")
            val deadline = System.currentTimeMillis() + 4500L
            var opened = false

            while (System.currentTimeMillis() < deadline) {
                if (AdbPortScanner.isPortListening(DEFAULT_ADB_PORT)) {
                    opened = true
                    break
                }
                SystemClock.sleep(200)
            }

            if (opened) {
                lastActivePort = DEFAULT_ADB_PORT
                _status.value = AdbStatus.Connected(DEFAULT_ADB_PORT)
                val msg = "127.0.0.1:5555 portu araç içinde açıldı ve dinlemede!"
                logInfo("✓ BAŞARILI! $msg")
                callback?.invoke(true, msg)
            } else {
                // Standart 5555 açılmadıysa dinamik port taraması yap
                logWarn("Port 5555 yanıt vermedi, dinamik açık port taranıyor...")
                val altPort = AdbPortScanner.findOpenPort()
                if (altPort != null) {
                    lastActivePort = altPort
                    _status.value = AdbStatus.Connected(altPort)
                    val msg = "Dinamik Kablosuz ADB portu tespit edildi: $altPort"
                    logInfo("✓ $msg")
                    callback?.invoke(true, msg)
                } else {
                    val errMsg = "Parametreler yazıldı ancak 127.0.0.1:5555 soketi dinlemeye geçmedi"
                    _status.value = AdbStatus.Error(errMsg)
                    logWarn("⚠ $errMsg")
                    callback?.invoke(false, errMsg)
                }
            }

        } catch (t: Throwable) {
            val err = "BYD ADB aktivasyon hatası: ${t.message}"
            _status.value = AdbStatus.Error(err)
            logError(err, t)
            callback?.invoke(false, err)
        } finally {
            isActivating.set(false)
        }
    }

    private fun connectToBydPropertyService(context: Context, timeoutMs: Long): Any? {
        val clazz: Class<*> = try {
            Class.forName("com.ts.lib.settings.SettingsAdapterManager")
        } catch (e: ClassNotFoundException) {
            val jar = File("/system/framework/ts-framework.jar")
            if (jar.exists() && jar.canRead()) {
                val loader = PathClassLoader(jar.absolutePath, context.classLoader)
                Class.forName("com.ts.lib.settings.SettingsAdapterManager", true, loader)
            } else {
                return null
            }
        }

        val getInstance = clazz.getMethod("getInstance", Context::class.java)
        val manager = getInstance.invoke(null, context.applicationContext) ?: return null

        val connectMethod = clazz.getMethod("connect")
        connectMethod.invoke(manager)

        val isServiceBound = clazz.getMethod("isServiceBound")
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            val bound = isServiceBound.invoke(manager) as? Boolean
            if (bound == true) break
            SystemClock.sleep(50)
        }

        val bound = isServiceBound.invoke(manager) as? Boolean
        if (bound != true) {
            logWarn("BYD SettingsService IPC bağlantı zaman aşımı.")
            return null
        }

        val getSettingsAdapterManager = clazz.getMethod("getSettingsAdapterManager", String::class.java)
        return getSettingsAdapterManager.invoke(manager, "property")
    }

    private fun readProperty(propertyService: Any, key: String): String? {
        return try {
            val m = propertyService.javaClass.getMethod("getSystemPersist", String::class.java)
            m.invoke(propertyService, key) as? String
        } catch (t: Throwable) {
            null
        }
    }

    private fun writeProperty(propertyService: Any, key: String, value: String) {
        val m = propertyService.javaClass.getMethod("setSystemPersist", String::class.java, String::class.java)
        m.invoke(propertyService, key, value)
    }

    /**
     * Araç ekranında Android Geliştirici Seçenekleri veya BYD Mühendislik Modunu doğrudan açar.
     * Kullanıcının Kablosuz Hata Ayıklama anahtarını tek tıkla açabilmesini sağlar.
     */
    fun openDeveloperSettings(context: Context): Boolean {
        val targets = arrayOf(
            "com.byd.engineermode",
            "com.android.car.developeroptions",
            "com.android.settings"
        )
        val pm = context.packageManager
        for (pkg in targets) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (ignored: Throwable) {}
        }

        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (ignored: Throwable) {
                false
            }
        }
    }

    private fun logInfo(msg: String) {
        Log.i(TAG, msg)
        try { logManager.info(TAG, msg) } catch (ignored: Throwable) {}
    }

    private fun logWarn(msg: String) {
        Log.w(TAG, msg)
        try { logManager.warn(TAG, msg) } catch (ignored: Throwable) {}
    }

    private fun logError(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
        try { logManager.error(TAG, msg, t) } catch (ignored: Throwable) {}
    }
}
