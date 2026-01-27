package julianh06.wynnextras.utils;

import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.type.ErrorOr;
import net.minecraft.item.ItemStack;

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
}
