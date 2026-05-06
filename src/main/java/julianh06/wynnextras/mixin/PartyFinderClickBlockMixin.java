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

/**
 * Cancels guild-raid slot clicks in the Party Finder unless the user holds Shift.
 *
 * Has to mixin {@link HandledScreen#onMouseClick(Slot, int, int, SlotActionType)} —
 * NOT {@link net.minecraft.screen.ScreenHandler#onSlotClick} — because clickSlot fires
 * the network packet BEFORE the local onSlotClick runs. Cancelling onSlotClick stops
 * local prediction only; the server still toggles the queue. onMouseClick is upstream
 * of both the packet send and the predict, so cancelling here actually prevents the toggle.
 */
@Mixin(HandledScreen.class)
public class PartyFinderClickBlockMixin {

    private static final String PARTY_FINDER_TITLE = "\uDAFF\uDFE1\uE00C";

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void blockGuildRaidClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.shiftDisableGuildRaid) return;
        if (slot == null || !slot.hasStack()) return;

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (!self.getTitle().getString().equals(PARTY_FINDER_TITLE)) return;

        String itemNameLower = slot.getStack().getName().getString().toLowerCase();
        if (!itemNameLower.contains("guild raid")) return;

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (!shiftHeld) {
            ci.cancel();
            GuildRaidBlockOverlay.trigger();
        }
    }
}
