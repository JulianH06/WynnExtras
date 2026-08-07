package julianh06.wynnextras.features.profileviewer.data;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public class AbilityMapData {
    public transient Map<String, Archetype> archetypes;
    public Map<Integer, List<Node>> pages;

    public static class Archetype {
        public String name;
        public String description;
        public String shortDescription;
        public Icon icon;
        public int slot;
    }

    public static class Coordinates {
        public int x;
        public int y;
    }

    public static class Icon {
        public String format;
        public Object value; // can be String or IconValue

        public static class IconValue {
            public String id;
            public String name;
            public Object customModelData; // can be a String or an object
        }
    }

    public static class Node {
        public String type; // "ability" or "connector"
        public Coordinates coordinates;
        public Meta meta;
        public List<String> family;
        public boolean unlocked = false; // optional: for merging with player data

        public static class Meta {
            public Object icon; // String or Icon
            public int page;
            public String id; // only for abilities
        }
    }

    // Assumes imports for net.minecraft.*
    public static Optional<ItemStack> iconToItemStack(Object metaIcon) {
        if (metaIcon == null) return Optional.empty();

        // Case: icon object
        if (metaIcon instanceof AbilityMapData.Icon) {
            AbilityMapData.Icon icon = (AbilityMapData.Icon) metaIcon;
            if (icon.value == null) return Optional.empty();

            if (icon.value instanceof String) {
                String val = (String) icon.value;
                Identifier id = Identifier.tryParse(val);
                if (id != null) {
                    Item item = Registries.ITEM.get(id);
                    if (item != Items.AIR) return Optional.of(new ItemStack(item));
                }
                return Optional.empty();
            }

            if (icon.value instanceof AbilityMapData.Icon.IconValue) {
                AbilityMapData.Icon.IconValue iv = (AbilityMapData.Icon.IconValue) icon.value;
                if (iv.id != null) {
                    Identifier id = Identifier.tryParse(iv.id);
                    if (id != null) {
                        Item item = Registries.ITEM.get(id);
                        if (item != Items.AIR) {
                            ItemStack stack = new ItemStack(item);
                            if (iv.customModelData != null) {
                                Integer cmd = parseCustomModelInt(iv.customModelData);
                                if (cmd != null) putCustomModelData(stack, cmd);
                                // Otherwise, parse complex NBT here if needed.
                            }
                            return Optional.of(stack);
                        }
                    }
                }
                return Optional.empty();
            }

            return Optional.empty();
        }

        // Case: metaIcon is directly a String (e.g. "connector_up_down" or "minecraft:stone")
        if (metaIcon instanceof String) {
            String val = (String) metaIcon;
            Identifier id = Identifier.tryParse(val);
            if (id != null) {
                Item item = Registries.ITEM.get(id);
                if (item != Items.AIR) return Optional.of(new ItemStack(item));
            }
            // The String can also be a connector name -> no ItemStack
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static void putCustomModelData(ItemStack stack, int cmd) {
        NbtCompound compTag = new NbtCompound();
        compTag.putInt("CustomModelData", cmd);
        NbtComponent customComp = NbtComponent.of(compTag);
        stack.set(DataComponentTypes.CUSTOM_DATA, customComp);
    }

    private static Integer parseCustomModelInt(Object o) {
        if (o instanceof Number) return ((Number)o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt((String)o); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static Optional<AbilityMapData.Icon> asIcon(Object raw) {
        if (raw instanceof AbilityMapData.Icon) return Optional.of((AbilityMapData.Icon) raw);
        return Optional.empty();
    }
    public static Optional<String> asIconString(Object raw) {
        if (raw instanceof String) return Optional.of((String) raw);
        return Optional.empty();
    }
}

