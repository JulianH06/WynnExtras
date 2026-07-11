// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Download (a NetResult backed by a local cache file).
 *
 * SLIM vs Wynntils: uses java.nio.file.Files for delete/mkdirs instead of commons-io FileUtils
 * (no external dependency), and drops the NetResultProcessedEvent. The local-cache + md5-fallback
 * download semantics (offline play) are otherwise faithful.
 */
package julianh06.wynnextras.wtshim.core.net;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Managers;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Download extends NetResult {
    private final File localFile;

    // Saved since we might need to get timestamps from the HttpResponse
    private CompletableFuture<HttpResponse<Path>> httpResponse = null;

    /** Cache-only download: the file is already present, just read it (no network). */
    public Download(String desc, File localFile) {
        super("DL:" + desc, null);
        this.localFile = localFile;
    }

    public Download(String desc, File localFile, HttpRequest request) {
        super("DL:" + desc, request);
        this.localFile = localFile;
    }

    public long getResponseTimestamp() {
        if (httpResponse == null) {
            // We have either not yet made the request, or we have read from the cache
            return System.currentTimeMillis();
        }

        try {
            HttpHeaders headers = httpResponse.get().headers();
            OptionalLong a = headers.firstValueAsLong("timestamp");
            if (a.isEmpty()) return System.currentTimeMillis();
            return a.getAsLong();
        } catch (InterruptedException | ExecutionException e) {
            return System.currentTimeMillis();
        }
    }

    @Override
    protected void onHandlingFailed() {
        // If handling of the file failed, our cache might be bad. Remove it so we
        // try to re-download the file next time
        WynntilsMod.warn("Deleting cached file due to handling error: " + localFile);
        deleteQuietly(localFile);
    }

    @Override
    protected CompletableFuture<InputStream> getInputStreamFuture() {
        if (request == null) {
            // File is already downloaded, just read from the cache
            return CompletableFuture.supplyAsync(this::getFileInputStreamFromCache);
        } else {
            prepareForDownload();
            return getDownloadInputStreamFuture().thenApply(response -> getFileInputStreamFromCache());
        }
    }

    private CompletableFuture<HttpResponse<Path>> getDownloadInputStreamFuture() {
        CompletableFuture<HttpResponse<Path>> future =
                Managers.Net.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofFile(localFile.toPath()));

        // We must save the response so we can get the timestamp
        this.httpResponse = future;
        return future;
    }

    private InputStream getFileInputStreamFromCache() {
        try {
            return new FileInputStream(localFile);
        } catch (FileNotFoundException e) {
            // This should not happen; we have checked for the file
            WynntilsMod.error("File went missing from cache", e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private void prepareForDownload() {
        deleteQuietly(localFile);
        File parent = localFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            WynntilsMod.error("Failed to create directories needed for " + localFile);
        }
    }

    private static void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }
}
