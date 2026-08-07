package julianh06.wynnextras.wynncraft.item;

public enum GearType {
    UNKNOWN,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    SPEAR,
    DAGGER,
    BOW,
    WAND,
    RELIK,
    RING,
    BRACELET,
    NECKLACE;

    public boolean isWeapon() {
        return this == SPEAR || this == DAGGER || this == BOW || this == WAND || this == RELIK;
    }

    public boolean isArmor() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    public boolean isAccessory() {
        return this == RING || this == BRACELET || this == NECKLACE;
    }
}
