package julianh06.wynnextras.features.mount;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.*;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum MaterialType {
    INGOT(Ingot.class, MountStat.ENERGY, MountStat.TOUGHNESS),
    GEM(Gem.class, MountStat.SPEED, MountStat.ENERGY, MountStat.TRAINING),
    PLANK(Plank.class, MountStat.SPEED, MountStat.ACCELERATION, MountStat.TOUGHNESS),
    PAPER(Paper.class, MountStat.ALTITUDE, MountStat.BOOST),
    STRING(StringMaterial.class, MountStat.ACCELERATION, MountStat.HANDLING, MountStat.BOOST),
    GRAINS(Grains.class, MountStat.SPEED, MountStat.ALTITUDE),
    OIL(Oil.class, MountStat.ALTITUDE, MountStat.HANDLING, MountStat.TRAINING),
    MEAT(Meat.class, MountStat.ACCELERATION, MountStat.ENERGY);

    private final Set<MountStat> stats = new HashSet<>();
    private final Class<? extends Enum<? extends IMaterial>> data;

    MaterialType(Class<? extends Enum<? extends IMaterial>> data, MountStat... stats) {
        this.stats.addAll(Arrays.stream(stats).collect(Collectors.toSet()));
        this.data = data;
    }

    public Set<MountStat> getStats() {
        return stats;
    }

    public Identifier getTexture(int level) {
        int ordinal = switch (level) {
            case 10 -> 1;
            case 20 -> 2;
            case 30 -> 3;
            case 40 -> 4;
            case 50 -> 5;
            case 60 -> 6;
            case 70 -> 7;
            case 80 -> 8;
            case 90 -> 9;
            case 100 -> 10;
            case 105 -> 11;
            case 110 -> 12;
            case 115 -> 13;
            default -> 0;
        };

        IMaterial mat = (IMaterial) data.getEnumConstants()[ordinal];
        return mat.getTexture();
    }

    public String getName(int level) {
        int ordinal = switch (level) {
            case 10 -> 1;
            case 20 -> 2;
            case 30 -> 3;
            case 40 -> 4;
            case 50 -> 5;
            case 60 -> 6;
            case 70 -> 7;
            case 80 -> 8;
            case 90 -> 9;
            case 100 -> 10;
            case 105 -> 11;
            case 110 -> 12;
            case 115 -> 13;
            default -> 0;
        };

        IMaterial mat = (IMaterial) data.getEnumConstants()[ordinal];
        return mat.getName();
    }
}
