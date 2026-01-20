package julianh06.wynnextras.mixin;

import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.render.WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor
    WorldRenderState getWorldRenderState();
}
