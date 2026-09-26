package julianh06.wynnextras.features.buildplanner.config;

import static org.junit.jupiter.api.Assertions.*;

import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrafterDraftManagerTest {
    @TempDir Path directory;

    @Test
    void restoresTheEditedCraftWithoutTouchingBuilderSaves() throws Exception {
        Path savedBuilds = directory.resolve("saved-builds.json");
        Files.writeString(savedBuilds, "existing saved builds");
        Path file = directory.resolve("crafter-draft.json");
        var manager = new CrafterDraftManager(file);
        var craft = CraftedItemCodec.emptyCraft("bow").withIngredient(0, 4001);
        assertTrue(manager.save(craft));
        assertEquals(craft, new CrafterDraftManager(file).draft());
        var modified = Files.getLastModifiedTime(file);
        assertTrue(manager.save(craft));
        assertEquals(modified, Files.getLastModifiedTime(file));
        assertEquals("existing saved builds", Files.readString(savedBuilds));
    }

    @Test
    void preservesUnreadableFilesAndKeepsChangesInMemory() throws Exception {
        Path file = directory.resolve("crafter-draft.json");
        Files.writeString(file, "{\"version\":1,\"code\":\"invalid\"}");
        var manager = new CrafterDraftManager(file);
        var craft = CraftedItemCodec.emptyCraft("ring");
        assertFalse(manager.save(craft));
        assertEquals(craft, manager.draft());
        assertFalse(manager.error().isEmpty());
        assertEquals("{\"version\":1,\"code\":\"invalid\"}", Files.readString(file));
    }

    @Test
    void retriesFailedWritesWithoutLosingTheInMemoryDraft() throws Exception {
        Path parent = directory.resolve("blocked");
        Files.writeString(parent, "not a directory");
        var manager = new CrafterDraftManager(parent.resolve("craft.json"));
        var craft = CraftedItemCodec.emptyCraft("bow").withIngredient(2, 4002);
        assertFalse(manager.save(craft));
        assertEquals(craft, manager.draft());
        assertFalse(manager.error().isEmpty());
        Files.delete(parent);
        assertTrue(manager.save(craft));
        assertTrue(manager.error().isEmpty());
        assertEquals(craft, new CrafterDraftManager(parent.resolve("craft.json")).draft());
    }
}
