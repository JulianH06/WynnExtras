package julianh06.wynnextras.mixin.compat.wynntils;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.wynntils.mc.event.SlotRenderEvent", remap = false)
public interface WynntilsSlotRenderEventAccessor {
    @Invoker("getSlot")
    Slot wynnextras$getSlot();
}
