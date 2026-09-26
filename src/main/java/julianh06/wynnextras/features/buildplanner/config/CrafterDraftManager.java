package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.Craft;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Legacy standalone draft store, retained unchanged when migrating into the shared tool workspace. */
public final class CrafterDraftManager {
    private final Path file;
    private Craft draft = CraftedItemCodec.emptyCraft("bow");
    private String written = "";
    private String error = "";
    private boolean unreadable;

    CrafterDraftManager(Path file) {
        this.file = file;
        if (!Files.exists(file)) return;
        try {
            SavedDraft saved = new Gson().fromJson(Files.readString(file, StandardCharsets.UTF_8), SavedDraft.class);
            if (saved == null || saved.version() != 1 || saved.code() == null) {
                throw new JsonParseException("Invalid crafting draft");
            }
            draft = CraftedItemCodec.readCraft(saved.code());
            written = CraftedItemCodec.encode(draft);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            unreadable = true;
            error = "Cannot read saved craft. Original file preserved; changes stay in memory.";
            PlannerLog.LOGGER.error("Could not load WynnCrafter draft {}.", file, exception);
        }
    }

    public static CrafterDraftManager getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized Craft draft() {
        return draft;
    }

    public synchronized String error() {
        return error;
    }

    public synchronized boolean save(Craft changed) {
        draft = changed;
        if (unreadable) return false;
        String code = CraftedItemCodec.encode(changed);
        if (code.equals(written)) {
            error = "";
            return true;
        }
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "crafter-draft-", ".tmp");
            Files.writeString(temporary, new Gson().toJson(new SavedDraft(1, code)), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            written = code;
            error = "";
            return true;
        } catch (IOException exception) {
            error = "Could not save craft. Draft remains in memory; see the log.";
            PlannerLog.LOGGER.error("Could not save WynnCrafter draft {}.", file, exception);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    PlannerLog.LOGGER.warn("Could not clean up WynnCrafter draft temporary file.", exception);
                }
            }
        }
    }

    private record SavedDraft(int version, String code) {}

    private static final class Holder {
        private static final CrafterDraftManager INSTANCE = new CrafterDraftManager(
                FabricLoader.getInstance().getConfigDir().resolve("wynnextras").resolve("buildplanner").resolve("crafter-draft.json"));
    }
}
