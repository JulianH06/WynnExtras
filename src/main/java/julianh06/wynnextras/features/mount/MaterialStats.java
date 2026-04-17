package julianh06.wynnextras.features.mount;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MaterialStats {
    private record LevelMapping(int two_one, int two_two, int three_one, int three_two, int three_three) {
        public int fromInt(int value) {
            return switch (value) {
                case 1 -> three_one;
                case 2 -> three_two;
                case 3 -> three_three;
                case 4 -> two_one;
                case 5 -> two_two;
                default -> 0;
            };
        }
    }

    private static final Map<MaterialType, MaterialStats> baseMap = new HashMap<>();
    private static final TreeMap<Integer, LevelMapping> lvlMap = new TreeMap<>();
    static {
        baseMap.put(MaterialType.INGOT, new MaterialStats().energy(4).tough(5));
        baseMap.put(MaterialType.GEM, new MaterialStats().speed(2).energy(1).train(3));
        baseMap.put(MaterialType.PLANK, new MaterialStats().speed(1).acc(3).tough(2));
        baseMap.put(MaterialType.PAPER, new MaterialStats().altitude(5).boost(4));
        baseMap.put(MaterialType.STRING, new MaterialStats().acc(1).hand(2).boost(3));
        baseMap.put(MaterialType.GRAINS, new MaterialStats().speed(5).altitude(4));
        baseMap.put(MaterialType.OIL, new MaterialStats().altitude(1).hand(3).train(2));
        baseMap.put(MaterialType.MEAT, new MaterialStats().acc(4).energy(5));

        lvlMap.put(1, new LevelMapping(4, 8, 2, 4, 6));
        lvlMap.put(10, new LevelMapping(5, 10, 2, 5, 8));
        lvlMap.put(20, new LevelMapping(5, 12, 3, 6, 9));
        lvlMap.put(30, new LevelMapping(6, 14, 3, 6, 11));
        lvlMap.put(40, new LevelMapping(6, 16, 3, 7, 12));
        lvlMap.put(50, new LevelMapping(7, 18, 4, 8, 14));
        lvlMap.put(60, new LevelMapping(8, 20, 4, 9, 15));
        lvlMap.put(70, new LevelMapping(8, 22, 4, 10, 17));
        lvlMap.put(80, new LevelMapping(9, 24, 4, 10, 18));
        lvlMap.put(90, new LevelMapping(9, 26, 5, 11, 20));
        lvlMap.put(100, new LevelMapping(10, 28, 5, 12, 21));
        lvlMap.put(105, new LevelMapping(10, 29, 5, 12, 22));
        lvlMap.put(110, new LevelMapping(11, 30, 5, 13, 23));
        lvlMap.put(115, new LevelMapping(11, 31, 5, 13, 23));
    }

    public static MaterialStats get(MaterialType type, int lvl) {
        Integer roundedLevel = lvlMap.floorKey(lvl);
        if (roundedLevel == null) {
            roundedLevel = lvlMap.firstKey();
        }
        MaterialStats base = baseMap.get(type);
        return base.applyLevel(roundedLevel); // TOOD also return rounded lvl
    }

    private final Map<MountStat, Integer> stats = new HashMap<>();
    private final int level;

    private MaterialStats() {
        this.level = -1;
    }

    private MaterialStats(Map<MountStat, Integer> other, int level) {
        this.stats.putAll(other);
        this.level = level;
    }

    public Map<MountStat, Integer> getStats() {
        return stats;
    }

    public int getLevel() {
        return level;
    }

    private MaterialStats speed(int value) {
        stats.put(MountStat.SPEED, value);
        return this;
    }

    private MaterialStats acc(int value) {
        stats.put(MountStat.ACCELERATION, value);
        return this;
    }

    private MaterialStats altitude(int value) {
        stats.put(MountStat.ALTITUDE, value);
        return this;
    }

    private MaterialStats energy(int value) {
        stats.put(MountStat.ENERGY, value);
        return this;
    }

    private MaterialStats hand(int value) {
        stats.put(MountStat.HANDLING, value);
        return this;
    }

    private MaterialStats tough(int value) {
        stats.put(MountStat.TOUGHNESS, value);
        return this;
    }

    private MaterialStats boost(int value) {
        stats.put(MountStat.BOOST, value);
        return this;
    }

    private MaterialStats train(int value) {
        stats.put(MountStat.TRAINING, value);
        return this;
    }

    private MaterialStats applyLevel(int lvl) {
        LevelMapping lvlMapping = lvlMap.get(lvl);
        var result = stats.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> lvlMapping.fromInt(entry.getValue())
                ));
        return new MaterialStats(result, lvl);
    }
}
