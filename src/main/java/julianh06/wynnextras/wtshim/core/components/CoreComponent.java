// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — base class for all components (Managers/Models/Services/Handlers).
 * Mirrors the Wynntils contract so subclasses WynnExtras references bind.
 */
package julianh06.wynnextras.wtshim.core.components;

import julianh06.wynnextras.wtshim.core.net.DownloadRegistry;

public abstract class CoreComponent {
    /**
     * Override to declare the data files this component downloads via the managed download layer.
     * Called once at init by {@link julianh06.wynnextras.wtshim.core.net.DownloadManager}.
     */
    public void registerDownloads(DownloadRegistry registry) {}
}
