package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.core.WynnExtras;
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
    private static final String CHARACTER_BANK_PREFIX = "characterbank_";
    private static final String JSON_SUFFIX = ".json";
    private static final int ACCOUNT_BANK_MAX_PAGES = 21;
    private static final int CHARACTER_BANK_MAX_PAGES = 12;

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
            files.filter(CrossClassBankSearch::isCharacterBankFile)
                 .forEach(file -> {
                     String characterId = getCharacterId(file);

                     if (isNullClassName(characterId)) return;

                     // Skip current character - it's already being searched normally
                     if (characterId.equals(currentCharacterId)) return;

                     // Load and search this character's bank
                     List<SearchResult> characterResults = searchCharacterBank(file, characterId, parsedQuery);
                     results.addAll(characterResults);
                 });
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
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
            if (data == null || data.getBankPages() == null) return results;
            if (isInvalidCharacterBank(characterId, data)) return results;

            String nickname = data.getCharacterNickname();
            int level = data.getCharacterLevel();

            for (Map.Entry<Integer, List<ItemStack>> entry : data.getBankPages().entrySet()) {
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
            WynnExtras.LOGGER.error("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
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
            files.filter(CrossClassBankSearch::isCharacterBankFile)
                 .forEach(file -> {
                     String characterId = getCharacterId(file);
                     if (isNullClassName(characterId)) return;
                     ids.add(characterId);
                 });
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
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
            WynnExtras.LOGGER.info("[WynnExtras] Config dir doesn't exist: " + configDir);
            return results;
        }

        String currentCharacterId = BankOverlay.currentCharacterID;
        WynnExtras.LOGGER.info("[WynnExtras] Current character ID: " + currentCharacterId);

        try (Stream<Path> files = Files.list(configDir)) {
            List<Path> bankFiles = files
                    .filter(CrossClassBankSearch::isCharacterBankFile)
                    .toList();

            WynnExtras.LOGGER.info("[WynnExtras] Found " + bankFiles.size() + " character bank files");

            for (Path file : bankFiles) {
                String characterId = getCharacterId(file);

                if (isNullClassName(characterId)) continue;

                // Skip current character
                if (characterId.equals(currentCharacterId)) {
                    WynnExtras.LOGGER.info("[WynnExtras] Skipping current character: " + characterId);
                    continue;
                }

                WynnExtras.LOGGER.info("[WynnExtras] Loading pages from character: " + characterId);

                // Load all pages from this character
                List<SearchResult> characterPages = loadAllPagesFromCharacter(file, characterId);
                results.addAll(characterPages);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        WynnExtras.LOGGER.info("[WynnExtras] Total cross-class pages: " + results.size());
        return results;
    }

    /**
     * Get ALL pages from ALL characters including the current one, with account bank on top
     */
    public static List<SearchResult> getAllCharacterPagesIncludingCurrent() {
        List<SearchResult> results = new ArrayList<>();

        if (McUtils.player() == null) return results;

        // Account bank first
        results.addAll(getAccountBankPages());

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        if (!Files.exists(configDir)) return results;

        try (Stream<Path> files = Files.list(configDir)) {
            List<Path> bankFiles = files
                    .filter(CrossClassBankSearch::isCharacterBankFile)
                    .toList();

            for (Path file : bankFiles) {
                String characterId = getCharacterId(file);
                if (isNullClassName(characterId)) continue;

                List<SearchResult> characterPages = loadAllPagesFromCharacter(file, characterId);
                results.addAll(characterPages);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        return results;
    }

    /**
     * Search across all character banks including the current one, with account bank on top
     */
    public static List<SearchResult> searchAllCharactersIncludingCurrent(String query) {
        List<SearchResult> results = new ArrayList<>();

        if (McUtils.player() == null) return results;

        // Search account bank first
        SearchQueryParser.ParsedQuery parsedQuery = SearchQueryParser.parse(query);
        results.addAll(searchAccountBank(parsedQuery));

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        if (!Files.exists(configDir)) return results;

        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(CrossClassBankSearch::isCharacterBankFile)
                 .forEach(file -> {
                     String characterId = getCharacterId(file);
                     if (isNullClassName(characterId)) return;

                     List<SearchResult> characterResults = searchCharacterBank(file, characterId, parsedQuery);
                     results.addAll(characterResults);
                 });
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
        }

        return results;
    }

    /**
     * Get all account bank pages as SearchResults
     */
    public static List<SearchResult> getAccountBankPages() {
        List<SearchResult> results = new ArrayList<>();
        try {
            AccountBankData data = AccountBankData.INSTANCE;
            if (data == null || data.getBankPages() == null) return results;

            int pageCount = Math.min(Math.max(data.getLastPage(), data.getBankPages().size()), ACCOUNT_BANK_MAX_PAGES);
            for (int pageNum = 0; pageNum < pageCount; pageNum++) {
                List<ItemStack> pageItems = data.getBankPages().get(pageNum);
                if (pageItems == null) pageItems = Collections.emptyList();
                results.add(new SearchResult("__account__", "Account Bank", 0, pageNum, pageItems, pageItems));
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error loading account bank pages: " + e.getMessage());
        }
        return results;
    }

    /**
     * Search account bank pages with a query
     */
    private static List<SearchResult> searchAccountBank(SearchQueryParser.ParsedQuery query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            AccountBankData data = AccountBankData.INSTANCE;
            if (data == null || data.getBankPages() == null) return results;

            for (Map.Entry<Integer, List<ItemStack>> entry : data.getBankPages().entrySet()) {
                int pageNum = entry.getKey();
                List<ItemStack> pageItems = entry.getValue();
                if (pageItems == null) continue;

                List<ItemStack> matchingItems = new ArrayList<>();
                for (ItemStack stack : pageItems) {
                    if (stack == null || stack.isEmpty()) continue;
                    WynnItem wynnItem = null;
                    Optional<WynnItem> opt = Models.Item.getWynnItem(stack);
                    if (opt.isPresent()) wynnItem = opt.get();
                    if (SearchQueryParser.matches(stack, wynnItem, query)) {
                        matchingItems.add(stack);
                    }
                }
                if (!matchingItems.isEmpty()) {
                    results.add(new SearchResult("__account__", "Account Bank", 0, pageNum, matchingItems, pageItems));
                }
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error searching account bank: " + e.getMessage());
        }
        return results;
    }

    /**
     * Load ALL pages from a character bank file (no filtering)
     */
    private static List<SearchResult> loadAllPagesFromCharacter(Path file, String characterId) {
        List<SearchResult> results = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            BankData data = BankData.getGson().fromJson(reader, CharacterBankData.class);
            if (data == null || data.getBankPages() == null) {
                WynnExtras.LOGGER.info("[WynnExtras] No bank data for character: " + characterId);
                return results;
            }
            if (isInvalidCharacterBank(characterId, data)) {
                WynnExtras.LOGGER.info("[WynnExtras] Skipping invalid character bank: " + characterId);
                return results;
            }

            String nickname = data.getCharacterNickname();
            int level = data.getCharacterLevel();
            WynnExtras.LOGGER.info("[WynnExtras] Character " + characterId + " (" + nickname + " Lv." + level + ") has " + data.getBankPages().size() + " pages");

            int pageCount = Math.min(Math.max(data.getLastPage(), data.getBankPages().size()), CHARACTER_BANK_MAX_PAGES);
            for (int pageNum = 0; pageNum < pageCount; pageNum++) {
                List<ItemStack> pageItems = data.getBankPages().get(pageNum);
                if (pageItems == null) pageItems = Collections.emptyList();
                results.add(new SearchResult(characterId, nickname, level, pageNum, pageItems, pageItems));
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private static boolean isCharacterBankFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(CHARACTER_BANK_PREFIX) && fileName.endsWith(JSON_SUFFIX);
    }

    private static String getCharacterId(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.substring(CHARACTER_BANK_PREFIX.length(), fileName.length() - JSON_SUFFIX.length());
    }

    private static boolean isInvalidCharacterBank(String characterId, BankData data) {
        return isNullClassName(characterId) || isNullClassName(data.getCharacterNickname());
    }

    private static boolean isNullClassName(String value) {
        return value != null && value.trim().equalsIgnoreCase("null");
    }
}
