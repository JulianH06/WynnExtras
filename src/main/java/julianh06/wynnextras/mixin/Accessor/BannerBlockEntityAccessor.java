package julianh06.wynnextras.mixin.Accessor;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.component.type.BannerPatternsComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BannerBlockEntity.class)
public interface BannerBlockEntityAccessor {
    @Accessor("patterns")
    public void setPatterns(BannerPatternsComponent patterns);
}
