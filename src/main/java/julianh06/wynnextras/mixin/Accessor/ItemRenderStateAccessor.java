package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layerCount") int getLayerCount();
    @Accessor("layers") ItemRenderState.LayerRenderState[] getLayers();
}
