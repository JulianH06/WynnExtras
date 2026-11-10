package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.compat.wynntils.WynntilsCustomModelAdapter;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import java.util.List;
import java.util.Locale;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

final class PlannerItemIcons {
    private PlannerItemIcons() {}

    static ItemStack create(String slot, WynnItem item) {
        String subType = item.subType() == null ? "" : item.subType();
        ItemStack fallback = new ItemStack(switch (slot) {
            case "HELMET" -> Items.IRON_HELMET;
            case "CHESTPLATE" -> Items.IRON_CHESTPLATE;
            case "LEGGINGS" -> Items.IRON_LEGGINGS;
            case "BOOTS" -> Items.IRON_BOOTS;
            case "RING_1", "RING_2", "BRACELET", "NECKLACE" -> Items.AIR;
            default -> switch (subType.toLowerCase(Locale.ROOT)) {
                case "bow" -> Items.BOW;
                case "wand" -> Items.BLAZE_ROD;
                case "dagger" -> Items.SHEARS;
                case "relik" -> Items.TOTEM_OF_UNDYING;
                default -> Items.IRON_SWORD;
            };
        });
        if (item.iconId() == null || item.iconId().isBlank()) return fallback;
        try {
            ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.of(item.iconId())));
            if (stack.isEmpty()) return fallback;
            float modelData = WynntilsCustomModelAdapter.getFloat(item.iconModel())
                    .orElse((float) item.customModelData());
            if (modelData > 0) {
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                        List.of(modelData), List.of(), List.of(), List.of()));
            }
            return stack;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return fallback;
        }
    }
}
