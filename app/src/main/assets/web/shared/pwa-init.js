/**
 * OverDrive PWA bootstrap.
 *
 * Loaded after auth.js on every dashboard page. Skips entirely on the
 * in-app WebView (loopback host) so notification permission prompts never
 * appear inside the car. On the user's external phone install, registers
 * the service worker, requests notification permission, subscribes to push,
 * and posts the resulting subscription to the head unit.
 *
 * No-op on browsers that don't support PWAs (e.g. older Android browsers).
 */
(function () {
    'use strict';

    if (typeof navigator === 'undefined') return;

    // Check if running inside the in-car native Android WebView
    var embeddedInNativeApp = typeof window.AndroidBridge !== 'undefined';
    if (embeddedInNativeApp) {
        // WebView or in-car head unit — suppress PWA and notification bootstrap
        return;
    }

    var isStandalone = window.matchMedia('(display-mode: standalone)').matches
        || window.navigator.standalone === true
        || (typeof document !== 'undefined' && document.referrer && document.referrer.includes('android-app://'));

    var deferredInstallPrompt = null;

    function log() {
        if (window.console && console.log) {
            console.log.apply(console, ['[pwa]'].concat([].slice.call(arguments)));
        }
    }

    function urlBase64ToUint8Array(b64) {
        // Web Push expects applicationServerKey as Uint8Array of the raw
        // 65-byte uncompressed P-256 point, decoded from base64url.
        var padding = '='.repeat((4 - b64.length % 4) % 4);
        var base64 = (b64 + padding).replace(/-/g, '+').replace(/_/g, '/');
        var raw = atob(base64);
        var arr = new Uint8Array(raw.length);
        for (var i = 0; i < raw.length; ++i) arr[i] = raw.charCodeAt(i);
        return arr;
    }

    function authedFetch(url, opts) {
        opts = opts || {};
        opts.headers = opts.headers || {};
        if (typeof BYDAuth !== 'undefined' && BYDAuth.getToken) {
            var t = BYDAuth.getToken();
            if (t) opts.headers['Authorization'] = 'Bearer ' + t;
        }
        return fetch(url, opts);
    }

    async function getCategoriesAndKey() {
        var r = await authedFetch('/api/notifications/categories');
        if (!r.ok) throw new Error('categories fetch ' + r.status);
        return r.json();
    }

    // Compare two byte sequences. Inputs may be ArrayBuffer (which is what
    // PushSubscription.options.applicationServerKey returns per spec),
    // Uint8Array (what urlBase64ToUint8Array produces), or null. Returns
    // true only if both are present and byte-identical.
    function bytesEqual(a, b) {
        if (!a || !b) return false;
        var ua = a instanceof Uint8Array ? a : new Uint8Array(a);
        var ub = b instanceof Uint8Array ? b : new Uint8Array(b);
        if (ua.length !== ub.length) return false;
        for (var i = 0; i < ua.length; i++) if (ua[i] !== ub[i]) return false;
        return true;
    }

    async function ensureSubscription(reg, vapidPublicKey) {
        var wantKey = urlBase64ToUint8Array(vapidPublicKey);
        var existing = await reg.pushManager.getSubscription();
        if (existing) {
            // Verify the existing sub is bound to the SERVER'S CURRENT
            // VAPID key. If the daemon was reinstalled (key file lost) or
            // a different OverDrive install ever ran on this origin, the
            // browser is holding a sub bound to a stale key. Calling
            // subscribe() again with a new key while a sub with a
            // different key exists is the canonical cause of "AbortError:
            // Registration failed - push service error" on Samsung
            // Internet, desktop Brave, and Edge — Chrome transparently
            // re-keys but stricter browsers reject. Unsubscribe first,
            // then proceed to a fresh subscribe.
            //
            // On older Samsung Internet (~v14 and below) PushSubscription
            // doesn't expose `.options`, so we can't compare. Treat the
            // missing-options case as "potentially stale" and unsubscribe
            // defensively — losing one good subscription is cheap (we
            // immediately re-subscribe) but skipping unsubscribe on a
            // stale sub is what's been blocking these browsers.
            var existingKey = existing.options && existing.options.applicationServerKey;
            if (existingKey && bytesEqual(existingKey, wantKey)) {
                return existing;
            }
            log(existingKey
                ? 'existing subscription bound to a different VAPID key — unsubscribing before re-subscribe'
                : 'existing subscription has no inspectable key (older browser) — unsubscribing defensively');
            try { await existing.unsubscribe(); } catch (e) { /* best-effort */ }
        }
        try {
            return await reg.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: wantKey
            });
        } catch (e) {
            // AbortError on a clean origin usually means the browser's
            // push service connection is wedged from a prior failure.
            // Some browsers (Samsung Internet 23+, Edge) recover after
            // an explicit clean-slate: drop ANY lingering sub on this
            // registration and retry once. If the retry also fails, the
            // browser genuinely can't reach its push service — bubble up.
            if (e && (e.name === 'AbortError' || e.name === 'InvalidStateError')) {
                log('subscribe ' + e.name + ' — retrying after explicit unsubscribe');
                try {
                    var stale = await reg.pushManager.getSubscription();
                    if (stale) await stale.unsubscribe();
                } catch (_) { /* ignore */ }
                return reg.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: wantKey
                });
            }
            throw e;
        }
    }

    async function postSubscription(sub, opts) {
        var json = sub.toJSON();
        var label = inferLabel();
        var body = {
            endpoint: json.endpoint,
            keys: json.keys,
            label: label
        };
        // Force-flag: only set when the user explicitly clicks "Enable" or
        // "Re-enable on this device". Without it, the server tombstone
        // window blocks silent re-creates that the user just removed.
        if (opts && opts.force) body.force = true;
        var r = await authedFetch('/api/push/subscribe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (r.status === 410) {
            // Server has a fresh tombstone for this id. Surface it as a
            // distinct error type so the silent self-heal can also drop the
            // browser-side subscription (otherwise we'd loop: server says
            // "no", browser still has live sub, next page load tries again).
            var err = new Error('subscribe POST 410 (tombstoned)');
            err.tombstoned = true;
            throw err;
        }
        if (!r.ok) throw new Error('subscribe POST ' + r.status);
        return r.json();
    }

    // DOMException stringifies as "[object DOMException]" in older WebViews;
    // we want the name + message so toasts read as "NotAllowedError:
    // Registration failed - push service error" rather than a generic blob.
    function errString(e) {
        if (!e) return '';
        if (e.name && e.message) return e.name + ': ' + e.message;
        if (e.name) return e.name;
        return String(e.message || e);
    }

    // Serial chain that serializes all pushManager.subscribe() + POST flows
    // in this page. Both init()'s silent resubscribe and Enable's
    // requestAndSubscribe() enqueue their work here; each caller observes
    // its own fn's return value (no result coupling), but the WORK runs
    // strictly in FIFO order. This prevents:
    //   - two concurrent pushManager.subscribe() calls (Samsung Internet
    //     has been observed to throw InvalidStateError on the second).
    //   - duplicate /api/push/subscribe POSTs from rapid Enable clicks.
    // The chain continues past errors (catch in the chain reassignment)
    // so a failed silent doesn't break a subsequent Enable. Each fn is
    // wrapped with a 30s watchdog so a hung subscribe can't block the
    // chain forever (rare on Brave/Edge with WNS connectivity issues).
    //
    // CAVEAT: do NOT call runSerial() from inside an enqueued fn — the
    // inner task would await its own outer continuation and deadlock
    // until the 30s watchdog fires. No current caller does this.
    var _subscribeChain = Promise.resolve();
    function runSerial(fn) {
        var timerId;
        var deadline = new Promise(function (_resolve, reject) {
            timerId = setTimeout(function () { reject(new Error('subscribe-timeout')); }, 30000);
        });
        // Suppress unhandledrejection if fn settles first — the deadline
        // promise will reject 30s later with no consumer otherwise.
        deadline.catch(function () {});
        function runOnce() {
            return Promise.race([fn(), deadline]).finally(function () {
                if (timerId) clearTimeout(timerId);
            });
        }
        var next = _subscribeChain.then(runOnce, runOnce);
        // Reassign the chain to the post-error continuation so a failed
        // task doesn't poison the chain.
        _subscribeChain = next.catch(function () {});
        return next;
    }

    function inferLabel() {
        // Best-effort device label from User-Agent; user can rename later.
        var ua = navigator.userAgent || '';
        if (/iPhone/i.test(ua)) return 'iPhone';
        if (/iPad/i.test(ua)) return 'iPad';
        if (/Android/i.test(ua)) {
            var m = ua.match(/Android[^;]*;\s*([^)]+)\)/);
            if (m) return m[1].trim();
            return 'Android';
        }
        return 'Browser';
    }

    function setupInstallListeners() {
        window.addEventListener('beforeinstallprompt', function (e) {
            log('beforeinstallprompt fired — app is installable');
            e.preventDefault();
            deferredInstallPrompt = e;
            window.deferredInstallPrompt = e;
            updateInstallUi();
            try {
                window.dispatchEvent(new CustomEvent('overdrive-pwa-installable'));
            } catch (err) {}
        });

        window.addEventListener('appinstalled', function () {
            log('PWA app installed successfully');
            deferredInstallPrompt = null;
            window.deferredInstallPrompt = null;
            isStandalone = true;
            try { localStorage.setItem('overdrive_pwa_installed', 'true'); } catch (e) {}
            updateInstallUi();
        });
    }

    function isBannerDismissed() {
        try {
            return sessionStorage.getItem('overdrive_pwa_dismissed') === '1'
                || localStorage.getItem('overdrive_pwa_installed') === 'true';
        } catch (e) {
            return false;
        }
    }

    function updateInstallUi() {
        // Update sidebar install button
        var sidebarBtn = document.getElementById('pwaSidebarInstallBtn');
        if (sidebarBtn) {
            sidebarBtn.style.display = isStandalone ? 'none' : 'flex';
        }

        // Show banner if installable and not dismissed and not in standalone
        var banner = document.getElementById('pwaInstallBanner');
        if (deferredInstallPrompt && !isStandalone && !isBannerDismissed()) {
            if (!banner) {
                injectInstallBanner();
            } else {
                banner.style.display = 'flex';
            }
        } else if (banner) {
            banner.style.display = 'none';
        }
    }

    function injectInstallBanner() {
        if (document.getElementById('pwaInstallBanner')) return;
        var banner = document.createElement('div');
        banner.id = 'pwaInstallBanner';
        banner.className = 'pwa-install-banner';
        banner.innerHTML = ''
            + '<div class="pwa-install-content">'
            +   '<img src="/shared/icon-192.png" class="pwa-install-icon" alt="OverDrive">'
            +   '<div class="pwa-install-text">'
            +     '<strong class="pwa-install-title">OverDrive\'ı Yükle</strong>'
            +     '<span class="pwa-install-desc">Hızlı erişim için telefonunuza uygulama olarak ekleyin</span>'
            +   '</div>'
            + '</div>'
            + '<div class="pwa-install-actions">'
            +   '<button type="button" class="pwa-install-btn" id="pwaBannerInstallBtn">Yükle</button>'
            +   '<button type="button" class="pwa-install-close" id="pwaBannerDismissBtn" aria-label="Kapat">&times;</button>'
            + '</div>';

        document.body.appendChild(banner);

        var installBtn = document.getElementById('pwaBannerInstallBtn');
        if (installBtn) {
            installBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                window.installPwa();
            });
        }

        var dismissBtn = document.getElementById('pwaBannerDismissBtn');
        if (dismissBtn) {
            dismissBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                try { sessionStorage.setItem('overdrive_pwa_dismissed', '1'); } catch (err) {}
                banner.style.display = 'none';
            });
        }
    }

    function showPwaHelpModal() {
        var existing = document.getElementById('pwaHelpModal');
        if (existing) {
            existing.style.display = 'flex';
            return;
        }

        var isSecure = window.isSecureContext;
        var isIos = /iPhone|iPad|iPod/i.test(navigator.userAgent);

        var step1, step2, tip;
        if (isIos) {
            step1 = '1. Safari\'de alt çubuktaki Paylaş (<strong><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align:middle"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/></svg></strong>) butonuna dokunun.';
            step2 = '2. Menüden <strong>"Ana Ekrana Ekle"</strong> seçeneğini seçin.';
            tip = 'Uygulama telefonunuzun ana ekranında bağımsız bir simge olarak yer alacaktır.';
        } else {
            step1 = '1. Chrome\'un sağ üst köşesindeki <strong>üç nokta (⋮)</strong> menüsüne dokunun.';
            step2 = '2. Menüden <strong>"Uygulamayı yükle"</strong> veya <strong>"Ana ekrana ekle"</strong> seçeneğini seçin.';
            tip = !isSecure 
                ? '⚠️ Not: Doğrudan IP üzerinden (HTTP) Chrome otomatik yükleme penceresi açmayabilir. Dashboard üzerindeki <strong>HTTPS Tünel</strong> adresini kullanarak tek tıkla yükleyebilirsiniz.'
                : 'Uygulama telefonunuza WebAPK olarak yüklenecek ve bağımsız tam ekran açılacaktır.';
        }

        var modal = document.createElement('div');
        modal.id = 'pwaHelpModal';
        modal.className = 'pwa-help-modal-overlay';
        modal.innerHTML = ''
            + '<div class="pwa-help-modal">'
            +   '<div class="pwa-help-modal-header">'
            +     '<div style="display:flex;align-items:center;gap:10px;">'
            +       '<img src="/shared/icon-192.png" width="28" height="28" style="border-radius:6px;" alt="OverDrive">'
            +       '<h3>OverDrive Uygulamasını Yükle</h3>'
            +     '</div>'
            +     '<button type="button" class="pwa-modal-close" id="pwaModalCloseBtn">&times;</button>'
            +   '</div>'
            +   '<div class="pwa-help-modal-body">'
            +     '<p>' + step1 + '</p>'
            +     '<p>' + step2 + '</p>'
            +     '<div class="pwa-help-modal-tip">' + tip + '</div>'
            +   '</div>'
            +   '<div class="pwa-help-modal-footer">'
            +     '<button type="button" class="pwa-install-btn" style="width:100%;" id="pwaModalAckBtn">Anladım</button>'
            +   '</div>'
            + '</div>';

        document.body.appendChild(modal);
        modal.style.display = 'flex';

        function closeModal() {
            modal.style.display = 'none';
        }
        document.getElementById('pwaModalCloseBtn').addEventListener('click', closeModal);
        document.getElementById('pwaModalAckBtn').addEventListener('click', closeModal);
        modal.addEventListener('click', function(e) {
            if (e.target === modal) closeModal();
        });
    }

    window.installPwa = async function () {
        if (deferredInstallPrompt) {
            try {
                deferredInstallPrompt.prompt();
                var choiceResult = await deferredInstallPrompt.userChoice;
                log('User install choice:', choiceResult.outcome);
                if (choiceResult.outcome === 'accepted') {
                    updateInstallUi();
                }
            } catch (err) {
                log('Install error:', err);
                showPwaHelpModal();
            }
            deferredInstallPrompt = null;
            window.deferredInstallPrompt = null;
        } else {
            showPwaHelpModal();
        }
    };

    async function init() {
        setupInstallListeners();
        updateInstallUi();

        if (!('serviceWorker' in navigator)) {
            log('ServiceWorker not supported in this browser');
            return;
        }

        try {
            var reg = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
            log('SW registered, scope:', reg.scope);

            // Wait for SW to be active before subscribing.
            await navigator.serviceWorker.ready;

            // Permission flow: only auto-prompt if not blocked. If denied, we
            // stop silently; if default, we wait for an explicit user action
            // (e.g. test button on /notifications.html).
            if (Notification.permission === 'denied') {
                log('notifications denied — skipping subscribe');
                return;
            }
            if (Notification.permission === 'default') {
                // Don't prompt on every page load — only the settings page
                // should trigger this. Here we just register the SW.
                log('notifications permission not yet granted');
                return;
            }

            // Permission granted but no live PushSubscription. This happens
            // when the browser dropped the subscription (Samsung Internet
            // routinely loses subs across app updates, Brave/Edge can drop
            // when the push service connection cycles, Chrome on Android
            // re-issues after FCM token refresh). Self-heal: fetch the
            // VAPID key and resubscribe transparently so the per-device
            // toggle UI on /notifications.html accurately reflects state.
            var existing = await reg.pushManager.getSubscription();
            if (existing) {
                log('push subscription is active');
                return;
            }
            log('permission granted but no active subscription — attempting silent resubscribe');
            try {
                await runSerial(async function () {
                    var meta = await getCategoriesAndKey();
                    if (!meta || !meta.vapidPublicKey) {
                        log('silent resubscribe skipped: backend has no VAPID key yet');
                        return;
                    }
                    // Route silent through ensureSubscription so it gets
                    // the same VAPID-mismatch handling and AbortError
                    // retry that Enable does. Without this, a stale-key
                    // sub on Samsung Internet / Edge / desktop Brave
                    // would throw AbortError silently and leave the user
                    // in "permission granted, no sub" state forever.
                    var sub = await ensureSubscription(reg, meta.vapidPublicKey);
                    try {
                        await postSubscription(sub);
                    } catch (e) {
                        if (e && e.tombstoned) {
                            // Server explicitly says "user removed this; do
                            // not auto-resubscribe." Drop the local push
                            // subscription so we stop generating live tokens
                            // tied to a forgotten id. Without this, every
                            // page load would round-trip to the server and
                            // get a 410 forever until the tombstone expires.
                            log('silent resubscribe blocked by server tombstone — dropping local subscription');
                            try { await sub.unsubscribe(); } catch (_) {}
                            return;
                        }
                        throw e;
                    }
                    log('silent resubscribe complete');
                });
            } catch (e) {
                // Don't escalate — the settings page will show the real
                // error when the user clicks Enable. Common silent-fail
                // causes: backend 503 (no Zrok yet), browser push service
                // unreachable, app not installed yet, watchdog timeout.
                log('silent resubscribe failed:', errString(e));
            }
        } catch (e) {
            log('init failed:', e && e.message ? e.message : e);
        }
    }

    // Expose minimal API for the settings page to drive permission prompts
    // and explicit (re)subscribe flow.
    window.OverdrivePush = {
        async requestAndSubscribe() {
            // Permission prompt happens OUTSIDE the serial chain — it's a
            // user-gesture-bound API and queueing it would lose the
            // gesture token on browsers that enforce it.
            var perm = await Notification.requestPermission();
            if (perm !== 'granted') return { ok: false, reason: 'permission-' + perm };
            var reg = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
            await navigator.serviceWorker.ready;
            // Run the subscribe + POST inside the serial chain so we
            // never race init()'s silent path or another rapid Enable
            // click. ensureSubscription() will return any live sub
            // produced by an earlier slot (silent or prior Enable),
            // turning duplicate clicks into a single registration.
            return runSerial(async function () {
                var meta;
                try {
                    meta = await getCategoriesAndKey();
                } catch (e) {
                    return { ok: false, reason: 'backend-unreachable', error: errString(e) };
                }
                if (!meta || !meta.vapidPublicKey) return { ok: false, reason: 'no-vapid-key' };
                // pushManager.subscribe() is the call that fails on
                // Samsung Internet without GMS, Brave with shields, and
                // Edge when WNS is unreachable. Capture the DOMException
                // name+message so the settings page can surface it
                // instead of a generic "could not enable" toast — these
                // are browser-level rejections we can't route around
                // but can at least diagnose.
                var sub;
                try {
                    sub = await ensureSubscription(reg, meta.vapidPublicKey);
                } catch (e) {
                    return { ok: false, reason: 'subscribe-failed', error: errString(e) };
                }
                try {
                    // User-driven Enable click — force past any server-side
                    // tombstone left by a recent Remove. Without force=true
                    // a user who immediately re-enables would get 410 and
                    // be stuck for 30 minutes.
                    var resp = await postSubscription(sub, { force: true });
                    return { ok: true, id: resp.id };
                } catch (e) {
                    return { ok: false, reason: 'register-failed', error: errString(e) };
                }
            });
        },
        async unsubscribe() {
            var reg = await navigator.serviceWorker.getRegistration();
            if (!reg) return { ok: true };
            var sub = await reg.pushManager.getSubscription();
            if (!sub) return { ok: true };
            try {
                await authedFetch('/api/push/unsubscribe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ endpoint: sub.endpoint })
                });
            } catch (e) { /* ignore — still try to local-unsubscribe */ }
            try { await sub.unsubscribe(); } catch (e) {}
            return { ok: true };
        },
        async sendTest(severity) {
            return authedFetch('/api/push/test', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ severity: severity || 'info' })
            }).then(function (r) { return r.json(); });
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
