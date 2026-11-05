package julianh06.wynnextras.features.buildplanner.data;

import java.util.Map;
import java.util.Set;

final class WynnBuilderIdentifications {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("hpBonus", "rawHealth"), Map.entry("hpbonus", "rawHealth"),
            Map.entry("mr", "manaRegen"),
            Map.entry("ms", "manaSteal"), Map.entry("ls", "lifeSteal"),
            Map.entry("hprRaw", "healthRegenRaw"), Map.entry("hprPct", "healthRegen"),
            Map.entry("spd", "walkSpeed"), Map.entry("damPct", "damage"),
            Map.entry("damRaw", "rawDamage"), Map.entry("mdRaw", "rawMainAttackDamage"),
            Map.entry("sdRaw", "rawSpellDamage"), Map.entry("mdPct", "mainAttackDamage"),
            Map.entry("sdPct", "spellDamage"), Map.entry("ref", "reflection"),
            Map.entry("healPct", "healingEfficiency"), Map.entry("eSteal", "stealing"),
            Map.entry("lb", "lootBonus"), Map.entry("lq", "lootQuality"),
            Map.entry("sprintReg", "sprintRegen"), Map.entry("xpb", "combatExperience"),
            Map.entry("gXp", "gatherXpBonus"), Map.entry("gSpd", "gatherSpeed"),
            Map.entry("expd", "exploding"), Map.entry("jh", "jumpHeight"),
            Map.entry("kb", "knockback"), Map.entry("atkTier", "rawAttackSpeed"),
            Map.entry("maxMana", "rawMaxMana"), Map.entry("critDamPct", "criticalDamageBonus"));
    private static final Set<String> BOOSTED = Set.of(
            "fDef", "wDef", "aDef", "tDef", "eDef", "hprPct", "mr", "sdPct", "mdPct", "ls", "ms",
            "ref", "thorns", "expd", "spd", "atkTier", "poison", "hpBonus", "hpbonus", "spRegen",
            "eSteal", "hprRaw", "sdRaw", "mdRaw", "fDamPct", "wDamPct", "aDamPct", "tDamPct",
            "eDamPct", "fDefPct", "wDefPct", "aDefPct", "tDefPct", "eDefPct", "spPct1", "spRaw1",
            "spPct2", "spRaw2", "spPct3", "spRaw3", "spPct4", "spRaw4", "rSdRaw", "sprint",
            "sprintReg", "jh", "fSdPct", "fSdRaw", "fMdPct", "fMdRaw", "wSdPct", "wSdRaw",
            "wMdPct", "wMdRaw", "aSdPct", "aSdRaw", "aMdPct", "aMdRaw", "tSdPct", "tSdRaw",
            "tMdPct", "tMdRaw", "eSdPct", "eSdRaw", "eMdPct", "eMdRaw", "nMdPct", "nMdRaw",
            "nSdPct", "nSdRaw", "nDamPct", "nDamRaw", "damPct", "damRaw", "rMdPct", "rMdRaw",
            "rSdPct", "rDamPct", "rDamRaw", "critDamPct", "healPct", "kb", "weakenEnemy",
            "slowEnemy", "rDefPct", "str", "dex", "int", "def", "agi");
    private static final Set<String> REVERSED = Set.of(
            "spPct1", "spRaw1", "spPct2", "spRaw2", "spPct3", "spRaw3", "spPct4", "spRaw4");
    private static final Map<Character, String> ELEMENTS = Map.of(
            'n', "Neutral", 'e', "Earth", 't', "Thunder", 'w', "Water",
            'f', "Fire", 'a', "Air", 'r', "Elemental");

    private WynnBuilderIdentifications() {
    }

    static String name(String key) {
        String alias = ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        if (key.matches("(str|dex|int|def|agi|sprint|thorns|poison|mainAttackRange|slowEnemy|weakenEnemy)")) {
            return key;
        }
        if (key.matches("sp(Pct|Raw)[1-4]")) {
            String ordinal = switch (key.charAt(key.length() - 1)) {
                case '1' -> "1st";
                case '2' -> "2nd";
                case '3' -> "3rd";
                default -> "4th";
            };
            return (key.startsWith("spRaw") ? "raw" : "") + ordinal + "SpellCost";
        }
        if (!key.isEmpty() && ELEMENTS.containsKey(key.charAt(0))) {
            String element = ELEMENTS.get(key.charAt(0));
            String suffix = key.substring(1);
            String stat = switch (suffix) {
                case "DamPct", "DamRaw" -> "Damage";
                case "MdPct", "MdRaw" -> "MainAttackDamage";
                case "SdPct", "SdRaw" -> "SpellDamage";
                case "DefPct" -> "Defence";
                default -> null;
            };
            if (stat != null) {
                return suffix.endsWith("Raw") ? "raw" + element + stat
                        : Character.toLowerCase(element.charAt(0)) + element.substring(1) + stat;
            }
        }
        throw new IllegalArgumentException("Unsupported WynnBuilder identification: " + key);
    }

    static String key(String name) {
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (entry.getValue().equals(name)) return entry.getKey();
        }
        if (name.matches("(str|dex|int|def|agi|sprint|thorns|poison|mainAttackRange|slowEnemy|weakenEnemy)")) {
            return name;
        }
        for (int spell = 1; spell <= 4; spell++) {
            for (String prefix : new String[]{"spPct", "spRaw"}) {
                String candidate = prefix + spell;
                if (name(candidate).equals(name)) return candidate;
            }
        }
        for (char element : ELEMENTS.keySet()) {
            for (String suffix : new String[]{"DamPct", "DamRaw", "MdPct", "MdRaw", "SdPct", "SdRaw", "DefPct"}) {
                String candidate = element + suffix;
                if (name(candidate).equals(name)) return candidate;
            }
        }
        return name;
    }

    static int boostedValue(String name, int value, double multiplier) {
        return (int) boostedValue(name, (double) value, multiplier);
    }

    static double boostedValue(String name, double value, double multiplier) {
        String key = key(name);
        if (multiplier == 1.0 || !BOOSTED.contains(key)) return value;
        if (REVERSED.contains(key) ? value >= 0 : value <= 0) return value;
        return Math.floor(value * multiplier + 1.0E-9);
    }
}
