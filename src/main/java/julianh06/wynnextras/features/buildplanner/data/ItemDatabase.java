package julianh06.wynnextras.features.buildplanner.data;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Builder-specific search index over the shared Wynncraft item data. */
public final class ItemDatabase {
    private static final ItemDatabase INSTANCE = new ItemDatabase();
    private static final String MASTERWORK_PREFIX = "Masterwork ";
    private final Map<String, WynnItem> itemsByName = new ConcurrentHashMap<>();
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile String lastError = "";
    private volatile long revision;

    private ItemDatabase() {}

    public static ItemDatabase getInstance() { return INSTANCE; }
    public synchronized List<WynnItem> allItems() { return List.copyOf(itemsByName.values()); }
    public long revision() { return revision; }

    public void fetchAll() {
        if (!loading.compareAndSet(false, true)) return;
        WynnDataService service = WynnDataService.getInstance();
        try {
            load(service.snapshot());
            List<WynnDataService.ItemData> refreshed = service.initializeItems().join();
            load(refreshed);
            lastError = "";
        } catch (CompletionException | IllegalStateException exception) {
            lastError = itemsByName.isEmpty() ? service.getStatusMessage() : "Using cached Wynncraft items.";
            PlannerLog.LOGGER.warn("Could not initialize shared Wynncraft item data for the planner.", exception);
        } finally {
            loading.set(false);
            PlannerLog.LOGGER.info("Planner item index ready. Items: {}, Error: '{}'", itemsByName.size(), lastError);
        }
    }

    public List<WynnItem> searchItems(String query) { return searchItems(query, true); }

    public List<WynnItem> searchItems(String query, boolean filterEquipment) {
        var stream = itemsByName.values().stream();
        if (filterEquipment) {
            stream = stream.filter(item -> {
                String type = lower(item.type());
                return type.equals("weapon") || type.equals("armour") || type.equals("accessory");
            });
        }
        if (query != null && !query.isBlank()) {
            String lowered = lower(query);
            stream = stream.filter(item -> lower(item.displayName()).contains(lowered));
        }
        return stream.sorted(Comparator.comparing(WynnItem::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(100).toList();
    }

    public List<WynnItem> searchItems(String query, String slotType) {
        String normalizedSlot = lower(slotType);
        String lowered = lower(query).trim();
        return itemsByName.values().stream()
                .filter(item -> matchesSlot(item, normalizedSlot))
                .filter(item -> lowered.isEmpty() || lower(item.displayName()).contains(lowered))
                .sorted(slotSearchComparator(normalizedSlot)).toList();
    }

    public WynnItem getItem(String name) {
        if (name == null || name.isBlank()) return null;
        if (CraftedItemCodec.isReference(name)) {
            try {
                return CraftedItemCodec.decode(name);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                PlannerLog.LOGGER.warn("Could not resolve crafted item reference: {}", exception.getMessage());
                return null;
            }
        }
        WynnItem direct = itemsByName.get(normalizeName(name));
        if (direct != null) return direct;
        return itemsByName.values().stream()
                .filter(item -> item.displayName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public boolean isReady() { return !itemsByName.isEmpty(); }
    public boolean isLoading() { return loading.get(); }
    public int getLoadedCount() { return itemsByName.size(); }
    public String getLastError() { return lastError; }

    private void load(WynnDataService.WynnDataSnapshot snapshot) {
        if (snapshot != null) load(snapshot.items());
    }

    private void load(List<WynnDataService.ItemData> source) {
        if (source == null) return;
        List<WynnItem> items = new ArrayList<>();
        for (WynnDataService.ItemData item : source) {
            try {
                items.add(convert(item));
            } catch (RuntimeException exception) {
                PlannerLog.LOGGER.warn("Skipping malformed shared Wynncraft item {}.", item.displayName(), exception);
            }
        }
        if (!items.isEmpty()) replaceItems(items);
    }

    private WynnItem convert(WynnDataService.ItemData source) {
        String displayName = source.internalName().startsWith(MASTERWORK_PREFIX)
                ? source.internalName() : source.displayName();
        Map<String, Identification> identifications = new LinkedHashMap<>();
        source.identifications().forEach((key, value) -> identifications.put(
                normalizeIdentificationKey(key), identification(value)));
        Map<String, Integer> baseStats = new LinkedHashMap<>();
        source.baseStats().forEach((key, value) -> copyStat(baseStats, key, value));
        copyRequirement(source.requirements(), baseStats, "level", "lvl");
        copyRequirement(source.requirements(), baseStats, "strength", "strReq");
        copyRequirement(source.requirements(), baseStats, "dexterity", "dexReq");
        copyRequirement(source.requirements(), baseStats, "intelligence", "intReq");
        copyRequirement(source.requirements(), baseStats, "defence", "defReq");
        copyRequirement(source.requirements(), baseStats, "agility", "agiReq");
        alias(baseStats, "baseHealth", "hp");
        alias(baseStats, "baseEarthDefence", "eDef");
        alias(baseStats, "baseThunderDefence", "tDef");
        alias(baseStats, "baseWaterDefence", "wDef");
        alias(baseStats, "baseFireDefence", "fDef");
        alias(baseStats, "baseAirDefence", "aDef");
        aliasRange(baseStats, "baseDamage", "nDam");
        aliasRange(baseStats, "baseEarthDamage", "eDam");
        aliasRange(baseStats, "baseThunderDamage", "tDam");
        aliasRange(baseStats, "baseWaterDamage", "wDam");
        aliasRange(baseStats, "baseFireDamage", "fDam");
        aliasRange(baseStats, "baseAirDamage", "aDam");
        if (source.powderSlots() != null) baseStats.put("powderSlots", source.powderSlots());
        var icon = source.icon();
        return new WynnItem(displayName, empty(source.type()), empty(source.subType()), empty(source.tier()),
                empty(source.attackSpeed()), identifications, baseStats, icon.id(), icon.model(),
                icon.customModelData(), "", List.of(), source.majorIds(), source.lore(),
                empty(source.restriction()), source.emblem());
    }

    private synchronized void replaceItems(List<WynnItem> items) {
        Map<String, WynnItem> replacement = new LinkedHashMap<>();
        for (WynnItem item : items) replacement.put(normalizeName(item.displayName()), item);
        if (replacement.equals(itemsByName)) return;
        itemsByName.clear();
        itemsByName.putAll(replacement);
        revision++;
    }

    private Identification identification(WynnDataService.StatValue value) {
        int raw = number(value.raw(), value.value(), value.minimum(), value.maximum());
        int minimum = number(value.minimum(), value.raw(), value.value(), value.maximum());
        int maximum = number(value.maximum(), value.raw(), value.value(), value.minimum());
        return new Identification(minimum, raw, maximum);
    }

    private void copyStat(Map<String, Integer> target, String key, WynnDataService.StatValue value) {
        Double scalar = first(value.raw(), value.value());
        if (scalar != null) target.put(key, scalar.intValue());
        if (value.minimum() != null) target.put(key + "Min", value.minimum().intValue());
        if (value.maximum() != null) target.put(key + "Max", value.maximum().intValue());
    }

    private void copyRequirement(Map<String, String> source, Map<String, Integer> target,
            String sourceKey, String targetKey) {
        String value = source.get(sourceKey);
        if (value == null) return;
        try {
            target.put(targetKey, (int) Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private void alias(Map<String, Integer> stats, String source, String target) {
        Integer value = stats.get(source);
        if (value != null) stats.put(target, value);
    }

    private void aliasRange(Map<String, Integer> stats, String source, String target) {
        alias(stats, source + "Min", target + "Min");
        alias(stats, source + "Max", target + "Max");
    }

    private Comparator<WynnItem> slotSearchComparator(String slot) {
        if (!slot.equals("weapon")) return Comparator.comparing(WynnItem::displayName, String.CASE_INSENSITIVE_ORDER);
        return Comparator.comparing(this::weaponSortName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(WynnItem::displayName, String.CASE_INSENSITIVE_ORDER);
    }

    private String weaponSortName(WynnItem item) {
        return isAscensionVariant(item) ? item.displayName().substring(MASTERWORK_PREFIX.length()) : item.displayName();
    }

    private boolean isAscensionVariant(WynnItem item) {
        if (!item.displayName().startsWith(MASTERWORK_PREFIX) || !item.tier().equalsIgnoreCase("mythic")) return false;
        WynnItem base = itemsByName.get(normalizeName(item.displayName().substring(MASTERWORK_PREFIX.length())));
        return base != null && base.tier().equalsIgnoreCase("mythic") && base.type().equalsIgnoreCase("weapon")
                && base.subType().equalsIgnoreCase(item.subType());
    }

    private boolean matchesSlot(WynnItem item, String slot) {
        String type = lower(item.type());
        String subType = lower(item.subType());
        return switch (slot) {
            case "helmet", "chestplate", "leggings", "boots" -> subType.equals(slot) || type.equals(slot);
            case "ring", "bracelet", "necklace" -> subType.equals(slot) || type.equals(slot);
            case "weapon" -> type.equals("weapon") || List.of("dagger", "spear", "wand", "bow", "relik").contains(subType);
            default -> false;
        };
    }

    private String normalizeIdentificationKey(String key) {
        return switch (key) {
            case "rawStrength" -> "str";
            case "rawDexterity", "DEXETERITY" -> "dex";
            case "rawIntelligence" -> "int";
            case "rawDefence" -> "def";
            case "rawAgility" -> "agi";
            default -> key;
        };
    }

    private static int number(Double... values) {
        Double value = first(values);
        return value == null ? 0 : value.intValue();
    }

    private static Double first(Double... values) {
        for (Double value : values) if (value != null) return value;
        return null;
    }

    private static String empty(String value) { return value == null ? "" : value; }
    private static String lower(String value) { return empty(value).toLowerCase(Locale.ROOT); }
    private static String normalizeName(String name) { return lower(name).trim(); }
}
