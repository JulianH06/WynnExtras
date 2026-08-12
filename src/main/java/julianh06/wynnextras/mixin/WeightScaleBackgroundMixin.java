package julianh06.wynnextras.mixin;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.features.inventory.ScaleBackgroundRenderState;
import julianh06.wynnextras.utils.ItemHighlightRenderer;
import julianh06.wynnextras.features.misc.SlotNumberDebugger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandledScreen.class, priority = 1100)
public abstract class WeightScaleBackgroundMixin {
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawScaleBackground(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = slot.getStack();
        WeightDisplay.ItemData itemData = ScaleBackgroundRenderState.resolve(stack);
        if (itemData == null) return;

        int index = 0;
        String cleanName = WeightDisplay.extractCleanName(stack);
        WeightDisplay.ItemData profileData = WeightDisplay.itemCache.get(cleanName);
        if (profileData != null) index = profileData.index();
        if (index >= itemData.data().size()) index = 0;
        int color = WeightDisplay.getScaleColor(itemData.data().get(index).score());

        int opacity = Math.clamp(WynnExtrasConfig.INSTANCE.scaleBackgroundOpacity, 0, 100);
        int alpha = Math.round(opacity * 255 / 100f);
        CustomColor backgroundColor = new CustomColor(
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF,
                alpha
        );
        ScaleBackgroundRenderState.begin(stack);
        ItemHighlightRenderer.drawScaleBackground(
                context,
                WynnExtrasConfig.INSTANCE.scaleBackgroundShape,
                backgroundColor,
                slot.x - 1,
                slot.y - 1,
                18,
                18
        );
        // GUI drawing is deferred and batched by render type in 1.21.11. Start a new
        // root layer so the item is guaranteed to be submitted above the background.
        context.createNewRootLayer();
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void clearScaleBackgroundRenderState(
            DrawContext context,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        ScaleBackgroundRenderState.end();
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawComparisonBorder(DrawContext context, Slot slot, int mx, int my, CallbackInfo ci) {
        SlotNumberDebugger.render(context, slot);

        if (!ScaleBackgroundRenderState.isInTradeMarket()) return;
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

}
