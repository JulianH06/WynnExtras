package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.inventory.MountColorBackground;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class MountColorBackgroundMixin {
    @Inject(
            method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawMountColorBackground(LivingEntity entity, World world, ItemStack stack,
                                          int x, int y, int seed, CallbackInfo ci) {
        MountColorBackground.draw((DrawContext) (Object) this, stack, x, y);
    }
}
