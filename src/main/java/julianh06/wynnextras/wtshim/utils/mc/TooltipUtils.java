// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TooltipUtils stub.
 * Used by BankOverlay for tooltip measurement/rendering. Minimal impl for compile;
 * runtime behavior fleshed out in Phase 5 once item parsing exists.
 */
package julianh06.wynnextras.wtshim.utils.mc;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public final class TooltipUtils {
    private TooltipUtils() {}

    public static List<Text> getWynnItemTooltip(Slot slot, int i) {
        if (slot == null) return List.of();
        return LoreUtils.getLore(slot.getStack());
    }

    public static List<Text> getWynnItemTooltip(ItemStack stack) {
        return LoreUtils.getLore(stack);
    }

    /** Two-arg (ItemStack, WynnItem) overload used by BankOverlay. */
    public static List<Text> getWynnItemTooltip(ItemStack stack, Object wynnItem) {
        return LoreUtils.getLore(stack);
    }

    public static List<TooltipComponent> getClientTooltipComponent(List<Text> tooltip) {
        if (tooltip == null) return List.of();
        return tooltip.stream()
                .map(t -> TooltipComponent.of(t.asOrderedText()))
                .toList();
    }

    public static int getTooltipHeight(List<TooltipComponent> components) {
        if (components == null || components.isEmpty()) return 0;
        int h = 0;
        for (TooltipComponent c : components) h += c.getHeight(MinecraftClient.getInstance().textRenderer);
        return h;
    }
}
