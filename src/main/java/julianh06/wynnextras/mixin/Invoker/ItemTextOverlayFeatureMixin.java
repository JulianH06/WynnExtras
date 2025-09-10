package julianh06.wynnextras.mixin.Invoker;

import com.wynntils.features.inventory.ItemTextOverlayFeature;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ItemTextOverlayFeature.class, remap = false)
public interface ItemTextOverlayFeatureMixin {
    @Invoker(value = "drawTextOverlay", remap = false)
    void invokeDrawTextOverlay(MatrixStack poseStack, ItemStack itemStack, int slotX, int slotY, boolean hotbar);
}
