// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — StatPossibleValues record. */
package julianh06.wynnextras.wtshim.models.stats.type;

import julianh06.wynnextras.wtshim.utils.type.RangedValue;

public record StatPossibleValues(StatType statType, RangedValue range, int baseValue, boolean preIdentified) {
}
