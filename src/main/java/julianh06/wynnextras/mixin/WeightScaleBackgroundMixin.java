package julianh06.wynnextras.mixin;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.utils.WynntilsHighlightUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class WeightScaleBackgroundMixin {
    @Unique private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011"  // Item listing / search
    );

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawScaleBackground(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled) return;
        if (!isInTradeMarket()) return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;
        if (!WeightDisplay.isTrackedMythic(stack)) return;

        int color;

        if (WeightDisplay.isUnidentified(stack)) {
            return;
        } else {
            int componentHash = stack.getComponents().hashCode();
            WeightDisplay.ItemData itemData = WeightDisplay.weightCacheByHash.get(componentHash);
            if (itemData == null || itemData.data().isEmpty()) {
                itemData = WeightDisplay.computeScale(stack);
                if (itemData != null && !itemData.data().isEmpty()) {
                    WeightDisplay.weightCacheByHash.put(componentHash, itemData);
                }
            }

            if (itemData == null || itemData.data().isEmpty()) {
                return;
            } else {
                int index = 0;
                String cleanName = WeightDisplay.extractCleanName(stack);
                WeightDisplay.ItemData profileData = WeightDisplay.itemCache.get(cleanName);
                if (profileData != null) index = profileData.index();
                if (index >= itemData.data().size()) index = 0;

                color = WeightDisplay.getScaleColor(itemData.data().get(index).score());
            }
        }

        int opacity = Math.clamp(WynnExtrasConfig.INSTANCE.scaleBackgroundOpacity, 0, 100);
        int alpha = Math.round(opacity * 255 / 100f);
        CustomColor backgroundColor = new CustomColor(
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF,
                alpha
        );
        WynntilsHighlightUtils.drawHighlightTexture(
                context,
                WynnExtrasConfig.INSTANCE.scaleBackgroundShape.texture(),
                backgroundColor,
                slot.x - 8,
                slot.y - 8
        );
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawComparisonBorder(DrawContext context, Slot slot, int mx, int my, CallbackInfo ci) {
        if (!isInTradeMarket()) return;
        if (!TradeMarketComparisonPanel.hasAnyComparison()) return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;

        int borderColor = TradeMarketComparisonPanel.getComparisonBorderColor(stack);
        if (borderColor == 0) return;

        int sx = slot.x, sy = slot.y;
        context.fill(sx - 2, sy - 2, sx + 18, sy, borderColor);
        context.fill(sx - 2, sy + 16, sx + 18, sy + 18, borderColor);
        context.fill(sx - 2, sy, sx, sy + 16, borderColor);
        context.fill(sx + 16, sy, sx + 18, sy + 16, borderColor);
    }

    @Unique
    private boolean isInTradeMarket() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;
        String title = mc.currentScreen.getTitle().getString();
        for (String t : TRADE_MARKET_TITLES) {
            if (title.contains(t)) return true;
        }
        return false;
    }
}
