package julianh06.wynnextras.features.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ProfessionOverlay {

    private static final long DISPLAY_DURATION_MS = 60 * 1000; // 1 minute
    private static final int MAX_HISTORY = 500;
    private static final long XP_PER_100_PERCENT_AT_132 = 66_287_449L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static ProfessionType lastProfession = null;

    public static ProfessionType getLastProfession() {
        return lastProfession;
    }
    private static int lastProfessionLevel = 0;
    private static long lastXpGainTime = 0;
    private static int saveCounter = 0;

    // Session XP history per charId:profession
    private static final Map<String, List<Float>> xpHistory = new HashMap<>();

    // Session XP gain per charId:profession
    private static final Map<String, Float> sessionXpGain = new HashMap<>();

    // Session action count, start time, and cached actions/hr per charId:profession
    private static final Map<String, Integer> sessionActionCount = new HashMap<>();
    private static final Map<String, Long> sessionStartTime = new HashMap<>();
    private static final Map<String, Double> cachedActionsPerHour = new HashMap<>();
    private static final Map<String, Double> cachedXpPerHour = new HashMap<>();

    // Leaderboard data per charId:profession
    private static final Map<String, LeaderboardEntry> leaderboardData = new HashMap<>();

    // Full leaderboard XP values sorted by rank (index 0 = rank 1) for estimated rank
    private static final Map<String, List<Long>> leaderboardXpList = new HashMap<>();

    public record LeaderboardEntry(int rank, long playerXp, long nextPlayerXp) {}

    private static String getOverflowKey(ProfessionType profession) {
        String charId = Models.Character.getId();
        if (charId == null || charId.isEmpty()) charId = "unknown";
        return charId + ":" + profession.getDisplayName();
    }

    public static float getOverflow(ProfessionType profession) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionOverflowXp == null) return 0;
        return c.professionOverflowXp.getOrDefault(getOverflowKey(profession), 0f);
    }

    public static void setOverflow(ProfessionType profession, float amount) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionOverflowXp == null) c.professionOverflowXp = new HashMap<>();
        c.professionOverflowXp.put(getOverflowKey(profession), amount);
        WynnExtrasConfig.save();
    }

    public static float getGoal(ProfessionType profession) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionGoals == null) return 0;
        return c.professionGoals.getOrDefault(getOverflowKey(profession), 0f);
    }

    public static void setGoal(ProfessionType profession, float amount) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionGoals == null) c.professionGoals = new HashMap<>();
        c.professionGoals.put(getOverflowKey(profession), amount);
        WynnExtrasConfig.save();
    }

    public static void clearGoal(ProfessionType profession) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionGoals == null) return;
        c.professionGoals.remove(getOverflowKey(profession));
        WynnExtrasConfig.save();
    }

    /**
     * Called on character swap to reset overlay state so stale data doesn't show.
     */
    public static void onCharacterSwap() {
        lastProfession = null;
        lastProfessionLevel = 0;
        lastXpGainTime = 0;
        sessionXpGain.clear();
        xpHistory.clear();
        sessionActionCount.clear();
        sessionStartTime.clear();
        cachedActionsPerHour.clear();
        cachedXpPerHour.clear();
    }

    public static void initOverflowFromApi(String characterId, CharacterData charData) {
        if (charData == null || charData.getProfessions() == null) return;
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (c.professionOverflowXp == null) c.professionOverflowXp = new HashMap<>();

        String charIdClean = (characterId == null || characterId.isEmpty()) ? "unknown" : characterId;

        for (Map.Entry<String, Profession> entry : charData.getProfessions().entrySet()) {
            Profession prof = entry.getValue();
            if (prof.getLevel() < 132) continue;

            // Convert API profession name to ProfessionType display name
            String apiName = entry.getKey(); // e.g. "mining", "armouring"
            ProfessionType profType = ProfessionType.fromString(apiName);
            if (profType == null) continue;

            String key = charIdClean + ":" + profType.getDisplayName();
            float apiOverflow = (float) ((long) prof.getXpPercent() * XP_PER_100_PERCENT_AT_132 / 100L);

            // Only set from API if we don't already have a higher value
            // (in-game tracking may be ahead of API cache)
            float existing = c.professionOverflowXp.getOrDefault(key, 0f);
            if (apiOverflow > existing) {
                c.professionOverflowXp.put(key, apiOverflow);
                WynnExtras.LOGGER.info("[WynnExtras] Initialized " + profType.getDisplayName() + " overflow from API: " + formatXp(apiOverflow) + " (xpPercent=" + prof.getXpPercent() + ")");
            }
        }
        WynnExtrasConfig.save();
    }

    public static void onXpGain(ProfessionType profession, float gainedXpRaw) {
        lastProfession = profession;
        lastProfessionLevel = Models.Profession.getLevel(profession);
        lastXpGainTime = System.currentTimeMillis();

        // Track history
        String key = getOverflowKey(profession);
        List<Float> history = xpHistory.computeIfAbsent(key, k -> new ArrayList<>());
        history.add(gainedXpRaw);
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }

        // Track session gain
        sessionXpGain.merge(key, gainedXpRaw, Float::sum);

        // Track action count + start time for actions/hour
        sessionActionCount.merge(key, 1, Integer::sum);
        sessionStartTime.putIfAbsent(key, System.currentTimeMillis());

        // Update cached actions/hr and xp/hr
        int count = sessionActionCount.get(key);
        if (count > 1) {
            long elapsedMs = System.currentTimeMillis() - sessionStartTime.get(key);
            if (elapsedMs > 0) {
                double hours = elapsedMs / 3_600_000.0;
                cachedActionsPerHour.put(key, count / hours);
                cachedXpPerHour.put(key, (double) sessionXpGain.get(key) / hours);
            }
        }

        // Track overflow
        int level = Models.Profession.getLevel(profession);
        if (level >= 132) {
            WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
            if (c.professionOverflowXp == null) c.professionOverflowXp = new HashMap<>();
            float current = c.professionOverflowXp.getOrDefault(key, 0f);
            c.professionOverflowXp.put(key, current + gainedXpRaw);
            saveCounter++;
            if (saveCounter >= 10) {
                saveCounter = 0;
                WynnExtrasConfig.save();
            }
        }
    }

    /**
     * Reload: resets session XP gain, clears xpHistory, re-fetches overflow from API, re-fetches leaderboard.
     */
    public static void reload() {
        sessionXpGain.clear();
        xpHistory.clear();
        leaderboardData.clear();
        leaderboardXpList.clear();
        sessionActionCount.clear();
        sessionStartTime.clear();
        cachedActionsPerHour.clear();
        cachedXpPerHour.clear();

        // Re-fetch overflow from API
        if (McUtils.player() != null) {
            String playerName = McUtils.player().getName().getString();
            julianh06.wynnextras.utils.WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(playerData -> {
                if (playerData == null) return;
                String characterId = Models.Character.getId();
                if (characterId == null || characterId.isEmpty()) return;

                Map<String, CharacterData> characters = playerData.getCharacters();
                if (characters == null) return;

                for (Map.Entry<String, CharacterData> entry : characters.entrySet()) {
                    String apiCharId = entry.getKey().replace("-", "");
                    if (apiCharId.contains(characterId) || characterId.contains(apiCharId.substring(0, Math.min(8, apiCharId.length())))) {
                        initOverflowFromApi(characterId, entry.getValue());
                        break;
                    }
                }
            });

            // Re-fetch leaderboard
            fetchLeaderboardForAllProfessions();
        }
    }

    /**
     * Fetch leaderboard data for all max-level professions on the current character.
     */
    public static void fetchLeaderboardForAllProfessions() {
        if (McUtils.player() == null) return;
        String playerUuid = McUtils.player().getUuidAsString();

        for (ProfessionType prof : ProfessionType.values()) {
            int level = Models.Profession.getLevel(prof);
            if (level < 132) continue;

            fetchLeaderboardForProfession(prof, playerUuid);
        }
    }

    private static void fetchLeaderboardForProfession(ProfessionType profession, String playerUuid) {
        // API uses lowercase profession name + "Level"
        String profName = profession.name().toLowerCase();
        String url = "https://api.wynncraft.com/v3/leaderboards/" + profName + "Level?resultLimit=999";

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    WynnExtras.LOGGER.error("[WynnExtras] Leaderboard fetch failed for " + profName + ": HTTP " + response.statusCode());
                    return;
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String key = getOverflowKeyStatic(profession);

                // Build sorted XP list (index 0 = rank 1) and find player
                String normalizedUuid = playerUuid.replace("-", "").toLowerCase();
                int playerRank = -1;
                long playerXp = 0;
                long nextPlayerXp = 0;

                // Collect all entries with their rank
                TreeMap<Integer, JsonObject> rankedEntries = new TreeMap<>();
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    try {
                        int rank = Integer.parseInt(entry.getKey());
                        rankedEntries.put(rank, entry.getValue().getAsJsonObject());
                    } catch (NumberFormatException e) {
                        // skip non-numeric keys
                    }
                }

                // Build XP list sorted by rank
                List<Long> xpList = new ArrayList<>();
                for (Map.Entry<Integer, JsonObject> entry : rankedEntries.entrySet()) {
                    JsonObject playerEntry = entry.getValue();
                    JsonObject metadata = playerEntry.getAsJsonObject("metadata");
                    xpList.add(metadata.get("xp").getAsLong());

                    // Check if this is our player
                    String entryUuid = playerEntry.get("uuid").getAsString().replace("-", "").toLowerCase();
                    if (entryUuid.equals(normalizedUuid)) {
                        playerRank = entry.getKey();
                        playerXp = metadata.get("xp").getAsLong();

                        // Person one rank above
                        if (playerRank > 1) {
                            String aboveKey = String.valueOf(playerRank - 1);
                            if (rankedEntries.containsKey(playerRank - 1)) {
                                JsonObject aboveMeta = rankedEntries.get(playerRank - 1).getAsJsonObject("metadata");
                                nextPlayerXp = aboveMeta.get("xp").getAsLong();
                            }
                        }
                    }
                }

                leaderboardXpList.put(key, xpList);

                if (playerRank > 0) {
                    leaderboardData.put(key, new LeaderboardEntry(playerRank, playerXp, nextPlayerXp));
                    WynnExtras.LOGGER.info("[WynnExtras] Leaderboard " + profName + ": #" + playerRank + " (XP: " + playerXp + ", next: " + nextPlayerXp + ")");
                } else {
                    // Not on leaderboard - store with rank -1
                    leaderboardData.put(key, new LeaderboardEntry(-1, 0, 0));
                    WynnExtras.LOGGER.info("[WynnExtras] Leaderboard " + profName + ": Unranked");
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Leaderboard fetch error for " + profName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Static version of getOverflowKey that uses current character ID.
     */
    private static String getOverflowKeyStatic(ProfessionType profession) {
        String charId = Models.Character.getId();
        if (charId == null || charId.isEmpty()) charId = "unknown";
        return charId + ":" + profession.getDisplayName();
    }

    private static float getAverage(List<Float> list, int n) {
        if (list == null || list.isEmpty()) return 0;
        int count = Math.min(n, list.size());
        float sum = 0;
        for (int i = list.size() - count; i < list.size(); i++) {
            sum += list.get(i);
        }
        return sum / count;
    }

    public static void register() {
        HudRenderCallback.EVENT.register(ProfessionOverlay::renderHud);
    }

    /**
     * Called from Screen.render mixin — renders on top of screens when in a crafting station.
     */
    public static void renderOnScreen(DrawContext ctx) {
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;
        doRender(ctx);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (MinecraftClient.getInstance().options.hudHidden) return;
        // Skip HUD render when a screen is open (renderOnScreen handles that case)
        if (MinecraftClient.getInstance().currentScreen != null) return;
        doRender(ctx);
    }

    private static void doRender(DrawContext ctx) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (!c.professionOverlayEnabled) return;
        if (!Models.WorldState.onWorld()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (lastProfession == null) return;

        long elapsed = System.currentTimeMillis() - lastXpGainTime;
        if (elapsed > DISPLAY_DURATION_MS) return;

        int level = lastProfessionLevel;
        CappedValue xp = Models.Profession.getXP(lastProfession);
        String key = getOverflowKey(lastProfession);

        // Line 1: Profession name + level
        String line1 = lastProfession.getDisplayName() + " Lv. " + level;

        // Line 2: Leaderboard info (only for max level)
        String line2lb = null;
        if (level >= 132) {
            LeaderboardEntry lb = leaderboardData.get(key);
            if (lb != null) {
                if (lb.rank() > 0) {
                    float sessionGain = sessionXpGain.getOrDefault(key, 0f);
                    long currentXp = lb.playerXp() + (long) sessionGain;

                    // Estimate current rank by checking if we've passed people above
                    int estimatedRank = lb.rank();
                    List<Long> xpList = leaderboardXpList.get(key);
                    if (xpList != null) {
                        // xpList index 0 = rank 1, so rank N = index N-1
                        for (int i = lb.rank() - 2; i >= 0; i--) {
                            if (currentXp >= xpList.get(i)) {
                                estimatedRank = i + 1; // rank is 1-indexed
                            } else {
                                break;
                            }
                        }
                    }

                    String lbStr;
                    if (estimatedRank != lb.rank()) {
                        lbStr = "LB: #" + lb.rank() + " -> ~#" + estimatedRank;
                    } else {
                        lbStr = "LB: #" + lb.rank();
                    }

                    // For Next: XP gap to the person one rank above estimated rank
                    if (xpList != null && estimatedRank > 1 && estimatedRank - 2 < xpList.size()) {
                        long aboveXp = xpList.get(estimatedRank - 2);
                        float forNext = Math.max(0, aboveXp - currentXp);
                        lbStr += " | For Next: " + formatXp(forNext);
                    }

                    line2lb = lbStr;
                } else {
                    line2lb = "LB: Unranked";
                }
            }
        }

        // Line 3: Overflow + session gain (or regular XP for non-max)
        String line3xp;
        if (level >= 132) {
            float overflow = getOverflow(lastProfession);
            float sessionGain = sessionXpGain.getOrDefault(key, 0f);
            line3xp = lastProfession.getDisplayName() + ": " + formatXp(overflow);
            if (sessionGain > 0) {
                line3xp += " || Gain +" + formatXp(sessionGain);
            }
        } else {
            double pct = xp.max() > 0 ? (double) xp.current() / xp.max() * 100.0 : 0;
            line3xp = formatXp(xp.current()) + "/" + formatXp(xp.max()) + " (" + String.format("%.1f", pct) + "%)";
            // Add actions to next level + time to level
            float remaining = xp.max() - xp.current();
            List<Float> historyForLevel = xpHistory.get(key);
            if (historyForLevel != null && !historyForLevel.isEmpty()) {
                float bestAvg = getAverage(historyForLevel, Math.min(100, historyForLevel.size()));
                if (bestAvg > 0) {
                    int actionsNeeded = (int) Math.ceil(remaining / bestAvg);
                    line3xp += " | ~" + actionsNeeded + " to lvl";
                }
            }
            Double xph = cachedXpPerHour.get(key);
            if (xph != null && xph > 0) {
                double hoursLeft = remaining / xph;
                line3xp += " | " + formatTime(hoursLeft);
            }
        }

        // Line 4: Averages
        List<Float> history = xpHistory.get(key);
        String line4avg = null;
        float avg100 = 0;
        if (history != null && !history.isEmpty()) {
            int total = history.size();
            float avg10 = getAverage(history, 10);
            StringBuilder sb = new StringBuilder("Avg xp: " + formatXp(avg10) + " (10)");
            if (total >= 100) {
                avg100 = getAverage(history, 100);
                sb.append(" | ").append(formatXp(avg100)).append(" (100)");
            }
            if (total > 100) {
                float avgAll = getAverage(history, total);
                avg100 = avg100 > 0 ? avg100 : getAverage(history, total);
                sb.append(" | ").append(formatXp(avgAll)).append(" (").append(total).append(")");
            }
            // Append cached actions/hour and xp/hour
            Double aph = cachedActionsPerHour.get(key);
            if (aph != null) {
                sb.append(" | ").append(String.format("%.0f", aph)).append("/hr");
            }
            Double xph = cachedXpPerHour.get(key);
            if (xph != null) {
                sb.append(" | ").append(formatXp(xph.floatValue())).append(" xp/hr");
            }
            line4avg = sb.toString();
        }

        // Line 5: Goal
        String line5goal = null;
        float goal = getGoal(lastProfession);
        if (goal > 0 && level >= 132) {
            float overflow = getOverflow(lastProfession);
            float remaining = goal - overflow;
            if (remaining <= 0) {
                line5goal = "Goal: " + formatXp(goal) + " | COMPLETE!";
            } else {
                String craftsStr = "";
                float bestAvg = avg100 > 0 ? avg100 : (history != null && !history.isEmpty() ? getAverage(history, history.size()) : 0);
                if (bestAvg > 0) {
                    int craftsNeeded = (int) Math.ceil(remaining / bestAvg);
                    craftsStr = " (~" + craftsNeeded + " crafts)";
                }
                line5goal = "Goal: " + formatXp(goal) + " | Left: " + formatXp(remaining) + craftsStr;
            }
        }

        // Render
        float scale = c.professionOverlayScale;
        int baseX = c.professionOverlayX;
        int baseY = c.professionOverlayY;
        int lineH = (int) (10 * scale);
        int lineIdx = 0;

        drawLine(ctx, mc, line1, baseX, baseY, lineH, lineIdx++, scale, 0xFFFFFF00);
        if (line2lb != null) {
            drawLine(ctx, mc, line2lb, baseX, baseY, lineH, lineIdx++, scale, 0xFFFFAA00);
        }
        drawLine(ctx, mc, line3xp, baseX, baseY, lineH, lineIdx++, scale, 0xFFFFFFFF);
        if (line4avg != null) {
            drawLine(ctx, mc, line4avg, baseX, baseY, lineH, lineIdx++, scale, 0xFFAAAAAA);
        }
        if (line5goal != null) {
            int goalColor = (goal > 0 && getOverflow(lastProfession) >= goal) ? 0xFF44FF44 : 0xFF55FFFF;
            drawLine(ctx, mc, line5goal, baseX, baseY, lineH, lineIdx++, scale, goalColor);
        }

    }

    private static void drawLine(DrawContext ctx, MinecraftClient mc, String text, int baseX, int baseY, int lineH, int lineIdx, float scale, int color) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY + lineIdx * lineH);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, text, 0, 0, color, true);
        ctx.getMatrices().popMatrix();
    }

    private static void drawLineText(DrawContext ctx, MinecraftClient mc, Text text, int baseX, int baseY, int lineH, int lineIdx, float scale) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY + lineIdx * lineH);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, text.asOrderedText(), 0, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().popMatrix();
    }

    private static String formatTime(double hours) {
        if (hours < 1.0 / 60) return "<1m";
        if (hours < 1) return String.format("%.0fm", hours * 60);
        if (hours < 24) return String.format("%.1fh", hours);
        return String.format("%.1fd", hours / 24);
    }

    private static String formatXp(float xp) {
        if (WynnExtrasConfig.INSTANCE.professionOverlayExactXp) {
            return String.format("%,.0f", xp);
        }
        if (xp >= 1_000_000_000) return String.format("%.1fB", xp / 1_000_000_000);
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000);
        if (xp >= 10_000) return String.format("%.1fK", xp / 1_000);
        return String.format("%.0f", xp);
    }
}
