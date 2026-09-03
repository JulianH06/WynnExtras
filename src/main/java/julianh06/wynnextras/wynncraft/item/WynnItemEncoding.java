package julianh06.wynnextras.wynncraft.item;

import java.util.Optional;

public final class WynnItemEncoding {
    private WynnItemEncoding() {}

    public static Optional<String> encode(WynnItemData item) {
        if (item == null) return Optional.empty();
        for (String line : item.lore()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("CI-") && trimmed.matches("CI-[A-Za-z0-9_-]+")) return Optional.of(trimmed);
        }
        return Optional.empty();
    }
}
