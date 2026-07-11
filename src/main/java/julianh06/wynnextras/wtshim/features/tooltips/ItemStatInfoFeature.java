// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ItemStatInfoFeature stand-in. Phase 8 — mixin targets only. */
package julianh06.wynnextras.wtshim.features.tooltips;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.core.persisted.config.Config;
import julianh06.wynnextras.wtshim.mc.event.ItemTooltipRenderEvent;

public class ItemStatInfoFeature extends Feature {
    public final Config<Boolean> identificationDecorations = new Config<>(true);
    public julianh06.wynnextras.wtshim.handlers.tooltip.type.TooltipIdentificationDecorator getIdentificationDecorator() {
        return null;
    }

    /** @Inject targets — WynnExtras injects its own tooltip extensions here. */
    private void onTooltipPre(ItemTooltipRenderEvent.Pre event) { /* no-op */ }
    private void onTooltipPreFinalize(ItemTooltipRenderEvent.Pre event) { /* no-op */ }
}
