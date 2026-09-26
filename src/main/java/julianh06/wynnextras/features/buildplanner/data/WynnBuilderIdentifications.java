package julianh06.wynnextras.features.buildplanner.data;

import java.util.Map;

final class WynnBuilderIdentifications {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("hpBonus", "rawHealth"), Map.entry("mr", "manaRegen"),
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
        if (key.matches("(str|dex|int|def|agi|sprint|thorns|poison|mainAttackRange)")) {
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
}
