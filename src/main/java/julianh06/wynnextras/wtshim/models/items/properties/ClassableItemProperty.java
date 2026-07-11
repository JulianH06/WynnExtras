// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ClassableItemProperty: items that require a specific class. */
package julianh06.wynnextras.wtshim.models.items.properties;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;

public interface ClassableItemProperty {
    ClassType getRequiredClass();
}
