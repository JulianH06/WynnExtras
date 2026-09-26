package julianh06.wynnextras.features.buildplanner.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedAbilityTreeManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsTreesAndReplacesMatchingNameWithinTheSameClass() {
        Path file = tempDir.resolve("saved-ability-trees.json");
        SavedAbilityTreeManager manager = new SavedAbilityTreeManager(file);

        assertTrue(manager.save(tree("Bossing", "archer", Set.of("root"), 1)));
        assertTrue(manager.save(tree("bossing", "archer", Set.of("root", "storm"), 2)));
        assertTrue(manager.save(tree("Bossing", "mage", Set.of("root"), 3)));

        SavedAbilityTreeManager reloaded = new SavedAbilityTreeManager(file);
        assertEquals(1, reloaded.getAll(AbilityTreeClass.ARCHER).size());
        assertEquals(
                Set.of("root", "storm"),
                reloaded.getAll(AbilityTreeClass.ARCHER).getFirst().selectedNodes());
        assertEquals(1, reloaded.getAll(AbilityTreeClass.MAGE).size());
    }

    @Test
    void ignoresInvalidLegacyEntries() throws Exception {
        Path file = tempDir.resolve("saved-ability-trees.json");
        Files.writeString(file, """
                [
                  {"name":"Valid","characterClass":"archer","selectedNodes":["root"],"savedAt":1},
                  {"name":"","characterClass":"archer","selectedNodes":[],"savedAt":2}
                ]
                """);

        SavedAbilityTreeManager manager = new SavedAbilityTreeManager(file);

        assertEquals(1, manager.getAll(AbilityTreeClass.ARCHER).size());
    }

    @Test
    void colorCodesDoNotCreateDuplicateVisibleNames() {
        Path file = tempDir.resolve("saved-ability-trees.json");
        SavedAbilityTreeManager manager = new SavedAbilityTreeManager(file);

        assertTrue(manager.save(tree("&aBossing", "archer", Set.of("root"), 1)));
        assertTrue(manager.save(tree("\u00a7cBossing", "archer", Set.of("root", "storm"), 2)));

        assertEquals(1, manager.getAll(AbilityTreeClass.ARCHER).size());
    }

    private static SavedAbilityTree tree(
            String name, String characterClass, Set<String> nodes, long savedAt
    ) {
        return new SavedAbilityTree(name, characterClass, nodes, savedAt);
    }
}
