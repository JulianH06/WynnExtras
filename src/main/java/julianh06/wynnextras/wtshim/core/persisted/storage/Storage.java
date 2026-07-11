// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Adapted for the WynnExtras standalone compat shim (wtshim).
 *
 * DEVIATION: Wynntils' Storage is backed by a persisted-config framework (survives restarts).
 * The shim has no such framework, so this is an IN-MEMORY-ONLY holder: get()/store()/touched()
 * keep the value for the current session but nothing is written to disk. This is acceptable for
 * RaidModel because WynnExtras never reads RaidModel's persisted fields (dry-streak counters,
 * bestTimes, historicRaids) — the only Models.Raid method WynnExtras calls is getCurrentRaid(),
 * and RaidList/RaidSessionTracker keep their own JSON persistence (raidlist.json).
 */
package julianh06.wynnextras.wtshim.core.persisted.storage;

public class Storage<T> {
    private T value;

    public Storage(T defaultValue) {
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void store(T value) {
        this.value = value;
    }

    // No-op: there is no backing store to flag dirty in the shim.
    public void touched() {}
}
