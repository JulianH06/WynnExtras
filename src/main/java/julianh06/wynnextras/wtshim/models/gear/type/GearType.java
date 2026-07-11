// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — GearType enum. */
package julianh06.wynnextras.wtshim.models.gear.type;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;

public enum GearType {
    SPEAR, WAND, DAGGER, BOW, RELIK,
    HELMET, CHESTPLATE, LEGGINGS, BOOTS,
    RING, BRACELET, NECKLACE, MASTERY_TOME,
    WEAPON_TOME, ARMOR_TOME, GUILD_TOME,
    CHARM, UNKNOWN;

    /** Maps a Wynncraft gear.json "subType" string (e.g. "spear", "chestplate") to a GearType. */
    public static GearType fromString(String subType) {
        if (subType == null) return UNKNOWN;
        try {
            return valueOf(subType.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /** The class a weapon requires (weapons derive their class requirement from the weapon type). */
    public ClassType getClassReq() {
        return switch (this) {
            case SPEAR -> ClassType.WARRIOR;
            case WAND -> ClassType.MAGE;
            case DAGGER -> ClassType.ASSASSIN;
            case BOW -> ClassType.ARCHER;
            case RELIK -> ClassType.SHAMAN;
            default -> ClassType.NONE;
        };
    }

    public boolean isWeapon() {
        return this == SPEAR || this == WAND || this == DAGGER || this == BOW || this == RELIK;
    }

    public boolean isArmor() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    public boolean isAccessory() {
        return this == RING || this == BRACELET || this == NECKLACE;
    }

    public boolean isValidWeapon(julianh06.wynnextras.wtshim.models.character.type.ClassType classType) {
        return isWeapon();
    }
}
