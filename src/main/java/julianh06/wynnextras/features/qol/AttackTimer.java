package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttackTimer {
    private static final Pattern ATTACK_PATTERN = Pattern.compile("§b- \\d\\d:\\d\\d §3.*", Pattern.CASE_INSENSITIVE);
    // Matches "<anything>: <Territory> defense is <Level>" anywhere in the line.
    // Doesn't anchor to start so it works inside guild chat prefixes like "[Guild] Name: ...".
    private static final Pattern DEFENSE_BROADCAST = Pattern.compile(
            ":\\s*(?<terr>[^:]+?)\\s+defense is\\s+(?<def>Very Low|Low|Medium|High|Very High)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WAR_START = Pattern.compile("The war for (?<terr>.+?) will start in \\d+ minutes?\\.");
    private static final int DEFAULT_NORMAL_COLOR = 0xFFAA00;
    private static final int DEFAULT_CURRENT_TERRITORY_COLOR = 0xFFFF55;
    private static final int DEFAULT_VERY_LOW_DEFENSE_COLOR = 0x55FF55;
    private static final int DEFAULT_LOW_DEFENSE_COLOR = 0x55FF55;
    private static final int DEFAULT_MEDIUM_DEFENSE_COLOR = 0xFFFF55;
    private static final int DEFAULT_HIGH_DEFENSE_COLOR = 0xFF5555;
    private static final int DEFAULT_VERY_HIGH_DEFENSE_COLOR = 0xAA0000;

    public static String soonestTerritory = null;
    // territory -> defense level ("Very Low" / "Low" / "Medium" / "High" / "Very High")
    private static final Map<String, String> cachedDefenses = new HashMap<>();
    private static final Set<String> activeAttackTerritories = new HashSet<>();
    // Last territory the local player personally looked up (for auto-broadcast)
    private static String lastSelfLookupTerritory = null;
    private static long lastSelfLookupAt = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(AttackTimer::render);
        WEEventBus.registerEventListener(new AttackTimer());
        // Scan open "Attacking: X" menus for defense info and cache it
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.attackTimerMenuEnabled) return;
            if (!(client.currentScreen instanceof GenericContainerScreen gcs)) return;
            String title = gcs.getTitle().getString();
            if (!title.contains("Attacking: ")) return;
            String territory = title.split(": ", 2)[1];
            ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
            if (handler == null || handler.slots.size() <= 13) return;
            ItemStack info = handler.slots.get(13).getStack();
            if (info.isEmpty()) return;
            LoreComponent lore = info.get(DataComponentTypes.LORE);
            if (lore == null) return;
            for (Text line : lore.lines()) {
                String clean = line.getString().replaceAll("§[0-9a-fk-or]", "");
                if (clean.contains("Territory Defences")) {
                    String[] parts = clean.split(":\\s*", 2);
                    if (parts.length == 2) {
                        cachedDefenses.put(territory, parts[1].trim());
                        lastSelfLookupTerritory = territory;
                        lastSelfLookupAt = System.currentTimeMillis();
                    }
                    return;
                }
            }
        });
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        if (!WynnExtrasConfig.INSTANCE.attackTimerMenuEnabled) return;
        try {
            String raw = event.message.getString().replaceAll("§[0-9a-fk-orx]", "").trim();
            if (raw.isEmpty()) return;

            // Guildmate defense broadcast
            Matcher m = DEFENSE_BROADCAST.matcher(raw);
            if (m.find()) {
                cachedDefenses.put(m.group("terr").trim(), m.group("def").trim());
                return;
            }

            // "The war for X will start in N minutes" — auto-broadcast our cached defense
            if (WynnExtrasConfig.INSTANCE.attackTimerAutoBroadcast) {
                Matcher ws = WAR_START.matcher(raw);
                if (ws.find()) {
                    String terr = ws.group("terr").trim();
                    if (terr.equals(lastSelfLookupTerritory)
                            && System.currentTimeMillis() - lastSelfLookupAt < 5000) {
                        String def = cachedDefenses.get(terr);
                        if (def != null && MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.networkHandler
                                    .sendChatCommand("g " + terr + " defense is " + def);
                            lastSelfLookupTerritory = null;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static List<String> getUpcomingAttacks() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return new ArrayList<>();

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<String> upcoming = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (ScoreboardObjective obj : scoreboard.getObjectives()) {
            for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(obj)) {
                String name = entry.name().getString();
                if (ATTACK_PATTERN.matcher(name).find()) {
                    String stripped = strip(name).substring(2);
                    if (!seen.contains(stripped)) {
                        seen.add(stripped);
                        upcoming.add(stripped);
                    }
                }
            }
            break;
        }
        return upcoming;
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }

    private static int rgb(Integer color, int fallback) {
        return color == null ? fallback : color & 0xFFFFFF;
    }

    private static int normalColor() {
        return rgb(WynnExtrasConfig.INSTANCE.attackTimerNormalColor, DEFAULT_NORMAL_COLOR);
    }

    private static int currentTerritoryColor() {
        return rgb(WynnExtrasConfig.INSTANCE.attackTimerCurrentTerritoryColor, DEFAULT_CURRENT_TERRITORY_COLOR);
    }

    private static int defenseColor(String def) {
        if (def == null) return 0xAAAAAA;
        return switch (def) {
            case "Very Low" -> rgb(WynnExtrasConfig.INSTANCE.attackTimerVeryLowDefenseColor, DEFAULT_VERY_LOW_DEFENSE_COLOR);
            case "Low" -> rgb(WynnExtrasConfig.INSTANCE.attackTimerLowDefenseColor, DEFAULT_LOW_DEFENSE_COLOR);
            case "Medium" -> rgb(WynnExtrasConfig.INSTANCE.attackTimerMediumDefenseColor, DEFAULT_MEDIUM_DEFENSE_COLOR);
            case "High" -> rgb(WynnExtrasConfig.INSTANCE.attackTimerHighDefenseColor, DEFAULT_HIGH_DEFENSE_COLOR);
            case "Very High" -> rgb(WynnExtrasConfig.INSTANCE.attackTimerVeryHighDefenseColor, DEFAULT_VERY_HIGH_DEFENSE_COLOR);
            default -> 0xAAAAAA;
        };
    }

    private static String getDefenseLevel(String territory) {
        String cached = cachedDefenses.get(territory);
        if (cached != null) return cached;

        try {
            var poi = Models.Territory.getTerritoryPoiFromAdvancement(territory);
            if (poi == null || poi.getTerritoryInfo() == null) return null;
            String def = strip(poi.getTerritoryInfo().getDefences().getAsString()).trim();
            if (def.isEmpty()) return null;
            cachedDefenses.put(territory, def);
            return def;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static String getAttackTerritory(String attack) {
        int firstSpace = attack.indexOf(' ');
        if (firstSpace < 0 || firstSpace + 1 >= attack.length()) return "";
        return attack.substring(firstSpace + 1).trim();
    }

    private static void clearFinishedAttackDefenses(List<String> attacks) {
        Set<String> currentTerritories = new HashSet<>();
        for (String attack : attacks) {
            String territory = getAttackTerritory(attack);
            if (!territory.isEmpty()) currentTerritories.add(territory);
        }

        activeAttackTerritories.removeIf(territory -> {
            if (currentTerritories.contains(territory)) return false;
            cachedDefenses.remove(territory);
            return true;
        });
        activeAttackTerritories.addAll(currentTerritories);
    }

    private static String getCurrentTerritory() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;

        try {
            Vec3d position = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            var profile = Models.Territory.getTerritoryProfileForPosition(position);
            return profile == null ? null : profile.getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static MutableText styledPart(String text, int color, boolean bold) {
        return Text.literal(text).styled(style -> style
                .withColor(net.minecraft.text.TextColor.fromRgb(color & 0xFFFFFF))
                .withBold(bold));
    }

    private static Text buildAttackLine(String time, String territory, String defense, boolean currentTerritory) {
        int lineColor = currentTerritory ? currentTerritoryColor() : normalColor();
        MutableText line = styledPart(time + " " + territory, lineColor, currentTerritory);
        if (defense != null) {
            line.append(styledPart(" (", lineColor, currentTerritory));
            line.append(styledPart(defense, defenseColor(defense), currentTerritory));
            line.append(styledPart(")", lineColor, currentTerritory));
        }
        return line;
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.attackTimerMenuEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<String> attacks = getUpcomingAttacks();
        if (attacks.isEmpty()) {
            for (String territory : activeAttackTerritories) {
                cachedDefenses.remove(territory);
            }
            activeAttackTerritories.clear();
            soonestTerritory = null;
            return;
        }

        // Sort by time ascending
        attacks.sort(Comparator.comparingInt(a -> {
            String[] words = a.split(" ");
            return words.length > 0 ? parseMinutes(words[0]) : Integer.MAX_VALUE;
        }));
        clearFinishedAttackDefenses(attacks);

        // Track soonest territory for beacon
        String firstTerritory = getAttackTerritory(attacks.get(0));
        if (!firstTerritory.isEmpty()) soonestTerritory = firstTerritory;

        int x = WynnExtrasConfig.INSTANCE.attackTimerX;
        int y = WynnExtrasConfig.INSTANCE.attackTimerY;
        int rowH = 12;

        // Build display lines with defense if known
        String currentTerritory = getCurrentTerritory();
        List<Text> lines = new ArrayList<>();
        for (String attack : attacks) {
            String[] words = attack.split(" ");
            String time = words.length > 0 ? words[0] : "";
            String terr = getAttackTerritory(attack);
            String def = getDefenseLevel(terr);
            boolean isCurrentTerritory = currentTerritory != null && currentTerritory.equalsIgnoreCase(terr);
            lines.add(buildAttackLine(time, terr, def, isCurrentTerritory));
        }

        int maxW = 0;
        for (Text line : lines) maxW = Math.max(maxW, mc.textRenderer.getWidth(line));
        ctx.fill(x - 2, y - 2, x + maxW + 4, y + lines.size() * rowH + 2, 0x66000000);

        int i = 0;
        for (Text line : lines) {
            ctx.drawTextWithShadow(mc.textRenderer, line, x, y + i * rowH, 0xFFFFFFFF);
            i++;
        }
    }
}
