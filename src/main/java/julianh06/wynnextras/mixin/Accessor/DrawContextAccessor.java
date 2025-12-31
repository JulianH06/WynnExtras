package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (DrawContext.class)
public interface DrawContextAccessor {
    @Accessor
    VertexConsumerProvider.Immediate getVertexConsumers();
}
