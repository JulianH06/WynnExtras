package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TomeDatabaseTest {
    @Test
    void ordersByRarityThenDescendingRomanTier() {
        List<WynnTome> tomes = new ArrayList<>(List.of(
                tome("Fabled II", "Fabled", 100),
                tome("Mythic II", "Mythic", 90),
                tome("Mythic III", "Mythic", 80),
                tome("Legendary III", "Legendary", 110),
                tome("Fabled III", "Fabled", 70)));

        tomes.sort(TomeDatabase.searchOrder());

        assertEquals(
                List.of("Mythic III", "Mythic II", "Fabled III", "Fabled II", "Legendary III"),
                tomes.stream().map(WynnTome::displayName).toList());
    }

    private static WynnTome tome(String name, String rarity, int level) {
        return new WynnTome(name, "", "weaponTome", rarity, level, Map.of());
    }
}
