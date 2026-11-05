package julianh06.wynnextras.features.buildplanner.data;

import java.util.List;
import java.util.Map;

public final class WynnBuilderTomeSlots {
    private static final List<String> ORDER = List.of(
            "weaponTome1", "weaponTome2",
            "armorTome1", "armorTome2", "armorTome3", "armorTome4",
            "guildTome1", "lootrunTome1",
            "gatherXpTome1", "gatherXpTome2",
            "dungeonXpTome1", "dungeonXpTome2",
            "mobXpTome1", "mobXpTome2");

    private WynnBuilderTomeSlots() {
    }

    public static String slot(int index) {
        return ORDER.get(index);
    }

    public static int size() {
        return ORDER.size();
    }

    public static List<Integer> encode(Map<String, WynnTome> selectedTomes) {
        return ORDER.stream()
                .map(slot -> selectedTomes.containsKey(slot) ? selectedTomes.get(slot).id() : null)
                .toList();
    }
}
