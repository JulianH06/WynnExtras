package julianh06.wynnextras.features.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.wtshim.models.raid.raids.*;
import julianh06.wynnextras.wtshim.models.raid.type.RaidInfo;
import julianh06.wynnextras.wtshim.models.raid.type.RaidRoomInfo;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.utils.type.Time;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.raid.tna.TnaApi;
import julianh06.wynnextras.mixin.RaidKindAccessor;
import julianh06.wynnextras.utils.ChatUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import julianh06.wynnextras.config.WynnExtrasConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public class RaidChatNotifier {
    private static final String WYNNEXTRAS_FOREGROUND_PILL_TEXT = "\uE016\uE018\uE00D\uE00D\uE004\uE017\uE013\uE011\uE000\uE012\uDB00\uDC06";
    private static boolean receiveEventsRegistered = false;
    private static RaidChatNotifier INSTANCE = new RaidChatNotifier();
    private Map<String, Long> raidPBs = new HashMap<>();

    public static long disableChiropUntil = 0;

    public RaidChatNotifier() {
        registerReceiveEvents();
    }

    private static void registerReceiveEvents() {
        if (receiveEventsRegistered) return;
        receiveEventsRegistered = true;

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                allowReceivedMessage(message));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || allowReceivedMessage(message));
    }

    private static boolean allowReceivedMessage(Text message) {
        String raw = stripColorCodes(message.getString());
        TnaApi.handleMessage(raw);

        if (!shouldBlockRaidTimestampMessage(raw)) return true;

        ChatNotificator.notify(message);
        handleMessage(raw);
        return false;
    }

    private static boolean shouldBlockRaidTimestampMessage(String raw) {
        if (!WynnExtrasConfig.INSTANCE.toggleRaidTimestamps) return false;
        if (raw == null || raw.isEmpty()) return false;
        if (raw.contains(WYNNEXTRAS_FOREGROUND_PILL_TEXT)) return false;

        String msgLower = raw.toLowerCase(Locale.ROOT);
        if (msgLower.contains(": ")) return false;

        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(msgLower).find()) {
                return true;
            }
        }
        return false;
    }

    private static final List<RaidMessageDetector> detectors = Arrays.asList(
            new SlimeGatheringDetector(),
            new BindingSealDetector(),
            new LightGatheringDetector(),
            new WatchPhaseDetector(),
            new ShadowlingDetector(),

            new SingleOccurrenceDetector(
                    "is preparing to descend! [1/2]",
                    "§bDescend 1/2 §c",
                    "descend1"
            ),
            new SingleOccurrenceDetector(
                    "is preparing to descend! [2/2]",
                    "§bDescend 2/2 §c",
                    "descend2"
            ),
            new SingleOccurrenceDetector(
                    "Upper Level must kill the Slime Chomper",
                    "§bSlime Chomper Spawned §c",
                    "slimemini"
            ),
            new SingleOccurrenceDetector(
                    "players on the Upper Level must kill the Carnivorous",
                    "§bCarnivore spawned §c",
                    "carnimini"
            ),
            new SingleOccurrenceDetector(
                    "players on the Upper Level must kill the Invasive",
                    "§bTarantula spawned §c",
                    "taramini"
            ),
            new SingleOccurrenceDetector(
                    "players on the Upper Level must kill the Unfurling",
                    "§bHorsefly spawned §c",
                    "horseflymini"
            ),
            new SingleOccurrenceDetector(
                    "The Void Holes have begun to destabi",
                    "§b[4/5] Void Matters §c",
                    "voidgathered"
            ),
//        new SingleOccurrenceDetector(
//                "All the Void Rifts have been destroyed! A path",
//                "§bBerry Room Done §c",
//                "berryroom"
//        ),
            new SingleOccurrenceDetector(
                    "A Void Pedestal has been activated! [1/2]",
                    "§bVoid Pedestal Activated [1/2] §c",
                    "voidpedestal1"
            ),
            new SingleOccurrenceDetector(
                    "A Void Pedestal has been activated! [2/2]",
                    "§bVoid Pedestal Activated [2/2] §c",
                    "voidpedestal2"
            ),
//        new SingleOccurrenceDetector(
//                "You have unblocked the voidhole out!",
//                "§bVoid Room done §c",
//                "voidholeroompb" //TNA 1st room
//        ),
//        new SingleOccurrenceDetector(
//                "The Giant Void Hole has opened! Use it to escape!",
//                "§bVoidgather Room done §c",
//                "voidgatherroompb"
//        ),
            new SingleOccurrenceDetector(
                    "The lower door has been unlocked",
                    "§bLower door unlocked §c",
                    "lowerdoorunlock"
            ),
            new SingleOccurrenceDetector(
                    "The Upper door has been unlocked",
                    "§bUpper door unlocked §c",
                    "upperdoorunlock"
            ),
            new SingleOccurrenceDetector(
                    "has picked up the Wings!",
                    "§bWings picked up §c",
                    "wings"
            ),
            new HubertKeyDetector(),

            new MultiOccurrenceDetector(
                    "A new platform has appeared on the Lower Area!",
                    "§bLower Mini spawned §c",
                    "lowermini"
            ),
            new MultiOccurrenceDetector(
                    "A Bulb Keeper has spawned!",
                    "§bBulb Keeper spawned §c",
                    "bulbspawned"
            ),
            new MultiOccurrenceDetector(
                    "A Red Bulb has been captured!",
                    "§bBulb captured §c",
                    "bulbcaptured"
            ),
            new MultiOccurrenceDetector(
                    "[+1 Void Matter]",
                    "§b[+1 Void Matter] §c",
                    "voidmattergathered"
            ),
            new MultiOccurrenceDetector(
                    "3/3 Clouds Purified",
                    "§bPurified 3/3 clouds §c",
                    "clouds"
            ),
            new MultiOccurrenceDetector(
                    "The Team has reached the Checkpoint!",
                    "§bReached Checkpoint §c",
                    "mazecheckpoint"
            ),
            new MultiOccurrenceDetector(
                    "100% Rock Destroyed",
                    "§bRock destroyed §c",
                    "rockdestroyed"
            ),
            new MultiOccurrenceDetector(
                    "[+1 Slimey Goo]",
                    "§fGot 1 Slimey Goo §c",
                    "slimegathered"
            ),
            new MultiOccurrenceDetector(
                    "[+2 Slimey Goo]",
                    "§fGot 2 Slimey Goo §c",
                    "2slimesgathered"
            ),
            new MultiOccurrenceDetector(
                    "+1 [Isoptera Heart]",
                    "§fGot heart §c",
                    "heart"
            ),
            new MultiOccurrenceDetector(
                    "has entered the tree",
                    "§bEntered the Tree §c",
                    "treeenter"
            ),
            new MultiOccurrenceDetector(
                    "A player must stand on the platform at",
                    "§bPlatform spawned §c",
                    "platformspawnedtcc"
            ),
            new MultiOccurrenceDetector(
                    "A miniboss has spawned! It has sped",
                    "§bMiniboss spawned §c",
                    "minibossspawnedtcc"
            ),
            new MultiOccurrenceDetector(
                    "The golem has been defeated, and the",
                    "§bGolem defeated §c",
                    "golemdefeated"
            ),
            new MultiOccurrenceDetector(
                    "A Colossal Core has spawned!",
                    "§bA Colossal Core has spawned! ",
                    "colossalcorespawned"
            )
    );

    static void savePB(String key, long time) {
        Long old = INSTANCE.raidPBs.get(key);

        if (old == null || time < old) {
            INSTANCE.raidPBs.put(key, time);
            INSTANCE.save();
        }
    }

    static Long getPB(String key) {
        return INSTANCE.raidPBs.get(key);
    }

    // Dedup so the mixin path and direct ChatEvent path don't double-process the same message.
    private static String lastHandledMsg = null;
    private static long lastHandledMs = 0;

    public static void handleMessage(String rawMsg) {
        if (!WynnExtrasConfig.INSTANCE.toggleRaidTimestamps) return;
        if (rawMsg == null) return;

        String msg = stripColorCodes(rawMsg).trim();
        if (msg.isEmpty()) return;
        if (msg.contains(": ")) return;

        long now = System.currentTimeMillis();
        if (msg.equals(lastHandledMsg) && now - lastHandledMs < 200) return;
        lastHandledMsg = msg;
        lastHandledMs = now;

        long currentTime = (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null)
                ? Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime()
                : 0;

        for (RaidMessageDetector detector : detectors) {
            if (detector.matches(msg)) {
                String timestamp = (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null)
                        ? formatTime(currentTime)
                        : "??:??.???";

                String progress = detector.extractProgress(msg);
                List<String> finalMessages = detector.getFormattedMessages(progress, timestamp);

                MinecraftClient.getInstance().execute(() -> {
                    for (String finalMsg : finalMessages) {
                        if (finalMsg.isEmpty()) continue;
                        McUtils.sendMessageToClient(
                                WynnExtras.addWynnExtrasPrefix(Text.of(finalMsg))
                        );
                    }
                });
                return;
            }
        }
    }




    private static String getCurrentRoomTimestamp() {
        if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null)
            return "??:??.???";
        return formatTime(Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime());
    }

    public static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    private static String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-orx]", "");
    }

    private interface RaidMessageDetector {
        boolean matches(String msg);

        String extractProgress(String msg);

        String getFormattedMessage(String progress, String timestamp);

        default List<String> getFormattedMessages(String progress, String timestamp) {
            String message = getFormattedMessage(progress, timestamp);
            return message.isEmpty() ? List.of() : List.of(message);
        }
    }


    public static final List<Pattern> BLOCKED_PATTERNS = Arrays.asList(
            Pattern.compile("is preparing to descend! \\[1/2", Pattern.CASE_INSENSITIVE),
            Pattern.compile("is preparing to descend! \\[2/2", Pattern.CASE_INSENSITIVE),
            Pattern.compile("upper level must kill the slime chomper", Pattern.CASE_INSENSITIVE),
            Pattern.compile("players on the upper level must kill the carnivorous", Pattern.CASE_INSENSITIVE),
            Pattern.compile("players on the upper level must kill the invasive", Pattern.CASE_INSENSITIVE),
            Pattern.compile("players on the upper level must kill the unfurling", Pattern.CASE_INSENSITIVE),
            Pattern.compile("the void holes have begun to destabi", Pattern.CASE_INSENSITIVE),
            Pattern.compile("a new platform has appeared on the lower area!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("3/3 clouds purified", Pattern.CASE_INSENSITIVE),
            Pattern.compile("the team has reached the checkpoint!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("100% rock destroyed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("1 slimey goo", Pattern.CASE_INSENSITIVE),
            Pattern.compile("2 slimey goo", Pattern.CASE_INSENSITIVE),
            Pattern.compile("1 \\[isoptera heart", Pattern.CASE_INSENSITIVE),
            //Pattern.compile("All the Void Rifts have been destroyed! A path", Pattern.CASE_INSENSITIVE),
            Pattern.compile("The void holes inside the tree are open!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("The Altar has opened to the void, you may leave through it.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A Red Bulb has been captured!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A Bulb Keeper has spawned!", Pattern.CASE_INSENSITIVE),
            //Pattern.compile("The Giant Void Hole has opened! Use it to escape!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A Void Pedestal has been activated! \\[1/2]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A Void Pedestal has been activated! \\[2/2]", Pattern.CASE_INSENSITIVE),
            //Pattern.compile("You have unblocked the voidhole out!", Pattern.CASE_INSENSITIVE),
            Pattern.compile(Pattern.quote("[+1 Void Matter]"), Pattern.CASE_INSENSITIVE),
            Pattern.compile("has entered the tree", Pattern.CASE_INSENSITIVE),
            Pattern.compile("goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("binding seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("light crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("the obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE),
            Pattern.compile("The lower door has been unlocked.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("The Upper door has been unlocked!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A player must stand on the platform", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A miniboss has spawned! It has sped", Pattern.CASE_INSENSITIVE),
            Pattern.compile("The golem has been defeated, and", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A Colossal Core has spawned!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("has picked up the Wings!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Key! \\[2/2]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Collected the Left Key!", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Collected the Right Key!", Pattern.CASE_INSENSITIVE)
    );



    private static class SlimeGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "slime";
        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                return "§aAdded Slime " + progress + " §c@ " + timestamp;
            }

            long elapsed = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();

            String key = PB_PREFIX + "_" + progress;
            Long pb = getPB(key);

            String output = "§aAdded Slime " + progress +
                    " §c@ " + formatTime(elapsed);

            if (pb == null || elapsed < pb) {
                savePB(key, elapsed);

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }

            return output;
        }
    }

    private static class BindingSealDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Binding Seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "seal";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            var raid = Models.Raid.getCurrentRaid();
            if (raid == null || raid.getCurrentRoom() == null) {
                return "§bCompleted Seal " + progress + " §c@ " + timestamp;
            }
            long currentMillis = raid.getCurrentRoom().getRoomTotalTime();

            String key = PB_PREFIX + "_" + progress;
            Long pb = getPB(key);

            String output = "§bCompleted Seal " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis < pb) {
                savePB(key, currentMillis);

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }
            return output;
        }
    }

    private static class LightGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Light Crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "lightgathering";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            var raid = Models.Raid.getCurrentRaid();
            if (raid == null || raid.getCurrentRoom() == null) {
                return "§bAdded light " + progress + " §c@ " + timestamp;
            }
            long currentMillis = raid.getCurrentRoom().getRoomTotalTime();
            String key = PB_PREFIX + "_" + progress;

            Long pb = getPB(key);

            String output = "§bAdded light " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis < pb) {
                savePB(key, currentMillis);

                if (pb != null) {
                    output += " §e[New PB! Old: " + formatTime(pb) + "]";
                } else {
                    output += " §e[First PB]";
                }
            }
            else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }
            return output;
        }
    }

    private static class ShadowlingDetector implements RaidMessageDetector {
        private static final Pattern PATTERN =
                Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);
        static final String PB_PREFIX = "shadowling";

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            Matcher matcher = PATTERN.matcher(msg);
            return matcher.find() ? matcher.group(1) : null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if ("3/3".equals(progress) && Time.now().timestamp() >= disableChiropUntil && WynnExtrasConfig.INSTANCE.chiropTimer) {
                startSpawnCountdown();
            }

            var raid = Models.Raid.getCurrentRaid();
            if (raid == null || raid.getCurrentRoom() == null) {
                return "§bKilled Shadowling " + progress + " §c@ " + timestamp;
            }
            long currentMillis = raid.getCurrentRoom().getRoomTotalTime();

            String key = PB_PREFIX + "_" + progress;
            Long pb = getPB(key);

            String output = "§bKilled Shadowling " + progress + " §c@ " + timestamp;

            if (pb == null || currentMillis < pb) {
                savePB(key, currentMillis);
                output += (pb == null
                        ? " §e[First PB]"
                        : " §e[New PB! Old: " + formatTime(pb) + "]");
            } else {
                output += " §7[PB: " + formatTime(pb) + "]";
            }

            return output;
        }

        private void startSpawnCountdown() {
            new Thread(() -> {
                try {
                    for (int i = 7; i >= 0; i--) {
                        int countdown = i;

                        MinecraftClient.getInstance().execute(() ->
                                ChatUtils.displayTitle(
                                        "§cSPAWNING IN: §f" + countdown,
                                        "",
                                        20, 0, 0
                                )
                        );

                        Thread.sleep(1000);
                    }

                    MinecraftClient.getInstance().execute(() ->
                            ChatUtils.displayTitle("", "", 0, 0, 0)
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private static class WatchPhaseDetector implements RaidMessageDetector {

        private long lastWatchPhaseTime = -1;
        private static final Pattern PATTERN = Pattern.compile(
                "The Obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE);

        public void resetForNewRaid() {
            lastWatchPhaseTime = -1;
        }

        @Override
        public boolean matches(String msg) {
            return PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
                return "§bStarted Watchphase (no raid data)";
            }

            long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            String message;

            if (lastWatchPhaseTime == -1) {
                message = "§bFirst Watchphase started §c@ " + timestamp;

                Long pb = INSTANCE.raidPBs.get("watch_phase_first");
                if (pb == null || currentTime < pb) {
                    INSTANCE.raidPBs.put("watch_phase_first", currentTime);
                    WynnExtrasConfig.save();

                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }

            } else {
                long duration = currentTime - lastWatchPhaseTime;
                message = "§bWatchphase started after §c" + formatTime(duration) + " §7(@" + timestamp + ")";

                Long pb = INSTANCE.raidPBs.get("watch_phase_duration");
                if (pb == null || duration < pb) {
                    INSTANCE.raidPBs.put("watch_phase_duration", duration);
                    WynnExtrasConfig.save();

                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }
            }

            lastWatchPhaseTime = currentTime;
            return message;
        }
    }


    private static String buildSingleOccurrenceMessage(String formattedMessage, String pbKey, String timestamp) {
        String message = formattedMessage + timestamp;

        if (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {
            long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            Long pb = getPB(pbKey);

            if (pb == null || currentTime < pb) {
                savePB(pbKey, currentTime);
                message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
            } else {
                message += " §7[PB: " + formatTime(pb) + "]";
            }
        }
        return message;
    }

    private static class HubertKeyDetector implements RaidMessageDetector {
        private static final Pattern LEFT_PATTERN = Pattern.compile(Pattern.quote("Collected the Left Key!"), Pattern.CASE_INSENSITIVE);
        private static final Pattern RIGHT_PATTERN = Pattern.compile(Pattern.quote("Collected the Right Key!"), Pattern.CASE_INSENSITIVE);
        private static final Pattern BOTH_PATTERN = Pattern.compile(Pattern.quote("Key! [2/2]"), Pattern.CASE_INSENSITIVE);

        @Override
        public boolean matches(String msg) {
            return LEFT_PATTERN.matcher(msg).find()
                    || RIGHT_PATTERN.matcher(msg).find()
                    || BOTH_PATTERN.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return msg;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            return "";
        }

        @Override
        public List<String> getFormattedMessages(String progress, String timestamp) {
            List<String> messages = new ArrayList<>();
            if (progress == null) return messages;

            if (LEFT_PATTERN.matcher(progress).find()) {
                messages.add(buildSingleOccurrenceMessage("§bLeft key collected §c", "hubertLeftKey", timestamp));
            }
            if (RIGHT_PATTERN.matcher(progress).find()) {
                messages.add(buildSingleOccurrenceMessage("§bRight key collected §c", "hubertRightKey", timestamp));
            }
            if (BOTH_PATTERN.matcher(progress).find()) {
                messages.add(buildSingleOccurrenceMessage("§bBoth keys collected §c", "hubertBothKeys", timestamp));
            }

            return messages;
        }
    }

    private static class SingleOccurrenceDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;
        private final String pbKey;

        public SingleOccurrenceDetector(String regex, String formattedMessage, String pbKey) {
            this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
            this.formattedMessage = formattedMessage;
            this.pbKey = pbKey;
        }

        @Override
        public boolean matches(String msg) {
            return pattern.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            return null;
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            return buildSingleOccurrenceMessage(formattedMessage, pbKey, timestamp);
        }
    }



    /** Like SingleOccurrenceDetector but only fires once per raid — subsequent matches in the
     *  same raid are silently ignored. Resets on raid start via {@link #resetCounters()}. */
    private static class FirstPerRaidDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;
        private final String pbKey;
        private boolean fired = false;

        public FirstPerRaidDetector(String regex, String formattedMessage, String pbKey) {
            this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
            this.formattedMessage = formattedMessage;
            this.pbKey = pbKey;
        }

        public void resetForNewRaid() { fired = false; }

        @Override public boolean matches(String msg) { return !fired && pattern.matcher(msg).find(); }
        @Override public String extractProgress(String msg) { return null; }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            fired = true;
            String message = formattedMessage + timestamp;
            if (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {
                long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
                Long pb = getPB(pbKey);
                if (pb == null || currentTime < pb) {
                    savePB(pbKey, currentTime);
                    message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + formatTime(pb) + "]");
                } else {
                    message += " §7[PB: " + formatTime(pb) + "]";
                }
            }
            return message;
        }
    }

    private static class MultiOccurrenceDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String baseMessage;
        private final String pbKeyPrefix;

        private int occurrenceCount = 0;

        private long lastTriggerTime = -1;

        public MultiOccurrenceDetector(String regex, String baseMessage, String pbKeyPrefix) {
            this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
            this.baseMessage = baseMessage;
            this.pbKeyPrefix = pbKeyPrefix;
        }

        @Override
        public boolean matches(String msg) {
            return pattern.matcher(msg).find();
        }

        @Override
        public String extractProgress(String msg) {
            long now = System.currentTimeMillis();
            if (lastTriggerTime != -1 && (now - lastTriggerTime) < 555) {
                return null; // anti-spam
            }
            lastTriggerTime = now;

            occurrenceCount++;
            return "[" + occurrenceCount + "]";
        }

        @Override
        public String getFormattedMessage(String progress, String timestamp) {
            if (progress == null) {
                return "";
            }
            if (timestamp == null) {
                timestamp = "??:??";
            }

            String key = pbKeyPrefix + "_" + occurrenceCount;
            String msg;

            if (Models.Raid.getCurrentRaid() != null
                    && Models.Raid.getCurrentRaid().getCurrentRoom() != null) {

                long currentTime = Models.Raid.getCurrentRaid()
                        .getCurrentRoom().getRoomTotalTime();

                Long pb = getPB(key);

                if (pb == null || currentTime < pb) {
                    savePB(key, currentTime);

                    if (pb == null) {
                        msg = baseMessage + progress + " §c@ " + timestamp +
                                " §e[First PB]";
                    } else {
                        msg = baseMessage + progress + " §c@ " + timestamp +
                                " §e[New PB! Old: " + formatTime(pb) + "]";
                    }
                } else {
                    msg = baseMessage + progress + " §c@ " + timestamp +
                            " §7[PB: " + formatTime(pb) + "]";
                }

            } else {
                msg = baseMessage + progress + " §c@ " + timestamp;
            }

            return msg;
        }
    }

    public static void resetCounters() {
        for (RaidMessageDetector detector : detectors) {
            if (detector instanceof MultiOccurrenceDetector m) {
                m.occurrenceCount = 0;
            }
            else if (detector instanceof WatchPhaseDetector w) {
                w.resetForNewRaid();
            }
            else if (detector instanceof FirstPerRaidDetector f) {
                f.resetForNewRaid();
            }
        }
        julianh06.wynnextras.features.chat.ChainsAttachedTracker.resetForNewRaid();
    }

    public static void onRoomCompleted(RaidInfo raidInfo) {
        if (!WynnExtrasConfig.INSTANCE.toggleRaidTimestamps) return;

        int challengeIndex = raidInfo.completedChallengeCount();
        RaidRoomInfo room = raidInfo.getRoomByNumber(challengeIndex);
        if (room == null) return;

        long time = room.getRoomTotalTime();
        String timestamp = formatTime(time);

        if (isBossChallenge(raidInfo, challengeIndex)) {
            handleBossCompleted(raidInfo, room, challengeIndex, time, timestamp);
        } else {
            handleRoomCompleted(raidInfo, room, timestamp);
        }
    }

    private static void handleRoomCompleted(
            RaidInfo raidInfo,
            RaidRoomInfo room,
            String timestamp
    ) {
        String roomName = room.getRoomName();
        long time = room.getRoomTotalTime();

        String pbKey = stableRaidKey(raidInfo.getRaidKind()) + "_" + roomName.replaceAll("\\s", "");

        Long pb = getPB(pbKey);

        String msg = "§b" + roomName + " done after §c" + timestamp;

        if (pb == null || time < pb) {
            savePB(pbKey, time);
            msg += (pb == null)
                    ? " §e[First PB]"
                    : " §e[New PB! Old: " + formatTime(pb) + "]";
        } else {
            msg += " §7[PB: " + formatTime(pb) + "]";
        }

        McUtils.sendMessageToClient(
                WynnExtras.addWynnExtrasPrefix(StyledText.fromString(msg).getComponent())
        );
    }

    private static void handleBossCompleted(
            RaidInfo raidInfo,
            RaidRoomInfo room,
            int index,
            long time,
            String timestamp
    ) {
        String bossName = room.getRoomName();
        String raidKey = stableRaidKey(raidInfo.getRaidKind());

        String pbKey = "boss_" + raidKey + "_" + index;
        Long pb = getPB(pbKey);

        String msg = "§a§l" + bossName + " §r§bdefeated after §c" + timestamp;

        if (pb == null || time < pb) {
            savePB(pbKey, time);
            msg += (pb == null)
                    ? " §e[First PB]"
                    : " §e[New PB! Old: " + formatTime(pb) + "]";
        } else {
            msg += " §7[PB: " + formatTime(pb) + "]";
        }

        McUtils.sendMessageToClient(
                WynnExtras.addWynnExtrasPrefix(Text.of(msg))
        );
    }

    private static boolean isBossChallenge(RaidInfo raidInfo, int challengeIndex) {
        RaidKind kind = raidInfo.getRaidKind();

        int totalChallenges = ((RaidKindAccessor) kind).getChallengeNames().size();

        if ("NOL".equals(kind.getAbbreviation())) {
            return challengeIndex >= totalChallenges - 1;
        }

        return challengeIndex == totalChallenges;
    }

    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/raidPBs.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write PB data:");
            e.printStackTrace();
        }
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/raidPBs.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't create config directory:");
            e.printStackTrace();
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                RaidChatNotifier loaded = gson.fromJson(reader, RaidChatNotifier.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Couldn't read PB data:");
                e.printStackTrace();
            }
        }

        if (INSTANCE.migratePBKeys()) INSTANCE.save();
    }

    /**
     * Stable PB-key prefix that survives Wynntils renaming the public abbreviation
     * (e.g. TWP -> WTP between 4.1.8 and 4.1.9). Derived from the RaidKind's class
     * simple name with the "Raid" suffix stripped.
     */
    static String stableRaidKey(RaidKind kind) {
        if (kind == null) return "?";
        String simple = kind.getClass().getSimpleName();
        if (simple.endsWith("Raid")) simple = simple.substring(0, simple.length() - 4);
        return simple;
    }

    // Old abbreviation -> stable class-derived key. Both TWP and WTP folded into
    // TheWartornPalace so old PBs survive the Wynntils 4.1.8 -> 4.1.9 rename.
    private static final Map<String, String> RAID_KEY_ALIASES = Map.of(
            "TWP", "TheWartornPalace",
            "WTP", "TheWartornPalace",
            "NOG", "NestOfTheGrootslangs",
            "NOL", "OrphionsNexusOfLight",
            "TCC", "TheCanyonColossus",
            "TNA", "TheNamelessAnomaly"
    );

    /**
     * Rewrites legacy PB keys ({@code <ABBR>_Room} / {@code boss_<ABBR>_idx}) to use the
     * stable raid key. When the new key already holds a PB, the better (lower) time wins
     * so users never lose a PB to a rename. Returns true if any entry was changed.
     */
    boolean migratePBKeys() {
        if (raidPBs == null || raidPBs.isEmpty()) return false;
        Map<String, Long> migrated = new HashMap<>();
        boolean changed = false;
        for (Map.Entry<String, Long> e : raidPBs.entrySet()) {
            String key = e.getKey();
            Long val = e.getValue();
            String rewritten = rewriteLegacyKey(key);
            if (!rewritten.equals(key)) changed = true;
            migrated.merge(rewritten, val, Math::min);
        }
        if (changed) raidPBs = migrated;
        return changed;
    }

    private static String rewriteLegacyKey(String key) {
        if (key.startsWith("boss_")) {
            int second = key.indexOf('_', 5);
            if (second > 5) {
                String abbr = key.substring(5, second);
                String stable = RAID_KEY_ALIASES.get(abbr);
                if (stable != null) return "boss_" + stable + key.substring(second);
            }
            return key;
        }
        int sep = key.indexOf('_');
        if (sep > 0) {
            String abbr = key.substring(0, sep);
            String stable = RAID_KEY_ALIASES.get(abbr);
            if (stable != null) return stable + key.substring(sep);
        }
        return key;
    }
}
