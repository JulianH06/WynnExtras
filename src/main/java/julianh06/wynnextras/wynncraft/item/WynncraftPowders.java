package julianh06.wynnextras.wynncraft.item;

import java.util.List;

public final class WynncraftPowders {
    public static final int ELEMENT_COUNT = 5;
    public static final int TIER_COUNT = 7;
    private static final List<String> ELEMENT_NAMES = List.of("Earth", "Thunder", "Water", "Fire", "Air");
    private static final List<String> TIER_NAMES = List.of("I", "II", "III", "IV", "V", "VI", "VII");
    private static final double[] DURABILITY = {-35, -52.5, -70, -91, -112, -133, -154};
    private static final int[] REQUIREMENTS = {0, 0, 10, 20, 28, 36, 44};
    private static final int[] HEALTH = {5, 10, 20, 30, 45, 60, 75};
    private static final int[][] STATS = {
            {4,5,17,2,1}, {6,7,21,5,2}, {7,9,25,9,3}, {8,9,31,14,4}, {9,11,38,22,7}, {11,12,46,29,7}, {12,14,52,37,12},
            {1,8,9,2,1}, {1,12,11,4,1}, {2,14,13,8,2}, {2,15,17,13,3}, {3,17,22,20,5}, {4,19,28,28,6}, {5,21,32,36,11},
            {3,4,13,3,1}, {5,6,15,6,1}, {6,8,17,11,3}, {7,8,21,16,4}, {8,10,26,23,6}, {10,13,32,32,10}, {11,15,38,40,15},
            {2,5,14,3,1}, {4,7,16,6,1}, {5,9,19,10,2}, {6,9,24,15,3}, {7,11,30,22,5}, {9,14,37,31,9}, {10,16,44,39,14},
            {2,6,11,3,1}, {3,9,14,6,2}, {4,11,17,10,3}, {5,11,22,16,5}, {7,12,28,23,7}, {8,15,35,30,8}, {9,17,42,38,13}
    };

    private WynncraftPowders() {}

    public static Powder get(int element, int tier) {
        if (element < 0 || element >= ELEMENT_COUNT || tier < 1 || tier > TIER_COUNT) return null;
        int[] stats = STATS[element * TIER_COUNT + tier - 1];
        return new Powder(element, tier, stats[0], stats[1], stats[2], stats[3], stats[4],
                HEALTH[tier - 1], DURABILITY[tier - 1], REQUIREMENTS[tier - 1]);
    }

    public static Powder fromId(int id) {
        return id < 0 ? null : get(id / TIER_COUNT, id % TIER_COUNT + 1);
    }

    public static Powder fromName(String name) {
        if (name == null) return null;
        for (int element = 0; element < ELEMENT_COUNT; element++) {
            for (int tier = 1; tier <= TIER_COUNT; tier++) {
                Powder powder = get(element, tier);
                if (powder.name().equals(name)) return powder;
            }
        }
        return null;
    }

    public record Powder(int element, int tier, int damageMinimum, int damageMaximum, int conversion,
            int defencePlus, int defenceMinus, int health, double durability, int requirement) {
        public String name() {
            return ELEMENT_NAMES.get(element) + " Powder " + TIER_NAMES.get(tier - 1);
        }
    }
}
