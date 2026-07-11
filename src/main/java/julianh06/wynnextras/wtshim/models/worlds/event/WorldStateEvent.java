// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim). Faithful shape (newState/oldState/
 * worldName/isFirstJoinWorld) — replaces the earlier Phase-1 stub.
 */
package julianh06.wynnextras.wtshim.models.worlds.event;

import julianh06.wynnextras.wtshim.models.worlds.type.WorldState;
import net.neoforged.bus.api.Event;

public class WorldStateEvent extends Event {
    private final WorldState newState;
    private final WorldState oldState;
    private final String worldName;
    private final boolean isFirstJoinWorld;

    public WorldStateEvent(WorldState newState, WorldState oldState, String worldName, boolean isFirstJoinWorld) {
        this.newState = newState;
        this.oldState = oldState;
        this.worldName = worldName;
        this.isFirstJoinWorld = isFirstJoinWorld;
    }

    public WorldState getNewState() {
        return newState;
    }

    public WorldState getOldState() {
        return oldState;
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean isFirstJoinWorld() {
        return isFirstJoinWorld;
    }
}
