package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.raid.PartyIgnoreOnRaid;
import julianh06.wynnextras.features.raid.RaidRoomData;
import julianh06.wynnextras.features.raid.RaidSnapshot;
import julianh06.wynnextras.features.raid.WERaidKind;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
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
    private static boolean awaitingRaidResume;
    private static int raidResumeTicksRemaining;
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
        observeChat(event.message.getString());
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        if (!isInRaid()) return;
        awaitingRaidResume = true;
        raidResumeTicksRemaining = 100;
    }

    public static void observeChat(String rawMessage) {
        String message = clean(rawMessage);
        if (message.contains(": ")) return;

        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("challenge completed") || lower.contains("room completed")) {
            completeRoom();
        } else if (lower.contains("raid completed") && !lower.contains(":")) {
            end(EndStatus.COMPLETED);
        } else if (lower.contains("raid failed") && !lower.contains(":")) {
            end(EndStatus.FAILED);
        }
    }

    public static void observeTitle(Text title) {
        if (title == null) return;
        String text = clean(title.getString());
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("raid completed")) {
            end(EndStatus.COMPLETED);
            return;
        }
        if (lower.contains("raid failed")) {
            end(EndStatus.FAILED);
            return;
        }

        WERaidKind detected = WERaidKind.fromEntryTitle(title);
        if (detected != WERaidKind.UNKNOWN && !isInRaid()) start(detected);
    }

    public static void observeScoreboard() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective == null) return;
            confirmRaidResume(clean(objective.getDisplayName().getString()));
            for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                String line = clean(entry.name().getString());
                confirmRaidResume(line);
                if (line.equals("Challenge Completed!")) {
                    completeRoom();
                    continue;
                }
                if (line.startsWith("Too many players have") || line.equals("You ran out of time!")) {
                    end(EndStatus.FAILED);
                    continue;
                }
                startRoomFromSignal(line);
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (awaitingRaidResume && --raidResumeTicksRemaining <= 0) interruptRaid();
        if (event.ticks % 5 != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        observeScoreboard();
        if (isInRaid() && WynncraftMenuService.isCurrent(MenuType.RAID_REWARD)) phase = Phase.REWARD;
    }

    private static void start(WERaidKind detected) {
        if (isInRaid()) return;
        if (System.currentTimeMillis() - endedAt < 5_000) return;
        raidKind = detected == null ? WERaidKind.UNKNOWN : detected;
        phase = Phase.STARTING;
        room = 0;
        roomName = "Unknown Room";
        startTime = System.currentTimeMillis();
        roomStartTime = 0;
        endStatus = EndStatus.NONE;
        awaitingRaidResume = false;
        raidResumeTicksRemaining = 0;
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
        RaidChatNotifier.onRoomCompleted(new CompletedRoom(stableRaidKey(raidKind), raidKind.abbreviation(), room,
                roomName, elapsed, totalChallenges(raidKind)));
        roomStartTime = 0;
        phase = Phase.STARTING;
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
        awaitingRaidResume = false;
        raidResumeTicksRemaining = 0;
        PlayerHider.onRaidEnded();
        if (status == EndStatus.COMPLETED) new RaidEndedEvent.Completed(snapshot).post();
        else new RaidEndedEvent.Failed(snapshot).post();
        roomStartTime = 0;
    }

    private static void confirmRaidResume(String scoreboardLine) {
        if (!awaitingRaidResume || !scoreboardLine.endsWith("Raid:")) return;
        awaitingRaidResume = false;
        raidResumeTicksRemaining = 0;
    }

    private static void interruptRaid() {
        if (!awaitingRaidResume || !isInRaid()) return;
        raidKind = WERaidKind.UNKNOWN;
        phase = Phase.IDLE;
        room = 0;
        roomName = "Unknown Room";
        startTime = 0;
        roomStartTime = 0;
        endStatus = EndStatus.NONE;
        endedAt = 0;
        awaitingRaidResume = false;
        raidResumeTicksRemaining = 0;
        ROOMS.clear();
        PlayerHider.onRaidEnded();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                Text.literal("§cRaid tracking has been interrupted; timestamps for the current raid are no longer available.")));
    }

    private static void startRoomFromSignal(String line) {
        if (!isInRaid() || roomStartTime > 0 || line.isBlank()) return;
        int nextRoom = ROOMS.size() + 1;
        String detectedName = roomName(raidKind, nextRoom, line);
        if (detectedName == null) return;

        room = nextRoom;
        roomName = detectedName;
        roomStartTime = System.currentTimeMillis();
        phase = Phase.ROOM;
    }

    private static String roomName(WERaidKind kind, int index, String line) {
        return switch (kind) {
            case NOTG -> switch (index) {
                case 1 -> match(line, "Hold the platform", "Slimey Platform", "Hold and defend", "Tower Defense");
                case 2 -> match(line, "Collect 10 Slimy Goo", "Slime Gathering");
                case 3 -> match(line, "Have a player pick up", "Tunnel Traversal", "2 players must", "Minibosses");
                case 4 -> match(line, "Slay the Restless", "Grootslang Wyrmling");
                default -> null;
            };
            case NOL -> switch (index) {
                case 1 -> match(line, "Hold the tower", "Decaying Tower");
                case 2 -> match(line, "Kill all Crystalline", "Cloud Decay", "Collect 10 Light", "Light Gathering");
                case 3 -> match(line, "Purify the decaying", "Light Tower", "Escort your party to", "Invisible Maze");
                case 4 -> match(line, "Save Him.", "Orphion");
                case 5 -> match(line, "Finish that which He", "The Parasite");
                default -> null;
            };
            case TCC -> switch (index) {
                case 1 -> match(line, "Hold the Upper and", "2 Platforms", "Use water on", "Lava Lake");
                case 2 -> match(line, "Find and reach the", "Labyrinth", "Wake the ancient", "Golem Escort");
                case 3 -> match(line, "Activate 4 Binding", "Binding Seal");
                case 4 -> match(line, "Calm the canyon's", "The Canyon Colossus");
                default -> null;
            };
            case TNA -> switch (index) {
                case 1 -> match(line, "One player must take", "Flooding Canyon", "Hold the stump for", "Sunken Grotto");
                case 2 -> match(line, "Find and kill", "Nameless Cave", "Offer souls to the", "Weeping Soulroot");
                case 3 -> match(line, "Protect the Bulb", "Blueshift Wilds", "Collect 5 Void Matter", "Twisted Jungle");
                case 4 -> match(line, "Survive.", "The ##### Anomaly");
                default -> null;
            };
            case TWP -> switch (index) {
                case 1 -> match(line, "Fight through the", "Grand Aisles", "Collect the sonic", "Regal Ballroom");
                case 2 -> match(line, "Slay the Knightmare", "Statuary Hall");
                case 3 -> match(line, "Rip out the artifact", "The Spire's Shadow");
                case 4 -> match(line, "Unknown", "Anathema");
                default -> null;
            };
            case UNKNOWN -> null;
        };
    }

    private static String match(String line, String... signalsAndNames) {
        for (int i = 0; i + 1 < signalsAndNames.length; i += 2) {
            if (line.contains(signalsAndNames[i])) return signalsAndNames[i + 1];
        }
        return null;
    }

    private static int totalChallenges(WERaidKind kind) {
        return switch (kind) {
            case NOTG, TCC, TNA, TWP -> 4;
            case NOL -> 5;
            default -> 0;
        };
    }

    private static String stableRaidKey(WERaidKind kind) {
        return switch (kind) {
            case NOTG -> "NestOfTheGrootslangs";
            case NOL -> "OrphionsNexusOfLight";
            case TCC -> "TheCanyonColossus";
            case TNA -> "TheNamelessAnomaly";
            case TWP -> "TheWartornPalace";
            case UNKNOWN -> "?";
        };
    }

    private static String clean(String value) { return value.replaceAll("§[0-9a-fk-orx]", "").trim(); }
}
