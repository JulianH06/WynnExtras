package julianh06.wynnextras.features.inventory;

import com.google.gson.*;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.Core;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    public record WeightData(String weightName, Map<String, Float> identifications, Float score) {}
    public record ItemData(String name, List<WeightData> data, int index) {}

    public static final Map<String, ItemData> itemCache = new ConcurrentHashMap<>();
    public static final Map<Integer, ItemData> weightCacheByHash = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, float[]>> itemStatRanges = new ConcurrentHashMap<>();
    public static final Map<Integer, Map<String, Float>> tooltipIdentCache = new ConcurrentHashMap<>();

    private static boolean upPressed = false;
    private static boolean downPressed = false;
    private static ItemStack currentHoveredStack = null;

    public static boolean isUpPressed() {
        return upPressed;
    }

    public static boolean isDownPressed() {
        return downPressed;
    }

    public static void clearCycleInput() {
        upPressed = false;
        downPressed = false;
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
             if (!itemCache.containsKey(cleanName)) return;
             if (isUnidentified(stack)) return;

             if (upPressed || downPressed) {
                 MinecraftClient mc = MinecraftClient.getInstance();
                 boolean isHovered = false;
                 if (mc.currentScreen instanceof HandledScreen<?> hs) {
                     Slot focused = ((HandledScreenAccessor) hs).getFocusedSlot();
                     isHovered = focused != null && ItemStack.areItemsAndComponentsEqual(focused.getStack(), stack);
                 }
                 if (isHovered) {
                     ItemData itemData = itemCache.get(cleanName);
                     if (itemData != null && !itemData.data().isEmpty()) {
                         int nextIndex = itemData.index();
                         if (downPressed) nextIndex = (nextIndex + 1) % itemData.data().size();
                         else nextIndex = (nextIndex - 1 + itemData.data().size()) % itemData.data().size();
                         itemCache.put(cleanName, new ItemData(itemData.name(), itemData.data(), nextIndex));
                     }
                     upPressed = false;
                     downPressed = false;
                 }
             }

             int hash = stack.getComponents().hashCode();
             ItemData scaleData = weightCacheByHash.get(hash);
             if (scaleData == null) {
                 scaleData = computeScale(stack);
                 if (scaleData != null && !scaleData.data().isEmpty()) weightCacheByHash.put(hash, scaleData);
             }
             if (scaleData == null || scaleData.data().isEmpty()) return;
             ItemData profile = itemCache.get(cleanName);
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
        String key = extractCleanName(stack);
        ItemData weightProfile = itemCache.get(key);
        if (weightProfile == null) return null;

        int hash = stack.getComponents().hashCode();
        Map<String, Float> identifications = tooltipIdentCache.get(hash);
        if (identifications == null || identifications.isEmpty()) {
            identifications = extractIdentificationsFromLore(stack, key);
        }
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

            calculatedList.add(new WeightData(weightData.weightName, scaled, score));
        }
        return new ItemData(key, calculatedList, 0);
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
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return Map.of();
        }

        Map<String, float[]> ranges = itemStatRanges.get(itemName);
        if (ranges == null) {
            return Map.of();
        }

        Map<String, Float> result = new HashMap<>();
        for (Text line : lore.lines()) {
            String raw = line.getString();
            java.util.regex.Matcher m = VANILLA_PATTERN.matcher(raw);
            if (!m.matches()) continue;

            String statName = m.group(1).strip();
            String rawValue = m.group(2);
            String[] keyAndRaw = resolveIdentKey(statName, rawValue);
            String apiKey = keyAndRaw[0];

            float[] range = ranges.get(apiKey);
            if (range == null) {
                continue;
            }

            java.util.regex.Matcher numM = java.util.regex.Pattern.compile("[+-]?([\\d,]+(?:\\.\\d+)?)").matcher(rawValue);
            if (!numM.find()) continue;
            float current = Float.parseFloat(numM.group(1).replace(",", ""));

            float min = range[0], max = range[1];
            if (max == min) continue;

            float percent = (current - min) / (max - min) * 100f;
            if(apiKey.contains("SpellCost") ^ rawValue.contains("-")) { //Invert for negative stats or (exclusive) if it's a spell cost stat
                percent = 100 - percent;
            }
            percent = Math.clamp(percent, 0f, 100f);
            result.put(apiKey, percent);
        }
        return result;
    }

    public static void populateStatRangesFromDatabase() {
        int retries = 30;
        while (WynncraftApiHandler.getCachedItemDatabase() == null && retries-- > 0) {
            // sleep shouldnt cause any problems here cause this function is only called asynchronously
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        if (WynncraftApiHandler.getCachedItemDatabase() == null) {
            return;
        }

        for (String itemName : itemCache.keySet()) {
            com.google.gson.JsonObject itemJson = WynncraftApiHandler.getCachedItemDatabase().get(itemName);
            if (itemJson == null || !itemJson.has("identifications")) continue;

            com.google.gson.JsonObject ids = itemJson.getAsJsonObject("identifications");
            Map<String, float[]> ranges = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : ids.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                com.google.gson.JsonObject rangeObj = entry.getValue().getAsJsonObject();
                if (!rangeObj.has("min") || !rangeObj.has("max")) continue;
                float a = Math.abs(rangeObj.get("min").getAsFloat());
                float b = Math.abs(rangeObj.get("max").getAsFloat());
                float[] range = new float[]{Math.min(a, b), Math.max(a, b)};
                ranges.put(entry.getKey(), range);
            }
            if (!ranges.isEmpty()) {
                itemStatRanges.put(itemName, ranges);
            }
        }
    }

    public static String extractCleanName(ItemStack stack) {
        return stack.getName().getString()
            .replace("À", "")
            .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
            .replaceAll("[^\\x20-\\x7E]", "")
            .replaceAll("^\\s*Shiny\\s+", "")
            .strip();
    }

    public static boolean isTrackedMythic(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return itemCache.containsKey(extractCleanName(stack));
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
        try {
            Class<?> featureClass = Class.forName("com.wynntils.features.tooltips.ItemStatInfoFeature");
            Class<?> managersClass = Class.forName("com.wynntils.core.components.Managers");
            Object featureManager = managersClass.getField("Feature").get(null);
            Object feature = featureManager.getClass().getMethod("getFeatureInstance", Class.class).invoke(featureManager, featureClass);
            if (feature == null) return false;
            return (boolean) feature.getClass().getMethod("isEnabled").invoke(feature);
        } catch (Exception e) {
            return false;
        }
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

                parseAndCacheWeights(response.toString());
            }
        } catch (IOException e) {
            Core.LOGGER.logError("IOException while getting Weights from Wynnpool API: " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseAndCacheWeights(String json) {
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

            WeightData weightData = new WeightData(weightName, scales, 0f);
            grouped.computeIfAbsent(itemName, k -> new ArrayList<>()).add(weightData);
        }

        for (Map.Entry<String, List<WeightData>> entry : grouped.entrySet()) {
            itemCache.put(entry.getKey(), new ItemData(entry.getKey(), entry.getValue(), 0));
        }
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
            Map.entry("Combat Experience", "xpBonus")
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
        if((event.getKey() == GLFW.GLFW_KEY_UP || event.getKey() == GLFW.GLFW_KEY_W) && event.getAction() == GLFW.GLFW_PRESS) {
            upPressed = true;
        }
        if((event.getKey() == GLFW.GLFW_KEY_DOWN || event.getKey() == GLFW.GLFW_KEY_S) && event.getAction() == GLFW.GLFW_PRESS) {
            downPressed = true;
        }
    }

    public static List<Text> modifyTooltip(List<Text> tooltips, ItemStack itemStack) {
        List<Text> modified = new ArrayList<>();

        String key = extractCleanName(itemStack);
        ItemData itemData = itemCache.getOrDefault(key, null);
        ItemData scaleData = weightCacheByHash.getOrDefault(itemStack.getComponents().hashCode(), null);

        for (int i = 1; i < tooltips.size(); i++) {
            Text line = tooltips.get(i);
            modified.add(line);

            if (i == 3 && WynnExtrasConfig.INSTANCE.showScales && WynnExtrasConfig.INSTANCE.showWeight && itemData != null && scaleData != null && !scaleData.data().isEmpty()) {
                final int index = itemData.index();

                modified.add(tooltips.getFirst().copy());

                final AtomicInteger aidx = new AtomicInteger(0);
                for (WeightData data : scaleData.data()) {
                    float score = data.score();
                    String scale = data.weightName();
                    boolean isCurrent = (index == aidx.get() && scaleData.data().size() > 1);
                    Formatting labelColor = isCurrent ? Formatting.WHITE : Formatting.GRAY;

                    Text scoreText = Text.literal(String.format(" %.1f%%", score))
                            .styled(s -> s.withColor(getScaleColor(score)));

                    Text statWeight = Text.literal("↳ " + scale + " Scale")
                            .formatted(labelColor)
                            .styled(s -> isCurrent ? s.withBold(true) : s)
                            .append(scoreText);
                    modified.add(Text.literal("  ").append(statWeight));
                    aidx.incrementAndGet();
                }
                if (scaleData.data().size() > 1) {
                    modified.add(Text.literal("  ↳ Use ↑ / ↓ (W / S) to cycle").formatted(Formatting.DARK_GRAY));
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
        ItemData itemData = itemCache.get(cleanName);
        if (itemData == null) return;

        List<Text> original = new ArrayList<>(lines);
        lines.clear();

        WeightData currentProfile = itemData.data().get(currentIdx);

        for (int i = 0; i < original.size(); i++) {
            Text line = original.get(i);
            if (i == 4 && WynnExtrasConfig.INSTANCE.showWeight) {
                lines.add(Text.empty());
                for (int j = 0; j < scaleData.data().size(); j++) {
                    WeightData wd = scaleData.data().get(j);
                    boolean cur = (j == currentIdx);
                    float score = wd.score();
                    Text scoreText = Text.literal(String.format(" [%.1f%%]", score))
                        .styled(s -> s.withColor(getScaleColor(score)).withBold(cur));
                    Text label = Text.literal("  ↳ " + wd.weightName() + " Scale")
                        .styled(s -> s.withColor(cur ? 0xFFFFFF : 0xAAAAAA).withBold(cur))
                        .copy().append(scoreText);
                    lines.add(label);
                }
                if (scaleData.data().size() > 1) {
                    lines.add(Text.literal("  ↳ Use ↑/↓ (W/S) to cycle").styled(s -> s.withColor(0x555555)));
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
