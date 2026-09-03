package julianh06.wynnextras.features.inventory;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Limits the model-data override to the item currently receiving a scale background.
 */
public final class ScaleBackgroundRenderState {
    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013",
            "\uDAFF\uDFE8\uE00F",
            "\uDAFF\uDFE8\uE010",
            "\uDAFF\uDFE8\uE011"
    );
    private static final ThreadLocal<Integer> ACTIVE_COMPONENT_HASH = new ThreadLocal<>();

    private ScaleBackgroundRenderState() {}

    public static void begin(ItemStack stack) {
        ACTIVE_COMPONENT_HASH.set(stack.getComponents().hashCode());
    }

    public static boolean isActive(ItemStack stack) {
        Integer activeHash = ACTIVE_COMPONENT_HASH.get();
        return activeHash != null && activeHash == stack.getComponents().hashCode();
    }

    public static WeightDisplay.ItemData resolve(ItemStack stack) {
        if (!WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled || !isInTradeMarket()) return null;
        if (stack == null || stack.isEmpty()) return null;
        if (!WeightDisplay.isTrackedMythic(stack) || WeightDisplay.isUnidentified(stack)) return null;

        int componentHash = stack.getComponents().hashCode();
        WeightDisplay.ItemData itemData = WeightDisplay.weightCacheByHash.get(componentHash);
        if (itemData == null || itemData.data().isEmpty()) {
            itemData = WeightDisplay.computeScale(stack);
            if (itemData != null && !itemData.data().isEmpty()) {
                WeightDisplay.weightCacheByHash.put(componentHash, itemData);
            }
        }
        return itemData == null || itemData.data().isEmpty() ? null : itemData;
    }

    public static void end() {
        ACTIVE_COMPONENT_HASH.remove();
    }

    public static boolean isInTradeMarket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return false;
        String title = client.currentScreen.getTitle().getString();
        return TRADE_MARKET_TITLES.stream().anyMatch(title::contains);
    }
}
