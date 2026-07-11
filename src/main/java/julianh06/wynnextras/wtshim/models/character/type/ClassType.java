// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ClassType enum (Wynncraft classes).
 */
package julianh06.wynnextras.wtshim.models.character.type;

public enum ClassType {
    NONE,
    ARCHER,
    ASSASSIN,
    MAGE,
    WARRIOR,
    SHAMAN;

    public boolean isReskinned() { return false; }

    public String getName() { return name(); }

    public String getFullName() { return getActualName(false); }

    public String getActualName(boolean reskinned) {
        return switch (this) {
            case ARCHER -> reskinned ? "Hunter" : "Archer";
            case ASSASSIN -> reskinned ? "Ninja" : "Assassin";
            case MAGE -> reskinned ? "Dark Wizard" : "Mage";
            case WARRIOR -> reskinned ? "Knight" : "Warrior";
            case SHAMAN -> reskinned ? "Skyseer" : "Shaman";
            case NONE -> "";
        };
    }
}
