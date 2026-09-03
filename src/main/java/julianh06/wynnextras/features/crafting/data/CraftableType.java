package julianh06.wynnextras.features.crafting.data;


import julianh06.wynnextras.features.crafting.model.ClassType;
import julianh06.wynnextras.utils.enums.WEProfessionType;

public enum CraftableType {
    HELMET(WEProfessionType.ARMOURING),
    CHESTPLATE(WEProfessionType.ARMOURING),
    LEGGINGS(WEProfessionType.TAILORING, "Pants"),
    BOOTS(WEProfessionType.TAILORING),

    SPEAR(WEProfessionType.WEAPONSMITHING, ClassType.WARRIOR),
    DAGGER(WEProfessionType.WEAPONSMITHING, ClassType.ASSASSIN),
    BOW(WEProfessionType.WOODWORKING, ClassType.ARCHER),
    WAND(WEProfessionType.WOODWORKING, ClassType.MAGE),
    RELIK(WEProfessionType.WOODWORKING, ClassType.SHAMAN),

    RING(WEProfessionType.JEWELING),
    BRACELET(WEProfessionType.JEWELING),
    NECKLACE(WEProfessionType.JEWELING),

    POTION(WEProfessionType.ALCHEMISM),
    SCROLL(WEProfessionType.SCRIBING),
    FOOD(WEProfessionType.COOKING);

    private final WEProfessionType station;
    private String craftingName = this.name();
    private ClassType classType = ClassType.NONE;

    CraftableType(WEProfessionType station) {
        this.station = station;
    }

    CraftableType(WEProfessionType station, String craftingName) {
        this.station = station;
        this.craftingName = craftingName;
    }

    CraftableType(WEProfessionType station, ClassType classType) {
        this.station = station;
        this.classType = classType;
    }

    public static CraftableType fromGearType(Enum<?> type) {
        try {
            return valueOf(type.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    public boolean canHaveHealth() {
        return isArmour() || isConsumable();
    }

    public ClassType getClassType() {
        return classType;
    }

    public String getDisplayName() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }

    public WEProfessionType getStation() {
        return this.station;
    }

    public String getCraftingName() {
        return this.craftingName;
    }
}

