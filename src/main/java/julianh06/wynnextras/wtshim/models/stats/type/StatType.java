// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — StatType. Richer def in Phase 5. */
package julianh06.wynnextras.wtshim.models.stats.type;

public class StatType {
    protected final String apiName;
    protected final String displayName;

    public StatType(String apiName, String displayName) {
        this.apiName = apiName;
        this.displayName = displayName;
    }

    public String getApiName() { return apiName; }
    public String getDisplayName() { return displayName; }
    public String getKey() { return apiName; }

    /* The "internalRollName" is the id used in the json lore of other players' items. The shim has
     * no id_keys.json registry, so it falls back to the apiName (callers try apiName/key too). */
    public String getInternalRollName() { return apiName; }

    /** True for stats where "bigger number = better for negative baseline", e.g. XP Bonus. */
    public boolean displayAsInverted() { return false; }

    /** Unit enum (percent/raw/per-mana/etc.) — simplified stub. */
    public StatUnit getUnit() { return StatUnit.RAW; }

    public enum StatUnit {
        RAW(""),
        PERCENT("%"),
        PER_SECOND("/s"),
        PER_MANA("/mana"),
        TIER("");

        private final String displayName;
        StatUnit(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
