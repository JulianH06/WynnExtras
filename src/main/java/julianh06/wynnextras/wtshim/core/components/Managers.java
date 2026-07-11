// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Managers registry. Static fields WynnExtras reads.
 */
package julianh06.wynnextras.wtshim.core.components;

import julianh06.wynnextras.wtshim.core.consumers.features.FeatureManager;
import julianh06.wynnextras.wtshim.core.consumers.functions.FunctionManager;
import julianh06.wynnextras.wtshim.core.mod.ConnectionManager;
import julianh06.wynnextras.wtshim.core.mod.TickSchedulerManager;
import julianh06.wynnextras.wtshim.core.net.DownloadManager;
import julianh06.wynnextras.wtshim.core.net.NetManager;
import julianh06.wynnextras.wtshim.core.net.UrlManager;

public final class Managers {
    private Managers() {}

    public static FeatureManager Feature = new FeatureManager();
    public static FunctionManager Function = new FunctionManager();
    public static TickSchedulerManager TickScheduler = new TickSchedulerManager();
    public static ConnectionManager Connection = new ConnectionManager();

    // Net stack. Net first (Url/Download reference Managers.Net at runtime).
    public static NetManager Net = new NetManager();
    public static UrlManager Url = new UrlManager();
    public static DownloadManager Download = new DownloadManager();
}
