package julianh06.wynnextras.wynncraft.codec;

import java.util.List;

/** Ordered WynnBuilder data versions. Full-build links store an index into this list. */
public final class WynnBuilderVersions {
    private static final List<String> VERSIONS = List.of(
            "2.0.1.1", "2.0.1.2", "2.0.2.1", "2.0.2.3", "2.0.3.1", "2.0.4.1", "2.0.4.3", "2.0.4.4",
            "2.1.0.0", "2.1.0.1", "2.1.1.0", "2.1.1.1", "2.1.1.2", "2.1.1.3", "2.1.1.4", "2.1.1.5",
            "2.1.1.6", "2.1.1.7", "2.1.2.0", "2.1.3.0", "2.1.3.4", "2.1.4.0", "2.1.5.0", "2.1.6.0",
            "2.2.0.0", "2.2.0.7", "2.2.0.12", "2.2.0.14", "2.2.0.19", "2.2.0.21", "2.2.0.31",
            "2.2.1.0", "2.2.2.0", "2.2.3.0", "2.2.4.0");

    private WynnBuilderVersions() {}

    public static String byIndex(int index) {
        return index >= 0 && index < VERSIONS.size() ? VERSIONS.get(index) : null;
    }

    public static String latest() {
        return VERSIONS.getLast();
    }

    public static int latestIndex() {
        return VERSIONS.size() - 1;
    }
}
