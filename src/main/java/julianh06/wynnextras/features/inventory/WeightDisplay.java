package julianh06.wynnextras.features.inventory;

import com.google.gson.*;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.tooltips.ItemStatInfoFeature;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.wynn.ColorScaleUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.Core;
import julianh06.wynnextras.event.KeyInputEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;


@WEModule
public class WeightDisplay {
//    private static final KeyBind cycleKeyBind = new KeyBind( //There were some issues with key conflicts
//            "Mythic item weight display cycle", GLFW.GLFW_KEY_TAB, //ill leve this here if i ever find a solution
//            true, null, (a) -> { tabPressed = true; }); //but until then ill use the arrow keys to go up/down

    public record WeightData(String weightName, Map<String, Float> identifications, Float score) {}
    public record ItemData(String name, List<WeightData> data, int index) {}

    //For the item info itself, e.g hero, warp
    public static Map<String, ItemData> itemCache = new ConcurrentHashMap<>();

    //For the individual items with calculated scales
    public static final Map<String, ItemData> weightCache = new ConcurrentHashMap<>();

    public static boolean upPressed = false;
    public static boolean downPressed = false;
    public static ItemStack currentHoveredStack = null;
    public static WynnItem currentHoveredWynnitem = null;

    public WeightDisplay() { //runs on gamestart
        CompletableFuture.runAsync(WeightDisplay::getWeightsFromWynnpool);
    }

    public static WeightData getCachedWeight(String encodedItem, ItemStack itemStack, List<Text> wynntilsTooltip) {
        return getCachedWeight(encodedItem, false, itemStack, wynntilsTooltip);
    }

    public static WeightData getCachedWeight(String encodedItem, boolean forceUpdate, ItemStack itemStack, List<Text> wynntilsTooltip) {
        ItemData itemData = weightCache.get(encodedItem);
        WeightData weightData = null;
        if(itemData != null) {
            weightData = itemData.data.get(itemData.index);
        }

        if (forceUpdate && weightData == null) {
            calculateScale(encodedItem, itemStack, wynntilsTooltip);
        }

        return weightData;
    }

    public static void calculateScale(String encodedItem, ItemStack itemStack, List<Text> wynntilsTooltip) {
        if(itemStack.getCustomName() == null) return;

        String key = itemStack.getCustomName().getString()
                .replace("À", "")
                .replaceAll("§[0-9a-fk-or]", "")
                .replace("⬡ Shiny ", "")
                .strip();
        ItemData weightProfile = itemCache.get(key);
        if (weightProfile == null) return;

        List<WeightData> calculatedList = new ArrayList<>();

        for (WeightData weightData : weightProfile.data) {
            Map<String, Float> identifications = extractIdentifications(wynntilsTooltip);
            if (identifications.isEmpty()) return;

            Map<String, Float> scaled = new HashMap<>();
            float score = 0f;

            for (Map.Entry<String, Float> entry : identifications.entrySet()) {
                String stat = entry.getKey();
                Float value = entry.getValue();
                Float scale = weightData.identifications.getOrDefault(stat, 0f);
                scaled.put(stat, value * scale);
                if(scale < 0) {
                    // If the weight is negative, we need to invert the percentage and make the scale positive
                    score += Math.abs((100 - value) * scale);
                } else {
                    score += value * scale;
                }
            }

            WeightData calculated = new WeightData(weightData.weightName, scaled, score);
            calculatedList.add(calculated);
        }

        ItemData result = new ItemData(encodedItem, calculatedList, 0);
        weightCache.put(encodedItem, result);
    }

    public static Map<String, Float> extractIdentifications(List<Text> wynntilsTooltip) {
        Map<String, Float> percentages = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return percentages;

        for (Text line : wynntilsTooltip) {
            String raw = line.getString().strip();

            //from wynntils: e.g: -100 health [50.0%]
            if (raw.matches(".*\\[\\d+(\\.\\d+)?%\\]$")) {
                int bracketStart = raw.lastIndexOf('[');
                int bracketEnd = raw.lastIndexOf('%');
                if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
                    try {
                        float percent = Float.parseFloat(raw.substring(bracketStart + 1, bracketEnd));

                        // Extrahiere Stat-Namen und prüfe ob raw oder nicht
                        String statPart = raw.substring(0, bracketStart).strip();
                        boolean isRaw = !statPart.contains("%");
//                        boolean isPerSecond

                        String[] parts = statPart.split("\\s+");
                        if (parts.length >= 2) {
                            String stat = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                            String key = statToApiKey.getOrDefault(stat, fallbackCamelCase(stat));
                            if (key.contains("Cost")) {
                                for (Map.Entry<String, String> entry : spellCostMap.entrySet()) {
                                    if (key.toLowerCase().contains(entry.getKey().toLowerCase())) {
                                        key = entry.getValue();
                                        break;
                                    }
                                }
                            }

                            if(isRaw && !key.equals("healthRegen") && !key.equals("manaRegen") && !key.contains("Steal") && !key.contains("poison") && !key.contains("jump") && !key.contains("AttackSpeed")) {
                                key = key.substring(0,1).toUpperCase() + key.substring(1);
                                key = "raw" + key;
                            } else if (isRaw && key.equals("healthRegen")) {
                                key = key + "Raw"; //healthRegen is the only stat that has "Raw" at the end of the string instead of the start
                            } else if (isRaw && key.contains("AttackSpeed")) {
                                key = "rawAttackSpeed"; //healthRegen is the only stat that has "Raw" at the end of the string instead of the start
                            }
                            percentages.put(key, percent);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }


        return percentages;
    }


    private static void getWeightsFromWynnpool() {
        try {
            URL url = new URI("https://api.wynnpool.com/item/weight/all").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
//            conn.setRequestProperty("Content-Type", "application/json");

//            String body = "{\"encoded_item\": \"" + encodedItem + "\"}";
//            try (OutputStream os = conn.getOutputStream()) {
//                os.write(body.getBytes(StandardCharsets.UTF_8));
//            }

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

                // Parse JSON
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

        // Indexing and caching
        for (Map.Entry<String, List<WeightData>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<WeightData> data = entry.getValue();
            ItemData itemData = new ItemData(name, data, 0); // index = 0 by default
            itemCache.put(name, itemData);
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
            Map.entry("Poison", "poison")
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
        if(event.getKey() == GLFW.GLFW_KEY_UP && event.getAction() == GLFW.GLFW_PRESS) {
            upPressed = true;
        }
        if(event.getKey() == GLFW.GLFW_KEY_DOWN && event.getAction() == GLFW.GLFW_PRESS) {
            downPressed = true;
        }
    }

    //the getWynnItemTooltipWithScale method has been moved to TooltipUtilsMixin

    public static List<Text> modifyTooltip(List<Text> tooltips, WeightDisplay.WeightData weightData, ItemStack itemStack, String encodedItem) {
        List<Text> modified = new ArrayList<>();

        if (!tooltips.isEmpty()) {
            if (itemStack.getCustomName() != null) {
                String key = itemStack.getCustomName().getString()
                        .replace("À", "")
                        .replaceAll("§[0-9a-fk-or]", "")
                        .replace("⬡ Shiny ", "")
                        .strip();

                ItemData itemData = itemCache.getOrDefault(key, null);
                ItemData scaleData = weightCache.getOrDefault(encodedItem, null);

                if (itemData != null && scaleData != null && !scaleData.data().isEmpty()) {
                    final int index = itemData.index();

                    Text first = tooltips.getFirst();
                    modified.add(first.copy());

                    final AtomicInteger idx = new AtomicInteger(0);
                    for(WeightData data : scaleData.data()) {
                        float score = data.score();
                        String scale = data.weightName();
                        Formatting color = (index == idx.get() && scaleData.data().size() > 1 && WynnExtrasConfig.INSTANCE.showScales) ? Formatting.WHITE : Formatting.GRAY;

                        ItemStatInfoFeature itemStatInfoFeature = Managers.Feature.getFeatureInstance(ItemStatInfoFeature.class);
                        Text statWeight = Text.literal("↳ " + scale + " Scale")
                                .formatted(color)
                                .styled(style -> index == idx.get() && scaleData.data().size() > 1 && WynnExtrasConfig.INSTANCE.showScales
                                        ? style.withBold(true)
                                        : style
                                )
                                .append(ColorScaleUtils.getPercentageTextComponent(
                                        itemStatInfoFeature.getColorMap(),
                                        score,
                                        itemStatInfoFeature.colorLerp.get(),
                                        itemStatInfoFeature.decimalPlaces.get()
                                ));
                        modified.add(Text.literal("  ").append(statWeight));
                        idx.incrementAndGet();
                    }
                    if(scaleData.data().size() > 1 && WynnExtrasConfig.INSTANCE.showScales) {
                        modified.add(Text.literal("  ↳ Use ↑ / ↓ to cycle").formatted(Formatting.DARK_GRAY));
                    }
                }
            }
        }

        for (int i = 1; i < tooltips.size(); i++) {
            Text line = tooltips.get(i);
            modified.add(line);

            if (!WynnExtrasConfig.INSTANCE.showScales) continue;

            // Try to find matching stat
            StyledText normed = StyledText.fromComponent(line).getNormalized();
            Matcher statMatcher = normed.getMatcher(WynnItemParser.IDENTIFICATION_STAT_PATTERN);
            if (!statMatcher.matches()) continue;

            StatType statType = Models.Stat.fromDisplayName(
                    statMatcher.group(6).split("§")[0], statMatcher.group(3)
            );
            if(statType == null) continue;

            String apiName = statType.getApiName();
            if(itemStack.getCustomName() == null) continue;
            String key = itemStack.getCustomName().getString()
                    .replace("À", "")
                    .replaceAll("§[0-9a-fk-or]", "")
                    .replace("⬡ Shiny ", "")
                    .strip();
            WeightDisplay.ItemData itemData = itemCache.getOrDefault(key, null);
            if(itemData == null) continue;

            Float weight = itemData.data().get(itemData.index()).identifications().get(apiName);
            if(weight == null) continue;
            String formattedWeight = String.format("%.02f", weight * 100);

            Text statWeight = Text.literal("  ↳ Weight: " + formattedWeight + "%")
                    .formatted(Formatting.DARK_GRAY);
            modified.add(statWeight);
        }

        return modified;
    }
}