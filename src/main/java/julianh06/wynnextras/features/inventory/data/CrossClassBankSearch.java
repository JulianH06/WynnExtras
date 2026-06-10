package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.core.WynnExtras;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
    private static final int SEARCH_THREADS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final long CLASS_SELECTION_WEAPON_CACHE_MS = 30000L;
    private static final ExecutorService SEARCH_COORDINATOR = Executors.newSingleThreadExecutor(daemonThreadFactory("WynnExtras Bank Search Coordinator"));
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newFixedThreadPool(SEARCH_THREADS, daemonThreadFactory("WynnExtras Bank Search"));
    private static final Map<Path, CachedWeaponData> CLASS_SELECTION_WEAPON_CACHE = new ConcurrentHashMap<>();

    public record SearchRequest(
            Path configDir,
            String currentCharacterId,
            String query,
            boolean includeCurrentCharacter,
            boolean includeAccountBank,
            boolean allPages,
            Map<Integer, List<ItemStack>> accountBankPages,
            int accountLastPage
    ) {}

    private record CachedWeaponData(List<CharacterWeaponData> characters, long loadedAtMs) {}
    private record CharacterWeaponData(String characterId, String nickname, int level, ItemStack weapon,
                                       boolean weaponInInventory, long modifiedAtMs) {}

    /**
     * Result of a cross-class search
     */
    public static class SearchResult {
        public enum Type {
            BANK_PAGE,
            PLAYER_INVENTORY
        }

        public final String characterId;
        public final String characterNickname; // e.g., "Dark Wizard", "Archer", etc.
        public final int characterLevel; // Combat level of the character
        public final int pageNumber;
        public final List<ItemStack> matchingItems;
        public final List<ItemStack> pageItems;
        public final List<ItemStack> armorItems;
        public final Type type;

        public SearchResult(String characterId, String characterNickname, int characterLevel, int pageNumber, List<ItemStack> matchingItems, List<ItemStack> pageItems) {
            this(characterId, characterNickname, characterLevel, pageNumber, matchingItems, pageItems, Collections.emptyList(), Type.BANK_PAGE);
        }

        public SearchResult(String characterId, String characterNickname, int characterLevel, int pageNumber, List<ItemStack> matchingItems, List<ItemStack> pageItems, List<ItemStack> armorItems, Type type) {
            this.characterId = characterId;
            this.characterNickname = characterNickname;
            this.characterLevel = characterLevel;
            this.pageNumber = pageNumber;
            this.matchingItems = matchingItems;
            this.pageItems = pageItems;
            this.armorItems = armorItems;
            this.type = type;
        }
    }

    /**
     * Search across all character banks for items matching the query.
     * @param query The search query (without the @ prefix)
     * @return List of search results from all characters
     */
    public static List<SearchResult> searchAllCharacters(String query) {
        return search(createRequest(query, false, false, false));
    }

    public static SearchRequest createRequest(String query, boolean includeCurrentCharacter, boolean includeAccountBank, boolean allPages) {
        if (McUtils.player() == null) return null;

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());

        Map<Integer, List<ItemStack>> accountBankPages = includeAccountBank
                ? snapshotAccountBankPages()
                : Collections.emptyMap();
        int accountLastPage = includeAccountBank && AccountBankData.INSTANCE != null
                ? AccountBankData.INSTANCE.getLastPage()
                : 0;

        return new SearchRequest(
                configDir,
                BankOverlay.currentCharacterID,
                query == null ? "" : query,
                includeCurrentCharacter,
                includeAccountBank,
                allPages,
                accountBankPages,
                accountLastPage
        );
    }

    public static CompletableFuture<List<SearchResult>> searchAsync(SearchRequest request) {
        if (request == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> search(request), SEARCH_COORDINATOR);
    }

    public static List<SearchResult> search(SearchRequest request) {
        List<SearchResult> results = new ArrayList<>();
        if (request == null) return results;

        SearchQueryParser.ParsedQuery parsedQuery = request.allPages()
                ? null
                : SearchQueryParser.parse(request.query());

        if (request.includeAccountBank()) {
            results.addAll(request.allPages()
                    ? getAccountBankPages(request.accountBankPages(), request.accountLastPage())
                    : searchAccountBank(request.accountBankPages(), parsedQuery));
        }

        if (request.configDir() == null || !Files.exists(request.configDir())) return results;

        List<Path> bankFiles = listCharacterBankFiles(request.configDir());
        if (bankFiles.isEmpty()) return results;

        List<CompletableFuture<List<SearchResult>>> futures = bankFiles.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> searchCharacterFile(file, request, parsedQuery), SEARCH_EXECUTOR))
                .toList();

        for (CompletableFuture<List<SearchResult>> future : futures) {
            try {
                results.addAll(future.join());
            } catch (CompletionException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Error searching character bank file: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Load a character bank file and search for matching items
     */
    private static List<SearchResult> searchCharacterBank(Path file, String characterId, SearchQueryParser.ParsedQuery query, String currentCharacterId) {
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

            if (!characterId.equals(currentCharacterId)) {
                SearchResult inventoryResult = searchPlayerInventory(characterId, nickname, level, data, query);
                if (inventoryResult != null) results.add(inventoryResult);
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

    public static ItemStack findLastHeldWeaponForClassSelection(String stableId, String name, String classType, int level,
                                                                boolean requireUniqueBankMatch) {
        if (McUtils.player() == null) return ItemStack.EMPTY;

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString());
        if (!Files.exists(configDir)) return ItemStack.EMPTY;

        return findLastHeldWeapon(configDir, stableId, name, classType, level, requireUniqueBankMatch);
    }

    public static void invalidateClassSelectionWeaponCache() {
        CLASS_SELECTION_WEAPON_CACHE.clear();
    }

    /**
     * Get ALL pages from ALL other characters (for just @ search with no filter)
     */
    public static List<SearchResult> getAllCharacterPages() {
        return search(createRequest("", false, false, true));
    }

    /**
     * Get ALL pages from ALL characters including the current one, with account bank on top
     */
    public static List<SearchResult> getAllCharacterPagesIncludingCurrent() {
        return search(createRequest("", true, true, true));
    }

    /**
     * Search across all character banks including the current one, with account bank on top
     */
    public static List<SearchResult> searchAllCharactersIncludingCurrent(String query) {
        return search(createRequest(query, true, true, false));
    }

    /**
     * Get all account bank pages as SearchResults
     */
    public static List<SearchResult> getAccountBankPages() {
        return getAccountBankPages(snapshotAccountBankPages(), AccountBankData.INSTANCE.getLastPage());
    }

    private static List<SearchResult> getAccountBankPages(Map<Integer, List<ItemStack>> accountBankPages, int accountLastPage) {
        List<SearchResult> results = new ArrayList<>();
        try {
            if (accountBankPages == null) return results;

            int pageCount = Math.min(Math.max(accountLastPage, accountBankPages.size()), ACCOUNT_BANK_MAX_PAGES);
            for (int pageNum = 0; pageNum < pageCount; pageNum++) {
                List<ItemStack> pageItems = accountBankPages.get(pageNum);
                if (pageItems == null) pageItems = Collections.emptyList();
                results.add(new SearchResult("__account__", "Account Bank", 0, pageNum, pageItems, pageItems));
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error loading account bank pages: " + e.getMessage());
        }
        return results;
    }

    private static ItemStack findLastHeldWeapon(Path configDir, String stableId, String name, String classType, int level,
                                                boolean requireUniqueBankMatch) {
        List<CharacterWeaponData> bankCharacters = getClassSelectionWeaponData(configDir);
        int bestScore = 0;
        int bestCount = 0;
        ItemStack bestWeapon = ItemStack.EMPTY;
        long bestModifiedAtMs = Long.MIN_VALUE;

        for (CharacterWeaponData character : bankCharacters) {
            int score = scoreClassSelectionBankMatch(character.characterId(), character.nickname(), character.level(),
                    stableId, name, classType, level);
            if (score <= 0) continue;

            if (score > bestScore) {
                bestScore = score;
                bestCount = 1;
                bestWeapon = character.weaponInInventory() ? character.weapon().copy() : ItemStack.EMPTY;
                bestModifiedAtMs = character.modifiedAtMs();
            } else if (score == bestScore) {
                bestCount++;
                if (!requireUniqueBankMatch && shouldPreferHeldWeaponCandidate(character, bestWeapon, bestModifiedAtMs)) {
                    bestWeapon = character.weapon().copy();
                    bestModifiedAtMs = character.modifiedAtMs();
                }
            }
        }

        if (bestScore < 70) return ItemStack.EMPTY;
        if (requireUniqueBankMatch && bestCount != 1) return ItemStack.EMPTY;
        return bestWeapon;
    }

    private static boolean shouldPreferHeldWeaponCandidate(CharacterWeaponData candidate, ItemStack bestWeapon, long bestModifiedAtMs) {
        if (!candidate.weaponInInventory()) return false;
        if (bestWeapon == null || bestWeapon.isEmpty()) return true;
        return candidate.modifiedAtMs() > bestModifiedAtMs;
    }

    private static List<CharacterWeaponData> getClassSelectionWeaponData(Path configDir) {
        long now = System.currentTimeMillis();
        CachedWeaponData cached = CLASS_SELECTION_WEAPON_CACHE.get(configDir);
        if (cached != null && now - cached.loadedAtMs() < CLASS_SELECTION_WEAPON_CACHE_MS) {
            return cached.characters();
        }

        List<CharacterWeaponData> characters = new ArrayList<>();
        for (Path file : listCharacterBankFiles(configDir)) {
            String characterId = getCharacterId(file);
            if (isNullClassName(characterId)) continue;

            try (Reader reader = Files.newBufferedReader(file)) {
                BankData data = BankData.getGson().fromJson(reader, CharacterBankData.class);
                if (data == null || isInvalidCharacterBank(characterId, data)) continue;

                ItemStack weapon = data.getLastHeldWeapon();
                boolean weaponInInventory = weapon != null && !weapon.isEmpty()
                        && hasSavedPlayerInventory(data)
                        && containsStack(data.getPlayerInventory(), weapon);
                long modifiedAtMs = Files.getLastModifiedTime(file).toMillis();
                characters.add(new CharacterWeaponData(
                        characterId,
                        data.getCharacterNickname(),
                        data.getCharacterLevel(),
                        weapon == null ? ItemStack.EMPTY : weapon.copy(),
                        weaponInInventory,
                        modifiedAtMs
                ));
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
            }
        }

        CLASS_SELECTION_WEAPON_CACHE.put(configDir, new CachedWeaponData(characters, now));
        return characters;
    }

    private static int scoreClassSelectionBankMatch(String characterId, String nickname, int bankLevel, String stableId,
                                                    String name, String classType, int level) {
        String normalizedStableId = safeLower(stableId).replace("-", "");
        String normalizedCharacterId = safeLower(characterId).replace("-", "");
        if (!normalizedStableId.isEmpty() && !normalizedCharacterId.isEmpty()
                && (normalizedStableId.equals(normalizedCharacterId)
                || normalizedStableId.contains(normalizedCharacterId)
                || normalizedCharacterId.contains(normalizedStableId))) return 100;

        if (level > 0 && bankLevel == level) {
            String normalizedNickname = normalizeClassSelectionMatchText(nickname);
            String normalizedName = normalizeClassSelectionMatchText(name);
            String normalizedClassType = normalizeClassSelectionMatchText(classType);
            if (!normalizedNickname.isEmpty()
                    && (normalizedNickname.equals(normalizedName) || normalizedNickname.equals(normalizedClassType))) {
                return 70;
            }
        }
        return 0;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeClassSelectionMatchText(String value) {
        return safeLower(value).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Search account bank pages with a query
     */
    private static List<SearchResult> searchAccountBank(SearchQueryParser.ParsedQuery query) {
        return searchAccountBank(snapshotAccountBankPages(), query);
    }

    private static List<SearchResult> searchAccountBank(Map<Integer, List<ItemStack>> accountBankPages, SearchQueryParser.ParsedQuery query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            if (accountBankPages == null) return results;

            for (Map.Entry<Integer, List<ItemStack>> entry : accountBankPages.entrySet()) {
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

    private static List<SearchResult> searchCharacterFile(Path file, SearchRequest request, SearchQueryParser.ParsedQuery parsedQuery) {
        String characterId = getCharacterId(file);

        if (isNullClassName(characterId)) return Collections.emptyList();
        if (!request.includeCurrentCharacter() && characterId.equals(request.currentCharacterId())) {
            return Collections.emptyList();
        }

        return request.allPages()
                ? loadAllPagesFromCharacter(file, characterId, request.currentCharacterId())
                : searchCharacterBank(file, characterId, parsedQuery, request.currentCharacterId());
    }

    private static List<Path> listCharacterBankFiles(Path configDir) {
        try (Stream<Path> files = Files.list(configDir)) {
            return files
                    .filter(CrossClassBankSearch::isCharacterBankFile)
                    .toList();
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error listing character bank files: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static Map<Integer, List<ItemStack>> snapshotAccountBankPages() {
        AccountBankData data = AccountBankData.INSTANCE;
        if (data == null || data.getBankPages() == null) return Collections.emptyMap();

        return data.getBankPages().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> copyItemList(entry.getValue())
                ));
    }

    private static List<ItemStack> copyItemList(List<ItemStack> items) {
        if (items == null) return Collections.emptyList();
        return items.stream()
                .map(stack -> stack == null ? null : stack.copy())
                .collect(Collectors.toList());
    }

    private static ThreadFactory daemonThreadFactory(String name) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Load ALL pages from a character bank file (no filtering)
     */
    private static List<SearchResult> loadAllPagesFromCharacter(Path file, String characterId, String currentCharacterId) {
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
            if (!characterId.equals(currentCharacterId) && hasSavedPlayerInventory(data)) {
                results.add(new SearchResult(
                        characterId,
                        nickname,
                        level,
                        -1,
                        data.getPlayerInventory(),
                        data.getPlayerInventory(),
                        data.getPlayerArmor(),
                        SearchResult.Type.PLAYER_INVENTORY
                ));
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error reading character bank file " + file + ": " + e.getMessage());
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing character bank file " + file + ": " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private static SearchResult searchPlayerInventory(String characterId, String nickname, int level, BankData data, SearchQueryParser.ParsedQuery query) {
        if (!hasSavedPlayerInventory(data)) return null;

        List<ItemStack> matchingItems = new ArrayList<>();
        for (ItemStack stack : data.getPlayerInventory()) {
            if (matchesStack(stack, query)) matchingItems.add(stack);
        }
        for (ItemStack stack : data.getPlayerArmor()) {
            if (matchesStack(stack, query)) matchingItems.add(stack);
        }

        if (matchingItems.isEmpty()) return null;
        return new SearchResult(
                characterId,
                nickname,
                level,
                -1,
                matchingItems,
                data.getPlayerInventory(),
                data.getPlayerArmor(),
                SearchResult.Type.PLAYER_INVENTORY
        );
    }

    private static boolean matchesStack(ItemStack stack, SearchQueryParser.ParsedQuery query) {
        if (stack == null || stack.isEmpty()) return false;
        WynnItem wynnItem = null;
        Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
        if (optWynnItem.isPresent()) {
            wynnItem = optWynnItem.get();
        }
        return SearchQueryParser.matches(stack, wynnItem, query);
    }

    private static boolean hasSavedPlayerInventory(BankData data) {
        return hasAnyItem(data.getPlayerInventory()) || hasAnyItem(data.getPlayerArmor());
    }

    private static boolean hasAnyItem(List<ItemStack> items) {
        if (items == null) return false;
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) return true;
        }
        return false;
    }

    private static boolean containsStack(List<ItemStack> items, ItemStack target) {
        if (items == null || target == null || target.isEmpty()) return false;
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getCount() == target.getCount() && ItemStack.areItemsAndComponentsEqual(stack, target)) return true;
        }
        return false;
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
        return isNullClassName(characterId);
    }

    private static boolean isNullClassName(String value) {
        return value != null && value.trim().equalsIgnoreCase("null");
    }
}
