package julianh06.wynnextras.mixin.Accessor;

import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.models.raid.type.RaidRoomInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin (value = RaidInfo.class, remap = false)
public interface RaidInfoAccessor {
    @Accessor("raidStartTime")
    long getRaidStartTime();

    @Accessor("challenges")
    Map<Integer, RaidRoomInfo> getChallenges();
}
