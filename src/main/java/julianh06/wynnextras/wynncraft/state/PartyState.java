package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class PartyState {
    private static final long STALE_AFTER_MS = 15_000;
    private static final Pattern LABELED_MEMBERS = Pattern.compile("(?i)(?:owner|leader|members?)\\s*:\\s*(.+)");
    private static final Pattern PLAYER_NAME = Pattern.compile("\\b[A-Za-z0-9_]{3,16}\\b");
    private static final Set<String> MEMBERS = new LinkedHashSet<>();
    private static long updatedAt;
    private static long requestStartedAt;
    private static boolean parsing;

    public static List<String> members() {
        return List.copyOf(MEMBERS);
    }

    public static long updatedAt() {
        return updatedAt;
    }

    public static boolean isStale() {
        return updatedAt == 0 || System.currentTimeMillis() - updatedAt > STALE_AFTER_MS;
    }

    public static void requestRefresh() {
        long now = System.currentTimeMillis();
        if (parsing && now - requestStartedAt < 250) return;
        sendCommand("party list");
        MEMBERS.clear();
        updatedAt = 0;
        parsing = true;
        requestStartedAt = now;
    }

    public static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || command == null || command.isBlank()) return;
        client.getNetworkHandler().sendChatCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        if (!parsing) return;
        if (System.currentTimeMillis() - requestStartedAt > 5_000) {
            finish();
            return;
        }
        String line = event.message.getString().replaceAll("§[0-9a-fk-or]", "").trim();
        String lower = line.toLowerCase();
        if (lower.contains("not currently in a party") || lower.contains("you are not in a party")
                || lower.contains("you must be in a party to use this")) {
            finish();
            return;
        }
        Matcher labeled = LABELED_MEMBERS.matcher(line);
        if (labeled.find()) addNames(labeled.group(1));
        else if (lower.startsWith("party members")) addNames(line.substring("party members".length()));
        else if (!MEMBERS.isEmpty() && (line.isEmpty() || line.startsWith("---"))) finish();
    }

    private static void addNames(String text) {
        List<String> ignored = new ArrayList<>(List.of("and", "owner", "leader", "member", "members", "online", "offline"));
        Matcher matcher = PLAYER_NAME.matcher(text);
        while (matcher.find()) {
            String name = matcher.group();
            if (!ignored.contains(name.toLowerCase())) MEMBERS.add(name);
        }
        if (!MEMBERS.isEmpty()) updatedAt = System.currentTimeMillis();
    }

    private static void finish() {
        parsing = false;
        updatedAt = System.currentTimeMillis();
        String self = MinecraftUtils.playerName();
        if (self != null) MEMBERS.add(self);
    }
}
