package julianh06.wynnextras.features.buildplanner.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuilderWorkspaceManagerTest {
    @TempDir
    Path directory;

    @Test
    void preservesDistinctCraftedCodesAcrossTabsAndNamedSaves() {
        String firstCode = "CR-1+W+W+W+W+W+W9b12";
        String secondCode = "CR-1+W+W+W+W+W+W9b92";
        var first = julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.decode(firstCode);
        var second = julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.decode(secondCode);
        assertEquals(first.displayName(), second.displayName());
        Path file = directory.resolve("workspace.json");
        BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
        manager.updateActive(build("First", "archer", first.reference(), "arrowbomb"));
        manager.addTab();
        manager.updateActive(build("Second", "archer", second.reference(), "arrowbomb"));
        BuilderWorkspaceManager restored = new BuilderWorkspaceManager(file);
        assertEquals(firstCode, restored.tabs().get(0).equipment().get("WEAPON"));
        assertEquals(secondCode, restored.tabs().get(1).equipment().get("WEAPON"));
        SavedBuildManager named = new SavedBuildManager(directory.resolve("saved-builds.json"));
        assertTrue(named.save(restored.activeBuild()));
        String restoredCode = new SavedBuildManager(directory.resolve("saved-builds.json"))
                .getAll().getFirst().equipment().get("WEAPON");
        assertEquals(second, julianh06.wynnextras.features.buildplanner.data.ItemDatabase.getInstance().getItem(restoredCode));
    }

    @Test
    void restoresAllDraftDataAndTheSelectedTabAfterRestart() {
        Path file = directory.resolve("workspace.json");
        BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
        assertEquals(1, manager.tabs().size());
        assertTrue(manager.updateActive(build("Archer", "archer", "Spring", "arrowbomb")));
        assertTrue(manager.addTab());
        assertTrue(manager.updateActive(build("Mage", "mage", "Lament", "meteor")));
        assertTrue(manager.select(0));
        BuilderWorkspaceManager restored = new BuilderWorkspaceManager(file);
        assertEquals(0, restored.activeIndex());
        assertEquals(2, restored.tabs().size());
        assertBuild(restored.activeBuild(), "archer", "Spring", "arrowbomb");
        assertTrue(restored.select(1));
        assertBuild(restored.activeBuild(), "mage", "Lament", "meteor");
        assertEquals(1, new BuilderWorkspaceManager(file).activeIndex());
    }

    @Test
    void switchingUpdatingAndClosingTabsDoNotModifyOtherTabsOrNamedSaves() throws Exception {
        Path file = directory.resolve("workspace.json");
        Path named = directory.resolve("saved-builds.json");
        SavedBuildManager savedBuilds = new SavedBuildManager(named);
        assertTrue(savedBuilds.save(build("Saved copy", "archer", "Spring", "arrowbomb")));
        String originalNamed = Files.readString(named);
        BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
        manager.updateActive(build("First", "archer", "Spring", "arrowbomb"));
        manager.addTab();
        assertTrue(manager.activeBuild().equipment().isEmpty());
        assertTrue(manager.activeBuild().abilityTrees().isEmpty());
        manager.updateActive(build("Second", "mage", "Lament", "meteor"));
        manager.addTab();
        manager.updateActive(build("Third", "archer", "Freedom", "arrowstorm"));
        manager.select(1);
        assertTrue(manager.closeActiveTab());
        assertEquals(2, manager.tabs().size());
        assertEquals("Third", manager.activeBuild().name());
        assertBuild(manager.tabs().getFirst(), "archer", "Spring", "arrowbomb");
        manager.closeActiveTab();
        manager.closeActiveTab();
        assertEquals(1, manager.tabs().size());
        assertTrue(manager.activeBuild().equipment().isEmpty());
        assertEquals(originalNamed, Files.readString(named));
    }

    @Test
    void unchangedDraftDoesNotRewriteTheFile() throws Exception {
        Path file = directory.resolve("workspace.json");
        BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
        manager.updateActive(build("Same", "archer", "Spring", "arrowbomb"));
        var timestamp = java.nio.file.attribute.FileTime.fromMillis(1000);
        Files.setLastModifiedTime(file, timestamp);
        assertTrue(manager.updateActive(build("Same", "archer", "Spring", "arrowbomb")));
        assertEquals(timestamp, Files.getLastModifiedTime(file));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void corruptOrUnsupportedWorkspacesAreNeverOverwritten() throws Exception {
        for (String contents : new String[]{
                "{broken", "null", "{\"version\":2,\"tabs\":[],\"activeIndex\":0}",
                "{\"version\":1,\"tabs\":[],\"activeIndex\":0}",
                "{\"version\":1,\"tabs\":[null],\"activeIndex\":0}"}) {
            Path file = directory.resolve("broken.json");
            Files.writeString(file, contents);
            BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
            assertFalse(manager.error().isBlank());
            assertFalse(manager.updateActive(build("In memory", "mage", "Lament", "meteor")));
            assertEquals("In memory", manager.activeBuild().name());
            assertEquals(contents, Files.readString(file));
        }
    }

    @Test
    void writeFailureKeepsDraftInMemoryAndCanRetry() throws Exception {
        Path parent = directory.resolve("blocked");
        Files.writeString(parent, "not a directory");
        Path file = parent.resolve("workspace.json");
        BuilderWorkspaceManager manager = new BuilderWorkspaceManager(file);
        assertFalse(manager.updateActive(build("Keep me", "mage", "Lament", "meteor")));
        assertFalse(manager.error().isEmpty());
        assertBuild(manager.activeBuild(), "mage", "Lament", "meteor");
        Files.delete(parent);
        assertTrue(manager.updateActive(manager.activeBuild()));
        assertTrue(manager.error().isEmpty());
        assertBuild(new BuilderWorkspaceManager(file).activeBuild(), "mage", "Lament", "meteor");
    }

    private static SavedBuild build(String name, String abilityClass, String weapon, String node) {
        return new SavedBuild(name, abilityClass, 106,
                Map.of("WEAPON", weapon), Map.of("WEAPON", "w6w"),
                new int[]{1, 2, 3, 4, 5},
                Map.of("weaponTome1", "Test tome"),
                Map.of("aspect1", new SavedBuild.SavedAspect("Test aspect", 3)),
                Map.of(abilityClass, Set.of(node)), 0);
    }

    private static void assertBuild(SavedBuild build, String abilityClass, String weapon, String node) {
        assertEquals(106, build.level());
        assertEquals(weapon, build.equipment().get("WEAPON"));
        assertEquals("w6w", build.powders().get("WEAPON"));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, build.assignedSkills());
        assertEquals("Test tome", build.tomes().get("weaponTome1"));
        assertEquals(3, build.aspects().get("aspect1").tier());
        assertEquals(Map.of(abilityClass, Set.of(node)), build.abilityTrees());
    }
}
