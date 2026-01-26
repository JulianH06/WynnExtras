package julianh06.wynnextras.features.crafting.data;


import com.wynntils.models.profession.type.ProfessionType;

public enum CraftableType {
    HELMET(ProfessionType.ARMOURING),
    CHESTPLATE(ProfessionType.ARMOURING),
    LEGGINGS(ProfessionType.TAILORING, "Pants"),
    BOOTS(ProfessionType.TAILORING),

    SPEAR(ProfessionType.WEAPONSMITHING),
    DAGGER(ProfessionType.WEAPONSMITHING),
    BOW(ProfessionType.WOODWORKING),
    WAND(ProfessionType.WOODWORKING),
    RELIK(ProfessionType.WOODWORKING),

    RING(ProfessionType.JEWELING),
    BRACELET(ProfessionType.JEWELING),
    NECKLACE(ProfessionType.JEWELING),

    POTION(ProfessionType.ALCHEMISM),
    SCROLL(ProfessionType.SCRIBING),
    FOOD(ProfessionType.COOKING);

    private final ProfessionType station;
    private final String craftingName;

    CraftableType(ProfessionType station) {
        this.station = station;
        this.craftingName = this.name();
    }

    CraftableType(ProfessionType station, String craftingName) {
        this.station = station;
        this.craftingName = craftingName;
    }

    public static CraftableType fromCraftingName(String typeStr) {
        for (CraftableType type : values()) {
            if (type.craftingName.equalsIgnoreCase(typeStr)) {
                return type;
            }
        }
        return null;
    }

    public boolean isArmour() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    public boolean isAccessory() {
        return this == RING || this == BRACELET || this == NECKLACE;
    }

    public boolean isWeapon() {
        return this == SPEAR || this == DAGGER || this == BOW || this == WAND || this == RELIK;
    }

    public boolean isConsumable() {
        return this == POTION || this == SCROLL || this == FOOD;
    }

    public boolean isEquipable() {
        return !isConsumable() && !isWeapon();
    }

    public boolean hasDurability() {
        return !isConsumable();
    }

    public String getDisplayName() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }

    public ProfessionType getStation() {
        return this.station;
    }

    public String getCraftingName() {
        return this.craftingName;
    }
}

