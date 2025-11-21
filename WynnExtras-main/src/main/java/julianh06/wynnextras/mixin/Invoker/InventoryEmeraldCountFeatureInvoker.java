package julianh06.wynnextras.mixin.Invoker;

import com.wynntils.features.inventory.InventoryEmeraldCountFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = InventoryEmeraldCountFeature.class, remap = false)
public interface InventoryEmeraldCountFeatureInvoker {
    @Invoker(value = "getRenderableEmeraldAmounts", remap = false)
    String[] invokeGetRenderableEmeraldAmounts(int emeralds);
}
