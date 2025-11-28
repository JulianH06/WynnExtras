package julianh06.wynnextras.mixin;

import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = TooltipUtils.class, remap = false)
public class TooltipUtilsMixin {

    @Inject(method = "getWynnItemTooltip", at = @At("RETURN"), cancellable = true)
    private static void injectWeights(ItemStack itemStack, WynnItem wynnItem, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltips = cir.getReturnValue();

        if (!SimpleConfig.getInstance(WynnExtrasConfig.class).showWeight) {
            return;
        }

        if (!ItemUtils.isTier(itemStack, GearTier.MYTHIC)) {
            return;
        }

        String itemString = ItemUtils.itemStackToItemString(itemStack);
        if (itemString == null) return;

        if(!(wynnItem instanceof GearItem)) return;
        itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.of(((GearItem) wynnItem).getName()));
        WeightDisplay.WeightData cached = WeightDisplay.getCachedWeight(itemString, true, itemStack, tooltips);
        if (cached == null) return;

        if ((WeightDisplay.upPressed || WeightDisplay.downPressed) && itemStack.getCustomName() != null) {

            String key = itemStack.getCustomName().getString()
                    .replace("À", "")
                    .replaceAll("§[0-9a-fk-or]", "")
                    .replace("⬡ Shiny ", "")
                    .strip();

            WeightDisplay.ItemData itemData = WeightDisplay.itemCache.get(key);
            if (itemData != null && !itemData.data().isEmpty()) {

                int nextIndex = itemData.index();

                if (WeightDisplay.downPressed) {
                    nextIndex = (itemData.index() + 1) % itemData.data().size();
                } else if (WeightDisplay.upPressed) {
                    nextIndex = (itemData.index() - 1 + itemData.data().size()) % itemData.data().size();
                }

                WeightDisplay.itemCache.put(key, new WeightDisplay.ItemData(itemData.name(), itemData.data(), nextIndex));
            }

            WeightDisplay.upPressed = false;
            WeightDisplay.downPressed = false;
        }

        List<Text> modified = WeightDisplay.modifyTooltip(tooltips, cached, itemStack, itemString);

        cir.setReturnValue(modified);
    }
}


