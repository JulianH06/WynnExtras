// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — registration handle passed to CoreComponent#registerDownloads (faithful port).
 * A component calls registry.registerDownload(urlId[, dependency]).handleJsonObject(this::handle).
 */
package julianh06.wynnextras.wtshim.core.net;

import julianh06.wynnextras.wtshim.core.components.CoreComponent;

public class DownloadRegistry {
    private final DownloadManager downloadManager;
    private final CoreComponent callerComponent;

    DownloadRegistry(DownloadManager downloadManager, CoreComponent callerComponent) {
        this.downloadManager = downloadManager;
        this.callerComponent = callerComponent;
    }

    public QueuedDownload registerDownload(UrlId urlId) {
        return registerDownload(urlId, Dependency.empty());
    }

    public QueuedDownload registerDownload(UrlId urlId, Dependency dependency) {
        return downloadManager.queueDownload(urlId, callerComponent, dependency);
    }
}
