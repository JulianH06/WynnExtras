package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Mixin to render weight scale color as background on item slots
 * Only renders in Trade Market screens using Wynntils CIRCLE_OPAQUE texture
 */
@Mixin(HandledScreen.class)
public abstract class WeightScaleBackgroundMixin {

    @Shadow protected int x;
    @Shadow protected int y;

    // Trade Market screen title identifiers
    @Unique
    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011"  // Item listing / search
    );

    // CIRCLE_OPAQUE is ordinal 2 in HighlightTexture enum
    @Unique
    private static final int CIRCLE_OPAQUE_ORDINAL = 2;

    // Inject at HEAD to render BEFORE the item (so it's behind)
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawScaleBackground(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled) return;
        if (!WynnExtrasConfig.INSTANCE.showScales) return;
        if (!Models.WorldState.onWorld()) return;

        // Only render in Trade Market
        if (!isInTradeMarket()) return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;

        // Check if this is a gear item
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(stack);
        if (wynnItemOpt.isEmpty()) return;
        WynnItem wynnItem = wynnItemOpt.get();

        int color;

        // Check for unidentified mythic (GearBoxItem)
        if (wynnItem instanceof GearBoxItem gearBox) {
            if (gearBox.getGearTier() != GearTier.MYTHIC) return;
            // Dark purple for unidentified mythics
            color = 0x550055;
        } else if (wynnItem instanceof GearItem gearItem) {
            // Only works for Mythic items (that's what Wynnpool has weight data for)
            if (gearItem.getGearTier() != GearTier.MYTHIC) return;

            // Get the encoded item string for cache lookup
            String encodedItem = ItemUtils.itemStackToItemString(stack);
            if (encodedItem == null) return;

            // Check if we have weight data for this item, if not try to calculate it
            WeightDisplay.ItemData itemData = WeightDisplay.weightCache.get(encodedItem);
            if (itemData == null || itemData.data().isEmpty()) {
                // Try to calculate the weight on-the-fly
                List<Text> tooltip = TooltipUtils.getWynnItemTooltip(stack, wynnItem);
                WeightDisplay.calculateScale(encodedItem, stack, tooltip);
                itemData = WeightDisplay.weightCache.get(encodedItem);
                if (itemData == null || itemData.data().isEmpty()) return;
            }

            // Get the currently selected weight data
            int index = 0;
            String itemName = stack.getName().getString()
                    .replace("À", "")
                    .replaceAll("§[0-9a-fk-or]", "")
                    .replace("⬡ Shiny ", "")
                    .strip();
            WeightDisplay.ItemData profileData = WeightDisplay.itemCache.get(itemName);
            if (profileData != null) {
                index = profileData.index();
            }

            if (index >= itemData.data().size()) {
                index = 0;
            }

            WeightDisplay.WeightData weightData = itemData.data().get(index);
            float score = weightData.score();

            // Get color from the score using Wynntils color scale
            color = getScaleColor(score);
        } else {
            return;
        }

        // Slot position (DrawContext already has translation applied)
        int slotX = slot.x;
        int slotY = slot.y;

        // Convert RGB to CustomColor for Wynntils RenderUtils (full opacity)
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        CustomColor customColor = new CustomColor(r, g, b, 255);

        // Render the CIRCLE_OPAQUE texture many times to make it fully opaque
        try {
            for (int i = 0; i < 20; i++) {
                RenderUtils.drawTexturedRect(
                        context,
                        Texture.HIGHLIGHT.identifier(),
                        customColor,
                        (float) (slotX - 1),
                        (float) (slotY - 1),
                        18.0F, 18.0F,
                        CIRCLE_OPAQUE_ORDINAL * 18,  // U offset for CIRCLE_OPAQUE
                        0.0F, 18.0F, 18.0F,
                        Texture.HIGHLIGHT.width(),
                        Texture.HIGHLIGHT.height()
                );
            }
        } catch (Exception ignored) {
            // Fallback to simple fill if texture fails
            int solidColor = (255 << 24) | (color & 0x00FFFFFF);
            context.fill(slotX, slotY, slotX + 16, slotY + 16, solidColor);
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

    // Inject at TAIL to render comparison borders ON TOP of everything
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawComparisonBorder(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
        if (!isInTradeMarket()) return;
        if (!TradeMarketComparisonPanel.hasAnyComparison()) return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;

        int borderColor = TradeMarketComparisonPanel.getComparisonBorderColor(stack);
        if (borderColor == 0) return;

        int slotX = slot.x;
        int slotY = slot.y;

        // Draw a 2px thick border around the slot
        // Top
        context.fill(slotX - 2, slotY - 2, slotX + 18, slotY, borderColor);
        // Bottom
        context.fill(slotX - 2, slotY + 16, slotX + 18, slotY + 18, borderColor);
        // Left
        context.fill(slotX - 2, slotY, slotX, slotY + 16, borderColor);
        // Right
        context.fill(slotX + 16, slotY, slotX + 18, slotY + 16, borderColor);
    }

    /**
     * Convert a score (0-100) to a color matching Wynntils' exact percentage color scale
     * Uses same color stops and linear interpolation (lerp) as Wynntils:
     * 0% = Red, 40% = Gold, 70% = Yellow, 90% = Green, 100% = Aqua
     */
    @Unique
    private int getScaleColor(float score) {
        // Clamp score to 0-100
        score = Math.max(0, Math.min(100, score));

        // Wynntils color stops (Minecraft formatting colors)
        // RED = 0xFF5555, GOLD = 0xFFAA00, YELLOW = 0xFFFF55, GREEN = 0x55FF55, AQUA = 0x55FFFF

        if (score < 40) {
            // Red (0xFF5555) -> Gold (0xFFAA00)
            return lerpColor(0xFF5555, 0xFFAA00, score / 40f);
        } else if (score < 70) {
            // Gold (0xFFAA00) -> Yellow (0xFFFF55)
            return lerpColor(0xFFAA00, 0xFFFF55, (score - 40) / 30f);
        } else if (score < 90) {
            // Yellow (0xFFFF55) -> Green (0x55FF55)
            return lerpColor(0xFFFF55, 0x55FF55, (score - 70) / 20f);
        } else {
            // Green (0x55FF55) -> Aqua (0x55FFFF)
            return lerpColor(0x55FF55, 0x55FFFF, (score - 90) / 10f);
        }
    }

    /**
     * Linear interpolation between two RGB colors
     */
    @Unique
    private int lerpColor(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));

        return (r << 16) | (g << 8) | b;
    }
}
