package julianh06.wynnextras.features.trademarket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects trade market item data for crowdsourcing.
 * When a player hovers over an item in the trade market, captures name, price,
 * rarity, stat rolls, overall percentage, and queues for batch upload.
 */
public class TradeMarketCollector {
    // Dedup: "name|price" -> timestamp (millis)
    private static final ConcurrentHashMap<String, Long> recentlyProcessed = new ConcurrentHashMap<>();
    private static final long DEDUP_TTL_MS = 5 * 60 * 1000; // 5 minutes

    // Queue for pending uploads
    private static final ConcurrentLinkedQueue<JsonObject> pendingListings = new ConcurrentLinkedQueue<>();

    // Flush every 600 ticks (30 seconds)
    private static final int FLUSH_INTERVAL_TICKS = 600;
    private static int tickCounter = 0;

    // Patterns for parsing price from lore
    private static final Pattern STX_PATTERN = Pattern.compile("(\\d+)stx");
    private static final Pattern LE_PATTERN = Pattern.compile("([\\d.]+)\u00BC");
    private static final Pattern EB_PATTERN = Pattern.compile("([\\d.]+)\u00B2\u00BD");
    private static final Pattern E_PATTERN = Pattern.compile("([\\d.]+)\u00B2(?!\u00BD)");

    // Pattern for stat roll percentages in Wynntils tooltip: [XX.X%]
    private static final Pattern PERCENT_PATTERN = Pattern.compile("\\[(\\d+\\.?\\d*)%\\]");

    // Pattern for overall percentage line
    private static final Pattern OVERALL_PATTERN = Pattern.compile("Overall:\\s*(\\d+\\.?\\d*)%");

    // Pattern for shiny stat line (Wynntils format)
    private static final Pattern SHINY_PATTERN = Pattern.compile("\u2B50\\s*(.+)");

    // Trade market screen title identifiers (unicode sequences used by Wynncraft)
    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011", // Item listing / search
            "\uDAFF\uDFE8\uE012"  // Sell confirmation
    );

    /**
     * Check if we're in any trade market screen.
     */
    private static boolean isInTradeMarket() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String title = mc.currentScreen.getTitle().getString();
        for (String marketTitle : TRADE_MARKET_TITLES) {
            if (title.contains(marketTitle)) return true;
        }
        // Fallback: check for "Trade Market" text or common trade market indicators
        return title.toLowerCase().contains("trade");
    }

    /**
     * Called from ItemStatInfoFeatureMixin when an item tooltip is rendered.
     * Extracts item data and queues for upload if in the trade market.
     */
    public static void onItemHovered(ItemStack stack, List<Text> tooltip) {
        if (!WynnExtrasConfig.INSTANCE.crowdSourceTradeMarket) return;
        if (stack == null || stack.isEmpty()) return;
        if (tooltip == null || tooltip.isEmpty()) return;

        // Must be in trade market
        if (!isInTradeMarket()) return;

        // Get WynnItem - accept any type
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(stack);

        String name;
        String rarity;
        String itemType; // "gear", "material", "ingredient", "powder", "tome", "charm", etc.
        String type; // sub-type like "helmet", "wand", etc.

        if (wynnItemOpt.isPresent()) {
            WynnItem wynnItem = wynnItemOpt.get();

            if (wynnItem instanceof GearItem gearItem) {
                name = gearItem.getName();
                GearTier tier = gearItem.getGearTier();
                GearType gearType = gearItem.getGearType();
                rarity = tier != null ? tier.name() : "UNKNOWN";
                itemType = "gear";
                type = gearType != null ? gearType.name().toLowerCase() : "unknown";
            } else if (wynnItem instanceof CraftedGearItem craftedGear) {
                name = stack.getName().getString().replaceAll("\u00A7.", "").trim();
                rarity = "CRAFTED";
                itemType = "crafted_gear";
                type = craftedGear.getGearType() != null ? craftedGear.getGearType().name().toLowerCase() : "unknown";
            } else if (wynnItem instanceof MaterialItem materialItem) {
                name = stack.getName().getString().replaceAll("\u00A7.", "").trim();
                int stars = materialItem.getQualityTier(); // 0-3
                rarity = stars + "_STAR";
                itemType = "material";
                type = "material";
            } else if (wynnItem instanceof IngredientItem ingredientItem) {
                name = ingredientItem.getName();
                int stars = ingredientItem.getQualityTier(); // 0-3
                rarity = stars + "_STAR";
                itemType = "ingredient";
                type = "ingredient";
            } else if (wynnItem instanceof PowderItem powderItem) {
                name = powderItem.getName();
                int tier = powderItem.getTier(); // 1-6
                rarity = "TIER_" + tier;
                itemType = "powder";
                type = "powder";
            } else if (wynnItem instanceof TomeItem tomeItem) {
                name = tomeItem.getName();
                GearTier tomeTier = tomeItem.getGearTier();
                rarity = tomeTier != null ? tomeTier.name() : "UNKNOWN";
                itemType = "tome";
                type = "tome";
            } else if (wynnItem instanceof CharmItem charmItem) {
                name = charmItem.getName();
                GearTier charmTier = charmItem.getGearTier();
                rarity = charmTier != null ? charmTier.name() : "UNKNOWN";
                itemType = "charm";
                type = "charm";
            } else {
                // Any other WynnItem type
                name = stack.getName().getString().replaceAll("\u00A7.", "").trim();
                rarity = getRarityFromTooltip(tooltip);
                itemType = wynnItem.getClass().getSimpleName().replace("Item", "").toLowerCase();
                type = itemType;
            }
        } else {
            // Not a recognized WynnItem but still in TM - capture anyway
            name = stack.getName().getString().replaceAll("\u00A7.", "").trim();
            rarity = getRarityFromTooltip(tooltip);
            itemType = "unknown";
            type = "unknown";
        }

        if (name == null || name.isEmpty()) return;

        // Extract price from lore lines
        long price = extractPrice(stack);
        if (price <= 0) return; // No price = not a trade market listing

        // Extract stat rolls and overall % from tooltip (only relevant for gear)
        JsonArray stats = new JsonArray();
        double overallPercentage = -1;
        String shinyStat = null;

        for (Text line : tooltip) {
            String raw = line.getString().strip();

            // Check for overall percentage
            Matcher overallMatcher = OVERALL_PATTERN.matcher(raw);
            if (overallMatcher.find()) {
                overallPercentage = Double.parseDouble(overallMatcher.group(1));
                continue;
            }

            // Check for stat roll percentages [XX.X%]
            Matcher percentMatcher = PERCENT_PATTERN.matcher(raw);
            if (percentMatcher.find()) {
                float percent = Float.parseFloat(percentMatcher.group(1));

                // Extract stat name and value from the line
                int bracketIdx = raw.lastIndexOf('[');
                if (bracketIdx > 0) {
                    String statPart = raw.substring(0, bracketIdx).strip();
                    String[] parts = statPart.split("\\s+");
                    if (parts.length >= 2) {
                        String value = parts[0];
                        String statName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

                        JsonObject statObj = new JsonObject();
                        statObj.addProperty("name", statName);
                        statObj.addProperty("value", value);
                        statObj.addProperty("percentage", percent);
                        stats.add(statObj);
                    }
                }
                continue;
            }

            // Check for shiny stat
            Matcher shinyMatcher = SHINY_PATTERN.matcher(raw);
            if (shinyMatcher.find()) {
                shinyStat = shinyMatcher.group(1).strip();
            }
        }

        // Dedup on name + price only (two same-name items at different prices are different listings)
        String dedupHash = name + "|" + price;

        long now = System.currentTimeMillis();
        Long lastSeen = recentlyProcessed.get(dedupHash);
        if (lastSeen != null && (now - lastSeen) < DEDUP_TTL_MS) {
            return;
        }
        recentlyProcessed.put(dedupHash, now);

        // Clean expired entries periodically
        if (recentlyProcessed.size() > 200) {
            recentlyProcessed.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_TTL_MS);
        }

        // Build listing JSON
        JsonObject listing = new JsonObject();
        listing.addProperty("name", name);
        listing.addProperty("rarity", rarity);
        listing.addProperty("itemType", itemType);
        listing.addProperty("type", type);
        listing.addProperty("listingPrice", price);
        if (overallPercentage >= 0) {
            listing.addProperty("overallPercentage", overallPercentage);
        }
        if (stats.size() > 0) {
            listing.add("stats", stats);
        }
        if (shinyStat != null) {
            listing.addProperty("shinyStat", shinyStat);
        }
        listing.addProperty("timestamp", now);

        pendingListings.add(listing);

        // Debug: show in chat what was captured
//        StringBuilder debugMsg = new StringBuilder();
//        debugMsg.append("§7[TM] §f").append(name);
//        debugMsg.append(" §7(").append(rarity).append(", ").append(itemType);
//        if (!itemType.equals(type)) debugMsg.append("/").append(type);
//        debugMsg.append(")");
//        debugMsg.append("\n§7  Price: §e").append(formatEmeralds(price));
//        if (overallPercentage >= 0) {
//            debugMsg.append(" §7| Overall: §a").append(String.format("%.1f", overallPercentage)).append("%");
//        }
//        if (stats.size() > 0) {
//            debugMsg.append("\n§7  Stats (").append(stats.size()).append("): ");
//            for (int i = 0; i < Math.min(stats.size(), 5); i++) {
//                JsonObject s = stats.get(i).getAsJsonObject();
//                debugMsg.append("§f").append(s.get("name").getAsString())
//                        .append("=").append(s.get("value").getAsString())
//                        .append(" §a[").append(String.format("%.1f", s.get("percentage").getAsFloat())).append("%]§7");
//                if (i < Math.min(stats.size(), 5) - 1) debugMsg.append(", ");
//            }
//            if (stats.size() > 5) debugMsg.append(" +").append(stats.size() - 5).append(" more");
//        }
//        if (shinyStat != null) {
//            debugMsg.append("\n§7  Shiny: §d").append(shinyStat);
//        }
//        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(debugMsg.toString()));
    }

    /**
     * Try to extract rarity from tooltip lines (for non-gear items).
     * Looks for color-coded rarity keywords.
     */
    private static String getRarityFromTooltip(List<Text> tooltip) {
        for (Text line : tooltip) {
            String raw = line.getString().strip().toLowerCase();
            if (raw.contains("mythic")) return "MYTHIC";
            if (raw.contains("fabled")) return "FABLED";
            if (raw.contains("legendary")) return "LEGENDARY";
            if (raw.contains("rare")) return "RARE";
            if (raw.contains("unique")) return "UNIQUE";
            if (raw.contains("set")) return "SET";
            if (raw.contains("normal")) return "NORMAL";
        }
        return "UNKNOWN";
    }

    private static String formatEmeralds(long emeralds) {
        long stx = emeralds / 262144;
        long remainingAfterStx = emeralds % 262144;
        long le = remainingAfterStx / 4096;
        long remainingAfterLe = remainingAfterStx % 4096;
        long eb = remainingAfterLe / 64;

        StringBuilder sb = new StringBuilder();
        if (stx > 0) sb.append(stx).append("stx ");
        if (le > 0) sb.append(le).append("le ");
        sb.append(eb).append("eb");
        return sb.toString().trim();
    }

    /**
     * Called every tick to handle flush timing.
     */
    public static void tick() {
        tickCounter++;
        if (tickCounter >= FLUSH_INTERVAL_TICKS) {
            tickCounter = 0;
            flush();
        }
    }

    /**
     * Drain the queue and upload batch.
     */
    private static void flush() {
        if (pendingListings.isEmpty()) return;

        List<JsonObject> batch = new ArrayList<>();
        JsonObject item;
        while ((item = pendingListings.poll()) != null) {
            batch.add(item);
        }

        if (batch.isEmpty()) return;

        // System.out.println("[WynnExtras] Flushing " + batch.size() + " trade market listings");
        WynncraftApiHandler.uploadTradeMarketListings(batch);
    }

    /**
     * Extract total price in emeralds from the item's lore lines.
     * Tries multiple patterns: "each" price line, "Price" line, or any line with currency symbols.
     */
    private static long extractPrice(ItemStack stack) {
        net.minecraft.component.type.LoreComponent loreComponent =
                stack.getComponents().get(net.minecraft.component.DataComponentTypes.LORE);
        if (loreComponent == null) return 0;

        List<Text> loreLines = loreComponent.lines();

        // First pass: look for "each" price line (most specific)
        for (Text loreLine : loreLines) {
            String line = loreLine.getString();
            if (line.contains("each")) {
                long total = parsePriceFromLine(line);
                if (total > 0) return total;
            }
        }

        // Second pass: look for "Price" line
        for (Text loreLine : loreLines) {
            String line = loreLine.getString();
            if (line.contains("Price") || line.contains("price")) {
                long total = parsePriceFromLine(line);
                if (total > 0) return total;
            }
        }

        // Third pass: look for any line with currency symbols
        for (Text loreLine : loreLines) {
            String line = loreLine.getString();
            if (line.contains("\u00BC") || line.contains("\u00B2") || line.contains("stx")) {
                long total = parsePriceFromLine(line);
                if (total > 0) return total;
            }
        }

        return 0;
    }

    /**
     * Parse emerald price from a single lore line containing currency notation.
     */
    private static long parsePriceFromLine(String line) {
        long stxValue = 0;
        long leValue = 0;
        long ebValue = 0;
        long eValue = 0;

        Matcher stxMatcher = STX_PATTERN.matcher(line);
        if (stxMatcher.find()) {
            stxValue = Long.parseLong(stxMatcher.group(1)) * 262144;
        }

        Matcher leMatcher = LE_PATTERN.matcher(line);
        if (leMatcher.find()) {
            double leAmount = Double.parseDouble(leMatcher.group(1));
            leValue = (long) (leAmount * 4096);
        }

        Matcher ebMatcher = EB_PATTERN.matcher(line);
        if (ebMatcher.find()) {
            double ebAmount = Double.parseDouble(ebMatcher.group(1));
            ebValue = (long) (ebAmount * 64);
        }

        Matcher eMatcher = E_PATTERN.matcher(line);
        if (eMatcher.find()) {
            eValue = (long) Double.parseDouble(eMatcher.group(1));
        }

        return stxValue + leValue + ebValue + eValue;
    }
}
