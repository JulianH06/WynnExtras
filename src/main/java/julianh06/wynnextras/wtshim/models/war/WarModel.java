// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * SLIM: only war active-state tracking is ported (the sole thing WynnExtras reads via
 * PlayerHider). War is detected by the "War:" scoreboard segment (WarScoreboardPart), the
 * same signal Wynntils uses. Dropped (no WynnExtras caller): Hades user proximity capture
 * in onWarStart, GuildWarEvent handling, historicWars persistence, WarBattleInfo.
 */
package julianh06.wynnextras.wtshim.models.war;

import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardPart;
import julianh06.wynnextras.wtshim.models.war.scoreboard.WarScoreboardPart;
import julianh06.wynnextras.wtshim.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.wtshim.models.worlds.type.WorldState;
import net.neoforged.bus.api.SubscribeEvent;

public final class WarModel extends Model {
    private static final ScoreboardPart WAR_SCOREBOARD_PART = new WarScoreboardPart();

    private boolean warActive = false;

    public WarModel() {
        Handlers.Scoreboard.addPart(WAR_SCOREBOARD_PART);
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.WORLD) {
            onWarEnd();
        }
    }

    public void onWarStart() {
        warActive = true;
    }

    public void onWarEnd() {
        warActive = false;
    }

    public boolean isWarActive() {
        return warActive;
    }
}
