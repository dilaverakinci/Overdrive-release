package com.overdrive.app.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class StaticAssetHandlerTest {

    @Test
    public void getContentTypeResolvesStandardWebFormats() {
        assertEquals("text/html; charset=utf-8", StaticAssetHandler.getContentType("index.html"));
        assertEquals("text/css; charset=utf-8", StaticAssetHandler.getContentType("styles.css"));
        assertEquals("application/javascript; charset=utf-8", StaticAssetHandler.getContentType("app.js"));
        assertEquals("application/json", StaticAssetHandler.getContentType("manifest.json"));
        assertEquals("application/wasm", StaticAssetHandler.getContentType("module.wasm"));
        assertEquals("image/png", StaticAssetHandler.getContentType("logo.png"));
        assertEquals("image/webp", StaticAssetHandler.getContentType("icon.webp"));
        assertEquals("model/gltf-binary", StaticAssetHandler.getContentType("seal.glb"));
        assertEquals("application/octet-stream", StaticAssetHandler.getContentType("unknown.bin"));
    }

    @Test
    public void assetContentEtagIsDeterministicSha256() {
        byte[] data1 = "test content".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test content".getBytes(StandardCharsets.UTF_8);
        byte[] different = "other content".getBytes(StandardCharsets.UTF_8);

        String etag1 = StaticAssetHandler.assetContentEtag(data1);
        String etag2 = StaticAssetHandler.assetContentEtag(data2);
        String etagDiff = StaticAssetHandler.assetContentEtag(different);

        assertNotNull(etag1);
        assertTrue(etag1.startsWith("\""));
        assertTrue(etag1.endsWith("\""));
        assertEquals(etag1, etag2);
        assertFalse(etag1.equals(etagDiff));
    }

    @Test
    public void directoryTraversalIsBlocked() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean served = StaticAssetHandler.serveStaticFile(out, "/tmp", "../etc/passwd", null);
        assertFalse(served);
        assertEquals(0, out.size());
    }
}
