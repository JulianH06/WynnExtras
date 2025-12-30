package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("focusedSlot")
    void setFocusedSlot(Slot slot);

    @Accessor("touchDragStack")
    ItemStack getTouchDragStack();

    @Accessor
    int getX();

    @Accessor
    int getY();
}
