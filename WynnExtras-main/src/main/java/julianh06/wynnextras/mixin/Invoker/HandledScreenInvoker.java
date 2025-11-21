package julianh06.wynnextras.mixin.Invoker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenInvoker {
    @Invoker("drawItem")
    void invokeDrawItem(DrawContext context, ItemStack stack, int x, int y, @Nullable String amountText);

    @Invoker("drawSlotHighlightBack")
    void invokeDrawSlotHighlightBack(DrawContext context);

    @Invoker("drawSlotHighlightFront")
    void invokeDrawSlotHighlightFront(DrawContext context);
}
