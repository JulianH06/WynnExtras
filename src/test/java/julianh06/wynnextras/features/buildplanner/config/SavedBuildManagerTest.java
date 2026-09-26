package julianh06.wynnextras.features.buildplanner.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedBuildManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsCompleteBuildAndReplacesMatchingNameAndClass() {
        Path file = tempDir.resolve("saved-builds.json");
        SavedBuildManager manager = new SavedBuildManager(file);
        SavedBuild original = build("Spring Build", 120, 1);

        assertTrue(manager.save(original));
        SavedBuild loaded = new SavedBuildManager(file).getAll().getFirst();
        assertEquals("Spring", loaded.equipment().get("WEAPON"));
        assertEquals("w6w6w6", loaded.powders().get("WEAPON"));
        assertArrayEquals(new int[]{5, 10, 15, 20, 25}, loaded.assignedSkills());
        assertEquals(
                "Nimble Tome of Combat Mastery I", loaded.tomes().get("weaponTome1"));
        assertEquals("Aspect of Focus", loaded.aspects().get("aspect1").name());
        assertEquals(Set.of("root", "storm"), loaded.abilityTrees().get("archer"));

        assertTrue(manager.save(build("spring build", 121, 2)));
        assertEquals(1, manager.getAll().size());
        assertEquals(121, manager.getAll().getFirst().level());
    }

    @Test
    void persistsConvertedCodesForAllFourAccessorySlotsAndMasterworkNames() throws Exception {
        String source;
        try (var stream = getClass().getResourceAsStream("/crafted-accessory-conversions.json")) {
            source = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonArray().get(0).getAsJsonObject().get("sourceModern").getAsString();
        }
        Map<String, String> equipment = Map.of(
                "RING_1", CraftedItemCodec.decodeForSlot(source, "ring").reference(),
                "RING_2", CraftedItemCodec.decodeForSlot(source, "ring").reference(),
                "BRACELET", CraftedItemCodec.decodeForSlot(source, "bracelet").reference(),
                "NECKLACE", CraftedItemCodec.decodeForSlot(source, "necklace").reference(),
                "WEAPON", "Masterwork Divzer");
        SavedBuild original = new SavedBuild("Accessories", "archer", 120, equipment, Map.of(),
                new int[5], Map.of(), Map.of(), Map.of(), 1);
        Path file = tempDir.resolve("accessories.json");
        assertTrue(new SavedBuildManager(file).save(original));
        SavedBuild loaded = new SavedBuildManager(file).getAll().getFirst();
        assertEquals(equipment, loaded.equipment());
        Map.of("RING_1", "ring", "RING_2", "ring", "BRACELET", "bracelet", "NECKLACE", "necklace")
                .forEach((slot, subtype) -> assertEquals(subtype,
                        ItemDatabase.getInstance().getItem(loaded.equipment().get(slot)).subType()));
    }

    @Test
    void loadsLegacyBuildWithoutTomes() throws Exception {
        Path file = tempDir.resolve("legacy-builds.json");
        Files.writeString(file, """
                [{
                  "name": "Legacy",
                  "characterClass": "archer",
                  "level": 106,
                  "equipment": {},
                  "powders": {},
                  "assignedSkills": [0, 0, 0, 0, 0],
                  "savedAt": 1
                }]
                """);

        SavedBuild loaded = new SavedBuildManager(file).getAll().getFirst();

        assertTrue(loaded.tomes().isEmpty());
        assertTrue(loaded.aspects().isEmpty());
        assertTrue(loaded.abilityTrees().isEmpty());
    }

    private static SavedBuild build(String name, int level, long savedAt) {
        return new SavedBuild(
                name,
                "archer",
                level,
                Map.of("HELMET", "Aphotic", "WEAPON", "Spring"),
                Map.of("WEAPON", "w6w6w6"),
                new int[]{5, 10, 15, 20, 25},
                Map.of("weaponTome1", "Nimble Tome of Combat Mastery I"),
                Map.of("aspect1", new SavedBuild.SavedAspect("Aspect of Focus", 3)),
                Map.of("archer", Set.of("root", "storm")),
                savedAt);
    }
}
