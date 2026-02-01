package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Mixin to disable Wynntils' purple mythic item highlight in Trade Market
 * when WynnExtras scale background is enabled (so our colors show instead)
 */
@Mixin(value = ItemHighlightFeature.class, remap = false)
public class ItemHighlightFeatureMixin {

    @Unique
    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011"  // Item listing / search
    );

    @Inject(method = "getHighlightColor", at = @At("HEAD"), cancellable = true)
    private void disableMythicHighlight(ItemStack itemStack, boolean hotbarHighlight, CallbackInfoReturnable<CustomColor> cir) {
        // Only intercept if our scale background is enabled
        if (!WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled) return;
        if (!WynnExtrasConfig.INSTANCE.showScales) return;

        // Only disable Wynntils highlight in Trade Market (elsewhere keep their purple)
        if (!isInTradeMarket()) return;

        // Check if this is a mythic item (identified or unidentified)
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) return;

        WynnItem wynnItem = wynnItemOpt.get();

        // Check for identified mythic
        if (wynnItem instanceof GearItem gearItem) {
            if (gearItem.getGearTier() == GearTier.MYTHIC) {
                cir.setReturnValue(CustomColor.NONE);
            }
        }
        // Check for unidentified mythic
        else if (wynnItem instanceof GearBoxItem gearBox) {
            if (gearBox.getGearTier() == GearTier.MYTHIC) {
                cir.setReturnValue(CustomColor.NONE);
            }
        }
    }

    @Unique
    private boolean isInTradeMarket() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String title = mc.currentScreen.getTitle().getString();
        for (String marketTitle : TRADE_MARKET_TITLES) {
            if (title.contains(marketTitle)) return true;
        }
        return false;
    }
}
