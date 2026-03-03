package julianh06.wynnextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.features.spellhider.SpellNamespace;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@WEModule
public class SpellHiderConfig {
    private static final Path MAPPINGS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("default_spell_mappings.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static SpellHiderConfig INSTANCE = new SpellHiderConfig();

    private final Map<String, SpellNamespace> idMappings = new HashMap<>();

    public void addSpellIdentifier(String path, SpellNamespace namespace) {
        idMappings.put(path, namespace);
    }

    public SpellNamespace getSpellMapping(Identifier id) {
        return idMappings.get(id.getPath());
    }

    public static void load() {
        try {
            if (Files.exists(MAPPINGS_PATH)) {
                String json = Files.readString(MAPPINGS_PATH);
                INSTANCE = GSON.fromJson(json, SpellHiderConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new SpellHiderConfig();
                }
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Failed to load spell mappings: " + e.getMessage());
            INSTANCE = new SpellHiderConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(MAPPINGS_PATH.getParent());
            Files.writeString(MAPPINGS_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[WynnExtras] Failed to save spell mappings: " + e.getMessage());
        }
    }
}
