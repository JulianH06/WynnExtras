// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of LoreUtils.
 * Helpers for reading an ItemStack's lore (tooltip) lines.
 */
package julianh06.wynnextras.wtshim.utils.mc;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class LoreUtils {
    private LoreUtils() {}

    public static List<Text> getLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        LoreComponent lc = stack.get(DataComponentTypes.LORE);
        return lc == null ? List.of() : lc.lines();
    }

    public static String getStringLore(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        for (Text line : getLore(stack)) {
            sb.append(line.getString()).append('\n');
        }
        return sb.toString();
    }

    public static List<Text> getTooltipLines(ItemStack stack) {
        // Fallback to raw lore — full tooltip API varies by yarn version; tooltip-rendering
        // features should render via DrawContext.drawTooltip instead.
        return getLore(stack);
    }
}
