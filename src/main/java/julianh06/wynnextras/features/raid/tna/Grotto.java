package julianh06.wynnextras.features.raid.tna;

import julianh06.wynnextras.utils.path.Node;

import java.util.Arrays;

public enum Grotto implements Node.Group {
    Entrance,
    Gray,
    Blue,
    White,
    Orange,
    Black,
    None,
    Outside;

    public static Grotto from(String s) {
        return Arrays.stream(Grotto.values()).filter(g -> g.name().equalsIgnoreCase(s)).findFirst().orElse(None);
    }
}
