package julianh06.wynnextras.features.inventory;

import com.google.gson.*;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.Core;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.ItemUtils;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@WEModule
public class WeightDisplay {
    public record WeightData(WynnExtrasConfig.MythicScaleSource source, String weightName,
                             Map<String, Float> identifications, Float score) {}
    public record ItemData(String name, List<WeightData> data, int index) {}
    private record ParsedIdentification(String apiKey, String rawValue, float current) {}
    private record StatRangeCandidate(String internalName, Map<String, float[]> ranges) {}

    public static final Map<String, ItemData> itemCache = new ConcurrentHashMap<>();
    private static final Map<String, ItemData> noriItemCache = new ConcurrentHashMap<>();
    private static final Map<String, ItemData> combinedItemCache = new ConcurrentHashMap<>();
    public static final Map<Integer, ItemData> weightCacheByHash = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, float[]>> itemStatRanges = new ConcurrentHashMap<>();
    public static final Map<Integer, Map<String, Float>> tooltipIdentCache = new ConcurrentHashMap<>();
    private static final Map<String, List<StatRangeCandidate>> itemStatRangeCandidates = new ConcurrentHashMap<>();

    private static boolean upPressed = false;
    private static boolean downPressed = false;
    private static boolean leftPressed = false;
    private static boolean rightPressed = false;
    private static ItemStack currentHoveredStack = null;

    public static boolean hasCycleInput() {
        if (!isContainerOpen()) {
            clearCycleInput();
            return false;
        }
        return upPressed || downPressed || leftPressed || rightPressed;
    }

    private static boolean isContainerOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?>;
    }

    public static void clearCycleInput() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    public static ItemData getSelectedItemData(String itemName) {
        WynnExtrasConfig.MythicScaleSource source = getSelectedScaleSource(itemName);
        if (source == WynnExtrasConfig.MythicScaleSource.BOTH) return getCombinedItemData(itemName);
        return source == WynnExtrasConfig.MythicScaleSource.NORI
                ? noriItemCache.get(itemName)
                : itemCache.get(itemName);
    }

    private static ItemData getCombinedItemData(String itemName) {
        ItemData cached = combinedItemCache.get(itemName);
        if (cached != null) return cached;

        ItemData wynnpool = itemCache.get(itemName);
        ItemData nori = noriItemCache.get(itemName);
        if (wynnpool == null || nori == null) return wynnpool != null ? wynnpool : nori;

        List<WeightData> profiles = new ArrayList<>(wynnpool.data());
        profiles.addAll(nori.data());
        ItemData combined = new ItemData(itemName, List.copyOf(profiles), 0);
        combinedItemCache.put(itemName, combined);
        return combined;
    }

    public static WynnExtrasConfig.MythicScaleSource getSelectedScaleSource(String itemName) {
        boolean hasWynnpool = itemCache.containsKey(itemName);
        boolean hasNori = noriItemCache.containsKey(itemName);
        if (!hasWynnpool) return WynnExtrasConfig.MythicScaleSource.NORI;
        if (!hasNori) return WynnExtrasConfig.MythicScaleSource.WYNNPOOL;
        return WynnExtrasConfig.INSTANCE.mythicScaleSource;
    }

    public static boolean hasMultipleScaleSources(String itemName) {
        return itemCache.containsKey(itemName) && noriItemCache.containsKey(itemName);
    }

    public static boolean shouldShowScaleSourceControls(String itemName) {
        return hasMultipleScaleSources(itemName) && !WynnExtrasConfig.INSTANCE.lockMythicScaleSource;
    }

    public static boolean isShowingBothScaleSources(String itemName) {
        return hasMultipleScaleSources(itemName)
                && getSelectedScaleSource(itemName) == WynnExtrasConfig.MythicScaleSource.BOTH;
    }

    public static int getScaleSourceHeaderCount(String itemName) {
        return isShowingBothScaleSources(itemName) ? 2 : 0;
    }

    public static void setConfiguredScaleSource(WynnExtrasConfig.MythicScaleSource source) {
        WynnExtrasConfig.INSTANCE.mythicScaleSource = source == null
                ? WynnExtrasConfig.MythicScaleSource.WYNNPOOL
                : source;
        weightCacheByHash.clear();
    }

    public static String getScaleLabel(String weightName) {
        return weightName + " Scale";
    }

    public static ItemData applyCycleInput(String itemName) {
        if ((leftPressed || rightPressed) && shouldShowScaleSourceControls(itemName)) {
            WynnExtrasConfig.MythicScaleSource current = getSelectedScaleSource(itemName);
            WynnExtrasConfig.MythicScaleSource[] sources = WynnExtrasConfig.MythicScaleSource.values();
            int direction = rightPressed ? 1 : -1;
            int nextIndex = Math.floorMod(current.ordinal() + direction, sources.length);
            setConfiguredScaleSource(sources[nextIndex]);
            WynnExtrasConfig.save();
        }

        ItemData itemData = getSelectedItemData(itemName);
        if ((upPressed || downPressed) && itemData != null && !itemData.data().isEmpty()) {
            int nextIndex = itemData.index();
            if (downPressed) nextIndex = (nextIndex + 1) % itemData.data().size();
            else nextIndex = (nextIndex - 1 + itemData.data().size()) % itemData.data().size();
            itemData = new ItemData(itemData.name(), itemData.data(), nextIndex);
            selectedItemCache(itemName).put(itemName, itemData);
        }
        clearCycleInput();
        return itemData;
    }

    private static Map<String, ItemData> selectedItemCache(String itemName) {
        return switch (getSelectedScaleSource(itemName)) {
            case NORI -> noriItemCache;
            case BOTH -> combinedItemCache;
            default -> itemCache;
        };
    }

    public static ItemStack getCurrentHoveredStack() {
        return currentHoveredStack;
    }

    public static void setCurrentHoveredStack(ItemStack stack) {
        currentHoveredStack = stack;
    }

    public WeightDisplay() {
         ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
             if (stack.isEmpty()) return;
             String cleanName = extractCleanName(stack);
             if (!isTrackedMythic(stack)) return;
             if (isUnidentified(stack)) return;

             // Fabric builds the vanilla tooltip before Wynntils replaces its rendered lines.
             // Snapshot the original item lore here so all later consumers use Wynncraft's values.
             captureOriginalIdentifications(stack, cleanName);

             if (hasCycleInput()) {
                 MinecraftClient mc = MinecraftClient.getInstance();
                 boolean isHovered = false;
                 if (mc.currentScreen instanceof HandledScreen<?> hs) {
                     Slot focused = HandledScreenAccess.focusedSlot(hs);
                     isHovered = focused != null && ItemStack.areItemsAndComponentsEqual(focused.getStack(), stack);
                 }
                 if (isHovered) {
                     applyCycleInput(cleanName);
                 }
             }

             int hash = stack.getComponents().hashCode();
             ItemData scaleData = weightCacheByHash.get(hash);
             if (scaleData == null) {
                 scaleData = computeScale(stack);
                 if (scaleData != null && !scaleData.data().isEmpty()) weightCacheByHash.put(hash, scaleData);
             }
             if (scaleData == null || scaleData.data().isEmpty()) return;
             ItemData profile = getSelectedItemData(cleanName);
             int idx = (profile != null) ? Math.min(profile.index(), scaleData.data().size() - 1) : 0;
             boolean wynntilsEnabled = isItemStatInfoFeatureEnabled();
             if (!wynntilsEnabled) {
                 //wynntils feature check cause if its enabled we have to append the annotations later otherwise they would be overwritten by wynntils
                 //if this is enabled then its appended in ItemStatInfoFeatureMixin
                 appendWeightAnnotations(lines, cleanName, idx, scaleData);
             }
         });
    }

    public static ItemData computeScale(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!isTrackedMythic(stack)) return null;
        String key = extractCleanName(stack);

        int hash = stack.getComponents().hashCode();
        Map<String, Float> identifications = tooltipIdentCache.get(hash);
        if (identifications == null || identifications.isEmpty()) {
            identifications = extractIdentificationsFromLore(stack, key);
        }
        return computeScale(key, identifications);
    }

    private static ItemData computeScale(String key, Map<String, Float> identifications) {
        ItemData weightProfile = getSelectedItemData(key);
        if (weightProfile == null) return null;
        if (identifications.isEmpty()) return null;

        List<WeightData> calculatedList = new ArrayList<>();
        for (WeightData weightData : weightProfile.data) {
            Map<String, Float> scaled = new HashMap<>();
            float score = 0f;
            for (Map.Entry<String, Float> entry : identifications.entrySet()) {
                String stat = entry.getKey();
                Float value = entry.getValue();
                Float scale = weightData.identifications.getOrDefault(stat, 0f);
                scaled.put(stat, value * scale);
                if (scale < 0) {
                    score += Math.abs((100 - value) * scale);
                } else {
                    score += value * scale;
                }
            }

            calculatedList.add(new WeightData(weightData.source, weightData.weightName, scaled, score));
        }
        return new ItemData(key, calculatedList, 0);
    }

    public static List<Text> appendChatItemAnnotations(String itemName, List<Text> tooltip) {
        if (!WynnExtrasConfig.INSTANCE.showWeight || itemName == null || tooltip == null || tooltip.isEmpty()) {
            return tooltip;
        }

        String cleanName = cleanName(itemName);
        ItemData itemData = getSelectedItemData(cleanName);
        if (itemData == null || itemData.data().isEmpty()) return tooltip;

        if (hasCycleInput()) {
            itemData = applyCycleInput(cleanName);
        }

        ItemData scaleData = computeScale(cleanName, extractIdentificationsFromTooltip(tooltip, cleanName));
        if (scaleData == null || scaleData.data().isEmpty()) return tooltip;

        List<Text> modified = new ArrayList<>(tooltip);
        appendWeightAnnotations(modified, cleanName,
                Math.min(itemData.index(), scaleData.data().size() - 1), scaleData,
                findChatScaleInsertionIndex(tooltip));
        return modified;
    }

    private static int findChatScaleInsertionIndex(List<Text> tooltip) {
        int lastHeaderLine = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            Text line = tooltip.get(i);
            if (usesFont(line, "tooltip/emblem/", "tooltip/banner", "banner/")) {
                lastHeaderLine = i;
            }
            if (lastHeaderLine >= 0
                    && usesFont(line, "tooltip/attribute/", "tooltip/requirement/", "tooltip/divider")) {
                break;
            }
        }
        if (lastHeaderLine >= 0) return lastHeaderLine + 1;
        return Math.min(4, tooltip.size() - 1);
    }

    private static boolean usesFont(Text line, String... fontPathPrefixes) {
        return line.visit((style, string) -> {
            if (!(style.getFont() instanceof StyleSpriteSource.Font(Identifier font))) {
                return Optional.empty();
            }
            for (String prefix : fontPathPrefixes) {
                if (font.getPath().startsWith(prefix)) return Optional.of(true);
            }
            return Optional.empty();
        }, Style.EMPTY).orElse(false);
    }

    private static final java.util.regex.Pattern VANILLA_PATTERN =
            java.util.regex.Pattern.compile("^([A-Z][A-Za-z ]*?)\\P{ASCII}.*?([+-][\\d,]+(?:\\.\\d+)?(?:%|/\\d+s)?) \\P{ASCII}.*$");

    private static final java.util.regex.Pattern WYNNTILS_PATTERN =
            java.util.regex.Pattern.compile("^([A-Z][A-Za-z ]*?)\\s*([+-][\\d,]+(?:\\.\\d+)?(?:%|/\\d+s)?)");

    public static String[] extractStatFromLine(String lineStr) {
        java.util.regex.Matcher m = VANILLA_PATTERN.matcher(lineStr);
        if (m.matches()) return new String[]{m.group(1).strip(), m.group(2)};

        String stripped = lineStr.replaceAll("[^\\x20-\\x7E]", "").trim();

        m = WYNNTILS_PATTERN.matcher(stripped);
        if (m.find()) return new String[]{m.group(1).strip(), m.group(2)};

        return null;
    }

    private static Map<String, Float> extractIdentificationsFromLore(ItemStack stack, String itemName) {
        try {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore == null || lore.lines().isEmpty()) {
                return Map.of();
            }

            List<ParsedIdentification> parsed = new ArrayList<>();
            for (Text line : lore.lines()) {
                String raw = line.getString();
                java.util.regex.Matcher m = VANILLA_PATTERN.matcher(raw);
                if (!m.matches()) continue;

                String statName = m.group(1).strip();
                String rawValue = m.group(2);
                String[] keyAndRaw = resolveIdentKey(statName, rawValue);
                String apiKey = keyAndRaw[0];

                java.util.regex.Matcher numM = java.util.regex.Pattern.compile("[+-]?([\\d,]+(?:\\.\\d+)?)").matcher(rawValue);
                if (!numM.find()) continue;
                float current = Float.parseFloat(numM.group(1).replace(",", ""));
                parsed.add(new ParsedIdentification(apiKey, rawValue, current));
            }

            Map<String, float[]> ranges = selectStatRanges(itemName, parsed);
            if (ranges == null) return Map.of();

            Map<String, Float> result = new HashMap<>();
            for (ParsedIdentification identification : parsed) {
                String apiKey = identification.apiKey();
                String rawValue = identification.rawValue();
                float[] range = ranges.get(apiKey);
                if (range == null) continue;
                float min = range[0], max = range[1];
                if (max == min) continue;

                float percent = (identification.current() - min) / (max - min) * 100f;
                if(apiKey.contains("SpellCost") ^ rawValue.contains("-")) { //Invert for negative stats or (exclusive) if it's a spell cost stat
                    percent = 100 - percent;
                }
                percent = Math.clamp(percent, 0f, 100f);
                result.put(apiKey, percent);
            }
            return result;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static Map<String, Float> extractIdentificationsFromTooltip(List<Text> tooltip, String itemName) {
        try {
            List<ParsedIdentification> parsed = new ArrayList<>();
            for (Text line : tooltip) {
                String[] stat = extractStatFromLine(line.getString());
                if (stat == null) continue;

                String apiKey = resolveIdentKey(stat[0], stat[1])[0];
                java.util.regex.Matcher number = java.util.regex.Pattern
                        .compile("[+-]?([\\d,]+(?:\\.\\d+)?)")
                        .matcher(stat[1]);
                if (!number.find()) continue;
                float current = Float.parseFloat(number.group(1).replace(",", ""));
                parsed.add(new ParsedIdentification(apiKey, stat[1], current));
            }

            Map<String, float[]> ranges = selectStatRanges(itemName, parsed);
            if (ranges == null) return Map.of();

            Map<String, Float> result = new HashMap<>();
            for (ParsedIdentification identification : parsed) {
                float[] range = ranges.get(identification.apiKey());
                if (range == null || range[1] == range[0]) continue;

                float percent = (identification.current() - range[0]) / (range[1] - range[0]) * 100f;
                if (identification.apiKey().contains("SpellCost") ^ identification.rawValue().contains("-")) {
                    percent = 100 - percent;
                }
                result.put(identification.apiKey(), Math.clamp(percent, 0f, 100f));
            }
            return result;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static Map<String, float[]> selectStatRanges(
            String itemName,
            List<ParsedIdentification> identifications
    ) {
        List<StatRangeCandidate> candidates = itemStatRangeCandidates.get(itemName);
        if (candidates == null || candidates.isEmpty()) return itemStatRanges.get(itemName);

        StatRangeCandidate best = null;
        double bestPenalty = Double.POSITIVE_INFINITY;
        int bestMatches = -1;
        boolean bestExactInternalName = false;

        for (StatRangeCandidate candidate : candidates) {
            double penalty = 0d;
            int matches = 0;
            for (ParsedIdentification identification : identifications) {
                float[] range = candidate.ranges().get(identification.apiKey());
                if (range == null) continue;
                matches++;
                float span = Math.max(1f, range[1] - range[0]);
                if (identification.current() < range[0]) {
                    penalty += (range[0] - identification.current()) / span;
                } else if (identification.current() > range[1]) {
                    penalty += (identification.current() - range[1]) / span;
                }
            }

            boolean exactInternalName = itemName.equals(candidate.internalName());
            if (penalty < bestPenalty
                    || (Double.compare(penalty, bestPenalty) == 0 && matches > bestMatches)
                    || (Double.compare(penalty, bestPenalty) == 0 && matches == bestMatches
                    && exactInternalName && !bestExactInternalName)) {
                best = candidate;
                bestPenalty = penalty;
                bestMatches = matches;
                bestExactInternalName = exactInternalName;
            }
        }
        return best == null ? itemStatRanges.get(itemName) : best.ranges();
    }

    private static void captureOriginalIdentifications(ItemStack stack, String itemName) {
        Map<String, Float> identifications = extractIdentificationsFromLore(stack, itemName);
        if (!identifications.isEmpty()) {
            tooltipIdentCache.put(stack.getComponents().hashCode(), Map.copyOf(identifications));
        }
    }

    public static void populateStatRangesFromDatabase() {
        WynnDataService.WynnDataSnapshot snapshot = WynnDataService.getInstance().snapshot();
        if (snapshot == null) return;

        Map<String, List<StatRangeCandidate>> candidatesByDisplayName = new HashMap<>();
        for (WynnDataService.ItemData item : snapshot.items()) {
            Map<String, float[]> ranges = extractStatRanges(item.identifications());
            if (ranges.isEmpty()) continue;

            StatRangeCandidate candidate = new StatRangeCandidate(item.internalName(), Map.copyOf(ranges));
            candidatesByDisplayName.computeIfAbsent(item.displayName(), ignored -> new ArrayList<>()).add(candidate);
            if (!item.displayName().equals(item.internalName())) {
                candidatesByDisplayName.computeIfAbsent(item.internalName(), ignored -> new ArrayList<>()).add(candidate);
            }
        }

        itemStatRanges.clear();
        itemStatRangeCandidates.clear();
        Set<String> trackedItems = new HashSet<>(itemCache.keySet());
        trackedItems.addAll(noriItemCache.keySet());
        for (String itemName : trackedItems) {
            List<StatRangeCandidate> candidates = candidatesByDisplayName.get(itemName);
            if (candidates == null || candidates.isEmpty()) continue;

            List<StatRangeCandidate> ordered = candidates.stream()
                    .distinct()
                    .sorted(Comparator.comparing(candidate -> !itemName.equals(candidate.internalName())))
                    .toList();
            itemStatRangeCandidates.put(itemName, ordered);
            itemStatRanges.put(itemName, ordered.getFirst().ranges());
        }
        tooltipIdentCache.clear();
        weightCacheByHash.clear();
    }

    private static Map<String, float[]> extractStatRanges(Map<String, WynnDataService.StatValue> ids) {
        Map<String, float[]> ranges = new HashMap<>();
        for (Map.Entry<String, WynnDataService.StatValue> entry : ids.entrySet()) {
            WynnDataService.StatValue value = entry.getValue();
            if (!value.isRange()) continue;
            float a = Math.abs(value.minimum().floatValue());
            float b = Math.abs(value.maximum().floatValue());
            ranges.put(entry.getKey(), new float[]{Math.min(a, b), Math.max(a, b)});
        }
        return ranges;
    }

    public static String extractCleanName(ItemStack stack) {
        return cleanName(stack.getName().getString());
    }

    private static String cleanName(String name) {
        return name
            .replace("À", "")
            .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
            .replaceAll("[^\\x20-\\x7E]", "")
            .replaceAll("^\\s*Shiny\\s+", "")
            .strip();
    }

    public static boolean isTrackedMythic(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return getSelectedItemData(extractCleanName(stack)) != null && ItemUtils.isTier(stack, "MYTHIC");
    }

    public static boolean isUnidentified(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        for (Text line : lore.lines()) {
            String s = line.getString();
            if (s.contains("This item's power has been sealed")) return true;
        }
        return false;
    }

    public static int getScaleColor(float score) {
        score = Math.max(0, Math.min(100, score));
        if (score < 40) return lerpColor(0xFF5555, 0xFFAA00, score / 40f);
        if (score < 70) return lerpColor(0xFFAA00, 0xFFFF55, (score - 40) / 30f);
        if (score < 90) return lerpColor(0xFFFF55, 0x55FF55, (score - 70) / 20f);
        return lerpColor(0x55FF55, 0x55FFFF, (score - 90) / 10f);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((c1 >> 16) & 0xFF) + t * (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)));
        int g = (int) (((c1 >>  8) & 0xFF) + t * (((c2 >>  8) & 0xFF) - ((c1 >>  8) & 0xFF)));
        int b = (int) ((c1 & 0xFF) + t * ((c2 & 0xFF) - (c1 & 0xFF)));
        return (r << 16) | (g << 8) | b;
    }

    public static String[] resolveIdentKey(String statName, String rawValue) {
        boolean isPercent = rawValue.endsWith("%");
        boolean isPerSecond = rawValue.endsWith("/5s");

        String key = statToApiKey.getOrDefault(statName, fallbackCamelCase(statName));
        if (key.contains("Cost")) {
            for (Map.Entry<String, String> entry : spellCostMap.entrySet()) {
                if (key.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    key = entry.getValue();
                    break;
                }
            }
        }
        if (!isPercent && !isPerSecond) {
            if (key.equals("healthRegen")) {
                key = key + "Raw";
            } else if (key.contains("AttackSpeed")) {
                key = "rawAttackSpeed";
            } else if (!key.equals("manaRegen") && !key.contains("Steal") && !key.contains("poison") && !key.contains("jump")) {
                key = "raw" + key.substring(0, 1).toUpperCase() + key.substring(1);
            }
        }
        return new String[]{key, rawValue};
    }

    private static boolean isItemStatInfoFeatureEnabled() {
        return julianh06.wynnextras.compat.wynntils.WynntilsTooltipAdapter.isItemStatInfoEnabled();
    }

    public static void getWeightsFromWynnpool() {
        try {
            URL url = new URI("https://api.wynnpool.com/item/weight/all").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return;
            }

            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                parseAndCacheWynnpoolWeights(response.toString());
            }
        } catch (IOException e) {
            Core.LOGGER.logError("IOException while getting Weights from Wynnpool API: " + e.getMessage());
        } catch (URISyntaxException e) {
            Core.LOGGER.logError("Invalid Wynnpool API URI: " + e.getMessage());
        } catch (RuntimeException e) {
            Core.LOGGER.logError("Invalid response from Wynnpool API: " + e.getMessage());
        }
    }

    public static void getWeightsFromNori() {
        try {
            URL url = new URI("https://nori.fish/api/item/mythic").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);

            if (conn.getResponseCode() != 200) return;

            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                parseAndCacheNoriWeights(response.toString());
            }
        } catch (IOException e) {
            Core.LOGGER.logError("IOException while getting Weights from nori.fish API: " + e.getMessage());
        } catch (URISyntaxException e) {
            Core.LOGGER.logError("Invalid nori.fish API URI: " + e.getMessage());
        } catch (RuntimeException e) {
            Core.LOGGER.logError("Invalid response from nori.fish API: " + e.getMessage());
        }
    }

    private static void parseAndCacheWynnpoolWeights(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        Map<String, List<WeightData>> grouped = new HashMap<>();

        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();

            String itemName = obj.get("item_name").getAsString();
            String weightName = obj.get("weight_name").getAsString();
            JsonObject identifications = obj.getAsJsonObject("identifications");

            Map<String, Float> scales = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : identifications.entrySet()) {
                scales.put(entry.getKey(), entry.getValue().getAsFloat());
            }

            WeightData weightData = new WeightData(
                    WynnExtrasConfig.MythicScaleSource.WYNNPOOL, weightName, scales, 0f);
            grouped.computeIfAbsent(itemName, k -> new ArrayList<>()).add(weightData);
        }

        replaceItemCache(itemCache, grouped);
    }

    private static void parseAndCacheNoriWeights(String json) {
        JsonObject weights = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("weights");
        Map<String, List<WeightData>> grouped = new HashMap<>();

        for (Map.Entry<String, JsonElement> itemEntry : weights.entrySet()) {
            List<WeightData> profiles = new ArrayList<>();
            for (Map.Entry<String, JsonElement> profileEntry : itemEntry.getValue().getAsJsonObject().entrySet()) {
                Map<String, Float> scales = new HashMap<>();
                for (Map.Entry<String, JsonElement> identification
                        : profileEntry.getValue().getAsJsonObject().entrySet()) {
                    scales.put(identification.getKey(), identification.getValue().getAsFloat() / 100f);
                }
                profiles.add(new WeightData(
                        WynnExtrasConfig.MythicScaleSource.NORI, profileEntry.getKey(), scales, 0f));
            }
            if (!profiles.isEmpty()) grouped.put(itemEntry.getKey(), profiles);
        }

        replaceItemCache(noriItemCache, grouped);
    }

    private static void replaceItemCache(Map<String, ItemData> cache, Map<String, List<WeightData>> grouped) {
        Map<String, ItemData> replacement = new HashMap<>();
        for (Map.Entry<String, List<WeightData>> entry : grouped.entrySet()) {
            ItemData previous = cache.get(entry.getKey());
            int index = previous == null ? 0 : Math.min(previous.index(), entry.getValue().size() - 1);
            replacement.put(entry.getKey(), new ItemData(entry.getKey(), List.copyOf(entry.getValue()), index));
        }
        cache.clear();
        cache.putAll(replacement);
        combinedItemCache.clear();
        weightCacheByHash.clear();
    }

    private static final Map<String, String> statToApiKey = Map.ofEntries(
            Map.entry("Health Regen", "healthRegen"),
            Map.entry("Health Regen Raw", "healthRegenRaw"),
            Map.entry("Fire Damage", "fireDamage"),
            Map.entry("Water Damage", "waterDamage"),
            Map.entry("Thunder Damage", "thunderDamage"),
            Map.entry("Earth Damage", "earthDamage"),
            Map.entry("Air Damage", "airDamage"),
            Map.entry("Spell Damage", "spellDamage"),
            Map.entry("Main Attack Damage", "mainAttackDamage"),
            Map.entry("Mana Steal", "manaSteal"),
            Map.entry("Life Steal", "lifeSteal"),
            Map.entry("Attack Speed", "attackSpeed"),
            Map.entry("Walk Speed", "walkSpeed"),
            Map.entry("Dexterity", "dexterity"),
            Map.entry("Defence", "defence"),
            Map.entry("Agility", "agility"),
            Map.entry("Intelligence", "intelligence"),
            Map.entry("Strength", "strength"),
            Map.entry("Jump Height", "jumpHeight"),
            Map.entry("Poison", "poison"),
            Map.entry("Loot", "lootBonus"),
            Map.entry("Combat Experience", "combatExperience")
    );

    private static String fallbackCamelCase(String stat) {
        String[] parts = stat.toLowerCase().split(" ");
        if (parts.length == 0) return stat;
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            builder.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return builder.toString();
    }

    private static final Map<String, String> spellCostMap = Map.ofEntries(
            Map.entry("heal", "1stSpellCost"),
            Map.entry("bash", "1stSpellCost"),
            Map.entry("arrowStorm", "1stSpellCost"),
            Map.entry("spinAttack", "1stSpellCost"),
            Map.entry("totem", "1stSpellCost"),

            Map.entry("teleport", "2ndSpellCost"),
            Map.entry("charge", "2ndSpellCost"),
            Map.entry("escape", "2ndSpellCost"),
            Map.entry("dash", "2ndSpellCost"),
            Map.entry("haul", "2ndSpellCost"),

            Map.entry("meteor", "3rdSpellCost"),
            Map.entry("uppercut", "3rdSpellCost"),
            Map.entry("arrowBomb", "3rdSpellCost"),
            Map.entry("multiHit", "3rdSpellCost"),
            Map.entry("aura", "3rdSpellCost"),

            Map.entry("iceSnake", "4thSpellCost"),
            Map.entry("warScream", "4thSpellCost"),
            Map.entry("arrowShield", "4thSpellCost"),
            Map.entry("smokeBomb", "4thSpellCost"),
            Map.entry("uproot", "4thSpellCost")
    );

    @SubscribeEvent
    public void onKey(KeyInputEvent event) {
        if (!isContainerOpen()) {
            clearCycleInput();
            return;
        }
        if((event.getKey() == GLFW.GLFW_KEY_UP || event.getKey() == GLFW.GLFW_KEY_W) && event.getAction() == GLFW.GLFW_PRESS) {
            upPressed = true;
        }
        if((event.getKey() == GLFW.GLFW_KEY_DOWN || event.getKey() == GLFW.GLFW_KEY_S) && event.getAction() == GLFW.GLFW_PRESS) {
            downPressed = true;
        }
        if((event.getKey() == GLFW.GLFW_KEY_LEFT || event.getKey() == GLFW.GLFW_KEY_A) && event.getAction() == GLFW.GLFW_PRESS) {
            leftPressed = true;
        }
        if((event.getKey() == GLFW.GLFW_KEY_RIGHT || event.getKey() == GLFW.GLFW_KEY_D) && event.getAction() == GLFW.GLFW_PRESS) {
            rightPressed = true;
        }
    }

    public static List<Text> modifyTooltip(List<Text> tooltips, ItemStack itemStack) {
        List<Text> modified = new ArrayList<>();

        String key = extractCleanName(itemStack);
        if (!isTrackedMythic(itemStack)) return tooltips;
        ItemData itemData = getSelectedItemData(key);
        ItemData scaleData = weightCacheByHash.getOrDefault(itemStack.getComponents().hashCode(), null);

        for (int i = 1; i < tooltips.size(); i++) {
            Text line = tooltips.get(i);
            modified.add(line);

            if (i == 3 && WynnExtrasConfig.INSTANCE.showScales && WynnExtrasConfig.INSTANCE.showWeight && itemData != null && scaleData != null && !scaleData.data().isEmpty()) {
                final int index = itemData.index();

                modified.add(tooltips.getFirst().copy());

                final AtomicInteger aidx = new AtomicInteger(0);
                WynnExtrasConfig.MythicScaleSource renderedSource = null;
                for (WeightData data : scaleData.data()) {
                    if (isShowingBothScaleSources(key) && data.source() != renderedSource) {
                        renderedSource = data.source();
                        modified.add(Text.literal("  ↳ " + renderedSource).formatted(Formatting.GRAY));
                    }
                    float score = data.score();
                    String scale = data.weightName();
                    boolean isCurrent = (index == aidx.get() && scaleData.data().size() > 1);
                    Formatting labelColor = isCurrent ? Formatting.WHITE : Formatting.GRAY;

                    Text scoreText = Text.literal(String.format(" %.1f%%", score))
                            .styled(s -> s.withColor(getScaleColor(score)));

                    String indent = isShowingBothScaleSources(key) ? "  ↳ " : "↳ ";
                    Text statWeight = Text.literal(indent + getScaleLabel(scale))
                            .formatted(labelColor)
                            .styled(s -> isCurrent ? s.withBold(true) : s)
                            .append(scoreText);
                    modified.add(Text.literal("  ").append(statWeight));
                    aidx.incrementAndGet();
                }
                if (scaleData.data().size() > 1) {
                    modified.add(Text.literal("  ↳ Use ↑ / ↓ (W / S) to cycle").formatted(Formatting.DARK_GRAY));
                }
                if (shouldShowScaleSourceControls(key)) {
                    modified.add(Text.literal("  ↳ Use ← / → (A / D) to switch source")
                            .formatted(Formatting.DARK_GRAY));
                    modified.add(Text.literal("  ↳ Currently using: " + getSelectedScaleSource(key))
                            .formatted(Formatting.DARK_GRAY));
                }
            }

            if (!WynnExtrasConfig.INSTANCE.showScales || !WynnExtrasConfig.INSTANCE.showWeight) continue;

            if (itemData == null) continue;
            String[] statParts = extractStatFromLine(line.getString());
            if (statParts == null) continue;
            String apiName = resolveIdentKey(statParts[0], statParts[1])[0];

            Float weight = itemData.data().get(itemData.index()).identifications().get(apiName);
            if (weight == null) continue;

            modified.add(Text.literal(String.format("  ↳ Weight: %.2f%%", weight * 100))
                    .formatted(Formatting.DARK_GRAY));
        }

        return modified;
    }

    public static void appendWeightAnnotations(List<Text> lines, String cleanName, int currentIdx, ItemData scaleData) {
        appendWeightAnnotations(lines, cleanName, currentIdx, scaleData, 4);
    }

    private static void appendWeightAnnotations(List<Text> lines, String cleanName, int currentIdx,
                                                ItemData scaleData, int scaleInsertionIndex) {
        ItemData itemData = getSelectedItemData(cleanName);
        if (itemData == null) return;

        List<Text> original = new ArrayList<>(lines);
        lines.clear();

        WeightData currentProfile = itemData.data().get(currentIdx);

        for (int i = 0; i < original.size(); i++) {
            Text line = original.get(i);
            if (i == scaleInsertionIndex && WynnExtrasConfig.INSTANCE.showWeight) {
                lines.add(Text.empty());
                WynnExtrasConfig.MythicScaleSource renderedSource = null;
                for (int j = 0; j < scaleData.data().size(); j++) {
                    WeightData wd = scaleData.data().get(j);
                    if (isShowingBothScaleSources(cleanName) && wd.source() != renderedSource) {
                        renderedSource = wd.source();
                        lines.add(Text.literal("  ↳ " + renderedSource)
                                .styled(s -> s.withColor(0xAAAAAA)));
                    }
                    boolean cur = (j == currentIdx);
                    float score = wd.score();
                    Text scoreText = Text.literal(String.format(" [%.1f%%]", score))
                        .styled(s -> s.withColor(getScaleColor(score)).withBold(cur));
                    String indent = isShowingBothScaleSources(cleanName) ? "    ↳ " : "  ↳ ";
                    Text label = Text.literal(indent + getScaleLabel(wd.weightName()))
                        .styled(s -> s.withColor(cur ? 0xFFFFFF : 0xAAAAAA).withBold(cur))
                        .copy().append(scoreText);
                    lines.add(label);
                }
                if (scaleData.data().size() > 1) {
                    lines.add(Text.literal("  ↳ Use ↑/↓ (W/S) to cycle").styled(s -> s.withColor(0x555555)));
                }
                if (shouldShowScaleSourceControls(cleanName)) {
                    lines.add(Text.literal("  ↳ Use ←/→ (A/D) to switch source")
                            .styled(s -> s.withColor(0x555555)));
                    lines.add(Text.literal("  ↳ Currently using: "
                                    + getSelectedScaleSource(cleanName))
                            .styled(s -> s.withColor(0x555555)));
                }
            }
            lines.add(line);

            if (!WynnExtrasConfig.INSTANCE.showScales || !WynnExtrasConfig.INSTANCE.showWeight) continue;

            String[] statParts = extractStatFromLine(line.getString());
            if (statParts == null) continue;

            String statApiName = resolveIdentKey(statParts[0], statParts[1])[0];

            Float scale = currentProfile.identifications.getOrDefault(statApiName, 0f);
            if (scale == null || scale == 0f) continue;

            lines.add(Text.literal(String.format("  ↳ Weight: %.1f%%", scale * 100))
                    .styled(s -> s.withColor(0x555555)));
        }
    }
}
