package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.misc.GuildRaidBlockOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScreenHandler.class, priority = 1500)
public class PartyFinderClickBlockMixin {

    private static final String PARTY_FINDER_TITLE = "\uDAFF\uDFE1\uE00C";

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void blockGuildRaidClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.shiftDisableGuildRaid) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return;
        String title = mc.currentScreen.getTitle().getString();

        ScreenHandler self = (ScreenHandler) (Object) this;
        if (slotIndex < 0 || slotIndex >= self.slots.size()) return;
        Slot slot = self.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) return;

        String itemName = slot.getStack().getName().getString();
        String itemNameLower = itemName.toLowerCase();

        // Diagnostic: log every click on items containing "raid" so we can confirm the
        // mixin fires + see exactly what title / item name Wynncraft sends.
        if (itemNameLower.contains("raid")) {
            julianh06.wynnextras.core.WynnExtras.LOGGER.info(
                    "[GuildRaidBlock] click: slot={} item='{}' title='{}' titleMatches={}",
                    slotIndex, itemName, title, title.equals(PARTY_FINDER_TITLE));
        }

        if (!title.equals(PARTY_FINDER_TITLE)) return;

        long window = mc.getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (itemNameLower.contains("guild raid") && !shiftHeld) {
            ci.cancel();
            GuildRaidBlockOverlay.trigger();
        }
    }
}
