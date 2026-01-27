package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BannerBlockEntityRenderer.class)
public interface BannerBlockEntityRendererAccessor {
    @Accessor("standingFlagModel")
    BannerFlagBlockModel getStandingFlagModel();
}
