package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.crafting.CraftingResultPreviewer;
import julianh06.wynnextras.features.inventory.TradeMarketOverlay;
import julianh06.wynnextras.features.misc.GuildRaidBlockOverlay;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.raid.TreeRoomMinimap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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
        GuildRaidBlockOverlay.render(context);

        boolean isInventory = MinecraftClient.getInstance().currentScreen instanceof InventoryScreen;
        boolean isChat = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        if (!isInventory && !isChat) return;

        TreeRoomMinimap.render(context, null);
    }
}