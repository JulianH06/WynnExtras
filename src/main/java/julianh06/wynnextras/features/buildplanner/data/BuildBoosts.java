package julianh06.wynnextras.features.buildplanner.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuildBoosts {
    public static final List<AbilityBoost> ABILITIES = List.of(
            new AbilityBoost("radiance", "Radiance (+12%)", "Increases positive identification values by 12%."),
            new AbilityBoost("divinehonor", "Divine Honor (+5%)", "Increases positive identification values by 5%."),
            new AbilityBoost("shine", "Shine (+4%)", "Increases positive identification values by 4%."),
            new AbilityBoost("warscream", "War Scream", "20% defense multiplier."),
            new AbilityBoost("emboldeningcry", "Emboldening Cry (+8%)", "8% damage and 5% defense multiplier."),
            new AbilityBoost("chantfanatic", "Fanatic Chant", "30% defense multiplier."),
            new AbilityBoost("totem", "Vengeful Spirit (+15%)", "15% damage multiplier."),
            new AbilityBoost("chantlunatic", "Lunatic Chant (+20%)", "20% damage multiplier."),
            new AbilityBoost("fortitude", "Fortitude (+30%)", "30% damage multiplier."),
            new AbilityBoost("hauntinglunatic", "Lunatic Haunt", "15% enemy weaken multiplier."),
            new AbilityBoost("eldritchcall", "Eldritch Call", "20% enemy weaken multiplier."),
            new AbilityBoost("hauntingfanatic", "Fanatic Haunt (+12%)", "12% vulnerability damage multiplier."),
            new AbilityBoost("judgement", "Judgement", "40% identification scaling and 20% damage and defense."),
            new AbilityBoost("weathering", "Weathering (+20%)", "20% defense multiplier."),
            new AbilityBoost("acidrain", "Acid Rain (+8%)", "8% damage multiplier."));

    public static final List<PowderSpecial> POWDER_SPECIALS = List.of(
            new PowderSpecial("Quake", "Earth", "Rage"),
            new PowderSpecial("Chain Lightning", "Thunder", "Kill Streak"),
            new PowderSpecial("Curse", "Water", "Concentration"),
            new PowderSpecial("Courage", "Fire", "Endurance"),
            new PowderSpecial("Wind Prison", "Air", "Dodge"));

    public static final List<Raid> RAIDS = List.of(
            raid("notg", "NOTG",
                    names("Lightbearer", "Bioluminescent", "Berserk", "Pestilent", "Bedrock"), "Palisade"),
            raid("nol", "NOL",
                    names("Cherubim", "Seraphim", "Ophanim", "Throne", "Anti"), "Neophyte"),
            raid("tcc", "TCC",
                    names("Intrepid", "Stonewalker", "Giant", "Elder", "Boulderbreaker"), "Cirrus"),
            raid("tna", "TNA",
                    names("Hollowed", "Sojourner", "Fading", "Insidious", "Hopeless"), "Manic"),
            new Raid("wtp", "WTP", List.of(
                    List.of("Relentless I", "Ingenious I", "Unrestrained I", "Opulent I", "Omniscient I"),
                    List.of("Relentless II", "Ingenious II", "Unrestrained II", "Opulent II", "Omniscient II"),
                    List.of("Restless", "Apathetic", "Faceless", "Prideful", "Isolated"))));

    private static final Map<String, Map<String, Integer>> RAID_STATS = raidStats();

    private BuildBoosts() {
    }

    public static Map<String, Integer> externalIdentifications(Set<String> selectedRaidBuffs) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String selected : selectedRaidBuffs) {
            for (Map.Entry<String, Integer> entry : RAID_STATS.getOrDefault(selected, Map.of()).entrySet()) {
                String name;
                try {
                    name = WynnBuilderIdentifications.name(entry.getKey());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                result.merge(name, entry.getValue(), Integer::sum);
            }
        }
        return Map.copyOf(result);
    }

    public static Map<String, Double> damageModifiers(
            Set<String> abilities, Map<String, Integer> powderSpecials
    ) {
        Map<String, Double> result = new LinkedHashMap<>();
        double potion = 0;
        if (abilities.contains("totem")) potion = Math.max(potion, 15);
        if (abilities.contains("chantlunatic")) potion = Math.max(potion, 20);
        if (abilities.contains("fortitude")) potion = Math.max(potion, 30);
        if (potion > 0) result.put("damMult.Potion", potion);
        if (abilities.contains("emboldeningcry")) result.put("damMult.Strength", 8.0);
        if (abilities.contains("hauntingfanatic")) result.put("damMult.Vulnerability", 12.0);
        if (abilities.contains("judgement")) result.put("damMult.Judgement", 20.0);
        if (abilities.contains("acidrain")) result.put("damMult.AcidRain", 8.0);
        addPowderMultiplier(result, powderSpecials, "Curse", 10, 2.5);
        addPowderMultiplier(result, powderSpecials, "Courage", 10, 2.5);
        addPowderMultiplier(result, powderSpecials, "Wind Prison", 100, 25);
        return Map.copyOf(result);
    }

    public static double globalDamageMultiplier(
            Set<String> abilities, Map<String, Integer> powderSpecials
    ) {
        double result = 1.0;
        for (double value : damageModifiers(abilities, powderSpecials).values()) {
            result *= 1 + value / 100.0;
        }
        return result;
    }

    public static double defenseDamageTakenMultiplier(Set<String> abilities) {
        double potion = 0;
        if (abilities.contains("warscream")) potion += 20;
        if (abilities.contains("emboldeningcry")) potion += 5;
        if (abilities.contains("chantfanatic")) potion = 30;
        double result = 1 - potion / 100.0;
        if (abilities.contains("hauntinglunatic")) result *= 0.85;
        if (abilities.contains("eldritchcall")) result *= 0.80;
        if (abilities.contains("judgement")) result *= 0.80;
        if (abilities.contains("weathering")) result *= 0.80;
        return result;
    }

    public static double identificationMultiplier(Set<String> abilities) {
        if (abilities.contains("judgement")) return 1.40;
        double result = 1.0;
        if (abilities.contains("radiance")) result += 0.12;
        if (abilities.contains("divinehonor")) result += 0.05;
        if (abilities.contains("shine")) result += 0.04;
        return result;
    }

    public static List<RaidEffect> raidEffects(String buff) {
        Map<String, Integer> stats = RAID_STATS.getOrDefault(buff, Map.of());
        if (stats.isEmpty()) return List.of();
        return stats.entrySet().stream().map(entry -> {
            String key;
            try {
                key = WynnBuilderIdentifications.name(entry.getKey());
            } catch (IllegalArgumentException ignored) {
                key = entry.getKey();
            }
            return new RaidEffect(
                    IdentificationUnits.format(key, entry.getValue()), readable(key), entry.getValue());
        }).toList();
    }

    private static String readable(String key) {
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static void addPowderMultiplier(
            Map<String, Double> result, Map<String, Integer> specials,
            String name, double first, double step
    ) {
        int level = specials.getOrDefault(name, 0);
        if (level > 0) result.put("damMult." + name, first + (level - 1) * step);
    }

    private static Raid raid(String id, String label, List<String> bases, String capstone) {
        return new Raid(id, label, List.of(
                bases.stream().map(name -> name + " I").toList(),
                bases.stream().map(name -> name + " II").toList(),
                java.util.stream.Stream.concat(
                        bases.stream().map(name -> name + " III"), java.util.stream.Stream.of(capstone)).toList()));
    }

    private static List<String> names(String... values) {
        return List.of(values);
    }

    private static Map<String, Map<String, Integer>> raidStats() {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        put(result, "Lightbearer I", "int", 25, "healPct", 20);
        put(result, "Lightbearer II", "mr", 15, "sdPct", 40);
        put(result, "Lightbearer III", "int", 40, "healPct", 35);
        put(result, "Bioluminescent I", "rDamPct", 30);
        put(result, "Bioluminescent II", "mdPct", 50, "sdPct", 50);
        put(result, "Bioluminescent III", "str", 10, "dex", 10, "int", 10, "def", 10, "agi", 10, "rDamPct", 50);
        put(result, "Berserk I", "mdPct", 75);
        put(result, "Berserk II", "expd", 25, "sprintReg", 100, "mdRaw", 500);
        put(result, "Berserk III", "str", 30, "mdPct", 75, "mdRaw", 500);
        put(result, "Pestilent I", "str", 20, "poison", 3750, "ls", 250);
        put(result, "Pestilent II", "str", 25, "poison", 5500, "ms", 12);
        put(result, "Pestilent III", "ls", 400, "poison", 7500);
        put(result, "Bedrock I", "hpBonus", 1000, "ms", 10, "ls", 150);
        put(result, "Bedrock II", "hpBonus", 1250, "ls", 300, "hprRaw", 700);
        put(result, "Bedrock III", "ms", 15, "hprRaw", 900);
        put(result, "Palisade", "ls", 300, "spPct4", -50);
        put(result, "Cherubim I", "spd", 45, "mdPct", 90);
        put(result, "Cherubim II", "ls", 500, "rDamPct", 40);
        put(result, "Cherubim III", "str", 20, "dex", 20, "int", 20, "def", 20, "agi", 20);
        put(result, "Seraphim I", "agi", 20, "sdPct", 25, "ref", 30);
        put(result, "Seraphim II", "mr", 20, "sdPct", 30);
        put(result, "Seraphim III", "dex", 30);
        put(result, "Ophanim I", "def", 30, "healPct", 30);
        put(result, "Ophanim II", "agi", 30, "healPct", 25, "hprPct", 40);
        put(result, "Ophanim III", "hpBonus", 5000, "hprRaw", 600);
        put(result, "Throne I", "int", 35, "rDamPct", 25);
        put(result, "Throne II", "mr", 25, "ms", 15);
        put(result, "Throne III", "int", 50);
        put(result, "Anti I", "expd", 50, "mainAttackRange", 50);
        put(result, "Anti II", "str", 30, "poison", 6000);
        put(result, "Anti III", "def", 50, "sprint", 300);
        put(result, "Neophyte", "mainAttackRange", 300, "ms", 50, "atkTier", -50);
        put(result, "Intrepid I", "spd", 40, "eSteal", 30);
        put(result, "Intrepid II", "hprRaw", 250, "hprPct", 20);
        put(result, "Intrepid III", "def", 25, "healPct", 25, "weakenEnemy", 5);
        put(result, "Stonewalker I", "str", 20, "mdRaw", 700, "thorns", 100);
        put(result, "Stonewalker II", "str", 30, "mdPct", 100, "expd", 50);
        put(result, "Stonewalker III", "mdRaw", 1500, "mdPct", 100);
        put(result, "Giant I", "str", 10, "def", 40);
        put(result, "Giant II", "hpBonus", 3000, "hprRaw", 400);
        put(result, "Giant III", "hpBonus", 4000, "hprRaw", 500, "hprPct", 20);
        put(result, "Elder I", "agi", 30, "sdPct", 25, "ref", 100);
        put(result, "Elder II", "int", 40, "mr", 12, "rDamPct", 30);
        put(result, "Elder III", "mr", 18, "sdPct", 30, "rDamPct", 30);
        put(result, "Boulderbreaker I", "dex", 20, "ls", 300);
        put(result, "Boulderbreaker II", "agi", 25, "sdPct", 30, "spd", 35);
        put(result, "Boulderbreaker III", "str", 30, "dex", 30, "mdPct", 80);
        put(result, "Cirrus", "agi", 30, "jh", 15, "healPct", 30);
        put(result, "Hollowed I", "def", 20, "hpBonus", 2000, "ref", 50);
        put(result, "Hollowed II", "mr", 30, "hprRaw", 400, "thorns", 50);
        put(result, "Hollowed III", "hpBonus", 5000, "hprRaw", 500, "damPct", -30);
        put(result, "Sojourner I", "agi", 20, "mr", 20, "sprintReg", 80);
        put(result, "Sojourner II", "def", 40, "sprint", -100);
        put(result, "Sojourner III", "str", 30, "dex", 30, "hprRaw", -250);
        put(result, "Fading I", "hprPct", 25, "spd", 30);
        put(result, "Fading II", "agi", 25, "ms", 15, "healPct", 25);
        put(result, "Fading III", "damRaw", 400, "damPct", 40, "str", -10, "dex", -10, "int", -10, "def", -10, "agi", -10);
        put(result, "Insidious I", "int", 30, "ms", 12, "sdPct", 25);
        put(result, "Insidious II", "ls", 325, "sdPct", 40, "maxMana", 50);
        put(result, "Insidious III", "sdPct", 60, "spd", -40);
        put(result, "Hopeless I", "str", 20, "mainAttackRange", 30, "mdPct", 75);
        put(result, "Hopeless II", "dex", 25, "expd", 50);
        put(result, "Hopeless III", "mdPct", 135, "spd", 60, "mr", -15);
        put(result, "Manic", "mr", 20, "rDefPct", -100);
        put(result, "Relentless I", "spd", 30, "mdRaw", 600, "expd", 35);
        put(result, "Relentless II", "weakenEnemy", 8, "spPct2", -75);
        put(result, "Ingenious I", "hpBonus", 3000, "spPct4", -40, "healPct", 25);
        put(result, "Ingenious II", "def", 30, "hprRaw", 1000);
        put(result, "Unrestrained I", "spRaw1", -5, "spRaw3", -5, "int", 25);
        put(result, "Unrestrained II", "damRaw", 250, "mdPct", 250, "poison", 7500);
        put(result, "Opulent I", "mr", 25, "maxMana", 40, "rDamPct", 25);
        put(result, "Opulent II", "rDamRaw", 200, "rDamPct", 30);
        put(result, "Omniscient I", "ls", 650, "maxMana", 40, "dex", 20);
        put(result, "Omniscient II", "ms", 20, "mainAttackRange", 75, "str", 20);
        put(result, "Restless", "sdPct", -35, "maxMana", -25);
        put(result, "Apathetic", "spd", -45, "mdPct", -40);
        put(result, "Faceless", "str", -10, "dex", -10, "def", -10, "agi", -10);
        put(result, "Prideful", "hpBonus", -3000, "rDefPct", -50);
        put(result, "Isolated", "hprRaw", -375, "mr", -10);
        return Map.copyOf(result);
    }

    private static void put(Map<String, Map<String, Integer>> target, String name, Object... values) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            stats.put((String) values[i], (Integer) values[i + 1]);
        }
        target.put(name, Map.copyOf(stats));
    }

    public record AbilityBoost(String id, String label, String effect) {}
    public record PowderSpecial(String name, String element, String passive) {}
    public record Raid(String id, String label, List<List<String>> tiers) {}
    public record RaidEffect(String value, String label, int rawValue) {}
}
