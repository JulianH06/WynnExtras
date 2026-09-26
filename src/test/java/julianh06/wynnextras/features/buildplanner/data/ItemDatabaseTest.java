package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemDatabaseTest {
    @Test
    void retainsPlainLoreAndTradeRestrictionsForTheInspectionCard() throws Exception {
        WynnItem item = database().getItem("Thunder Sanctuary");
        assertEquals("The home of the storms, where the cramped clouds crackle and power surges in sharp bolts.",
                item.lore());
        assertEquals("untradable", item.restriction());
        assertEquals("One & two\n\"Three\" <four> 'five'",
                ItemDatabase.cleanItemText("<span>One &amp; two</span><br />&quot;Three&quot; &lt;four&gt; &#39;five&#39;"));
        WynnItem legacy = new WynnItem("Old", "accessory", "ring", "normal", "", Map.of(), Map.of());
        assertEquals("", legacy.lore());
        assertEquals("", legacy.restriction());
    }

    @Test
    void savedReferencesRoundTripWithoutConfusingOriginalAndMasterworkItems() throws Exception {
        ItemDatabase database = database();
        SavedBuild saved = new SavedBuild("Masterwork build", "archer", 120,
                Map.of("WEAPON", database.getItem("Masterwork Divzer").reference()), Map.of(),
                new int[5], Map.of(), Map.of(), Map.of(), 0);
        Gson gson = new Gson();
        SavedBuild restored = gson.fromJson(gson.toJson(saved), SavedBuild.class);
        assertEquals(database.getItem("Masterwork Divzer"), database.getItem(restored.equipment().get("WEAPON")));
        assertNotEquals(database.getItem("Divzer"), database.getItem(restored.equipment().get("WEAPON")));
    }

    @Test
    void keepsMasterworksSeparateFromTheirOriginalsAndSearchableByEquipmentType() throws Exception {
        ItemDatabase database = database();
        for (String name : List.of("Divzer", "Guardian", "Warp", "Epoch", "Cataclysm", "Immolation",
                "Sunstar", "Singularity", "Idol", "Nirvana", "Lament", "Stratiformis", "Olympic",
                "Apocalypse", "Aftershock", "Az", "Archangel", "Pure")) {
            WynnItem original = database.getItem(name);
            WynnItem masterwork = database.getItem("Masterwork " + name);
            assertNotNull(original, name);
            assertNotNull(masterwork, name);
            assertNotEquals(original, masterwork);
            assertEquals("Masterwork " + name, masterwork.reference());
            assertTrue(database.searchItems(name, "weapon").containsAll(List.of(original, masterwork)));
            assertFalse(database.searchItems(name, "ring").contains(masterwork));
        }
        assertEquals(69, database.searchItems("Masterwork ").size());
        assertNotNull(database.getItem("Alstroemania"));
    }

    @Test
    void usesActualApiRollsRequirementsBaseDamageIconsAndMajorIdDescriptions() throws Exception {
        ItemDatabase database = database();
        WynnItem guardian = database.getItem("Masterwork Guardian");
        assertEquals(109, guardian.stat("lvl"));
        assertEquals(110, guardian.stat("defReq"));
        assertEquals(50, guardian.stat("nDamMin"));
        assertEquals(220, guardian.stat("fDamMax"));
        assertEquals(10000, guardian.identifications().get("rawHealth").max());
        assertEquals(18, guardian.identifications().get("manaRegen").max());
        assertEquals("spear", guardian.subType());
        assertEquals("normal", guardian.attackSpeed());
        assertEquals(3845, guardian.customModelData());
        assertTrue(guardian.majorIds().containsKey("Heroine's Blessing"));
        assertFalse(guardian.majorIds().get("Heroine's Blessing").contains("<span"));
        WynnItem divzer = database.getItem("Masterwork Divzer");
        assertEquals("bow", divzer.subType());
        assertEquals("superFast", divzer.attackSpeed());
        assertEquals(111, divzer.stat("lvl"));
        assertEquals(115, divzer.stat("dexReq"));
        assertEquals(214, divzer.stat("tDamMin"));
        assertEquals(214, divzer.stat("tDamMax"));
        assertEquals(37, divzer.identifications().get("dex").max());
        assertEquals(-550, divzer.identifications().get("agi").max());
        assertEquals(-385, divzer.identifications().get("fireDamage").max());
        assertTrue(divzer.majorIds().containsKey("Phase Vector"));
        assertNotEquals(database.getItem("Guardian").stat("lvl"), guardian.stat("lvl"));
        assertNotEquals(database.getItem("Divzer").stat("tDamMin"), divzer.stat("tDamMin"));
    }

    private static ItemDatabase database() throws Exception {
        try (var stream = ItemDatabaseTest.class.getResourceAsStream("/api-masterwork-items.json")) {
            ItemDatabase database = new ItemDatabase();
            database.replaceItems(database.parseItems(JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))));
            return database;
        }
    }
}
