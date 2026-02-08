package julianh06.wynnextras.features.crafting.data;


import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.profession.type.ProfessionType;

public enum CraftableType {
    HELMET(ProfessionType.ARMOURING),
    CHESTPLATE(ProfessionType.ARMOURING),
    LEGGINGS(ProfessionType.TAILORING, "Pants"),
    BOOTS(ProfessionType.TAILORING),

    SPEAR(ProfessionType.WEAPONSMITHING, ClassType.WARRIOR),
    DAGGER(ProfessionType.WEAPONSMITHING, ClassType.ASSASSIN),
    BOW(ProfessionType.WOODWORKING, ClassType.ARCHER),
    WAND(ProfessionType.WOODWORKING, ClassType.MAGE),
    RELIK(ProfessionType.WOODWORKING, ClassType.SHAMAN),

    RING(ProfessionType.JEWELING),
    BRACELET(ProfessionType.JEWELING),
    NECKLACE(ProfessionType.JEWELING),

    POTION(ProfessionType.ALCHEMISM),
    SCROLL(ProfessionType.SCRIBING),
    FOOD(ProfessionType.COOKING);

    private final ProfessionType station;
    private String craftingName = this.name();
    private ClassType classType = ClassType.NONE;

    CraftableType(ProfessionType station) {
        this.station = station;
    }

    CraftableType(ProfessionType station, String craftingName) {
        this.station = station;
        this.craftingName = craftingName;
    }

    CraftableType(ProfessionType station, ClassType classType) {
        this.station = station;
        this.classType = classType;
    }

    public static CraftableType fromGearType(GearType type) {
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

    public ProfessionType getStation() {
        return this.station;
    }

    public String getCraftingName() {
        return this.craftingName;
    }
}

