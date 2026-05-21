package julianh06.wynnextras.mixin;

import com.sun.source.tree.Tree;
import julianh06.wynnextras.features.crafting.CraftingResultPreviewer;
import julianh06.wynnextras.features.inventory.TradeMarketOverlay;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.raid.TreeRoomMinimap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public class RaidLootOverlayClickMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseClick(long window, MouseInput input, int action, CallbackInfo ci) {
        int mods = input.modifiers();
        int button = input.button();

        Mouse mouse = (Mouse) (Object) this;
        double mouseX = mouse.getX();
        double mouseY = mouse.getY();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() != null) {
            double scale = mc.getWindow().getScaleFactor();
            mouseX = mouseX / scale;
            mouseY = mouseY / scale;
        }

        // Check if ctrl/shift is held
        boolean ctrlHeld = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftHeld = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        if (action == 1) {
            // On mousedown: short-circuit after first overlay claims the drag
            boolean dragClaimed = false;

            if (!dragClaimed) {
                boolean was = RaidLootTrackerOverlay.isDragging();
                RaidLootTrackerOverlay.handleClick(mouseX, mouseY, button, action, ctrlHeld, shiftHeld);
                if (!was && RaidLootTrackerOverlay.isDragging()) dragClaimed = true;
            }
            if (!dragClaimed) {
                boolean was = TradeMarketOverlay.isDragging();
                TradeMarketOverlay.handleClick(mouseX, mouseY, button, action);
                if (!was && TradeMarketOverlay.isDragging()) dragClaimed = true;
            }
            if (!dragClaimed) {
                boolean was = CraftingResultPreviewer.isDragging();
                CraftingResultPreviewer.handleClick(mouseX, mouseY, button, action);
                if (!was && CraftingResultPreviewer.isDragging()) dragClaimed = true;
            }
            if (!dragClaimed) {
                TreeRoomMinimap.handleClick(mouseX, mouseY, button, action, ctrlHeld, shiftHeld);
            }
        } else {
            // On mouseup: all overlays release drag independently
            RaidLootTrackerOverlay.handleClick(mouseX, mouseY, button, action, ctrlHeld, shiftHeld);
            TradeMarketOverlay.handleClick(mouseX, mouseY, button, action);
            CraftingResultPreviewer.handleClick(mouseX, mouseY, button, action);
            TreeRoomMinimap.handleClick(mouseX, mouseY, button, action, ctrlHeld, shiftHeld);
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() != null) {
            double scale = mc.getWindow().getScaleFactor();
            x = x / scale;
            y = y / scale;
        }

        RaidLootTrackerOverlay.handleMouseMove(x, y);

        TradeMarketOverlay.handleMouseMove(x, y);

        CraftingResultPreviewer.handleMouseMove(x, y);

        TreeRoomMinimap.handleMouseMove(x, y);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Mouse mouse = (Mouse) (Object) this;
        double mouseX = mouse.getX();
        double mouseY = mouse.getY();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() != null) {
            double scale = mc.getWindow().getScaleFactor();
            mouseX = mouseX / scale;
            mouseY = mouseY / scale;
        }
        if (TreeRoomMinimap.handleScroll(mouseX, mouseY, vertical)) {
            ci.cancel();
        }
    }
}