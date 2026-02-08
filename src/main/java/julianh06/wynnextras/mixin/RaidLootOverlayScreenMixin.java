package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.crafting.CraftingResultPreviewer;
import julianh06.wynnextras.features.inventory.TradeMarketOverlay;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class RaidLootOverlayScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderOverlayOnScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RaidLootTrackerOverlay.renderOnScreen(context);
        TradeMarketOverlay.renderOnScreen(context);
        CraftingResultPreviewer.onRender(context);
    }
}