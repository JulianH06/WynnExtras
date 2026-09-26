package julianh06.wynnextras.features.buildplanner.data;

import java.util.Set;

public final class IdentificationUnits {
    private static final Set<String> SKILL_POINTS = Set.of("str", "dex", "int", "def", "agi");
    private static final Set<String> PER_FIVE_SECONDS = Set.of("manaRegen");
    private static final Set<String> PER_THREE_SECONDS = Set.of("manaSteal", "lifeSteal", "poison");
    private static final Set<String> PER_FOUR_SECONDS = Set.of("healthRegenRaw");
    private static final Set<String> BLOCKS = Set.of("mainAttackRange", "jumpHeight");
    private static final Set<String> RAW_VALUES = Set.of(
            "rawMaxMana", "rawAttackSpeed",
            "raw1stSpellCost", "raw2ndSpellCost", "raw3rdSpellCost", "raw4thSpellCost");

    private IdentificationUnits() {
    }

    public static Unit unit(String key) {
        if (key == null || key.isBlank()) {
            return Unit.RAW;
        }
        if (PER_FIVE_SECONDS.contains(key)) return Unit.PER_FIVE_SECONDS;
        if (PER_THREE_SECONDS.contains(key)) return Unit.PER_THREE_SECONDS;
        if (PER_FOUR_SECONDS.contains(key)) return Unit.PER_FOUR_SECONDS;
        if (BLOCKS.contains(key)) return Unit.BLOCKS;
        if (SKILL_POINTS.contains(key) || RAW_VALUES.contains(key) || key.startsWith("raw")) {
            return Unit.RAW;
        }
        return Unit.PERCENT;
    }

    public static String format(String key, int value) {
        String number = value > 0 ? "+" + value : Integer.toString(value);
        return switch (unit(key)) {
            case PERCENT -> number + "%";
            case PER_FIVE_SECONDS -> number + "/5s";
            case PER_FOUR_SECONDS -> number + "/4s";
            case PER_THREE_SECONDS -> number + "/3s";
            case BLOCKS -> number + " blocks";
            case RAW -> number;
        };
    }

    public enum Unit {
        PERCENT,
        PER_FIVE_SECONDS,
        PER_FOUR_SECONDS,
        PER_THREE_SECONDS,
        BLOCKS,
        RAW
    }
}
