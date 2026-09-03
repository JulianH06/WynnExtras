package julianh06.wynnextras.features.spellhider;

import com.google.common.reflect.TypeToken;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.InitEvent;
import julianh06.wynnextras.utils.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static julianh06.wynnextras.features.spellhider.SpellHider.GSON;

@WEModule
public class SpellProfiles {
    private static final Path PROFILES_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("spell_profiles");

    private static String currentName = "";
    private static Map<SpellNamespace, SpellModifiers> currentProfile = new HashMap<>();

    private static final List<String> profileNames = new ArrayList<>();
    static {
        profileNames.add("default_off");
        profileNames.add("default_low");
        profileNames.add("default_medium");
        profileNames.add("default_high");
        profileNames.add("default_all");
    }

    public static boolean isEverythingInvisible() {
        return currentName.equals("default_all");
    }

    // TODO surely you can register a config change listener of some sort
    public static SpellModifiers getModifiers(SpellNamespace namespace) {
        String config = WynnExtrasConfig.INSTANCE.spellProfile;
        if (!currentName.equals(config)) {
            try {
                loadProfile(config);
            } catch (IOException e) {
                WynnExtras.LOGGER.error("failed to load profile: {}", config);
            }
        }
        if (currentName.equals("default_off")) return null;
        return currentProfile.get(namespace);
    }

    public static boolean addToProfile(String fileName, SpellNamespace namespace, SpellModifier modifierType, Object value) {
        if (!currentName.equals(fileName)) {
            try {
                if (!loadProfile(fileName)) return false;
            } catch (IOException e) {
                WynnExtras.LOGGER.error("Could not load profile file {}, not adding to it", fileName, e);
                ChatUtils.sendMessage("Could not load profile file " + fileName);
                return false;
            }
        }
        SpellModifiers modifiers = currentProfile.compute(namespace, (k, v) -> v == null ? new SpellModifiers() : v);
        boolean result = modifiers.set(modifierType, value);
        saveProfile();
        return result;
    }

    public static void addAll(Map<SpellNamespace, SpellModifiers> allModifiers) {
        currentProfile.putAll(allModifiers);
        saveProfile();
    }

    public static boolean loadProfile(String fileName) throws IOException {
        Type mapType = new TypeToken<@NotNull Map<SpellNamespace, SpellModifiers>>() {}.getType();
        Map<SpellNamespace, SpellModifiers> fromJson;
        try {
            if (fileName.startsWith("default_")) {
                InputStream input = SpellProfiles.class.getClassLoader().getResourceAsStream(SpellHider.RESOURCES_PATH + fileName + ".json");
                if (input == null) {
                    WynnExtras.LOGGER.warn("failed to load default profile file {}", fileName);
                    return false;
                }
                try (Reader reader = new InputStreamReader(input)) {
                    fromJson = GSON.fromJson(reader, mapType);
                }
            } else {
                Path file = PROFILES_PATH.resolve(fileName + ".json");
                if (Files.exists(file)) {
                    String jsonString = Files.readString(file);
                    fromJson = GSON.fromJson(jsonString, mapType);
                } else {
                    WynnExtras.LOGGER.warn("profile doesn't exist: {}", fileName);
                    return false;
                }
            }
        } catch (RuntimeException e) {
            WynnExtras.LOGGER.error("Failed to parse spell profile {}.", fileName, e);
            return false;
        }

        if (fromJson == null) {
            WynnExtras.LOGGER.warn("failed to load json");
            return false;
        }

        currentProfile = fromJson;
        currentName = fileName;
        WynnExtrasConfig.INSTANCE.spellProfile = fileName;
        return true;
    }

    @SubscribeEvent
    public void load(InitEvent empty) {
        Path names = PROFILES_PATH.resolve("names.json");
        if (Files.exists(names)) {
            try {
                String json = Files.readString(names);
                Type listType = new TypeToken<@NotNull List<String>>() {}.getType();
                List<String> loadedNames = GSON.fromJson(json, listType);
                if (loadedNames != null) {
                    profileNames.clear();
                    profileNames.addAll(loadedNames);
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.warn("failed to load spell profile name list", e);
            }
        } else saveProfilesNames();

        try {
            loadProfile(WynnExtrasConfig.INSTANCE.spellProfile);
        } catch (IOException e) {
            WynnExtras.LOGGER.warn("failed to load profile from config");
        }
    }

    public static void createProfile(String fileName) {
        if (profileNames.contains(fileName)) {
            WynnExtras.LOGGER.warn("profile already exists: {}", fileName);
            return;
        }
        profileNames.add(fileName);
        currentName = fileName;
        currentProfile = new HashMap<>();
        saveProfile();
        saveProfilesNames();
    }

    private static void saveProfilesNames() {
        Path names = PROFILES_PATH.resolve("names.json");
        try {
            Files.createDirectories(names.getParent());
            Files.writeString(names, GSON.toJson(getProfileNames()));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("Could not create directory {}", names, e);
        }
    }

    public static List<String> getProfileNames() {
        return profileNames;
    }

    public static void saveProfile() {
        if (currentName.isEmpty()) {
            WynnExtras.LOGGER.error("attempt to save profile but no profile is loaded");
            return;
        }
        try {
            Path file = PROFILES_PATH.resolve(currentName + ".json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(currentProfile));
        } catch (IOException e) {
            WynnExtras.LOGGER.warn("failed to save profile: {}", currentName);
        }
    }
}
