// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.type;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ContainerContentVerification {
    boolean verify(ContainerContent container, Int2ObjectMap<ItemStack> changes, ContainerContentChangeType changeType);
}
