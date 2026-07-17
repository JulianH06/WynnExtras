package julianh06.wynnextras.features.wci.service.wynnbuilder;

import java.net.URI;

public record WynnBuilderUrlParts(String sourceUrl, String path, String rawQuery, String rawFragment) {
    public static WynnBuilderUrlParts parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("WynnBuilder URL must not be blank");
        }
        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed.replace("|", "%7C"));
            if (!isAllowedHost(uri)) {
                throw new IllegalArgumentException("Unsupported WynnBuilder URL host: " + uri.getHost());
            }
            return new WynnBuilderUrlParts(trimmed, uri.getPath(), uri.getRawQuery(), uri.getRawFragment());
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Unsupported WynnBuilder")) {
                throw e;
            }
            throw new IllegalArgumentException("Malformed WynnBuilder URL: " + trimmed, e);
        }
    }

    public boolean isBuilderUrl() {
        return path != null && path.toLowerCase().contains("/builder");
    }

    public boolean isBarePayload() {
        return rawQuery == null && rawFragment == null && path != null && !path.isBlank() && !path.contains("/");
    }

    private static boolean isAllowedHost(URI uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || host.isBlank()) {
            return true;
        }
        if (host.equalsIgnoreCase("wynnbuilder.github.io")
                || host.equalsIgnoreCase("wynnbuilder-beta.github.io")
                || host.equalsIgnoreCase("hppeng-wynn.github.io")) {
            return true;
        }
        return host.equalsIgnoreCase("alex-guha.github.io")
                && path != null
                && path.startsWith("/wynnbuilder-beta.github.io/");
    }
}
