package julianh06.wynnextras.features.qol;

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
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttackTimerMenu {
    private static final Pattern ATTACK_PATTERN = Pattern.compile("§b- \\d\\d:\\d\\d §3.*", Pattern.CASE_INSENSITIVE);
    // Matches "<anything>: <Territory> defense is <Level>" anywhere in the line.
    // Doesn't anchor to start so it works inside guild chat prefixes like "[Guild] Name: ...".
    private static final Pattern DEFENSE_BROADCAST = Pattern.compile(
            ":\\s*(?<terr>[^:]+?)\\s+defense is\\s+(?<def>Very Low|Low|Medium|High|Very High)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WAR_START = Pattern.compile("The war for (?<terr>.+?) will start in \\d+ minutes?\\.");

    public static String soonestTerritory = null;
    // territory -> defense level ("Very Low" / "Low" / "Medium" / "High" / "Very High")
    private static final Map<String, String> cachedDefenses = new HashMap<>();
    // Last territory the local player personally looked up (for auto-broadcast)
    private static String lastSelfLookupTerritory = null;
    private static long lastSelfLookupAt = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(AttackTimerMenu::render);
        WEEventBus.registerEventListener(new AttackTimerMenu());
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

    private static String defenseColor(String def) {
        if (def == null) return "§7";
        return switch (def) {
            case "Very Low" -> "§a";
            case "Low" -> "§a";
            case "Medium" -> "§e";
            case "High" -> "§c";
            case "Very High" -> "§4";
            default -> "§7";
        };
    }

    private static int parseMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.attackTimerMenuEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<String> attacks = getUpcomingAttacks();
        if (attacks.isEmpty()) { soonestTerritory = null; return; }

        // Sort by time ascending
        attacks.sort(Comparator.comparingInt(a -> {
            String[] words = a.split(" ");
            return words.length > 0 ? parseMinutes(words[0]) : Integer.MAX_VALUE;
        }));

        // Track soonest territory for beacon
        String[] firstWords = attacks.get(0).split(" ");
        if (firstWords.length >= 2) {
            StringBuilder territory = new StringBuilder();
            for (int i = 1; i < firstWords.length; i++) {
                if (i > 1) territory.append(" ");
                territory.append(firstWords[i]);
            }
            soonestTerritory = territory.toString();
        }

        int x = WynnExtrasConfig.INSTANCE.attackTimerX;
        int y = WynnExtrasConfig.INSTANCE.attackTimerY;
        int rowH = 12;

        // Build display lines with defense if known
        List<String> lines = new ArrayList<>();
        for (String attack : attacks) {
            String[] words = attack.split(" ");
            String time = words.length > 0 ? words[0] : "";
            StringBuilder terr = new StringBuilder();
            for (int i = 1; i < words.length; i++) {
                if (i > 1) terr.append(" ");
                terr.append(words[i]);
            }
            String def = cachedDefenses.get(terr.toString());
            String defSuffix = def != null ? " (" + defenseColor(def) + def + "§6)" : "";
            lines.add("§6" + time + " " + terr + defSuffix);
        }

        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, mc.textRenderer.getWidth(l));
        ctx.fill(x - 2, y - 2, x + maxW + 4, y + lines.size() * rowH + 2, 0x66000000);

        int i = 0;
        for (String line : lines) {
            Integer colorOverride = WynnExtrasConfig.INSTANCE.hudColorOverrides.get("attackTimer");
            int lineColor = colorOverride != null ? (colorOverride | 0xFF000000) : 0xFFFFAA00;
            ctx.drawTextWithShadow(mc.textRenderer, line, x, y + i * rowH, lineColor);
            i++;
        }
    }
}
