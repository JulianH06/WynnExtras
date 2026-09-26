package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WynnTomeTest {
    @Test
    void summarizesTomeEffectsWithoutTheItemName() {
        WynnTome tome = new WynnTome(
                "Abyssal Tome of Combat Mastery III",
                "",
                "weaponTome",
                "Mythic",
                105,
                Map.of("waterDamage", 8));

        assertEquals("Water Damage", tome.effectSummary());
        assertEquals(
                "Abyssal Tome of Combat Mastery III (Water Damage)",
                tome.nameWithStats());
    }

    @Test
    void summarizesAllFiveSkillsAsRainbow() {
        WynnTome tome = new WynnTome(
                "Example",
                "",
                "armorTome",
                "Fabled",
                100,
                Map.of("str", 1, "dex", 1, "int", 1, "def", 1, "agi", 1));

        assertEquals("Rainbow", tome.effectSummary());
    }
}
