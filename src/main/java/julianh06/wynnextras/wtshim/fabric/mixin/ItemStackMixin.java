// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — makes vanilla ItemStack implement ItemStackExtension so WynnExtras' code can do
 * {@code ((ItemStackExtension)(Object) stack).setAnnotation(...)} without a ClassCastException.
 *
 * Source: Wynntils/common/.../mc/mixin/ItemStackMixin.java — same duck-typing pattern.
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import julianh06.wynnextras.wtshim.handlers.item.ItemAnnotation;
import julianh06.wynnextras.wtshim.mc.extension.ItemStackExtension;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackExtension {
    @Unique
    private ItemAnnotation wynnextras$annotation;

    @Override
    @Unique
    public ItemAnnotation getAnnotation() {
        return wynnextras$annotation;
    }

    @Override
    @Unique
    public void setAnnotation(ItemAnnotation annotation) {
        this.wynnextras$annotation = annotation;
    }
}
