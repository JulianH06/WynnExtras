package julianh06.wynnextras.utils;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.items.WynnItem;
import julianh06.wynnextras.wtshim.models.items.encoding.type.EncodingSettings;
import julianh06.wynnextras.wtshim.models.items.items.game.GearItem;
import julianh06.wynnextras.wtshim.utils.EncodedByteBuffer;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class ItemUtils {
    public static GearTier getTier(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        Optional<GearItem> gearItemOptional = Models.Item.asWynnItem(itemStack, GearItem.class);
        if (gearItemOptional.isEmpty()) {
            return null;
        }

        GearItem gearItem = gearItemOptional.get();

        return gearItem.getGearTier();
    }

    public static boolean isTier(ItemStack itemStack, GearTier tier) {
        GearTier itemTier = getTier(itemStack);
        return itemTier != null && itemTier == tier;
    }

    public static String itemStackToItemString(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) {
            return null;
        }

        WynnItem wynnItem = wynnItemOpt.get();

        EncodingSettings settings = new EncodingSettings(
                Models.ItemEncoding.extendedIdentificationEncoding.get(),
                Models.ItemEncoding.shareItemName.get()
        );

        ErrorOr<EncodedByteBuffer> errorOrEncoded = Models.ItemEncoding.encodeItem(wynnItem, settings);
        if (errorOrEncoded.hasError()) {
            return null;
        }

        return Models.ItemEncoding.makeItemString(wynnItem, errorOrEncoded.getValue());
    }

    public static Float getFirsCustomModelDataFloat(ItemStack itemStack) {
        CustomModelDataComponent modelData = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (modelData == null) return null;
        List<Float> floats = modelData.floats();
        if (floats == null || floats.isEmpty()) return null;
        return floats.getFirst();
    }
}
