package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.InitEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static julianh06.wynnextras.features.spellhider.SpellHider.GSON;

@WEModule
public class SpellHiderMappings {
    private static final Path MAPPINGS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("spell_mappings.json");

    public static SpellHiderMappings INSTANCE = new SpellHiderMappings();

    private final Map<Integer, SpellNamespace> idMappings;

    public Collection<SpellNamespace> getAllNamespaces() {
        return INSTANCE.idMappings.values();
    }

    public boolean namespaceExists(String FQName) {
        return idMappings.values().stream().anyMatch((v) -> v.getFQName().equals(FQName));
    }

    public SpellHiderMappings() {
        idMappings = new HashMap<>();
    }

    public SpellHiderMappings(Map<Integer, SpellNamespace> idMappings) {
        this.idMappings = idMappings;
    }

    public void addSpellIdentifier(Integer hash, SpellNamespace namespace) {
        idMappings.put(hash, namespace);
    }

    public SpellNamespace getSpellMapping(Identifier id) {
        SpellData fromPath = SpellHider.getFromPath(id.getPath());
        if (fromPath == null) return null;

        return idMappings.get(fromPath.getHash());
    }

    public void changeNamespace(String oldName, String newName) {
        SpellNamespace oldNamespace = SpellNamespace.from(oldName);
        SpellNamespace newNamespace = SpellNamespace.from(newName);

        for (Map.Entry<Integer, SpellNamespace> entry : idMappings.entrySet()) {
            if (oldNamespace.equals(entry.getValue())) {
                idMappings.put(entry.getKey(), newNamespace);
            }
        }
    }

    @SubscribeEvent
    public void init(InitEvent empty) {
        InputStream input = SpellHiderMappings.class.getClassLoader().getResourceAsStream(SpellHider.RESOURCES_PATH + "spell_mappings.json");
        if (input == null) {
            WynnExtras.LOGGER.warn("failed to find spell mapping, spell hider will not work");
            INSTANCE = new SpellHiderMappings();
            return;
        }
        Reader reader = new InputStreamReader(input);
        SaveFormat jsonData = GSON.fromJson(reader, SaveFormat.class);
        INSTANCE = jsonData.toConfig();
    }

    // basically flips the map so each value stores a list of its keys
    // stops it from storing so many duplicate names, and it's easier to read
    public static class SaveFormat {
        private final MappedNamespace spellMappings;

        public SaveFormat(SpellHiderMappings config) {
            spellMappings = new MappedNamespace("");
            for (Map.Entry<Integer, SpellNamespace> entry : config.idMappings.entrySet()) {
                Integer hash = entry.getKey();
                SpellNamespace namespace = entry.getValue();
                if (namespace.isEmpty()) continue;
                String[] namespaceParts = namespace.getFQName().split(":");
                MappedNamespace tracker = spellMappings;
                for (String part : namespaceParts) {
                    tracker = tracker.putIfAbsent(part);
                }
                tracker.addMapping(hash);
            }
        }

        public SpellHiderMappings toConfig() {
            Map<Integer, SpellNamespace> result = new HashMap<>();
            spellMappings.recurseAdd(result, "");
            return new SpellHiderMappings(result);
        }

        public static class MappedNamespace {
            public String self;
            public List<Integer> mappings;
            public List<MappedNamespace> children;

            public void addMapping(Integer hash) {
                mappings.add(hash);
            }

            public MappedNamespace(String self) {
                this.self = self;
                this.children = new ArrayList<>();
                this.mappings = new ArrayList<>();
            }

            public boolean is(Object s) {
                if (s instanceof MappedNamespace) {
                    return this.self.equals(((MappedNamespace) s).self);
                } else if (s instanceof String) {
                    return this.self.equals(s);
                }
                return false;
            }

            private MappedNamespace putIfAbsent(String part) {
                for (MappedNamespace mapping : children) {
                    if (mapping.is(part)) return mapping;
                }
                MappedNamespace mapping = new MappedNamespace(part);
                children.add(mapping);
                return mapping;
            }

            public void recurseAdd(Map<Integer, SpellNamespace> result, String current) {
                current = current.isEmpty() ? this.self : current + ":" + this.self;
                for (Integer hash : this.mappings) {
                    result.put(hash, SpellNamespace.from(current));
                }
                for (MappedNamespace child : this.children) {
                    child.recurseAdd(result, current);
                }
            }
        }
    }

    public static void reloadFromFile() {
        try {
            if (Files.exists(MAPPINGS_PATH)) {
                String json = Files.readString(MAPPINGS_PATH);
                SaveFormat saveFormat = GSON.fromJson(json, SaveFormat.class);
                INSTANCE = saveFormat.toConfig();
                if (INSTANCE == null) {
                    INSTANCE = new SpellHiderMappings();
                }
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load spell mappings: " + e.getMessage());
            INSTANCE = new SpellHiderMappings();
        }
    }

    public static void saveToFile() {
        try {
            Files.createDirectories(MAPPINGS_PATH.getParent());
            Files.writeString(MAPPINGS_PATH, GSON.toJson(new SaveFormat(INSTANCE)));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save spell mappings: " + e.getMessage());
        }
    }
}
