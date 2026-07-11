// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — DownloadManager: the managed download layer components register against.
 *
 * KEPT FAITHFULLY: the registration surface. A CoreComponent overrides registerDownloads(registry)
 * and calls registry.registerDownload(urlId[, dependency]).handleJsonObject/handleReader/handleJsonArray.
 * Downloads with dependencies only start once their dependencies have completed. So the gear-data
 * phase's registration code (as in Wynntils' GearInfoRegistry / MapService) ports over unchanged.
 *
 * SLIM vs Wynntils: the full DownloadDependencyGraph (topological node graph, circular-dependency
 * detection, retry, parallel-slot regulation, DownloadEvent progress reporting) is replaced with a
 * simple ready-check scheduler: repeatedly start every WAITING download whose dependencies are all
 * COMPLETED; on each completion/failure, re-run the scan. All ready downloads are started at once
 * (java.net.http already runs them async) rather than capped to N parallel.
 *
 * DEVIATION: a dependency pointing at a (component, urlId) that was never registered is treated as
 * satisfied (with a warning) instead of throwing — the fork may not port every upstream component.
 */
package julianh06.wynnextras.wtshim.core.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.CoreComponent;
import julianh06.wynnextras.wtshim.core.components.Manager;
import julianh06.wynnextras.wtshim.core.components.Managers;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DownloadManager extends Manager {
    private enum State {
        WAITING,
        IN_PROGRESS,
        COMPLETED,
        ERROR
    }

    private final List<QueuedDownload> registeredDownloads = new ArrayList<>();
    private final Map<QueuedDownload, State> states = new ConcurrentHashMap<>();
    private boolean registrationLock = false;

    /** Called by DownloadRegistry. */
    QueuedDownload queueDownload(UrlId urlId, CoreComponent callerComponent, Dependency dependency) {
        if (registrationLock) {
            throw new IllegalStateException("Cannot queue downloads after registration is locked.");
        }
        QueuedDownload queuedDownload = new QueuedDownload(callerComponent, urlId, dependency);
        registeredDownloads.add(queuedDownload);
        return queuedDownload;
    }

    /** Lets every component declare its downloads, then locks registration. */
    public void registerDownloads(Collection<CoreComponent> components) {
        for (CoreComponent component : components) {
            component.registerDownloads(new DownloadRegistry(this, component));
        }
        registrationLock = true;
    }

    public List<QueuedDownload> registeredDownloads() {
        return Collections.unmodifiableList(registeredDownloads);
    }

    /** Starts (or restarts) all registered downloads, respecting dependency ordering. */
    public synchronized void startDownloads() {
        if (registeredDownloads.isEmpty()) {
            WynntilsMod.info("[DownloadManager] No downloads registered.");
            return;
        }

        states.clear();
        registeredDownloads.forEach(download -> states.put(download, State.WAITING));

        WynntilsMod.info("[DownloadManager] Starting {} download(s).", registeredDownloads.size());
        startReadyDownloads();
    }

    private synchronized void startReadyDownloads() {
        boolean startedAny;
        do {
            startedAny = false;
            for (QueuedDownload download : registeredDownloads) {
                if (states.get(download) != State.WAITING) continue;

                DependencyStatus status = dependencyStatus(download);
                if (status == DependencyStatus.FAILED) {
                    states.put(download, State.ERROR);
                    continue;
                }
                if (status == DependencyStatus.PENDING) continue;

                states.put(download, State.IN_PROGRESS);
                start(download);
                startedAny = true;
            }
        } while (startedAny);

        checkFinished();
    }

    private enum DependencyStatus {
        READY,
        PENDING,
        FAILED
    }

    private DependencyStatus dependencyStatus(QueuedDownload download) {
        for (Pair<CoreComponent, UrlId> dependency : download.dependency().dependencies()) {
            QueuedDownload match = findRegistered(dependency.a(), dependency.b());
            if (match == null) {
                WynntilsMod.warn("[DownloadManager] {} depends on unregistered {} from {} — treating as satisfied.",
                        download.urlId(), dependency.b(), dependency.a());
                continue;
            }
            State depState = states.get(match);
            if (depState == State.ERROR) return DependencyStatus.FAILED;
            if (depState != State.COMPLETED) return DependencyStatus.PENDING;
        }
        return DependencyStatus.READY;
    }

    private QueuedDownload findRegistered(CoreComponent component, UrlId urlId) {
        return registeredDownloads.stream()
                .filter(download -> download.callerComponent() == component && download.urlId() == urlId)
                .findAny()
                .orElse(null);
    }

    private void start(QueuedDownload queuedDownload) {
        Download download = Managers.Net.download(queuedDownload.urlId());

        Consumer<Reader> readerHandler = queuedDownload.onCompletionReader();
        if (readerHandler != null) {
            download.handleReader(wrapSuccess(readerHandler, queuedDownload), wrapFailure(queuedDownload));
            return;
        }

        Consumer<JsonObject> jsonObjectHandler = queuedDownload.onCompletionJsonObject();
        if (jsonObjectHandler != null) {
            download.handleJsonObject(wrapSuccess(jsonObjectHandler, queuedDownload), wrapFailure(queuedDownload));
            return;
        }

        Consumer<JsonArray> jsonArrayHandler = queuedDownload.onCompletionJsonArray();
        if (jsonArrayHandler != null) {
            download.handleJsonArray(wrapSuccess(jsonArrayHandler, queuedDownload), wrapFailure(queuedDownload));
            return;
        }

        throw new IllegalStateException("Queued download has no handler set: " + queuedDownload);
    }

    private <T> Consumer<T> wrapSuccess(Consumer<T> handler, QueuedDownload download) {
        return (T result) -> {
            handler.accept(result);
            states.put(download, State.COMPLETED);
            startReadyDownloads();
        };
    }

    private Consumer<Throwable> wrapFailure(QueuedDownload download) {
        return (throwable) -> {
            WynntilsMod.warn("[DownloadManager] Download failed: {} -> {}",
                    download.callerComponent().getClass().getSimpleName(), download.urlId());
            states.put(download, State.ERROR);
            startReadyDownloads();
        };
    }

    private void checkFinished() {
        boolean allDone = states.values().stream()
                .allMatch(state -> state == State.COMPLETED || state == State.ERROR);
        if (!allDone) return;

        long failed = states.values().stream().filter(state -> state == State.ERROR).count();
        if (failed == 0) {
            WynntilsMod.info("[DownloadManager] All downloads finished successfully.");
        } else {
            WynntilsMod.warn("[DownloadManager] Downloads finished with {} failure(s) of {}.",
                    failed, states.size());
        }
    }
}
