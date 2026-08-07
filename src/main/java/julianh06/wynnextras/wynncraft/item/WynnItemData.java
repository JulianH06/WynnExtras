package julianh06.wynnextras.wynncraft.item;

import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.wynncraft.state.SkillPoint;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class WynnItemData {
    public record Amount(int current, int maximum) {
        public int percentage() {
            if (maximum <= 0) return 0;
            return Math.clamp(Math.round(current * 100f / maximum), 0, 100);
        }

        public float fraction() {
            return maximum <= 0 ? 0 : Math.clamp(current / (float) maximum, 0, 1);
        }
    }

    private final String name;
    private final ItemCategory category;
    private final GearType gearType;
    private final ItemTier tier;
    private final EnumMap<SkillPoint, Integer> requirements;
    private final EnumMap<SkillPoint, Integer> bonuses;
    private final Amount durability;
    private final Integer consumableUses;
    private final Amount emeraldPouch;
    private final boolean crafted;
    private final boolean unidentified;
    private final Integer level;
    private final CharacterClass requiredClass;
    private final String profession;
    private final Map<String, Integer> identifications;
    private final List<String> lore;

    WynnItemData(String name, ItemCategory category, GearType gearType, ItemTier tier,
                 Map<SkillPoint, Integer> requirements, Map<SkillPoint, Integer> bonuses,
                 Amount durability, Integer consumableUses, Amount emeraldPouch,
                 boolean crafted, boolean unidentified, Integer level, CharacterClass requiredClass,
                 String profession, Map<String, Integer> identifications, List<String> lore) {
        this.name = name;
        this.category = category;
        this.gearType = gearType;
        this.tier = tier;
        this.requirements = new EnumMap<>(requirements);
        this.bonuses = new EnumMap<>(bonuses);
        this.durability = durability;
        this.consumableUses = consumableUses;
        this.emeraldPouch = emeraldPouch;
        this.crafted = crafted;
        this.unidentified = unidentified;
        this.level = level;
        this.requiredClass = requiredClass;
        this.profession = profession;
        this.identifications = Map.copyOf(identifications);
        this.lore = List.copyOf(lore);
    }

    public String name() { return name; }
    public ItemCategory category() { return category; }
    public GearType gearType() { return gearType; }
    public ItemTier tier() { return tier; }
    public int requirement(SkillPoint skill) { return requirements.getOrDefault(skill, 0); }
    public int bonus(SkillPoint skill) { return bonuses.getOrDefault(skill, 0); }
    public int[] requirementsArray() { return skillArray(requirements); }
    public int[] bonusesArray() { return skillArray(bonuses); }
    public Optional<Amount> durability() { return Optional.ofNullable(durability); }
    public OptionalInt consumableUses() { return consumableUses == null ? OptionalInt.empty() : OptionalInt.of(consumableUses); }
    public Optional<Amount> emeraldPouch() { return Optional.ofNullable(emeraldPouch); }
    public boolean crafted() { return crafted; }
    public boolean unidentified() { return unidentified; }
    public OptionalInt level() { return level == null ? OptionalInt.empty() : OptionalInt.of(level); }
    public CharacterClass requiredClass() { return requiredClass; }
    public Optional<String> profession() { return Optional.ofNullable(profession); }
    public Map<String, Integer> identifications() { return identifications; }
    public List<String> lore() { return lore; }

    private static int[] skillArray(Map<SkillPoint, Integer> values) {
        int[] result = new int[SkillPoint.values().length];
        for (SkillPoint skill : SkillPoint.values()) result[skill.ordinal()] = values.getOrDefault(skill, 0);
        return result;
    }
}
