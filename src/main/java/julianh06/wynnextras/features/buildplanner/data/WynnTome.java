package julianh06.wynnextras.features.buildplanner.data;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record WynnTome(
        String displayName,
        String alias,
        String type,
        String tier,
        int level,
        Map<String, Integer> identifications
) {
    private static final Map<String, String> STAT_NAMES = Map.ofEntries(
            Map.entry("rawHealth", "Health"),
            Map.entry("earthDefence", "Earth Defense"),
            Map.entry("thunderDefence", "Thunder Defense"),
            Map.entry("waterDefence", "Water Defense"),
            Map.entry("fireDefence", "Fire Defense"),
            Map.entry("airDefence", "Air Defense"),
            Map.entry("1stSpellCost", "1st Spell Cost"),
            Map.entry("2ndSpellCost", "2nd Spell Cost"),
            Map.entry("3rdSpellCost", "3rd Spell Cost"),
            Map.entry("4thSpellCost", "4th Spell Cost"),
            Map.entry("manaRegen", "Mana Regen"),
            Map.entry("manaSteal", "Mana Steal"),
            Map.entry("lifeSteal", "Life Steal"),
            Map.entry("healthRegenRaw", "Health Regen"),
            Map.entry("healthRegen", "Health Regen"),
            Map.entry("walkSpeed", "Walk Speed"),
            Map.entry("sprint", "Sprint"),
            Map.entry("sprintRegen", "Sprint Regen"),
            Map.entry("damage", "Damage"),
            Map.entry("earthDamage", "Earth Damage"),
            Map.entry("thunderDamage", "Thunder Damage"),
            Map.entry("waterDamage", "Water Damage"),
            Map.entry("fireDamage", "Fire Damage"),
            Map.entry("airDamage", "Air Damage"),
            Map.entry("rawMainAttackDamage", "Raw Main Attack Damage"),
            Map.entry("rawSpellDamage", "Raw Spell Damage"),
            Map.entry("mainAttackDamage", "Main Attack Damage"),
            Map.entry("spellDamage", "Spell Damage"),
            Map.entry("thorns", "Thorns"),
            Map.entry("reflection", "Reflection"),
            Map.entry("healingEfficiency", "Healing Efficiency"),
            Map.entry("stealing", "Emerald Stealing"),
            Map.entry("lootBonus", "Loot Bonus"),
            Map.entry("str", "Strength"),
            Map.entry("dex", "Dexterity"),
            Map.entry("int", "Intelligence"),
            Map.entry("def", "Defense"),
            Map.entry("agi", "Agility"));

    public WynnTome {
        identifications = Map.copyOf(
                new LinkedHashMap<>(identifications == null ? Map.of() : identifications));
    }

    public String nameWithStats() {
        String stats = effectSummary();
        return stats.isEmpty() ? displayName : displayName + " (" + stats + ")";
    }

    public String effectSummary() {
        Set<String> statKeys = new LinkedHashSet<>(identifications.keySet());
        Set<String> skillKeys = Set.of("str", "dex", "int", "def", "agi");
        boolean rainbow = statKeys.containsAll(skillKeys);
        if (rainbow) {
            statKeys.removeAll(skillKeys);
        }
        String stats = statKeys.stream()
                .map(key -> STAT_NAMES.getOrDefault(key, key))
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        if (rainbow) {
            stats = stats.isEmpty() ? "Rainbow" : "Rainbow, " + stats;
        }
        return stats;
    }
}
