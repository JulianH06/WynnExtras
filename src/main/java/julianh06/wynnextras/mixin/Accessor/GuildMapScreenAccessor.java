package julianh06.wynnextras.mixin.Accessor;

import julianh06.wynnextras.wtshim.models.territories.type.GuildResourceValues;
import julianh06.wynnextras.wtshim.screens.maps.GuildMapScreen;
import julianh06.wynnextras.wtshim.services.map.type.TerritoryFilterType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuildMapScreen.class, remap = false)
public interface GuildMapScreenAccessor {
    @Accessor
    TerritoryFilterType getTerritoryDefenseFilterType();

    @Accessor
    GuildResourceValues getTerritoryDefenseFilterLevel();

    @Accessor
    boolean isTerritoryDefenseFilterEnabled();

    @Accessor
    boolean isHybridMode();
}

