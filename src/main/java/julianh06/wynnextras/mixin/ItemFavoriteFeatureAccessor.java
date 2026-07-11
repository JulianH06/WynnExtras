package julianh06.wynnextras.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(julianh06.wynnextras.wtshim.features.inventory.ItemFavoriteFeature.class)
public interface ItemFavoriteFeatureAccessor {
    @Invoker
    boolean callIsFavorited(ItemStack itemStack);
}
