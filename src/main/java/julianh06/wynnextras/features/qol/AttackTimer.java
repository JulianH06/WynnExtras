package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.wynncraft.state.TerritoryState;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import julianh06.wynnextras.features.achievements.AchievementTracking;
import julianh06.wynnextras.utils.MinecraftUtils;
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
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
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
import java.util.Locale;
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
    private static final Pattern WAR_START = Pattern.compile("The war for (?<terr>.+?) will start in (?<minutes>\\d+) minutes?\\.");
    private static final int DEFAULT_NORMAL_COLOR = 0xFFAA00;
    private static final int DEFAULT_CURRENT_TERRITORY_COLOR = 0xFFFF55;
    private static final int DEFAULT_VERY_LOW_DEFENSE_COLOR = 0x55FF55;
    private static final int DEFAULT_LOW_DEFENSE_COLOR = 0x55FF55;
    private static final int DEFAULT_MEDIUM_DEFENSE_COLOR = 0xFFFF55;
    private static final int DEFAULT_HIGH_DEFENSE_COLOR = 0xFF5555;
    private static final int DEFAULT_VERY_HIGH_DEFENSE_COLOR = 0xAA0000;
    private static final long WORLD_JOIN_BASELINE_DELAY_MS = 5_000L;
    private static final Pattern FORMAT_CODE = Pattern.compile("§[0-9a-fk-or]");
    private static final long ATTACK_SCOREBOARD_CACHE_MS = 1_000L;

    public static String soonestTerritory = null;
    // territory -> defense level ("Very Low" / "Low" / "Medium" / "High" / "Very High")
    private static final Map<String, String> cachedDefenses = new HashMap<>();
    // Defences read directly from an "Attacking: X" menu, keyed by normalized territory name.
    private static final Map<String, String> menuDefenses = new HashMap<>();
    // A defence snapshot made when the client observes the war being queued. These are deliberately
    // kept in memory only: a war that was already queued when the game was opened will not count since its defence is unknown.
    private static final Map<String, QueuedDefense> queuedAttackDefenses = new HashMap<>();
    private static final Set<String> activeAttackTerritories = new HashSet<>();
    private static long worldJoinObservedAt = -1;
    private static boolean attackBaselineReady;
    // Last territory the local player personally looked up (for auto-broadcast)
    private static String lastSelfLookupTerritory = null;
    private static long lastSelfLookupAt = 0;
    private static List<String> cachedUpcomingAttacks = List.of();
    private static long upcomingAttacksCacheExpiresAt;

    private static final SubCommand territoryDefencesDebugCommand = new SubCommand(
            "territorydefences",
            "lists territory defence snapshots saved during this game session",
            context -> {
                printTerritoryDefenseDebug();
                return 1;
            },
            null,
            null
    );

    private static final SubCommand clearAchievementsDebugCommand = new SubCommand(
            "clearAchievements",
            "clears all locally stored WynnExtras achievements",
            context -> {
                AchievementTracking.clearAchievements();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aCleared all stored achievements."));
                return 1;
            },
            null,
            null
    );

    private static final Command debugCommand = new Command(
            "debug",
            "WynnExtras debug commands",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we debug <territorydefences|clearAchievements>"));
                return 1;
            },
            List.of(territoryDefencesDebugCommand, clearAchievementsDebugCommand),
            null
    );

    public static void register() {
        TerritoryState.initialize();
        HudRenderCallback.EVENT.register(AttackTimer::render);
        WEEventBus.registerEventListener(new AttackTimer());
        // Scan open "Attacking: X" menus for defense info and cache it
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            observeNewAttacks(client);
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
                        cacheMenuDefense(territory, parts[1].trim());
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
        try {
            String raw = event.message.getString()
                    .replaceAll("§[0-9a-fk-orx]", "")
                    .replaceAll("[\\p{Co}\\s]+", " ")
                    .trim();
            if (raw.isEmpty()) return;

            // Guildmate defense broadcast
            Matcher m = DEFENSE_BROADCAST.matcher(raw);
            if (m.find()) {
                cachedDefenses.put(territoryKey(m.group("terr")), m.group("def").trim());
                TerritoryState.cacheDefense(m.group("terr").trim(), m.group("def").trim());
                return;
            }

            Matcher ws = WAR_START.matcher(raw);
            if (ws.find()) {
                String terr = ws.group("terr").trim();
                captureQueuedAttack(terr, Integer.parseInt(ws.group("minutes")) * 60L);

                // "The war for X will start in N minutes" — auto-broadcast our cached defense
                if (WynnExtrasConfig.INSTANCE.attackTimerAutoBroadcast) {
                    if (terr.equals(lastSelfLookupTerritory)
                            && System.currentTimeMillis() - lastSelfLookupAt < 5000) {
                        String def = cachedDefenses.get(territoryKey(terr));
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

    // Reading the scoreboard walks every score holder, so doing it per frame dominates the render
    // thread. The sidebar only changes about once a second, so the result is cached briefly and
    // shared between the HUD renderer and the tick handler.
    public static List<String> getUpcomingAttacks() {
        long now = System.currentTimeMillis();
        if (now < upcomingAttacksCacheExpiresAt) return cachedUpcomingAttacks;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            cachedUpcomingAttacks = List.of();
            upcomingAttacksCacheExpiresAt = now + ATTACK_SCOREBOARD_CACHE_MS;
            return cachedUpcomingAttacks;
        }

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<String> upcoming = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective != null) {
            for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                String name = entry.name().getString();
                if (ATTACK_PATTERN.matcher(name).find()) {
                    String stripped = strip(name).substring(2);
                    if (seen.add(stripped)) upcoming.add(stripped);
                }
            }
        }

        cachedUpcomingAttacks = List.copyOf(upcoming);
        upcomingAttacksCacheExpiresAt = now + ATTACK_SCOREBOARD_CACHE_MS;
        return cachedUpcomingAttacks;
    }

    private static String strip(String s) {
        return s == null ? "" : FORMAT_CODE.matcher(s).replaceAll("");
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
        String cached = cachedDefenses.get(territoryKey(territory));
        if (cached != null) return cached;

        String def = TerritoryState.defense(territory).map(AttackTimer::strip).orElse("").trim();
        if (def.isEmpty()) return null;
        cachedDefenses.put(territoryKey(territory), def);
        return def;
    }

    private static void observeNewAttacks(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            worldJoinObservedAt = -1;
            attackBaselineReady = false;
            activeAttackTerritories.clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (worldJoinObservedAt < 0) {
            worldJoinObservedAt = now;
            return;
        }

        List<String> attacks = getUpcomingAttacks();
        Set<String> currentTerritories = new HashSet<>();
        for (String attack : attacks) {
            String territory = getAttackTerritory(attack);
            if (!territory.isEmpty()) currentTerritories.add(territory);
        }

        // Do not infer existing attacks immediately after joining. They could have been queued
        // before this client was started, so their defence snapshot is unknown.
        if (!attackBaselineReady) {
            if (now - worldJoinObservedAt < WORLD_JOIN_BASELINE_DELAY_MS) return;
            activeAttackTerritories.addAll(currentTerritories);
            attackBaselineReady = true;
            return;
        }

        for (String territory : currentTerritories) {
            if (activeAttackTerritories.contains(territory)) continue;

            // This is a new attack on this territory. Never let an earlier attack's value count
            // if we cannot determine the defence for this one.
            queuedAttackDefenses.remove(territoryKey(territory));
            String attack = attacks.stream().filter(entry -> getAttackTerritory(entry).equals(territory)).findFirst().orElse("");
            String time = attack.isEmpty() ? "" : attack.split(" ", 2)[0];
            captureQueuedAttack(territory, parseMinutes(time));
        }

        activeAttackTerritories.clear();
        activeAttackTerritories.addAll(currentTerritories);
    }

    private static void captureQueuedAttack(String territory, long remainingSeconds) {
        long startsAt = System.currentTimeMillis() + remainingSeconds * 1000L;
        String defense = readDefenseAtQueue(territory);
        queuedAttackDefenses.put(territoryKey(territory), new QueuedDefense(territory, defense, startsAt, false));
        if (defense != null) {
            WynnExtras.LOGGER.info("[WE Achievement Debug] Captured queued defence '{}' for '{}'", defense, territory);
        } else {
            WynnExtras.LOGGER.info("[WE Achievement Debug] Could not capture queued defence for '{}'", territory);
        }
    }

    private static String readDefenseAtQueue(String territory) {
        String defense = TerritoryState.defense(territory).map(AttackTimer::strip).orElse("").trim();
        if (!defense.isEmpty()) {
            cachedDefenses.put(territoryKey(territory), defense);
            return defense;
        }
        return menuDefenses.get(territoryKey(territory));
    }

    /**
     * Returns the defence captured when this client saw the current attack get queued.
     * No API fallback is intentional: later buffs must not change the achievement tier.
     */
    public static String getQueuedAttackDefense(String territory) {
        String key = territoryKey(territory);
        QueuedDefense queuedDefense = queuedAttackDefenses.get(key);
        if (queuedDefense == null || queuedDefense.used || queuedDefense.defense == null) {
            return null;
        }
        return queuedDefense.defense;
    }

    public static void markQueuedAttackDefenseUsed(String territory) {
        String key = territoryKey(territory);
        QueuedDefense queuedDefense = queuedAttackDefenses.get(key);
        if (queuedDefense == null || queuedDefense.used) return;
        queuedAttackDefenses.put(key, new QueuedDefense(queuedDefense.territory, queuedDefense.defense,
                queuedDefense.startsAt, true));
    }

    private static void cacheMenuDefense(String territory, String defense) {
        String key = territoryKey(territory);
        cachedDefenses.put(key, defense);
        menuDefenses.put(key, defense);
        TerritoryState.cacheDefense(territory, defense);
    }

    private static String territoryKey(String territory) {
        return territory == null ? "" : territory.trim().toLowerCase(Locale.ROOT);
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

    private static String formatTimerTime(long remainingMs) {
        long totalSeconds = Math.max(0, remainingMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        // Called every frame, so this avoids String.format's parser.
        return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private static String formatDebugTime(long differenceMs) {
        long totalSeconds = Math.abs(differenceMs) / 1000L;
        String time = (totalSeconds / 60L) + "min";
        if (totalSeconds % 60L != 0) time += " " + String.format("%02ds", totalSeconds % 60L);
        return differenceMs < 0 ? "-" + time : time;
    }

    private static void printTerritoryDefenseDebug() {
        if (queuedAttackDefenses.isEmpty()) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7No territory defence snapshots saved this session."));
            return;
        }

        long now = System.currentTimeMillis();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eSaved territory defence snapshots:"));
        queuedAttackDefenses.values().stream()
                .sorted(Comparator.comparingLong(QueuedDefense::startsAt))
                .forEach(attack -> MinecraftUtils.sendMessageToClient(Text.of("§7- §f" + attack.territory
                        + "§7: " + (attack.defense == null ? "§cUnknown" : "§f" + attack.defense)
                        + " §7(" + (attack.startsAt - now < 0 ? "§c" : "§a")
                        + formatDebugTime(attack.startsAt - now) + "§7)"
                        + (attack.used ? " §8(used)" : ""))));
    }

    private static String getCurrentTerritory() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;

        return TerritoryState.currentTerritory().orElse(null);
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

    private record QueuedDefense(String territory, String defense, long startsAt, boolean used) {}

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.attackTimerMenuEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        long now = System.currentTimeMillis();
        Map<String, QueuedDefense> visibleAttacks = new HashMap<>();
        for (Map.Entry<String, QueuedDefense> entry : queuedAttackDefenses.entrySet()) {
            if (entry.getValue().startsAt > now) visibleAttacks.put(entry.getKey(), entry.getValue());
        }

        // Keep displaying wars that were already queued when the client joined if the scoreboard
        // knows about them, but do not save them as achievement snapshots.
        for (String attack : getUpcomingAttacks()) {
            String territory = getAttackTerritory(attack);
            String key = territoryKey(territory);
            if (territory.isEmpty() || visibleAttacks.containsKey(key)) continue;
            String[] words = attack.split(" ", 2);
            visibleAttacks.put(key, new QueuedDefense(territory, getDefenseLevel(territory),
                    now + parseMinutes(words[0]) * 1000L, false));
        }

        List<QueuedDefense> attacks = new ArrayList<>(visibleAttacks.values());
        attacks.sort(Comparator.comparingLong(QueuedDefense::startsAt));
        if (attacks.isEmpty()) {
            soonestTerritory = null;
            return;
        }
        soonestTerritory = attacks.getFirst().territory;

        int x = WynnExtrasConfig.INSTANCE.attackTimerX;
        int y = WynnExtrasConfig.INSTANCE.attackTimerY;
        int rowH = 12;

        // Build display lines with defense if known
        String currentTerritory = getCurrentTerritory();
        List<Text> lines = new ArrayList<>();
        for (QueuedDefense attack : attacks) {
            String time = formatTimerTime(attack.startsAt - now);
            boolean isCurrentTerritory = currentTerritory != null && currentTerritory.equalsIgnoreCase(attack.territory);
            lines.add(buildAttackLine(time, attack.territory, getDefenseLevel(attack.territory), isCurrentTerritory));
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
