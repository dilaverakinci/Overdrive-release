/**
 * OverDrive Service Worker (PWA Modernization v5)
 *
 * Responsibilities:
 *  1. Web Push fanout — receives Web Push payloads from the head unit,
 *     renders notifications with rich action buttons, and routes taps into the PWA.
 *  2. App Shell & Static Assets precaching — ensures 0ms instant loading for
 *     design tokens, shared CSS/JS, 3D vehicle assets, and core pages.
 *  3. Navigation & Offline resilience — transparent NetworkFirst caching for HTML pages
 *     so the PWA opens cleanly even when disconnected from vehicle/tunnel.
 *  4. Transparent read-only API cache — caches last known vehicle state (/api/status,
 *     /api/trips, /api/charging, /api/parking) so existing page scripts display
 *     cached telemetry instead of failing when offline.
 *
 * Preserves 100% of existing theme tokens (#00876C brand primary, #0E1311 dark surface)
 * without requiring any modifications to existing page HTML/DOM structures.
 */

const STATIC_CACHE = 'overdrive-static-v5';
const PAGES_CACHE = 'overdrive-pages-v5';
const DATA_CACHE = 'overdrive-data-v5';

const CURRENT_CACHES = [STATIC_CACHE, PAGES_CACHE, DATA_CACHE];

// Core static assets and 3D vehicle pipeline needed across pages
const PRECACHE_URLS = [
  // Shell styles & tokens (mirrors colors_m3 and branding)
  '/shared/styles.css',
  '/shared/design-tokens.css',
  '/shared/app-shell.css',
  '/shared/leaflet.css',

  // Core shell scripts
  '/shared/core.js',
  '/shared/auth.js',
  '/shared/app-shell.js',
  '/shared/pwa-init.js',

  // 3D vehicle pipeline
  '/shared/ev-card-3d.js',
  '/shared/ev-card-sprite-cache.js',
  '/shared/vendor/three.min.js',
  '/shared/vendor/GLTFLoader.js',
  '/shared/vendor/DRACOLoader.js',
  '/shared/vendor/draco/draco_decoder.js',
  '/shared/vendor/draco/draco_wasm_wrapper.js',
  '/shared/vendor/draco/draco_decoder.wasm',
  '/shared/models/seal.glb',

  // Icons & Branding
  '/shared/app-icon-dark.webp',
  '/shared/app-icon.webp',
  '/shared/app-icon-ios.webp',
  '/manifest.json',

  // Primary entry shell
  '/index.html'
];

// Read-only GET API endpoints safe to serve from cache when offline
const READONLY_API_PREFIXES = [
  '/api/status',
  '/api/trips',
  '/api/charging',
  '/api/parking',
  '/api/vehicle/status',
  '/api/notifications/categories'
];

// ==================== INSTALL ====================

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    try {
      const cache = await caches.open(STATIC_CACHE);
      await cache.addAll(PRECACHE_URLS.map((u) => new Request(u, { cache: 'reload' })));
    } catch (e) {
      if (self.console) self.console.warn('[sw] Precache error (non-fatal):', e);
    }
    await self.skipWaiting();
  })());
});

// ==================== ACTIVATE ====================

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    // Reclaim old caches from prior versions (overdrive-3d-*, older static/pages)
    const names = await caches.keys();
    await Promise.all(
      names
        .filter((n) => n.startsWith('overdrive-') && !CURRENT_CACHES.includes(n))
        .map((n) => caches.delete(n))
    );
    await self.clients.claim();
  })());
});

// ==================== FETCH ====================

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  let url;
  try {
    url = new URL(req.url);
  } catch (e) {
    return;
  }

  // Same-origin requests only
  if (url.origin !== self.location.origin) return;

  const pathname = url.pathname;

  // Never intercept WebSockets, raw camera streams, or live media feeds
  if (
    pathname.startsWith('/ws/') ||
    pathname.startsWith('/stream/') ||
    pathname.startsWith('/camera/') ||
    pathname.endsWith('.m3u8') ||
    pathname.endsWith('.ts')
  ) {
    return;
  }

  // 1. Navigation requests (HTML pages) -> Network-First with Cache Fallback
  const isNav =
    req.mode === 'navigate' ||
    (req.headers.get('accept') && req.headers.get('accept').includes('text/html')) ||
    pathname.endsWith('.html') ||
    pathname === '/';

  if (isNav) {
    event.respondWith(handleNavigationRequest(req));
    return;
  }

  // 2. Read-only Data APIs -> Network-First with Cache Fallback
  const isReadonlyApi = READONLY_API_PREFIXES.some((prefix) => pathname.startsWith(prefix));
  if (isReadonlyApi) {
    event.respondWith(handleReadonlyApiRequest(req));
    return;
  }

  // 3. Static assets (CSS, JS, 3D models, fonts, images) -> Stale-While-Revalidate
  const isStatic =
    pathname.startsWith('/shared/') ||
    pathname === '/manifest.json' ||
    PRECACHE_URLS.includes(pathname);

  if (isStatic) {
    event.respondWith(handleStaticAssetRequest(req));
    return;
  }
});

/**
 * Handle navigation / HTML requests:
 * Fetch from network and update cache. If offline/error, return cached page or index.html.
 */
async function handleNavigationRequest(req) {
  try {
    const fresh = await fetch(req);
    if (fresh && fresh.ok) {
      const cache = await caches.open(PAGES_CACHE);
      try {
        await cache.put(req, fresh.clone());
      } catch (e) {}
      return fresh;
    }
  } catch (e) {
    // Network failed or tunnel unreachable
  }

  // Fallback to cached version of the requested page
  const pagesCache = await caches.open(PAGES_CACHE);
  const cached = await pagesCache.match(req);
  if (cached) return cached;

  // Fallback to index.html from static cache
  const staticCache = await caches.open(STATIC_CACHE);
  const fallbackIndex = await staticCache.match('/index.html');
  if (fallbackIndex) return fallbackIndex;

  return new Response(
    '<!DOCTYPE html><html><head><meta charset="utf-8"><title>OverDrive</title>' +
      '<meta name="viewport" content="width=device-width, initial-scale=1">' +
      '<style>body{background:#0E1311;color:#DEE4E0;font-family:sans-serif;display:flex;align-items:center;' +
      'justify-content:center;height:100vh;margin:0;text-align:center;padding:16px;}' +
      'h1{color:#00876C;font-size:24px;margin-bottom:8px;}p{color:#BFC9C3;font-size:14px;}</style></head>' +
      '<body><div><h1>OverDrive Çevrimdışı</h1><p>Araca şu anda ulaşılamıyor. Lütfen tünel veya Wi-Fi bağlantınızı kontrol edin.</p></div></body></html>',
    { headers: { 'Content-Type': 'text/html; charset=utf-8' } }
  );
}

/**
 * Handle read-only API requests:
 * Transparently cache responses so existing page JavaScript continues
 * to render last-known state even when disconnected.
 */
async function handleReadonlyApiRequest(req) {
  try {
    const fresh = await fetch(req);
    if (fresh && fresh.ok && fresh.type === 'basic') {
      const cache = await caches.open(DATA_CACHE);
      try {
        await cache.put(req, fresh.clone());
      } catch (e) {}
      return fresh;
    }
  } catch (e) {
    // Network failed
  }

  // Return last-known JSON payload from cache
  const cache = await caches.open(DATA_CACHE);
  const cached = await cache.match(req);
  if (cached) return cached;

  return new Response(JSON.stringify({ error: 'offline', cached: false }), {
    status: 504,
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * Handle static assets (CSS, JS, images, models):
 * Stale-While-Revalidate: return cache instantly for 0ms speed,
 * fetch from network in background to update cache.
 */
async function handleStaticAssetRequest(req) {
  const cache = await caches.open(STATIC_CACHE);
  const cached = await cache.match(req, { ignoreSearch: true });

  const networkFetch = fetch(req)
    .then(async (fresh) => {
      if (fresh && fresh.ok && fresh.type === 'basic') {
        try {
          await cache.put(req, fresh.clone());
        } catch (e) {}
      }
      return fresh;
    })
    .catch(() => null);

  if (cached) {
    // Return cached immediately; update cache in background
    return cached;
  }

  // Not in cache, wait for network
  const fresh = await networkFetch;
  if (fresh) return fresh;

  return new Response('', { status: 504, statusText: 'offline' });
}

// ==================== PUSH NOTIFICATIONS ====================

self.addEventListener('push', (event) => {
  let payload;
  try {
    payload = event.data ? event.data.json() : {};
  } catch (e) {
    payload = { title: 'OverDrive', body: '(unreadable payload)', severity: 'info' };
  }

  event.waitUntil(showFromPayload(payload));
});

function showFromPayload(payload) {
  const title = payload.title || 'OverDrive';
  const severity = payload.severity || 'info';

  const options = {
    body: payload.body || '',
    icon: '/shared/app-icon-dark.webp',
    badge: '/shared/app-icon-dark.webp',
    tag: payload.tag || payload.category || 'overdrive',
    timestamp: payload.ts || Date.now(),
    data: payload,
    renotify: severity === 'critical'
  };

  // Action buttons for notifications on supported browsers
  if (severity === 'critical' || severity === 'warn') {
    options.actions = [
      { action: 'action_view_live', title: '🔴 Canlı İzle' },
      { action: 'action_view_events', title: '📋 Olaylar' }
    ];
    options.requireInteraction = true;
    options.vibrate = [300, 100, 300, 100, 300];
  } else {
    options.actions = [
      { action: 'action_view_dash', title: '📊 Dashboard' }
    ];
  }

  // Snapshot handling (pre-signed token in URL)
  var snap = payload.data && payload.data.snapshot;
  var stage = payload.data && payload.data.stage;
  if (snap && stage !== 'start') {
    options.image = snap;
    options.data = Object.assign({}, payload, { heroUrl: snap });
  }

  return self.registration.showNotification(title, options);
}

// ==================== NOTIFICATION CLICK ====================

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const data = event.notification.data || {};
  let url = data.url || '/';

  // Route based on action button clicked
  if (event.action === 'action_view_live') {
    url = '/live-view.html';
  } else if (event.action === 'action_view_events') {
    url = '/events.html';
  } else if (event.action === 'action_view_dash') {
    url = '/index.html';
  }

  // Forward heroUrl into target URL for iOS Safari support
  if (data.heroUrl) {
    const sep = url.indexOf('?') >= 0 ? '&' : '?';
    url = url + sep + 'hero=' + encodeURIComponent(data.heroUrl);
  }

  event.waitUntil((async () => {
    const wins = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const sameOrigin = wins.find((w) => {
      try {
        return new URL(w.url).origin === self.location.origin;
      } catch (e) {
        return false;
      }
    });

    if (sameOrigin) {
      try {
        await sameOrigin.focus();
      } catch (e) {}
      try {
        await sameOrigin.navigate(url);
        return;
      } catch (e) {}
      try {
        sameOrigin.postMessage({ type: 'notification-click', payload: data, action: event.action });
        return;
      } catch (e) {}
    }

    if (self.clients.openWindow) {
      await self.clients.openWindow(url);
    }
  })());
});
