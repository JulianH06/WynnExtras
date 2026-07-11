// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TooltipFittingFeature.
 * Mixin targets: WynnExtras @Injects into onTooltipPre; method stub so injection applies.
 */
package julianh06.wynnextras.wtshim.features.tooltips;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.mc.event.ItemTooltipRenderEvent;

public class TooltipFittingFeature extends Feature {
    /** @Inject target. Empty — WynnExtras runs its fitting logic via injected code. */
    private void onTooltipPre(ItemTooltipRenderEvent.Pre event) { /* no-op */ }
}
