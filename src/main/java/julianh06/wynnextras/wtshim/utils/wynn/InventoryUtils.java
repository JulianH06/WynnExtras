// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — InventoryUtils. */
package julianh06.wynnextras.wtshim.utils.wynn;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public final class InventoryUtils {
    private InventoryUtils() {}

    public static PlayerInventory inventory() {
        return MinecraftClient.getInstance().player == null
                ? null
                : MinecraftClient.getInstance().player.getInventory();
    }
}
