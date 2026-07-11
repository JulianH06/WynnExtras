// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — DurabilityOverlayFeature stand-in.
 *
 * WynnExtras' BankOverlay2 looks this feature up and its DurabilityOverlayFeatureInvoker
 * @Invokers into the three draw* methods to render durability decorations on bank slots.
 * In standalone mode there's no Wynntils renderer behind these, so they're no-ops for now —
 * durability-overlay rendering in the bank is a follow-up (needs the item's durability data).
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.core.persisted.config.Config;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class DurabilityOverlayFeature extends Feature {
    public Optional<Config<?>> getConfigOptionFromString(String name) {
        return Optional.empty();
    }

    // @Invoker targets used by DurabilityOverlayFeatureInvoker.
    public void drawDurabilityArc(DrawContext context, ItemStack stack, int x, int y) {}
    public void drawDurabilityBar(DrawContext context, ItemStack stack, int x, int y) {}
    public void drawDurabilityPercentage(DrawContext context, ItemStack stack, int x, int y) {}
}
