package julianh06.wynnextras.features.shoppinglist.service;

import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.data.BankData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.inventory.data.MiscBucketData;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingEntry;
import julianh06.wynnextras.features.shoppinglist.model.RequirementType;
import julianh06.wynnextras.features.shoppinglist.util.IngredientNormalizer;
import julianh06.wynnextras.features.shoppinglist.util.ShoppingListMaterialNameNormalizer;
import com.wynntils.core.components.Models;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ShoppingListHaveCountService {
    private static final long SIGNATURE_CHECK_INTERVAL_MS = 350L;
    private static final long BANK_STABILITY_WINDOW_MS = 500L;
    private static final int STACK_SNAPSHOT_CACHE_LIMIT = 4096;
    private static final int VISIBLE_PAGE_KEY = Integer.MIN_VALUE;

    private final Map<StackSnapshotKey, StackSnapshot> stackSnapshotCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<StackSnapshotKey, StackSnapshot> eldest) {
            return size() > STACK_SNAPSHOT_CACHE_LIMIT;
        }
    };
    private SourceSignature lastObservedSignature;
    private SourceSignature cachedSnapshotSignature;
    private SourceSnapshot cachedSnapshot;
    private long lastSignatureCheckMs = Long.MIN_VALUE;
    private long lastSignatureChangeMs = Long.MIN_VALUE;

    public ShoppingListHaveCount count(ShoppingEntry entry) {
        return count(entry, cachedSnapshot().snapshot());
    }

    public StackSnapshot snapshot(ItemStack stack) {
        return toSnapshot(stack, MinecraftClient.getInstance().player);
    }

    public ShoppingListHaveCount count(ShoppingEntry entry, SourceSnapshot snapshot) {
        if (entry == null || snapshot == null) {
            return new ShoppingListHaveCount(0, 0, 0, false, false);
        }

        int inventory = countStacks(entry, snapshot.inventoryStacks());
        int accountBank = countStackPages(entry, snapshot.accountBankPages());
        int characterBank = countStackPages(entry, snapshot.characterBankPages());
        int miscBucket = countStackPages(entry, snapshot.miscBucketPages());
        return new ShoppingListHaveCount(
                inventory,
                accountBank,
                characterBank,
                miscBucket,
                snapshot.bankCacheAvailable(),
                snapshot.bankCachePossiblyIncomplete());
    }

    public ShoppingListHaveCount countSnapshots(
            ShoppingEntry entry,
            Collection<StackSnapshot> inventoryStacks,
            Map<Integer, List<StackSnapshot>> accountBankPages,
            Map<Integer, List<StackSnapshot>> characterBankPages,
            boolean bankCachePossiblyIncomplete) {
        return countSnapshots(entry, inventoryStacks, accountBankPages, characterBankPages, Map.of(),
                bankCachePossiblyIncomplete);
    }

    public ShoppingListHaveCount countSnapshots(
            ShoppingEntry entry,
            Collection<StackSnapshot> inventoryStacks,
            Map<Integer, List<StackSnapshot>> accountBankPages,
            Map<Integer, List<StackSnapshot>> characterBankPages,
            Map<Integer, List<StackSnapshot>> miscBucketPages,
            boolean bankCachePossiblyIncomplete) {
        boolean hasAccountPages = hasCachedPages(accountBankPages);
        boolean hasCharacterPages = hasCachedPages(characterBankPages);
        boolean hasMiscPages = hasCachedPages(miscBucketPages);
        boolean bankCacheAvailable = hasAccountPages || hasCharacterPages || hasMiscPages;
        SourceSnapshot snapshot = new SourceSnapshot(
                copyStacks(inventoryStacks),
                copyPages(accountBankPages),
                copyPages(characterBankPages),
                copyPages(miscBucketPages),
                true,
                bankCacheAvailable,
                bankCacheAvailable && (bankCachePossiblyIncomplete || !hasAccountPages || !hasCharacterPages));
        return count(entry, snapshot);
    }

    public SourceSnapshot snapshot() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client == null ? null : client.player;
            InventorySource inventorySource = liveInventorySource(player);
            if (!inventorySource.liveInventoryAvailable() && BankOverlay.hasValidCurrentCharacterId()) {
                inventorySource = savedInventorySource(CharacterBankData.INSTANCE, player);
            }

            VisibleBankSource visibleBankSource = visibleBankSource(client, player);
            Map<Integer, List<StackSnapshot>> accountPages = bankPageSnapshots(AccountBankData.INSTANCE, player);
            accountPages = withVisibleBankPage(accountPages, AccountBankData.INSTANCE, visibleBankSource, player);
            Map<Integer, List<StackSnapshot>> characterPages = BankOverlay.hasValidCurrentCharacterId()
                    ? bankPageSnapshots(CharacterBankData.INSTANCE, player)
                    : Map.of();
            characterPages = withVisibleBankPage(characterPages, CharacterBankData.INSTANCE, visibleBankSource, player);
            Map<Integer, List<StackSnapshot>> miscPages = bankPageSnapshots(MiscBucketData.INSTANCE, player);
            miscPages = withVisibleBankPage(miscPages, MiscBucketData.INSTANCE, visibleBankSource, player);
            boolean hasAccountPages = hasCachedPages(accountPages);
            boolean hasCharacterPages = hasCachedPages(characterPages);
            boolean hasMiscPages = hasCachedPages(miscPages);
            boolean bankCacheAvailable = hasAccountPages || hasCharacterPages || hasMiscPages;
            boolean bankCachePossiblyIncomplete = bankCacheAvailable && (!hasAccountPages || !hasCharacterPages
                    || pagesPossiblyIncomplete(AccountBankData.INSTANCE)
                    || pagesPossiblyIncomplete(CharacterBankData.INSTANCE)
                    || pagesPossiblyIncomplete(MiscBucketData.INSTANCE));

            return new SourceSnapshot(
                    inventorySource.stacks(),
                    accountPages,
                    characterPages,
                    miscPages,
                    inventorySource.liveInventoryAvailable(),
                    bankCacheAvailable,
                    bankCachePossiblyIncomplete);
        } catch (RuntimeException ex) {
            return new SourceSnapshot(List.of(), Map.of(), Map.of(), Map.of(), false, false, false);
        }
    }

    public SnapshotResult cachedSnapshot() {
        return cachedSnapshot(System.currentTimeMillis());
    }

    public SnapshotResult cachedSnapshot(boolean allowRefresh) {
        return cachedSnapshot(allowRefresh, System.currentTimeMillis());
    }

    public SnapshotResult cachedSnapshot(boolean allowRefresh, long nowMs) {
        if (!allowRefresh) {
            return new SnapshotResult(snapshotOrEmpty(cachedSnapshot), true, true);
        }
        return cachedSnapshot(nowMs);
    }

    public SnapshotResult cachedSnapshot(long nowMs) {
        try {
            if (shouldReuseSnapshotBeforeSignatureCheck(cachedSnapshot, nowMs, lastSignatureCheckMs)) {
                return new SnapshotResult(cachedSnapshot,
                        isCachedSnapshotKnownStale(cachedSnapshotSignature, lastObservedSignature),
                        true);
            }

            SourceSignature signature = sourceSignature();
            lastSignatureCheckMs = nowMs;
            if (!Objects.equals(signature, lastObservedSignature)) {
                lastObservedSignature = signature;
                lastSignatureChangeMs = nowMs;
                if (cachedSnapshot != null) {
                    return new SnapshotResult(cachedSnapshot, true);
                }
            }

            if (cachedSnapshot != null && !isSourceStable(nowMs, lastSignatureChangeMs)) {
                return new SnapshotResult(cachedSnapshot, true);
            }

            if (cachedSnapshot == null || !Objects.equals(signature, cachedSnapshotSignature)) {
                cachedSnapshot = snapshot();
                cachedSnapshotSignature = signature;
            }
            return new SnapshotResult(cachedSnapshot, false);
        } catch (RuntimeException ex) {
            SourceSnapshot fallback = cachedSnapshot == null
                    ? new SourceSnapshot(List.of(), Map.of(), Map.of(), Map.of(), false, false, false)
                    : cachedSnapshot;
            return new SnapshotResult(fallback, false);
        }
    }

    public void invalidateCache() {
        cachedSnapshot = null;
        cachedSnapshotSignature = null;
        lastObservedSignature = null;
        lastSignatureCheckMs = Long.MIN_VALUE;
        lastSignatureChangeMs = Long.MIN_VALUE;
        stackSnapshotCache.clear();
    }

    static boolean shouldReuseSnapshotBeforeSignatureCheck(SourceSnapshot snapshot, long nowMs, long lastSignatureCheckMs) {
        return snapshot != null
                && lastSignatureCheckMs != Long.MIN_VALUE
                && nowMs - lastSignatureCheckMs < SIGNATURE_CHECK_INTERVAL_MS;
    }

    static boolean isSourceStable(long nowMs, long lastSignatureChangeMs) {
        return lastSignatureChangeMs == Long.MIN_VALUE
                || nowMs - lastSignatureChangeMs >= BANK_STABILITY_WINDOW_MS;
    }

    static boolean isCachedSnapshotKnownStale(SourceSignature cachedSnapshotSignature,
                                              SourceSignature lastObservedSignature) {
        return lastObservedSignature != null && !Objects.equals(cachedSnapshotSignature, lastObservedSignature);
    }

    static SourceSnapshot snapshotOrEmpty(SourceSnapshot snapshot) {
        return snapshot == null ? emptySnapshot() : snapshot;
    }

    static SourceSnapshot emptySnapshot() {
        return new SourceSnapshot(List.of(), Map.of(), Map.of(), Map.of(), false, false, false);
    }

    public static int countStacks(ShoppingEntry entry, Collection<StackSnapshot> stacks) {
        if (entry == null || stacks == null || stacks.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (StackSnapshot stack : stacks) {
            if (stack != null && stack.count() > 0 && matches(entry, stack)) {
                count += stack.count();
            }
        }
        return count;
    }

    public static boolean matches(ShoppingEntry entry, StackSnapshot stack) {
        if (entry == null || stack == null || stack.displayName().isBlank()) {
            return false;
        }
        if (entry.type() == RequirementType.MATERIAL) {
            return matchesMaterial(entry, stack);
        }
        return ingredientKeys(entry).contains(key(stack.displayName()));
    }

    private static boolean matchesMaterial(ShoppingEntry entry, StackSnapshot stack) {
        int tier = Math.max(1, entry.materialTier());
        String displayName = stack.displayName();
        Integer detectedTier = stack.materialTier() != null
                ? stack.materialTier()
                : ShoppingListMaterialNameNormalizer.detectTier(displayName, stack.loreLines());
        if (detectedTier == null || detectedTier != tier) {
            return false;
        }
        return materialBaseKeys(entry).contains(ShoppingListMaterialNameNormalizer.key(displayName));
    }

    private static int countStackPages(ShoppingEntry entry, Map<Integer, List<StackSnapshot>> pages) {
        if (pages == null || pages.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (List<StackSnapshot> page : pages.values()) {
            count += countStacks(entry, page);
        }
        return count;
    }

    private static Set<String> ingredientKeys(ShoppingEntry entry) {
        return keys(
                IngredientNormalizer.key(entry.id()),
                IngredientNormalizer.key(entry.displayName()));
    }

    private static Set<String> materialBaseKeys(ShoppingEntry entry) {
        return keys(
                ShoppingListMaterialNameNormalizer.key(entry.id()),
                ShoppingListMaterialNameNormalizer.key(entry.displayName()));
    }

    private static Set<String> keys(String... values) {
        Set<String> keys = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                keys.add(value);
            }
        }
        return Set.copyOf(keys);
    }

    private static String key(String value) {
        return IngredientNormalizer.key(ShoppingListTextCleaner.clean(value));
    }

    private InventorySource liveInventorySource(PlayerEntity player) {
        if (player == null) {
            return new InventorySource(List.of(), false);
        }

        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(player.getInventory().getMainStacks());
        stacks.add(player.getOffHandStack());
        stacks.add(player.getEquippedStack(EquipmentSlot.HEAD));
        stacks.add(player.getEquippedStack(EquipmentSlot.CHEST));
        stacks.add(player.getEquippedStack(EquipmentSlot.LEGS));
        stacks.add(player.getEquippedStack(EquipmentSlot.FEET));
        return new InventorySource(toSnapshots(stacks, player), true);
    }

    private InventorySource savedInventorySource(BankData data, PlayerEntity player) {
        if (data == null) {
            return new InventorySource(List.of(), false);
        }

        List<ItemStack> stacks = new ArrayList<>();
        if (data.getPlayerInventory() != null) {
            stacks.addAll(data.getPlayerInventory());
        }
        if (data.getPlayerArmor() != null) {
            stacks.addAll(data.getPlayerArmor());
        }
        return new InventorySource(toSnapshots(stacks, player), false);
    }

    private Map<Integer, List<StackSnapshot>> bankPageSnapshots(BankData data, PlayerEntity player) {
        if (data == null || data.getBankPages() == null || data.getBankPages().isEmpty()) {
            return Map.of();
        }

        Map<Integer, List<StackSnapshot>> snapshots = new LinkedHashMap<>();
        data.getBankPages().forEach((page, stacks) -> snapshots.put(page, toSnapshots(stacks, player)));
        return Map.copyOf(snapshots);
    }

    private Map<Integer, List<StackSnapshot>> withVisibleBankPage(
            Map<Integer, List<StackSnapshot>> cachedPages,
            BankData expectedData,
            VisibleBankSource visibleSource,
            PlayerEntity player) {
        if (expectedData == null || visibleSource == null || visibleSource.data() != expectedData
                || visibleSource.stacks().isEmpty()) {
            return copyPages(cachedPages);
        }

        List<StackSnapshot> visibleSnapshots = toSnapshots(visibleSource.stacks(), player);
        List<StackSnapshot> cachedPage = cachedPages.get(visibleSource.pageKey());
        if (visibleSnapshots.isEmpty() && cachedPage != null && !cachedPage.isEmpty()) {
            return copyPages(cachedPages);
        }

        Map<Integer, List<StackSnapshot>> merged = new LinkedHashMap<>(copyPages(cachedPages));
        merged.put(visibleSource.pageKey(), visibleSnapshots);
        return Map.copyOf(merged);
    }

    private VisibleBankSource visibleBankSource(MinecraftClient client, PlayerEntity player) {
        if (client == null || player == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return VisibleBankSource.empty();
        }
        BankOverlay.updateOverlayType();
        if (BankOverlay.currentData == null || BankOverlay.shouldWait || BankOverlay2.isBankTypeSwitchInProgress()) {
            return VisibleBankSource.empty();
        }

        List<ItemStack> stacks = visibleContainerStacks(handledScreen.getScreenHandler(), player.getInventory());
        if (stacks.isEmpty()) {
            return VisibleBankSource.empty();
        }
        int pageKey = currentBankPageKey();
        return new VisibleBankSource(BankOverlay.currentData, pageKey, stacks);
    }

    private static int currentBankPageKey() {
        try {
            int currentPage = Models.Bank.getCurrentPage();
            if (currentPage > 0) {
                return currentPage - 1;
            }
        } catch (RuntimeException ignored) {
            // Fall back to the overlay page below.
        }
        return BankOverlay.activeInv >= 0 ? BankOverlay.activeInv : VISIBLE_PAGE_KEY;
    }

    private static List<ItemStack> visibleContainerStacks(ScreenHandler screenHandler, Inventory playerInventory) {
        if (screenHandler == null || playerInventory == null || screenHandler.slots == null) {
            return List.of();
        }

        List<ItemStack> stacks = new ArrayList<>(45);
        for (Slot slot : screenHandler.slots) {
            if (slot == null || slot.inventory == playerInventory) {
                continue;
            }
            stacks.add(slot.getStack().copy());
            if (stacks.size() >= 45) {
                break;
            }
        }
        return stacks.size() < 45 ? List.of() : List.copyOf(stacks);
    }

    private List<StackSnapshot> toSnapshots(Collection<ItemStack> stacks, PlayerEntity player) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }

        List<StackSnapshot> snapshots = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            StackSnapshot snapshot = toSnapshot(stack, player);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return List.copyOf(snapshots);
    }

    private StackSnapshot toSnapshot(ItemStack stack, PlayerEntity player) {
        if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
            return null;
        }

        String displayName = stack.getName().getString();
        StackSnapshotKey key = stackSnapshotKey(stack, displayName);
        StackSnapshot cached = stackSnapshotCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<String> loreLines = shouldReadLoreForTier(displayName)
                ? tooltipLines(stack, player)
                : List.of();
        Integer materialTier = detectedMaterialTier(stack, displayName, loreLines);
        StackSnapshot snapshot = new StackSnapshot(displayName, loreLines, stack.getCount(), materialTier);
        stackSnapshotCache.put(key, snapshot);
        return snapshot;
    }

    private static Integer detectedMaterialTier(ItemStack stack, String displayName, List<String> loreLines) {
        Integer componentTier = detectTierFromComponents(stack);
        if (componentTier != null) {
            return componentTier;
        }
        return ShoppingListMaterialNameNormalizer.detectTier(displayName, loreLines);
    }

    static Integer detectTierFromComponents(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        try {
            return ShoppingListMaterialNameNormalizer.detectTierFromComponentStrings(
                    List.of(String.valueOf(stack.getComponents())));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static boolean shouldReadLoreForTier(String displayName) {
        return ShoppingListMaterialNameNormalizer.needsLoreForTier(displayName);
    }

    private static List<String> tooltipLines(ItemStack stack, PlayerEntity player) {
        List<String> loreLines = new ArrayList<>();
        try {
            for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, player, TooltipType.BASIC)) {
                loreLines.add(line.getString());
            }
        } catch (RuntimeException ignored) {
            loreLines.clear();
        }
        return loreLines.isEmpty() ? List.of() : List.copyOf(loreLines);
    }

    private static boolean pagesPossiblyIncomplete(BankData data) {
        if (data == null || data.getBankPages() == null || data.getBankPages().isEmpty()) {
            return false;
        }

        int expectedPages = Math.max(1, data.getLastPage());
        if (data.getBankPages().size() < expectedPages) {
            return true;
        }
        for (int page = 0; page < expectedPages; page++) {
            if (!data.getBankPages().containsKey(page)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCachedPages(Map<Integer, ?> pages) {
        return pages != null && !pages.isEmpty();
    }

    private static List<StackSnapshot> copyStacks(Collection<StackSnapshot> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        return List.copyOf(stacks);
    }

    private static Map<Integer, List<StackSnapshot>> copyPages(Map<Integer, List<StackSnapshot>> pages) {
        if (pages == null || pages.isEmpty()) {
            return Map.of();
        }

        Map<Integer, List<StackSnapshot>> copy = new LinkedHashMap<>();
        pages.forEach((page, stacks) -> copy.put(page, copyStacks(stacks)));
        return Map.copyOf(copy);
    }

    public SourceSignature sourceSignature() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client == null ? null : client.player;
            VisibleBankSource visibleSource = visibleBankSource(client, player);
            long visibleSignature = visibleBankSignature(visibleSource);
            long accountSignature = combinedSignature(
                    bankSignature(AccountBankData.INSTANCE),
                    visibleSource.data() == AccountBankData.INSTANCE ? visibleSignature : 0);
            long characterSignature = combinedSignature(
                    BankOverlay.hasValidCurrentCharacterId() ? bankSignature(CharacterBankData.INSTANCE) : 0,
                    visibleSource.data() == CharacterBankData.INSTANCE ? visibleSignature : 0);
            long miscBucketSignature = combinedSignature(
                    bankSignature(MiscBucketData.INSTANCE),
                    visibleSource.data() == MiscBucketData.INSTANCE ? visibleSignature : 0);
            return new SourceSignature(
                    inventorySignature(player),
                    accountSignature,
                    characterSignature,
                    miscBucketSignature,
                    BankOverlay.hasValidCurrentCharacterId());
        } catch (RuntimeException ex) {
            return new SourceSignature(0, 0, 0, 0, false);
        }
    }

    private static long combinedSignature(long... parts) {
        long signature = 1;
        if (parts != null) {
            for (long part : parts) {
                signature = signature * 31 + part;
            }
        }
        return signature;
    }

    private static long inventorySignature(PlayerEntity player) {
        if (player == null) {
            return 0;
        }

        long signature = 1;
        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(player.getInventory().getMainStacks());
        stacks.add(player.getOffHandStack());
        stacks.add(player.getEquippedStack(EquipmentSlot.HEAD));
        stacks.add(player.getEquippedStack(EquipmentSlot.CHEST));
        stacks.add(player.getEquippedStack(EquipmentSlot.LEGS));
        stacks.add(player.getEquippedStack(EquipmentSlot.FEET));
        for (ItemStack stack : stacks) {
            signature = signature * 31 + stackSignature(stack);
        }
        return signature;
    }

    private static long bankSignature(BankData data) {
        if (data == null || data.getBankPages() == null || data.getBankPages().isEmpty()) {
            return 0;
        }

        long signature = data.getLastPage();
        for (Map.Entry<Integer, List<ItemStack>> entry : data.getBankPages().entrySet()) {
            signature = signature * 31 + entry.getKey();
            List<ItemStack> stacks = entry.getValue();
            signature = signature * 31 + (stacks == null ? 0 : stacks.size());
            signature = signature * 31 + System.identityHashCode(stacks);
        }
        return signature;
    }

    private static long visibleBankSignature(VisibleBankSource visibleSource) {
        if (visibleSource == null || visibleSource.data() == null || visibleSource.stacks().isEmpty()) {
            return 0;
        }

        List<ItemStack> cachedPage = visibleSource.data().getBankPages().get(visibleSource.pageKey());
        if (sameStacks(cachedPage, visibleSource.stacks())
                || (containsItems(cachedPage) && !containsItems(visibleSource.stacks()))) {
            return 0;
        }

        long signature = 31L * System.identityHashCode(visibleSource.data()) + visibleSource.pageKey();
        for (ItemStack stack : visibleSource.stacks()) {
            signature = signature * 31 + stackSignature(stack);
        }
        return signature;
    }

    private static boolean sameStacks(List<ItemStack> first, List<ItemStack> second) {
        if (first == null || second == null || first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (stackSignature(first.get(i)) != stackSignature(second.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsItems(List<ItemStack> stacks) {
        if (stacks == null) {
            return false;
        }
        return stacks.stream().anyMatch(stack -> stack != null && !stack.isEmpty());
    }

    private static long stackSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return 31L * (31L * Registries.ITEM.getRawId(stack.getItem()) + stack.getCount())
                + componentHash(stack);
    }

    private static StackSnapshotKey stackSnapshotKey(ItemStack stack, String displayName) {
        return new StackSnapshotKey(
                Registries.ITEM.getRawId(stack.getItem()),
                stack.getCount(),
                displayName,
                componentHash(stack));
    }

    private static int componentHash(ItemStack stack) {
        try {
            return stack.getComponents() == null ? 0 : stack.getComponents().hashCode();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private record InventorySource(List<StackSnapshot> stacks, boolean liveInventoryAvailable) {}

    private record VisibleBankSource(BankData data, int pageKey, List<ItemStack> stacks) {
        private VisibleBankSource {
            stacks = stacks == null ? List.of() : List.copyOf(stacks);
        }

        private static VisibleBankSource empty() {
            return new VisibleBankSource(null, VISIBLE_PAGE_KEY, List.of());
        }
    }

    public record SourceSnapshot(
            List<StackSnapshot> inventoryStacks,
            Map<Integer, List<StackSnapshot>> accountBankPages,
            Map<Integer, List<StackSnapshot>> characterBankPages,
            Map<Integer, List<StackSnapshot>> miscBucketPages,
            boolean liveInventoryAvailable,
            boolean bankCacheAvailable,
            boolean bankCachePossiblyIncomplete) {
        public SourceSnapshot {
            inventoryStacks = inventoryStacks == null ? List.of() : List.copyOf(inventoryStacks);
            accountBankPages = copyPages(accountBankPages);
            characterBankPages = copyPages(characterBankPages);
            miscBucketPages = copyPages(miscBucketPages);
        }
    }

    public record SourceSignature(
            long inventory,
            long accountBank,
            long characterBank,
            long miscBucket,
            boolean characterBankCurrent) {}

    public record SnapshotResult(SourceSnapshot snapshot, boolean bankCountsUpdating, boolean signatureCheckThrottled) {
        public SnapshotResult(SourceSnapshot snapshot, boolean bankCountsUpdating) {
            this(snapshot, bankCountsUpdating, false);
        }
    }

    public record StackSnapshot(String displayName, List<String> loreLines, int count, Integer materialTier) {
        public StackSnapshot {
            displayName = displayName == null ? "" : displayName;
            loreLines = loreLines == null ? List.of() : List.copyOf(loreLines);
            count = Math.max(0, count);
        }

        public StackSnapshot(String displayName, List<String> loreLines, int count) {
            this(displayName, loreLines, count, null);
        }

        public StackSnapshot(String displayName, int count) {
            this(displayName, List.of(), count, null);
        }

        public StackSnapshot withMaterialTier(Integer materialTier) {
            return new StackSnapshot(displayName, loreLines, count, materialTier);
        }
    }

    private record StackSnapshotKey(int itemId, int count, String displayName, int componentHash) {}
}
