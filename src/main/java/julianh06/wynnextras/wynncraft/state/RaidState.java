package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.raid.PartyIgnoreOnRaid;
import julianh06.wynnextras.features.raid.RaidRoomData;
import julianh06.wynnextras.features.raid.RaidSnapshot;
import julianh06.wynnextras.features.raid.WERaidKind;
import julianh06.wynnextras.utils.BossBarUtils;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

@WEModule
public final class RaidState {
    public enum Phase { IDLE, STARTING, ROOM, REWARD, COMPLETED, FAILED }
    public enum EndStatus { NONE, COMPLETED, FAILED }
    public record CompletedRoom(String raidKey, String abbreviation, int index, String name,
                                long time, int totalChallenges) {}

    private static WERaidKind raidKind = WERaidKind.UNKNOWN;
    private static Phase phase = Phase.IDLE;
    private static int room;
    private static String roomName = "Unknown Room";
    private static long startTime;
    private static long roomStartTime;
    private static EndStatus endStatus = EndStatus.NONE;
    private static long endedAt;
    private static final Map<Integer, RaidRoomData> ROOMS = new LinkedHashMap<>();

    public static WERaidKind raidKind() { return raidKind; }
    public static Phase phase() { return phase; }
    public static int room() { return room; }
    public static String roomName() { return roomName; }
    public static long startTime() { return startTime; }
    public static EndStatus endStatus() { return endStatus; }
    public static boolean isInRaid() { return phase == Phase.STARTING || phase == Phase.ROOM || phase == Phase.REWARD; }

    public static OptionalLong currentRoomTimeOptional() {
        return roomStartTime <= 0 || !isInRaid() ? OptionalLong.empty()
                : OptionalLong.of(System.currentTimeMillis() - roomStartTime);
    }

    public static long currentRoomTime() { return currentRoomTimeOptional().orElse(0); }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String message = clean(event.message.getString());
        if (message.contains(": ")) return;
        WERaidKind detected = detectKind(message);
        if (detected != WERaidKind.UNKNOWN && !isInRaid()) start(detected);
        else if (detected != WERaidKind.UNKNOWN) raidKind = detected;

        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("raid started") || lower.contains("raid has begun") || lower.contains("the raid begins")) {
            start(detected);
        } else if (lower.contains("challenge completed") || lower.contains("room completed")) {
            completeRoom();
        } else if (lower.contains("raid completed") && !lower.contains(":")) {
            end(EndStatus.COMPLETED);
        } else if (lower.contains("raid failed") && !lower.contains(":")) {
            end(EndStatus.FAILED);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 5 != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
            String text = clean(bar.getName().getString());
            WERaidKind detected = detectKind(text);
            if (detected != WERaidKind.UNKNOWN) {
                if (!isInRaid()) start(detected);
                else raidKind = detected;
            }
            if (isInRaid() && looksLikeRoomBar(text)) updateRoomName(text);
        }
        if (isInRaid() && WynncraftMenuService.isCurrent(MenuType.RAID_REWARD)) phase = Phase.REWARD;
    }

    private static void start(WERaidKind detected) {
        if (isInRaid()) {
            if (detected != null && detected != WERaidKind.UNKNOWN) raidKind = detected;
            return;
        }
        if (System.currentTimeMillis() - endedAt < 5_000) return;
        raidKind = detected == null ? WERaidKind.UNKNOWN : detected;
        phase = Phase.STARTING;
        room = 1;
        roomName = "Room 1";
        startTime = System.currentTimeMillis();
        roomStartTime = startTime;
        endStatus = EndStatus.NONE;
        ROOMS.clear();
        RaidChatNotifier.resetCounters();
        PartyIgnoreOnRaid.onRaidStarted();
        PlayerHider.onRaidStarted(raidKind);
    }

    private static void completeRoom() {
        if (!isInRaid() || roomStartTime <= 0) return;
        long now = System.currentTimeMillis();
        long elapsed = now - roomStartTime;
        ROOMS.put(room, new RaidRoomData(roomName, elapsed, now));
        RaidChatNotifier.onRoomCompleted(new CompletedRoom(raidKind.name(), raidKind.abbreviation(), room,
                roomName, elapsed, totalChallenges(raidKind)));
        room++;
        roomName = "Room " + room;
        roomStartTime = now;
        phase = Phase.ROOM;
    }

    private static void end(EndStatus status) {
        if (!isInRaid()) return;
        if (status == EndStatus.COMPLETED && roomStartTime > 0 && !ROOMS.containsKey(room)) {
            completeRoom();
        } else if (roomStartTime > 0 && !ROOMS.containsKey(room)) {
            long now = System.currentTimeMillis();
            ROOMS.put(room, new RaidRoomData(roomName, now - roomStartTime, now));
        }
        long now = System.currentTimeMillis();
        long roomTotal = ROOMS.values().stream().mapToLong(value -> Math.max(0, value.totalTime())).sum();
        RaidSnapshot snapshot = new RaidSnapshot(raidKind, ROOMS, startTime, Math.max(0, now - startTime), roomTotal);
        endStatus = status;
        endedAt = now;
        phase = status == EndStatus.COMPLETED ? Phase.COMPLETED : Phase.FAILED;
        PlayerHider.onRaidEnded();
        if (status == EndStatus.COMPLETED) new RaidEndedEvent.Completed(snapshot).post();
        else new RaidEndedEvent.Failed(snapshot).post();
        roomStartTime = 0;
    }

    private static void updateRoomName(String text) {
        String clean = text.replaceAll("(?i)\\s*(?:-|\\|).*", "").trim();
        if (clean.isBlank() || clean.equalsIgnoreCase(raidKind.displayName())) return;
        roomName = clean;
        if (phase == Phase.STARTING) phase = Phase.ROOM;
    }

    private static boolean looksLikeRoomBar(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("challenge") || lower.contains("objective") || lower.contains("room");
    }

    private static WERaidKind detectKind(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("GROOTSLANG") || upper.matches(".*\\bNOG\\b.*")) return WERaidKind.NOTG;
        if (upper.contains("NEXUS OF LIGHT") || upper.matches(".*\\bNOL\\b.*")) return WERaidKind.NOL;
        if (upper.contains("CANYON COLOSSUS") || upper.matches(".*\\bTCC\\b.*")) return WERaidKind.TCC;
        if (upper.contains("NAMELESS ANOMALY") || upper.matches(".*\\bTNA\\b.*")) return WERaidKind.TNA;
        if (upper.contains("WARTORN PALACE") || upper.matches(".*\\bWTP\\b.*") || upper.matches(".*\\bTWP\\b.*")) return WERaidKind.TWP;
        return WERaidKind.UNKNOWN;
    }

    private static int totalChallenges(WERaidKind kind) {
        return switch (kind) {
            case NOTG -> 3;
            case NOL, TCC, TNA, TWP -> 4;
            default -> 0;
        };
    }

    private static String clean(String value) { return value.replaceAll("§[0-9a-fk-orx]", "").trim(); }
}
