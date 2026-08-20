package julianh06.wynnextras.features.inventory;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.wynncraft.item.MountColorParser;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class MountColorBackground {
    private record CacheEntry(LoreComponent lore, CustomModelDataComponent modelData,
                              int count, ItemStack renderedStack) {}

    private static final Identifier TIER_BACKGROUND =
            Identifier.of("minecraft", "textures/wynn/gui/layer/tier/full.png");
    private static final Map<ItemStack, CacheEntry> CACHE = new WeakHashMap<>();

    private MountColorBackground() {}

    public static ItemStack apply(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        CustomModelDataComponent modelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        synchronized (CACHE) {
            CacheEntry cached = CACHE.get(stack);
            if (cached != null && cached.lore() == lore
                    && cached.modelData() == modelData && cached.count() == stack.getCount()) {
                return cached.renderedStack();
            }
        }

        return MountColorParser.parse(stack).map(colors -> {
            List<String> strings = modelData == null ? List.of() : modelData.strings();
            if (strings.isEmpty() || strings.getFirst().isEmpty()) return stack;

            ItemStack copy = stack.copy();
            List<String> withRarity = new ArrayList<>(strings);
            withRarity.set(0, "");
            copy.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    modelData == null ? List.of() : modelData.floats(),
                    modelData == null ? List.of() : modelData.flags(),
                    withRarity,
                    modelData == null ? List.of() : modelData.colors()
            ));
            synchronized (CACHE) {
                CACHE.put(stack, new CacheEntry(lore, modelData, stack.getCount(), copy));
            }
            return copy;
        }).orElse(stack);
    }

    public static void draw(DrawContext context, ItemStack stack, int x, int y) {
        if (!WynnExtrasConfig.INSTANCE.mountPrimaryColorBackground) return;

        MountColorParser.parse(stack).ifPresent(colors -> {
            RenderUtils.drawTexturedRect(context, TIER_BACKGROUND, colors.primaryColor(),
                    x - 8, y - 8, 32, 32, 32, 32);
            context.createNewRootLayer();
        });
    }
}
