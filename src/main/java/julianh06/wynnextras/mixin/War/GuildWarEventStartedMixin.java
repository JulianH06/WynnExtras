package julianh06.wynnextras.mixin.War;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResourceValues; // adjust if needed
import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.models.war.type.WarBattleInfo;
import com.wynntils.models.war.type.WarTowerState;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.RangedValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuildWarEvent.Started.class)
public class GuildWarEventStartedMixin {

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void started(WarBattleInfo warBattleInfo, CallbackInfo ci) {
        System.out.println("[WynnExtras] GuildWarEvent.Started mixin injected");

        WarBattleInfo info = warBattleInfo;
        WarTowerState towerState = info.getInitialState();

        long health = towerState.health();
        double defense = towerState.defense();
        double attackspeed = towerState.attackSpeed();
        RangedValue damage = towerState.damage();

        String warTerritory = info.getTerritory();
        System.out.println("[WynnExtras] War territory: " + warTerritory);

        TerritoryInfo territoryInfo = null;

        for (TerritoryPoi poi : Models.Territory.getTerritoryPois()) {
            // name from Territories model vs name from WarBattleInfo
            if (!poi.isFakeTerritoryInfo()
                    && poi.getTerritoryInfo() != null
                    && poi.getName().equals(warTerritory)) {
                territoryInfo = poi.getTerritoryInfo();
                break;
            }
        }

        if (territoryInfo == null) {
            System.out.println("[WynnExtras] No usable TerritoryInfo found for: " + warTerritory);
            return;
        }

        GuildResourceValues def = territoryInfo.getDefences();
        System.out.println("[WynnExtras] Territory defences value: " + def);
    }
}