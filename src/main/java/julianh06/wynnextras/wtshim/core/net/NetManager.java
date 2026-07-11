// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — NetManager.
 *
 * KEPT FAITHFULLY: java.net.http.HttpClient with a User-Agent, disk cache under
 * {gameDir}/wynnextras/wtshim-cache/, md5-validated local-cache fallback (offline play), and the
 * download(UrlId)/download(URI, name[, hash]) surface used by the DownloadManager and future models.
 *
 * IMPORTANT: openLink(UrlId, Map) is the injection target of julianh06.wynnextras.mixin.NetManagerMixin
 * (descriptor (Ljulianh06/wynnextras/wtshim/core/net/UrlId;Ljava/util/Map;)V). Do not change its
 * name or parameter types.
 *
 * SLIM vs Wynntils:
 *  - md5 via java.security.MessageDigest instead of commons-codec DigestUtils (no extra dependency).
 *  - Timeout hardcoded (Wynntils persists it in config); the shim has no persisted-config framework.
 *  - callApi/ApiResponse (REST + POST helpers) are omitted — the future consumers (gear data, guild
 *    map) only use download(). Re-add ApiResponse + createApiResponse when a POST/REST call is needed.
 */
package julianh06.wynnextras.wtshim.core.net;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Manager;
import julianh06.wynnextras.wtshim.core.components.Managers;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import net.minecraft.util.Util;

public final class NetManager extends Manager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final File CACHE_DIR = WynntilsMod.getModStorageDir("wtshim-cache");
    private static final int TIMEOUT_MILLIS = 10000;
    private static final String USER_AGENT = String.format(
            "WynnExtras/%s+MC-%s (%s) wtshim",
            WynntilsMod.getVersion(),
            WynntilsMod.getMinecraftVersion(),
            WynntilsMod.isDevelopmentEnvironment() ? "dev" : "client");

    HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    public Download download(URI uri, String localFileName) {
        File localFile = new File(CACHE_DIR, localFileName);
        return download(uri, localFile);
    }

    public Download download(URI uri, String localFileName, String expectedHash) {
        File localFile = new File(CACHE_DIR, localFileName);
        return download(uri, localFile, expectedHash);
    }

    /**
     * Download a file identified by a UrlId, saving it to the cache directory.
     * If the UrlId carries an md5, the cached copy is used when it validates (offline-friendly).
     */
    public Download download(UrlId urlId) {
        UrlManager.UrlInfo urlInfo = Managers.Url.getUrlInfo(urlId);
        URI uri;
        if (urlInfo.path().isPresent()) {
            uri = URI.create(Managers.Url.getDownloadSourceUrl() + urlInfo.path().get());
        } else {
            uri = URI.create(urlInfo.url());
        }
        File localFile = new File(CACHE_DIR, urlId.getId());

        if (urlInfo.md5().isPresent()) {
            return download(uri, localFile, urlInfo.md5().get());
        }
        return download(uri, localFile);
    }

    private Download download(URI uri, File localFile) {
        return new Download(localFile.getName(), localFile, createGetRequest(uri));
    }

    private Download download(URI uri, File localFile, String expectedHash) {
        // If a valid cached copy exists, use it without hitting the network
        if (checkLocalHash(localFile, expectedHash)) {
            return new Download(localFile.getName(), localFile);
        }
        return download(uri, localFile);
    }

    public File getCacheDir() {
        return CACHE_DIR;
    }

    public File getCacheFile(String localFileName) {
        return new File(CACHE_DIR, localFileName);
    }

    public void openLink(URI url) {
        Util.getOperatingSystem().open(url);
    }

    /**
     * Opens the browser link for the given UrlId. WynnExtras' NetManagerMixin injects at the HEAD
     * of this method (e.g. to redirect the Wynncraft player-stats link to the built-in PV screen).
     */
    public void openLink(UrlId urlId, Map<String, String> arguments) {
        URI uri = URI.create(Managers.Url.buildUrl(urlId, arguments));
        openLink(uri);
    }

    public int getTimeoutMillis() {
        return TIMEOUT_MILLIS;
    }

    private HttpRequest createGetRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .header("User-Agent", USER_AGENT)
                .build();
    }

    private boolean checkLocalHash(File localFile, String expectedHash) {
        if (!localFile.exists()) return false;

        try (InputStream is = Files.newInputStream(localFile.toPath())) {
            String fileHash = md5Hex(is);
            return fileHash.equalsIgnoreCase(expectedHash);
        } catch (IOException e) {
            WynntilsMod.warn("Error when calculating md5 for " + localFile.getPath(), e);
            return false;
        }
    }

    private static String md5Hex(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed to be present on every JVM
            throw new IOException("MD5 algorithm unavailable", e);
        }
    }
}
