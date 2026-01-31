package julianh06.wynnextras.mixin.Accessor;

import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.services.map.type.TerritoryFilterType;
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

