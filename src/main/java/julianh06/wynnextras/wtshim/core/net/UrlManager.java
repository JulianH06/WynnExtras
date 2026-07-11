// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — UrlManager.
 *
 * SLIM vs Wynntils: Wynntils loads urls.json from three sources (bundled resource, local cache,
 * remote download) and merges them by version/hash. The shim loads ONLY the bundled resource
 * (assets/wynnextras/wtshim/urls.json). This drops the remote urls.json refresh, the 3-way merge,
 * the local url cache, and the JVM-property overrides.
 *
 * KEPT FAITHFULLY: the public lookup API (getUrlInfo/getUrl/buildUrl), the UrlInfo record, the
 * Method/Encoding enums (argument encoding matters for CDN URLs), the UrlProfile json shape and
 * the %{arg} substitution with url-encoding. The md5 hashes from urls.json are preserved on
 * UrlInfo so Download can validate/cache CDN files.
 *
 * DEVIATION: Wynntils' readUrlMapper returns EMPTY if any UrlId value is missing from urls.json.
 * The shim only requires the ids it actually references, so we simply skip unknown/missing ids
 * instead of nuking the whole map.
 */
package julianh06.wynnextras.wtshim.core.net;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Manager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class UrlManager extends Manager {
    // Fixed to CDN. Wynntils lets the user switch source / set a custom source via persisted config;
    // the shim has no persisted-config framework, so we hardcode the CDN base.
    private static final DownloadSource DOWNLOAD_SOURCE = DownloadSource.CDN;

    private UrlMapper urlMapper = UrlMapper.EMPTY;

    public UrlManager() {}

    public UrlInfo getUrlInfo(UrlId urlId) {
        return urlMapper.get(urlId);
    }

    public String getUrl(UrlId urlId) {
        UrlInfo urlInfo = urlMapper.get(urlId);
        // Only valid for POST URLs, or GET URLs with no arguments
        assert (urlInfo.method() == Method.POST || urlInfo.arguments().isEmpty());
        return urlInfo.url();
    }

    public String buildUrl(UrlId urlId, Map<String, String> arguments) {
        return buildUrl(urlMapper.get(urlId), arguments);
    }

    public String buildUrl(UrlInfo urlInfo, Map<String, String> arguments) {
        // Verify that arguments match with what is specified
        assert (arguments.keySet().equals(new HashSet<>(urlInfo.arguments())))
                : "Arguments mismatch for " + urlInfo.url() + ", expected: " + urlInfo.arguments() + " got: "
                        + arguments.keySet();

        String url;
        if (urlInfo.path().isPresent()) {
            url = getDownloadSourceUrl() + urlInfo.path().get();
        } else {
            url = urlInfo.url();
        }

        // Replace %{argKey} with arg value in URL string
        return arguments.keySet().stream()
                .reduce(
                        url,
                        (str, argKey) -> str.replaceAll(
                                "%\\{" + argKey + "\\}",
                                // First encode with the specified encoder (if any), then always url-encode
                                encodeUrl(urlInfo.encoding().encode(arguments.get(argKey)))));
    }

    public String getDownloadSourceUrl() {
        return DOWNLOAD_SOURCE.getUrl().orElse(DownloadSource.CDN.getUrl().get());
    }

    /** Loads the bundled urls.json. Synchronous — no network. */
    public void loadUrls() {
        try (InputStream is = WynntilsMod.getModResourceAsStream("urls.json")) {
            if (is == null) {
                throw new IOException("Bundled urls.json not found on classpath");
            }
            urlMapper = readUrlMapper(is);
            WynntilsMod.info("Loaded bundled url list. Version: {}, URLs: {}", urlMapper.version(), urlMapper.urls().size());
        } catch (IOException | JsonSyntaxException e) {
            // Catastrophic: nothing downloadable will work. Mirror Wynntils and fail loudly.
            throw new RuntimeException(
                    "ERROR: Bundled urls.json is missing or malformed. This likely indicates a corrupt build.", e);
        }
    }

    private UrlMapper readUrlMapper(InputStream inputStream) throws IOException, JsonSyntaxException {
        byte[] data = inputStream.readAllBytes();
        String json = new String(data, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<UrlProfile>>() {}.getType();
        List<UrlProfile> urlProfiles = WynntilsMod.GSON.fromJson(json, type);

        Map<UrlId, UrlInfo> newMap = new HashMap<>();

        int version = 0;
        for (UrlProfile urlProfile : urlProfiles) {
            if (urlProfile.version != 0) {
                // Special version record
                version = urlProfile.version;
                continue;
            }
            List<String> arguments = urlProfile.arguments == null ? List.of() : urlProfile.arguments;
            Optional<UrlId> urlId = UrlId.from(urlProfile.id);

            // Ignore ids the shim does not model (Wynntils ships far more ids than the fork uses)
            if (urlId.isEmpty()) continue;

            newMap.put(
                    urlId.get(),
                    new UrlInfo(
                            urlProfile.url,
                            Optional.ofNullable(urlProfile.path),
                            arguments,
                            Method.from(urlProfile.method),
                            Encoding.from(urlProfile.encoding),
                            Optional.ofNullable(urlProfile.md5)));
        }

        return new UrlMapper(version, newMap);
    }

    /** Minimal url-encoder for query arguments (mirrors Wynntils' StringUtils.encodeUrl). */
    private static String encodeUrl(String input) {
        return java.net.URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    public enum Method {
        GET,
        POST;

        private static Method from(String str) {
            if (str == null || str.isEmpty()) return GET; // GET is default if unspecified
            return Method.valueOf(str.toUpperCase(Locale.ROOT));
        }
    }

    public enum Encoding {
        NONE(s -> s),
        CARGO(s -> "'" + s.replace("'", "\\'") + "'"),
        WIKI(s -> s.replace(" ", "_"));

        private final Function<String, String> encoder;

        Encoding(Function<String, String> encoder) {
            this.encoder = encoder;
        }

        private static Encoding from(String str) {
            if (str == null || str.isEmpty()) return NONE; // NONE is default if unspecified
            return Encoding.valueOf(str.toUpperCase(Locale.ROOT));
        }

        String encode(String input) {
            return encoder.apply(input);
        }
    }

    public record UrlInfo(
            String url,
            Optional<String> path,
            List<String> arguments,
            Method method,
            Encoding encoding,
            Optional<String> md5) {
        public UrlInfo withoutMd5() {
            return new UrlInfo(url, path, arguments, method, encoding, Optional.empty());
        }
    }

    private static final class UrlProfile {
        int version;
        String id;
        String url;
        String path;
        String method;
        List<String> arguments;
        String md5;
        String encoding;
    }

    private record UrlMapper(int version, Map<UrlId, UrlInfo> urls) {
        static final UrlMapper EMPTY = new UrlMapper(-1, Map.of());

        UrlInfo get(UrlId urlId) {
            return urls.get(urlId);
        }
    }
}
