package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.models.war.type.WarBattleInfo;
import com.wynntils.models.war.type.WarTowerState;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;

@WEModule
public class wartest {
    private Command testCMD = new Command(
        "wartest",
        context -> {
            for (TerritoryPoi poi : Models.Territory.getTerritoryPoisFromAdvancement()) {
                // name from Territories model vs name from WarBattleInfo
                if (!poi.isFakeTerritoryInfo()) {
                    if(poi.getTerritoryInfo() != null) {
                        System.out.println("[WynnExtras] Usable TerritoryInfo found for: " + poi.getName() + " defence: " + poi.getTerritoryInfo().getDefences());
                    } else {
                        System.out.println("[WynnExtras] No usable TerritoryInfo found for: " + poi.getName());
                    }
                }
            }
            return 1;
        }
    );
}