package julianh06.wynnextras.mixin.Accessor;

import com.wynntils.models.raid.type.RaidInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (value = RaidInfo.class, remap = false)
public interface RaidInfoAccessor {
    @Accessor("raidStartTime")
    long getRaidStartTime();
}
