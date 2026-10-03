package com.overdrive.app.byd.adb

import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android 11+ Kablosuz Hata Ayıklama (Wireless Debugging) modunda dinamik olarak
 * atanan rastgele portları (ör. 30000-50000) yerel döngü (127.0.0.1) üzerinde tespit eden
 * yüksek performanslı çok kanallı soket tarayıcısı.
 */
object AdbPortScanner {

    private const val TAG = "AdbPortScanner"
    private const val DEFAULT_START_PORT = 30000
    private const val DEFAULT_END_PORT = 50000
    private const val DEFAULT_WORKER_THREADS = 64
    private const val SOCKET_CONNECT_TIMEOUT_MS = 100

    /**
     * Yerel açık ADB dinleme portunu eşzamanlı olarak tarar.
     * İlk açık port tespit edildiğinde diğer tüm tarama thread'leri anında sonlandırılır.
     *
     * @return Bulunan açık port numarası veya bulunamazsa null
     */
    fun findOpenPort(
        startPort: Int = DEFAULT_START_PORT,
        endPort: Int = DEFAULT_END_PORT,
        workerThreads: Int = DEFAULT_WORKER_THREADS,
        timeoutMs: Long = 4000L
    ): Int? {
        val foundPort = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(workerThreads) { r ->
            Thread(r, "AdbPortScanWorker").apply { isDaemon = true }
        }
        val latch = CountDownLatch(1)

        val totalPorts = endPort - startPort + 1
        for (i in 0 until totalPorts) {
            val port = startPort + i
            pool.execute {
                if (foundPort.get() != 0) return@execute
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress("127.0.0.1", port), SOCKET_CONNECT_TIMEOUT_MS)
                        if (foundPort.compareAndSet(0, port)) {
                            Log.i(TAG, "✓ Açık ADB loopback portu bulundu: $port")
                            latch.countDown()
                        }
                    }
                } catch (ignored: Throwable) {
                    // Port kapalı veya bağlantı reddedildi
                }
            }
        }

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            pool.shutdownNow()
        }

        val detected = foundPort.get()
        return if (detected > 0) detected else null
    }

    /**
     * Belirli bir portun 127.0.0.1 üzerinde dinlemede olup olmadığını hızlıca kontrol eder.
     */
    fun isPortListening(port: Int, timeoutMs: Int = 150): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                true
            }
        } catch (t: Throwable) {
            false
        }
    }
}
