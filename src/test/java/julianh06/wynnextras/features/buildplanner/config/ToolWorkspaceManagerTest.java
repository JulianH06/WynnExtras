package julianh06.wynnextras.features.buildplanner.config;

import static org.junit.jupiter.api.Assertions.*;
import julianh06.wynnextras.features.buildplanner.data.AtlasState;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolWorkspaceManagerTest {
    @TempDir Path directory;

    private BuilderWorkspaceManager legacy() {
        return new BuilderWorkspaceManager(directory.resolve("builder-workspace.json"));
    }
    private CrafterDraftManager crafter() {
        return new CrafterDraftManager(directory.resolve("crafter-draft.json"));
    }
    private ToolWorkspaceManager open() {
        return new ToolWorkspaceManager(directory.resolve("tool-workspace.json"), legacy(), crafter());
    }
    private SavedBuild build(String name, String weapon) {
        return new SavedBuild(name, "archer", 120, Map.of("WEAPON", weapon), Map.of("WEAPON", "w6w"),
                new int[]{1, 2, 3, 4, 5}, Map.of("guildTome1", "Test tome"), Map.of(),
                Map.of("archer", Set.of("arrowstorm")), 0);
    }

    @Test
    void migratesEveryExistingBuildAndCraftWithoutModifyingOriginalFiles() throws Exception {
        var legacy = legacy();
        legacy.updateActive(build("Archer", "Divzer"));
        legacy.addTab();
        legacy.updateActive(build("Other", "Masterwork Divzer"));
        legacy.select(0);
        var craft = CraftedItemCodec.emptyCraft("ring").withIngredient(0, 4001);
        crafter().save(craft);
        Files.writeString(directory.resolve("saved-builds.json"), "untouched named saves");
        String originalBuilds = Files.readString(directory.resolve("builder-workspace.json"));
        String originalCraft = Files.readString(directory.resolve("crafter-draft.json"));
        var workspace = open();
        assertEquals(3, workspace.tabs().size());
        assertEquals("Archer", workspace.activeTab().name());
        assertEquals("Masterwork Divzer", workspace.tabs().get(1).build().equipment().get("WEAPON"));
        assertEquals(CraftedItemCodec.encode(craft), workspace.tabs().get(2).craftCode());
        assertEquals(Set.of("arrowstorm"), workspace.tabs().get(0).build().abilityTrees().get("archer"));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, workspace.tabs().get(0).build().assignedSkills());
        assertEquals("w6w", workspace.tabs().get(0).build().powders().get("WEAPON"));
        workspace.add(ToolWorkspaceManager.Type.ATLAS);
        assertEquals(originalBuilds, Files.readString(directory.resolve("builder-workspace.json")));
        assertEquals(originalCraft, Files.readString(directory.resolve("crafter-draft.json")));
        assertEquals("untouched named saves", Files.readString(directory.resolve("saved-builds.json")));
        assertEquals(4, open().tabs().size());
    }

    @Test
    void resetNameIsAvailableAndPersistsWithoutRenamingOtherTabs() {
        var workspace = open();
        String first = workspace.activeId();
        String second = workspace.add(ToolWorkspaceManager.Type.BUILDER);
        workspace.updateBuild(second, build("Custom name", "Freedom"));
        String atlas = workspace.add(ToolWorkspaceManager.Type.ATLAS);
        assertEquals("Build 1", workspace.defaultBuildName(first));
        assertEquals("Build 2", workspace.defaultBuildName(second));
        workspace.updateBuild(second, ToolWorkspaceManager.emptyBuild(workspace.defaultBuildName(second)));
        var restored = open();
        assertEquals("Build 2", restored.find(second).name());
        assertEquals("Build 2", restored.find(second).build().name());
        assertEquals("Build 1", restored.find(first).name());
        assertEquals(atlas, restored.activeId());
        assertEquals("Build 2", restored.defaultBuildName(second));
        assertThrows(IllegalArgumentException.class, () -> restored.defaultBuildName(atlas));
    }

    @Test
    void newAtlasTabsKeepEmptyFiltersAfterReload() {
        var workspace = open();
        String atlas = workspace.add(ToolWorkspaceManager.Type.ATLAS);
        assertTrue(workspace.find(atlas).atlas().filters().isEmpty());
        assertTrue(open().find(atlas).atlas().filters().isEmpty());
        assertEquals("~none", open().find(atlas).atlas().category());
        workspace.updateAtlas(atlas, AtlasState.defaults(true));
        assertTrue(open().find(atlas).atlas().ingredients());
        assertEquals("~none", open().find(atlas).atlas().category());
    }

    @Test
    void lateScreenRemovalSavesToItsOwnIdEvenAfterAnotherToolBecomesActive() {
        var workspace = open();
        String first = workspace.activeId();
        String second = workspace.add(ToolWorkspaceManager.Type.BUILDER);
        workspace.updateBuild(second, build("Second", "Freedom"));
        workspace.updateBuild(first, build("First late save", "Spring"));
        assertEquals(second, workspace.activeId());
        assertEquals("Freedom", workspace.activeTab().build().equipment().get("WEAPON"));
        String atlas = workspace.add(ToolWorkspaceManager.Type.ATLAS);
        workspace.updateBuild(second, build("Second final save", "Masterwork Divzer"));
        assertEquals(ToolWorkspaceManager.Type.ATLAS, workspace.activeTab().type());
        String craftId = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        var filters = new AtlasState(true, "mana regen", "ALCHEMISM", "3", "50", "119", "level", true, 2, "ingredient:861");
        workspace.updateAtlas(atlas, filters);
        assertEquals(craftId, workspace.activeId());
        var changed = CraftedItemCodec.emptyCraft("bow").withIngredient(3, 4001);
        workspace.select(first);
        workspace.updateCraft(craftId, changed);
        var restored = open();
        assertEquals(first, restored.activeId());
        assertEquals(filters, restored.find(atlas).atlas());
        assertEquals(CraftedItemCodec.encode(changed), restored.find(craftId).craftCode());
        assertEquals("Masterwork Divzer", restored.find(second).build().equipment().get("WEAPON"));
    }

    @Test
    void keepsCraftsIndependentAndPersistsTheirExactEquipmentDestination() {
        var workspace = open();
        String build = workspace.activeId();
        String first = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        String second = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        var craft = CraftedItemCodec.emptyCraft("ring");
        workspace.updateCraft(first, craft);
        workspace.updateCraft(second, craft.withIngredient(2, 4001));
        workspace.bindCraft(second, build, "RING_2");
        var restored = open();
        assertNotEquals(restored.find(first).craftCode(), restored.find(second).craftCode());
        assertEquals("RING_2", restored.craftTarget(second).slot());
        assertEquals("ring", restored.craftTarget(second).slotType());
        restored.close(build);
        assertNull(restored.craftTarget(second));
        assertNotNull(restored.find(second));
        assertThrows(IllegalArgumentException.class, () -> restored.updateBuild(build, build("Closed", "Divzer")));
        assertThrows(IllegalArgumentException.class, () -> restored.updateBuild(second, build("Wrong tool", "Divzer")));
    }

    @Test
    void alwaysLeavesOneTabAndDoesNotRewriteUnchangedState() throws Exception {
        var workspace = open();
        Path file = directory.resolve("tool-workspace.json");
        var timestamp = java.nio.file.attribute.FileTime.fromMillis(1000);
        Files.setLastModifiedTime(file, timestamp);
        workspace.updateBuild(workspace.activeId(), workspace.activeTab().build());
        assertEquals(timestamp, Files.getLastModifiedTime(file));
        workspace.close(workspace.activeId());
        assertEquals(1, workspace.tabs().size());
        assertTrue(workspace.activeTab().build().equipment().isEmpty());
        assertEquals(workspace.activeId(), open().activeId());
    }

    @Test
    void protectsCorruptWorkspacesAndLegacyMigrationSources() throws Exception {
        Path file = directory.resolve("tool-workspace.json");
        for (String text : new String[]{"{broken", "null", "{\"version\":99,\"tabs\":[]}",
                "{\"version\":1,\"activeId\":\"x\",\"tabs\":[{\"id\":\"x\",\"type\":\"ATLAS\",\"name\":\"Atlas\"}]}"}) {
            Files.writeString(file, text);
            var workspace = open();
            assertFalse(workspace.error().isEmpty());
            workspace.add(ToolWorkspaceManager.Type.ATLAS);
            assertEquals(text, Files.readString(file));
        }
        Files.delete(file);
        Files.writeString(directory.resolve("builder-workspace.json"), "{broken legacy");
        var workspace = open();
        workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        assertFalse(workspace.error().isEmpty());
        assertFalse(Files.exists(file));
        assertEquals("{broken legacy", Files.readString(directory.resolve("builder-workspace.json")));
    }

    @Test
    void persistsMultipleSelectionsAndAllFilterRowsWithoutTouchingOtherTabs() throws Exception {
        var workspace = open();
        String builder = workspace.activeId();
        String atlas = workspace.add(ToolWorkspaceManager.Type.ATLAS);
        var state = new AtlasState(true, "test", "ARMOURING,TAILORING", "2,3", "", "", "filters", false, 2, "ingredient:1",
                List.of(new AtlasState.NumericFilter("lvl", "", "120", false),
                        new AtlasState.NumericFilter("manaRegen", "-2", "", true)),
                List.of("duration"), List.of());
        workspace.updateAtlas(atlas, state);
        workspace.select(builder);
        var restored = open();
        assertEquals(state, restored.find(atlas).atlas());
        assertEquals(builder, restored.activeId());
        assertEquals(state.filters(), state.withPage(0, "").filters());

        var file = directory.resolve("tool-workspace.json");
        var json = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        var legacyAtlas = json.getAsJsonArray("tabs").get(1).getAsJsonObject().getAsJsonObject("atlas");
        legacyAtlas.remove("filters");
        legacyAtlas.remove("excluded");
        legacyAtlas.remove("strings");
        legacyAtlas.addProperty("minLevel", "50");
        legacyAtlas.addProperty("maxLevel", "100");
        legacyAtlas.addProperty("sort", "level");
        Files.writeString(file, json.toString());
        var migrated = open().find(atlas).atlas();
        assertEquals(List.of(new AtlasState.NumericFilter("lvl", "50", "100", false)), migrated.filters());
        assertEquals("ARMOURING,TAILORING", migrated.category());
    }

    @Test
    void retainsAllTabsInMemoryAfterWriteFailuresAndCanRetry() throws Exception {
        Path blocked = directory.resolve("blocked");
        Files.writeString(blocked, "not a directory");
        var workspace = new ToolWorkspaceManager(blocked.resolve("workspace.json"), legacy(), crafter());
        String id = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        assertFalse(workspace.error().isEmpty());
        Files.delete(blocked);
        assertTrue(workspace.select(id));
        assertEquals(id, new ToolWorkspaceManager(blocked.resolve("workspace.json"), legacy(), crafter()).activeId());
    }
}
