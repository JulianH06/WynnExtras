package julianh06.wynnextras.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@org.spongepowered.asm.mixin.Mixin(com.wynntils.models.raid.raids.RaidKind.class)
public interface RaidKindAccessor {
    @Accessor(remap = false)
    Map<Integer, Map<String, String>> getChallengeNames();
}