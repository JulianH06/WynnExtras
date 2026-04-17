package julianh06.wynnextras.features.mount;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum MaterialType {
    INGOT(MountStat.ENERGY, MountStat.TOUGHNESS),
    GEM(MountStat.SPEED, MountStat.ENERGY, MountStat.TRAINING),
    PLANK(MountStat.SPEED, MountStat.ACCELERATION, MountStat.TOUGHNESS),
    PAPER(MountStat.ALTITUDE, MountStat.BOOST),
    STRING(MountStat.ACCELERATION, MountStat.HANDLING),
    GRAINS(MountStat.SPEED, MountStat.ALTITUDE),
    OIL(MountStat.ALTITUDE, MountStat.HANDLING),
    MEAT(MountStat.ACCELERATION, MountStat.ENERGY);

    private final Set<MountStat> stats = new HashSet<>();

    MaterialType(MountStat... stats) {
        this.stats.addAll(Arrays.stream(stats).collect(Collectors.toSet()));
    }

    public Set<MountStat> getStats() {
        return stats;
    }
}
