package julianh06.wynnextras.features.mount;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum MountStat {
    SPEED("Speed"),
    ACCELERATION("Acceleration"),
    ALTITUDE("Altitude", "Jump Height"),
    ENERGY("Energy"),
    HANDLING("Handling"),
    TOUGHNESS("Toughness"),
    BOOST("Boost"),
    TRAINING("Training");

    private final Set<String> display = new HashSet<>();

    MountStat(String... names) {
        display.addAll(Arrays.stream(names).collect(Collectors.toSet()));
    }

    public static final Pattern PATTERN = Pattern.compile(
            "(?<stat>Speed|Acceleration|Altitude|Jump Height|Energy|Handling|Toughness|Boost|Training)[^\\d/()]+" +
                    "(?<current>\\d+)/(?<limit>\\d+) \\(\\s*(?<max>\\d+)\\)\\D+"
    );

    public static MountStat fromString(String name) {
        for (MountStat mountStat : MountStat.values()) {
            if (mountStat.display.contains(name)) {
                return mountStat;
            }
        }
        return null;
    }
}
