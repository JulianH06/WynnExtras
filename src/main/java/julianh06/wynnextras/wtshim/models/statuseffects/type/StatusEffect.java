// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — StatusEffect record (returns StyledText for names to match Wynntils API). */
package julianh06.wynnextras.wtshim.models.statuseffects.type;

import julianh06.wynnextras.wtshim.core.text.StyledText;

public record StatusEffect(StyledText name, StyledText displayName, int duration) {
    public StyledText getName() { return name; }
    public StyledText getDisplayName() { return displayName; }
    public int getDuration() { return duration; }
    public StyledText asString() { return name; }
}
