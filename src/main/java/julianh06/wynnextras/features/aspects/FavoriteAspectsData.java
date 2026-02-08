package julianh06.wynnextras.features.aspects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoriteAspectsData {
    public static FavoriteAspectsData INSTANCE = new FavoriteAspectsData();

    public Set<String> favoriteAspects = new HashSet<>();
    public List<String> recentSearches = new ArrayList<>();
    private static final int MAX_RECENT_SEARCHES = 5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/favorite_aspects.json");

    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                FavoriteAspectsData loaded = GSON.fromJson(reader, FavoriteAspectsData.class);
                if (loaded != null) {
                    if (loaded.favoriteAspects != null) {
                        this.favoriteAspects = loaded.favoriteAspects;
                    }
                    if (loaded.recentSearches != null) {
                        this.recentSearches = loaded.recentSearches;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Note: Wynntils import is done when aspect screen opens (player needs to be logged in)
    }

    private boolean wynntilsImportAttempted = false;

    /**
     * Try to import Wynntils favorites (called when aspect screen opens)
     */
    public void tryImportWynntils() {
        if (wynntilsImportAttempted) return;
        wynntilsImportAttempted = true;

        int imported = importFromWynntils();
        if (imported > 0) {
            System.out.println("[WynnExtras] Imported " + imported + " aspect favorites from Wynntils");
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFavorite(String aspectName) {
        return favoriteAspects.contains(aspectName);
    }

    public void toggleFavorite(String aspectName) {
        if (favoriteAspects.contains(aspectName)) {
            favoriteAspects.remove(aspectName);
        } else {
            favoriteAspects.add(aspectName);
        }
        save();
    }

    public List<String> getRecentSearches() {
        return recentSearches;
    }

    public void addRecentSearch(String playerName) {
        // Remove if already exists (to move to top)
        recentSearches.remove(playerName);
        // Add at the beginning
        recentSearches.add(0, playerName);
        // Trim to max size
        while (recentSearches.size() > MAX_RECENT_SEARCHES) {
            recentSearches.remove(recentSearches.size() - 1);
        }
        save();
    }

    // Archetype names for mythic aspect detection
    private static final String[] ARCHETYPES = {
        // Warrior
        "Fallen", "Paladin", "Battle Monk",
        // Shaman
        "Summoner", "Ritualist", "Acolyte",
        // Mage
        "Riftwalker", "Light Bender", "Arcanist",
        // Archer
        "Boltslinger", "Sharpshooter", "Trapper",
        // Assassin
        "Shadestepper", "Trickster", "Acrobat"
    };

    /**
     * Check if an item name is an aspect
     * Regular aspects: "Aspect of ..."
     * Mythic aspects: "{Archetype}'s Embodiment of ..."
     */
    private boolean isAspect(String itemName) {
        if (itemName.startsWith("Aspect of")) {
            return true;
        }
        // Check for mythic aspects (archetype embodiments)
        for (String archetype : ARCHETYPES) {
            if (itemName.startsWith(archetype + "'s")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Import aspect favorites from Wynntils config
     * Wynntils stores favorites in {gameDir}/wynntils/config/{uuid}.conf.json under "itemFavoriteFeature.favoriteItems"
     * @return number of aspects imported
     */
    public int importFromWynntils() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }

        // UUID without dashes (how Wynntils stores it)
        String uuid = client.player.getUuidAsString().replace("-", "");

        // Wynntils config is in {gameDir}/wynntils/config/, not in the config folder
        // getConfigDir() returns {gameDir}/config, so we go up one level
        Path gameDir = FabricLoader.getInstance().getConfigDir().getParent();
        Path wynntilsConfig = gameDir.resolve("wynntils/config/" + uuid + ".conf.json");

        System.out.println("[WynnExtras] Looking for Wynntils config at: " + wynntilsConfig);

        if (!Files.exists(wynntilsConfig)) {
            System.out.println("[WynnExtras] Wynntils config not found");
            return 0;
        }

        int imported = 0;
        try (Reader reader = Files.newBufferedReader(wynntilsConfig)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("itemFavoriteFeature.favoriteItems")) {
                JsonArray favorites = root.getAsJsonArray("itemFavoriteFeature.favoriteItems");

                for (JsonElement element : favorites) {
                    String itemName = element.getAsString();
                    // Only import aspects
                    if (isAspect(itemName) && !favoriteAspects.contains(itemName)) {
                        favoriteAspects.add(itemName);
                        imported++;
                    }
                }

                if (imported > 0) {
                    save();
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Failed to import Wynntils favorites: " + e.getMessage());
        }

        return imported;
    }
}
