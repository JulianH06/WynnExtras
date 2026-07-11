// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — TooltipIdentificationDecorator stub. */
package julianh06.wynnextras.wtshim.handlers.tooltip.type;

import julianh06.wynnextras.wtshim.models.stats.type.StatActualValue;
import julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues;
import net.minecraft.text.MutableText;

public interface TooltipIdentificationDecorator {
    MutableText getSuffix(StatActualValue actual, StatPossibleValues possible, String name);
}
