// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — StatActualValue record. */
package julianh06.wynnextras.wtshim.models.stats.type;

public record StatActualValue(StatType statType, int value, int stars, StatPossibleValues possibleValues) {
    public int baseValue() { return value; }
}
