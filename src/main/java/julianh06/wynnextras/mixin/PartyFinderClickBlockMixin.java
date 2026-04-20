package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.misc.GuildRaidBlockOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandledScreen.class, priority = 1500)
public class PartyFinderClickBlockMixin {

    private static final String PARTY_FINDER_TITLE = "\uDAFF\uDFE1\uE00C";

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void blockGuildRaidClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.shiftDisableGuildRaid) return;
        if (slot == null || !slot.hasStack()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        String title = screen.getTitle().getString();
        if (!title.equals(PARTY_FINDER_TITLE)) return;

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        String itemName = slot.getStack().getName().getString().toLowerCase();
        if (itemName.contains("guild raid available") && !shiftHeld) {
            ci.cancel();
            GuildRaidBlockOverlay.trigger();
        }
    }
}
