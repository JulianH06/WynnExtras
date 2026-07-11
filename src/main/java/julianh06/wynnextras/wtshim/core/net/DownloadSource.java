// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — download source (CDN base URLs for path-based UrlInfos). */
package julianh06.wynnextras.wtshim.core.net;

import java.util.Optional;

public enum DownloadSource {
    CDN(Optional.of("https://cdn.wynntils.com/static/")),
    GITHUB(Optional.of("https://raw.githubusercontent.com/Wynntils/Static-Storage/refs/heads/main/")),
    CUSTOM(Optional.empty());

    private final Optional<String> url;

    DownloadSource(Optional<String> url) {
        this.url = url;
    }

    public Optional<String> getUrl() {
        return url;
    }
}
