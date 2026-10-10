package com.overdrive.app.server;

import android.content.Context;

import com.overdrive.app.daemon.CameraDaemon;
import com.overdrive.app.daemon.DaemonBootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

/**
 * Handles static asset serving, MIME resolution, and HTTP caching/ETag validation.
 * Extracted from HttpServer as part of Phase 3 (God Class Refactoring).
 */
public final class StaticAssetHandler {

    private StaticAssetHandler() {}

    /**
     * Serves static files from WEB_ROOT with streaming for large files.
     */
    public static boolean serveStaticFile(OutputStream out, String webRoot, String relativePath) {
        return serveStaticFile(out, webRoot, relativePath, null);
    }

    public static boolean serveStaticFile(OutputStream out, String webRoot, String relativePath, String ifNoneMatch) {
        if (relativePath == null || relativePath.contains("..")) {
            return false;
        }

        // Catalogs are versioned with the APK. Prefer the bundled bytes over
        // /data/local/tmp so a stale extraction from the previous app version
        // can never hide a new or corrected translation.
        if (relativePath.startsWith("i18n/")
                && relativePath.endsWith(".json")
                && serveAssetFallback(out, relativePath, ifNoneMatch)) {
            return true;
        }

        File file = new File(webRoot, relativePath);
        if (!file.exists() || !file.isFile()) {
            // Fall back to the persistent models cache for GLBs downloaded at runtime.
            if (relativePath.startsWith("shared/models/") && relativePath.endsWith(".glb")) {
                String fileName = relativePath.substring("shared/models/".length());
                File cached = ModelsApiHandler.cachedModelFile(fileName);
                if (cached != null) {
                    file = cached;
                } else {
                    return serveAssetFallback(out, relativePath, ifNoneMatch);
                }
            } else {
                return serveAssetFallback(out, relativePath, ifNoneMatch);
            }
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            String contentType = getContentType(relativePath);
            
            // HTML pages must always revalidate so the user gets the latest UI logic.
            String cacheControl;
            String fileName = new File(relativePath).getName();
            if (relativePath.endsWith(".html")
                    || fileName.equals("sw.js")
                    || fileName.equals("manifest.json")) {
                cacheControl = "no-store, no-cache, must-revalidate, max-age=0";
            } else {
                cacheControl = "public, max-age=3600, must-revalidate";
            }

            // Validator over (mtime, size).
            String etag = "\"" + Long.toHexString(file.lastModified())
                    + "-" + Long.toHexString(file.length()) + "\"";
            if (ifNoneMatch != null && etag.equals(ifNoneMatch.trim())) {
                out.write(("HTTP/1.1 304 Not Modified\r\n"
                        + "ETag: " + etag + "\r\n"
                        + "Cache-Control: " + cacheControl + "\r\n"
                        + "Connection: close\r\n\r\n").getBytes());
                out.flush();
                return true;
            }

            StringBuilder headers = new StringBuilder();
            headers.append("HTTP/1.1 200 OK\r\n")
                   .append("Content-Type: ").append(contentType).append("\r\n")
                   .append("Content-Length: ").append(file.length()).append("\r\n")
                   .append("ETag: ").append(etag).append("\r\n")
                   .append("Cache-Control: ").append(cacheControl).append("\r\n");
            if (relativePath.endsWith(".html")) {
                headers.append("Pragma: no-cache\r\n")
                       .append("Expires: 0\r\n");
            }
            headers.append("Connection: close\r\n\r\n");
            out.write(headers.toString().getBytes());
            
            // Stream in 16KB chunks
            byte[] buffer = new byte[16384];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            
            CameraDaemon.log("Served static: " + relativePath + " (" + file.length() + " bytes)");
            return true;
            
        } catch (Exception e) {
            CameraDaemon.log("Static file error: " + relativePath + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Stable validator for APK assets. Hashing the contents avoids treating a
     * same-length translation edit as unchanged across app versions.
     */
    public static String assetContentEtag(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2 + 2);
            hex.append('"');
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return hex.append('"').toString();
        } catch (Exception impossible) {
            return "\"" + Integer.toHexString(Arrays.hashCode(data)) + "\"";
        }
    }

    /**
     * APK asset fallback for web catalogs. Phone installs may not be able to
     * create /data/local/tmp/web, and a running daemon may still have files
     * extracted by an older app version.
     */
    public static boolean serveAssetFallback(OutputStream out, String relativePath, String ifNoneMatch) {
        if (relativePath == null
                || !relativePath.startsWith("i18n/")
                || !relativePath.endsWith(".json")) {
            return false;
        }
        String file = relativePath.substring("i18n/".length());
        String requested = file.substring(0, file.length() - ".json".length());
        if (!LocaleManager.isSupported(requested)) return false;

        Context context = DaemonBootstrap.getContext();
        if (context == null || context.getAssets() == null) return false;
        try (InputStream in = context.getAssets().open("web/i18n/" + requested + ".json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            byte[] data = bytes.toByteArray();
            String cacheControl = "public, max-age=3600, must-revalidate";
            String etag = assetContentEtag(data);
            if (ifNoneMatch != null && etag.equals(ifNoneMatch.trim())) {
                out.write(("HTTP/1.1 304 Not Modified\r\n"
                        + "ETag: " + etag + "\r\n"
                        + "Cache-Control: " + cacheControl + "\r\n"
                        + "Connection: close\r\n\r\n").getBytes("UTF-8"));
                out.flush();
                return true;
            }
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json; charset=utf-8\r\n"
                    + "Content-Length: " + data.length + "\r\n"
                    + "ETag: " + etag + "\r\n"
                    + "Cache-Control: " + cacheControl + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(headers.getBytes("UTF-8"));
            out.write(data);
            out.flush();
            return true;
        } catch (Exception e) {
            CameraDaemon.log("Asset catalog error: " + relativePath + " - " + e.getMessage());
            return false;
        }
    }

    public static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".wasm")) return "application/wasm";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".glb")) return "model/gltf-binary";
        if (path.endsWith(".gltf")) return "model/gltf+json";
        return "application/octet-stream";
    }
}
