package julianh06.wynnextras.mixin.Invoker;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.class)
public interface GetTransformationInvoker {
    @Invoker("getTransformation") AffineTransformation invokeGetTransformation(DataTracker dataTracker);
}
