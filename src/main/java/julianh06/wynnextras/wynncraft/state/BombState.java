package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.BossBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class BombState {
    public record Bomb(String type, String displayName, String server, long expiresAt) {
        public boolean active() { return expiresAt > System.currentTimeMillis(); }
        public long remainingLong() { return Math.max(0, expiresAt - System.currentTimeMillis()); }
        public String remainingString() {
            long remaining = remainingLong();
            long minutes = remaining / 60_000;
            long seconds = remaining / 1000 - minutes * 60;
            return "%02dm %02ds".formatted(minutes, seconds);
        }
    }

    record ParsedBomb(String type, String server, long durationMillis) {}

    private static final String BOMB_NAME = "(?:Combat XP|Combat Experience|Dungeon|Free Dungeon Entry|Loot|"
            + "Profession Speed|Profession XP|Profession Experience|Loot Chest|More Chest Loot)";
    private static final String SERVER_NAME = "(?:NA|EU|AS)\\d+";
    private static final Pattern SERVER = Pattern.compile("(?i)^" + SERVER_NAME + "$");
    private static final Pattern BOMB_BELL = Pattern.compile(
            "(?is).*?\\bhas thrown an?\\s+(" + BOMB_NAME + ")\\s+Bomb\\s+on\\s+.*?\\b(" + SERVER_NAME + ")\\s*$");
    private static final Pattern LOCAL_BOMB = Pattern.compile("(?i)^(" + BOMB_NAME + ")\\s+Bomb$");
    private static final Pattern EXPIRED_BOMB = Pattern.compile(
            "(?is)^.*\\b(" + BOMB_NAME + ")\\s+Bomb has expired!.*$");
    private static final Pattern INFO_BAR_BOMB = Pattern.compile(
            "(?i)^(?:Double\\s+)?(" + BOMB_NAME + ")\\s+from\\s+.+?\\s+\\[(\\d+)(m|s)]$");
    private static final List<Bomb> BOMBS = new ArrayList<>();

    public static List<Bomb> bombs() {
        return List.copyOf(BOMBS);
    }

    public static String currentWorld() {
        return CharacterState.world().orElse("");
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String line = event.message == null ? "" : event.message.getString();
        String currentWorld = currentWorld();

        ParsedBomb bomb = parseChatBomb(line, currentWorld);
        if (bomb != null) {
            add(bomb, true);
            return;
        }

        String expiredType = parseExpiredBomb(line);
        if (expiredType != null && !currentWorld.isEmpty()) remove(expiredType, currentWorld);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 20 == 0) BOMBS.removeIf(bomb -> !bomb.active());
        if (event.ticks % 5 != 0) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            String currentWorld = currentWorld();
            if (client.player == null || currentWorld.isEmpty()) return;
            for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
                ParsedBomb bomb = parseInfoBarBomb(bar.getName().getString(), currentWorld);
                if (bomb != null) add(bomb, false);
            }
        } catch (Throwable ignored) {}
    }

    static ParsedBomb parseChatBomb(String value, String currentWorld) {
        if (isPlayerChat(value)) return null;
        String line = clean(value);
        Matcher bell = BOMB_BELL.matcher(line);
        if (bell.matches() && hasMarker(value, '\uE01E')) return parsed(bell.group(1), bell.group(2), -1);

        Matcher local = LOCAL_BOMB.matcher(line);
        if (!local.matches() || !hasMarker(value, '\uE014') || currentWorld == null || currentWorld.isBlank()) return null;
        return parsed(local.group(1), currentWorld, -1);
    }

    static String parseExpiredBomb(String value) {
        if (isPlayerChat(value) || !hasMarker(value, '\uE014')) return null;
        Matcher matcher = EXPIRED_BOMB.matcher(clean(value));
        return matcher.matches() ? type(matcher.group(1)) : null;
    }

    static ParsedBomb parseInfoBarBomb(String value, String currentWorld) {
        if (currentWorld == null || currentWorld.isBlank()) return null;
        Matcher matcher = INFO_BAR_BOMB.matcher(clean(value));
        if (!matcher.matches()) return null;
        try {
            long length = Long.parseLong(matcher.group(2));
            long duration = matcher.group(3).equalsIgnoreCase("m")
                    ? length * 60_000L + 30_000L
                    : length * 1000L;
            return parsed(matcher.group(1), currentWorld, duration);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static ParsedBomb parsed(String name, String server, long duration) {
        String type = type(name);
        if (type == null) return null;
        String normalizedServer = clean(server).toUpperCase(Locale.ROOT);
        if (!SERVER.matcher(normalizedServer).matches()) return null;
        long actualDuration = duration >= 0 ? duration : activeMinutes(type) * 60_000L;
        return new ParsedBomb(type, normalizedServer, actualDuration);
    }

    private static void add(ParsedBomb parsed, boolean replace) {
        Bomb existing = BOMBS.stream()
                .filter(bomb -> bomb.type.equals(parsed.type) && bomb.server.equals(parsed.server))
                .findFirst().orElse(null);
        if (existing != null && !replace) return;
        if (existing != null) BOMBS.remove(existing);
        BOMBS.add(new Bomb(parsed.type, display(parsed.type), parsed.server,
                System.currentTimeMillis() + parsed.durationMillis));
    }

    private static void remove(String type, String server) {
        BOMBS.removeIf(bomb -> bomb.type.equals(type) && bomb.server.equalsIgnoreCase(server));
    }

    private static String type(String value) {
        if (value == null) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "combat xp", "combat experience" -> "COMBAT_XP";
            case "dungeon", "free dungeon entry" -> "DUNGEON";
            case "loot" -> "LOOT";
            case "profession speed" -> "PROFESSION_SPEED";
            case "profession xp", "profession experience" -> "PROFESSION_XP";
            case "loot chest", "more chest loot" -> "LOOT_CHEST";
            default -> null;
        };
    }

    private static int activeMinutes(String type) {
        return switch (type) {
            case "PROFESSION_SPEED", "DUNGEON" -> 10;
            default -> 20;
        };
    }

    private static String display(String type) {
        return switch (type) {
            case "PROFESSION_SPEED" -> "Profession Speed Bomb";
            case "PROFESSION_XP" -> "Profession XP Bomb";
            case "COMBAT_XP" -> "Combat XP Bomb";
            case "DUNGEON" -> "Dungeon Bomb";
            case "LOOT" -> "Loot Bomb";
            case "LOOT_CHEST" -> "Loot Chest Bomb";
            default -> type;
        };
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replaceAll("§(?:#[0-9a-fA-F]{8}|[0-9a-fk-orA-FK-OR])", "")
                .replaceAll("\\p{Co}", "")
                .trim();
    }

    private static boolean isPlayerChat(String value) {
        return clean(value).contains(": ");
    }

    private static boolean hasMarker(String value, char marker) {
        return value != null && (value.indexOf(marker) >= 0 || value.indexOf('\uE001') >= 0);
    }
}
