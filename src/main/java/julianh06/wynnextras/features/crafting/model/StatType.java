package julianh06.wynnextras.features.crafting.model;

import java.util.Objects;

public final class StatType {
    public record Unit(String displayName) {
        public String getDisplayName() { return displayName; }
    }

    private final String apiName;
    private final String displayName;
    private final Unit unit;

    private StatType(String apiName) {
        this.apiName = apiName;
        this.displayName = apiName.replaceAll("([a-z])([A-Z])", "$1 $2");
        this.unit = new Unit(apiName.toLowerCase().contains("percent") || apiName.toLowerCase().endsWith("pct") ? "%" : "");
    }

    public static StatType fromApiName(String apiName) { return apiName == null ? null : new StatType(apiName); }
    public String getDisplayName() { return displayName; }
    public String getApiName() { return apiName; }
    public String getInternalRollName() { return apiName; }
    public String getKey() { return apiName; }
    public Unit getUnit() { return unit; }
    public boolean displayAsInverted() { return false; }
    @Override public boolean equals(Object other) { return other instanceof StatType type && apiName.equals(type.apiName); }
    @Override public int hashCode() { return Objects.hash(apiName); }
}
