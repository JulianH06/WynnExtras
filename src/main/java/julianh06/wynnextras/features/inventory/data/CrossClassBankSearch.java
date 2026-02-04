package julianh06.wynnextras.features.inventory.data;

import com.google.gson.Gson;
import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.utils.SearchQueryParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Utility for searching across all character bank files.
 * Triggered when search query contains '@'.
 */
public class CrossClassBankSearch {

    /**
     * Result of a cross-class search
     */
    public static class SearchResult {
        public final String characterId;
        public final String characterNickname; // e.g., "Dark Wizard", "Archer", etc.
        public final int characterLevel; // Combat level of the character
        public final int pageNumber;
        public final List<ItemStack> matchingItems;
        public final List<ItemStack> pageItems;

        public SearchResult(String characterId, String characterNickname, int characterLevel, int pageNumber, List<ItemStack> matchingItems, List<ItemStack> pageItems) {
            this.characterId = characterId;
            this.characterNickname = characterNickname;
            this.characterLevel = characterLevel;
            this.pageNumber = pageNumber;
            this.matchingItems = matchingItems;
            this.pageItems = pageItems;
        }
    }

    /**
     * Search across all character banks for items matching the query.
     * @param query The search query (without the @ prefix)
     * @return List of search results from all characters
     */
    public static List<SearchResult> searchAllCharacters(String query) {
        List<SearchResult> results = new ArrayList<>();

        if (McUtils.player() == null) return results;

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        if (!Files.exists(configDir)) return results;

        SearchQueryParser.ParsedQuery parsedQuery = SearchQueryParser.parse(query);
        String currentCharacterId = BankOverlay.currentCharacterID;

        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(p -> p.getFileName().toString().startsWith("characterbank_"))
                 .filter(p -> p.getFileName().toString().endsWith(".json"))
                 .forEach(file -> {
                     String fileName = file.getFileName().toString();
                     // Extract character ID from filename: characterbank_XXXX.json
                     String characterId = fileName.substring("characterbank_".length(), fileName.length() - ".json".length());

                     // Skip current character - it's already being searched normally
                     if (characterId.equals(currentCharacterId)) return;

                     // Load and search this character's bank
                     List<SearchResult> characterResults = searchCharacterBank(file, characterId, parsedQuery);
                     results.addAll(characterResults);
                 });
        } catch (IOException e) {
            System.err.println("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        return results;
    }

    /**
     * Load a character bank file and search for matching items
     */
    private static List<SearchResult> searchCharacterBank(Path file, String characterId, SearchQueryParser.ParsedQuery query) {
        List<SearchResult> results = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            BankData data = BankData.getGson().fromJson(reader, CharacterBankData.class);
            if (data == null || data.BankPages == null) return results;

            String nickname = data.characterNickname;
            int level = data.characterLevel;

            for (Map.Entry<Integer, List<ItemStack>> entry : data.BankPages.entrySet()) {
                int pageNum = entry.getKey();
                List<ItemStack> pageItems = entry.getValue();

                if (pageItems == null) continue;

                List<ItemStack> matchingItems = new ArrayList<>();

                for (ItemStack stack : pageItems) {
                    if (stack == null || stack.isEmpty()) continue;

                    // Get WynnItem if available
                    WynnItem wynnItem = null;
                    Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
                    if (optWynnItem.isPresent()) {
                        wynnItem = optWynnItem.get();
                    }

                    if (SearchQueryParser.matches(stack, wynnItem, query)) {
                        matchingItems.add(stack);
                    }
                }

                if (!matchingItems.isEmpty()) {
                    results.add(new SearchResult(characterId, nickname, level, pageNum, matchingItems, pageItems));
                }
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
        }

        return results;
    }

    /**
     * Get all character IDs that have bank data saved
     */
    public static List<String> getAllCharacterIds() {
        List<String> ids = new ArrayList<>();

        if (McUtils.player() == null) return ids;

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        if (!Files.exists(configDir)) return ids;

        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(p -> p.getFileName().toString().startsWith("characterbank_"))
                 .filter(p -> p.getFileName().toString().endsWith(".json"))
                 .forEach(file -> {
                     String fileName = file.getFileName().toString();
                     String characterId = fileName.substring("characterbank_".length(), fileName.length() - ".json".length());
                     ids.add(characterId);
                 });
        } catch (IOException e) {
            System.err.println("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        return ids;
    }

    /**
     * Get ALL pages from ALL other characters (for just @ search with no filter)
     */
    public static List<SearchResult> getAllCharacterPages() {
        List<SearchResult> results = new ArrayList<>();

        if (McUtils.player() == null) return results;

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        if (!Files.exists(configDir)) {
            System.out.println("[WynnExtras] Config dir doesn't exist: " + configDir);
            return results;
        }

        String currentCharacterId = BankOverlay.currentCharacterID;
        System.out.println("[WynnExtras] Current character ID: " + currentCharacterId);

        try (Stream<Path> files = Files.list(configDir)) {
            List<Path> bankFiles = files
                    .filter(p -> p.getFileName().toString().startsWith("characterbank_"))
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .toList();

            System.out.println("[WynnExtras] Found " + bankFiles.size() + " character bank files");

            for (Path file : bankFiles) {
                String fileName = file.getFileName().toString();
                String characterId = fileName.substring("characterbank_".length(), fileName.length() - ".json".length());

                // Skip current character
                if (characterId.equals(currentCharacterId)) {
                    System.out.println("[WynnExtras] Skipping current character: " + characterId);
                    continue;
                }

                System.out.println("[WynnExtras] Loading pages from character: " + characterId);

                // Load all pages from this character
                List<SearchResult> characterPages = loadAllPagesFromCharacter(file, characterId);
                results.addAll(characterPages);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        System.out.println("[WynnExtras] Total cross-class pages: " + results.size());
        return results;
    }

    /**
     * Load ALL pages from a character bank file (no filtering)
     */
    private static List<SearchResult> loadAllPagesFromCharacter(Path file, String characterId) {
        List<SearchResult> results = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            BankData data = BankData.getGson().fromJson(reader, CharacterBankData.class);
            if (data == null || data.BankPages == null) {
                System.out.println("[WynnExtras] No bank data for character: " + characterId);
                return results;
            }

            String nickname = data.characterNickname;
            int level = data.characterLevel;
            System.out.println("[WynnExtras] Character " + characterId + " (" + nickname + " Lv." + level + ") has " + data.BankPages.size() + " pages");

            for (Map.Entry<Integer, List<ItemStack>> entry : data.BankPages.entrySet()) {
                int pageNum = entry.getKey();
                List<ItemStack> pageItems = entry.getValue();

                if (pageItems == null || pageItems.isEmpty()) continue;

                // Check if page has any non-empty items
                boolean hasItems = pageItems.stream().anyMatch(s -> s != null && !s.isEmpty());
                if (hasItems) {
                    results.add(new SearchResult(characterId, nickname, level, pageNum, pageItems, pageItems));
                }
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
