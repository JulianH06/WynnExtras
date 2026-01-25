package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.inventory.TradeMarketOverlay;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        RaidLootTrackerOverlay.handleClick(mouseX, mouseY, button, action, ctrlHeld, shiftHeld);
        TradeMarketOverlay.handleClick(mouseX, mouseY, button, action);
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() != null) {
            double scale = mc.getWindow().getScaleFactor();
            x = x / scale;
            y = y / scale;
        }

        if (RaidLootTrackerOverlay.isDragging()) {
            RaidLootTrackerOverlay.handleMouseMove(x, y);
        }
        if (TradeMarketOverlay.isDragging()) {
            TradeMarketOverlay.handleMouseMove(x, y);
        }
    }
}