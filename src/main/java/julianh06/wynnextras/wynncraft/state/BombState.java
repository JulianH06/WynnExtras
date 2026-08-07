package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
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
            long seconds = remainingLong() / 1000;
            return "%d:%02d".formatted(seconds / 60, seconds % 60);
        }
    }

    private static final Pattern WORLD = Pattern.compile("(?i)\\b(WC\\d+)\\b");
    private static final Pattern MINUTES = Pattern.compile("(?i)(\\d+)\\s*(?:minutes?|mins?)");
    private static final List<Bomb> BOMBS = new ArrayList<>();

    public static List<Bomb> bombs() {
        return List.copyOf(BOMBS);
    }

    public static String currentWorld() {
        return CharacterState.world().orElse("");
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String line = event.message.getString().replaceAll("§[0-9a-fk-or]", "");
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.contains("bomb") || (!lower.contains("thrown") && !lower.contains("active"))) return;
        Matcher worldMatcher = WORLD.matcher(line);
        if (!worldMatcher.find()) return;
        String type = type(lower);
        if (type == null) return;
        Matcher minutesMatcher = MINUTES.matcher(line);
        long duration = minutesMatcher.find() ? Long.parseLong(minutesMatcher.group(1)) * 60_000L : 20 * 60_000L;
        String world = worldMatcher.group(1).toUpperCase(Locale.ROOT);
        BOMBS.removeIf(bomb -> bomb.type.equals(type) && bomb.server.equals(world));
        BOMBS.add(new Bomb(type, display(type), world, System.currentTimeMillis() + duration));
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 20 == 0) BOMBS.removeIf(bomb -> !bomb.active());
    }

    private static String type(String text) {
        if (text.contains("profession") && text.contains("speed")) return "PROFESSION_SPEED";
        if (text.contains("profession") && (text.contains("xp") || text.contains("experience"))) return "PROFESSION_XP";
        if (text.contains("combat") && text.contains("xp")) return "COMBAT_XP";
        if (text.contains("loot")) return "LOOT";
        return null;
    }

    private static String display(String type) {
        return switch (type) {
            case "PROFESSION_SPEED" -> "Profession Speed Bomb";
            case "PROFESSION_XP" -> "Profession XP Bomb";
            case "COMBAT_XP" -> "Combat XP Bomb";
            case "LOOT" -> "Loot Bomb";
            default -> type;
        };
    }
}
