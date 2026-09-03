package julianh06.wynnextras.features.crafting.model;

public enum ClassType {
    NONE("None", "None"), WARRIOR("Warrior", "Knight"), ASSASSIN("Assassin", "Ninja"),
    ARCHER("Archer", "Hunter"), MAGE("Mage", "Dark Wizard"), SHAMAN("Shaman", "Skyseer");

    private final String name;
    private final String reskin;

    ClassType(String name, String reskin) {
        this.name = name;
        this.reskin = reskin;
    }

    public String getActualName(boolean reskinned) {
        return reskinned ? reskin : name;
    }
}
