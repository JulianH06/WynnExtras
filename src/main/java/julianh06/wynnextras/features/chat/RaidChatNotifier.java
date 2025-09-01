package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.core.components.Models;
import julianh06.wynnextras.event.ChatEvent;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class RaidChatNotifier {

    private static final WynnExtrasConfig config = SimpleConfig.getInstance(WynnExtrasConfig.class);


    @SubscribeEvent
    public void handleMessage(ChatEvent event) {
        Text message = event.message;
        if (!config.toggleRaidTimestamps) {
              return; //
        }

        String msg = stripColorCodes(message.getString());

        for (RaidMessageDetector detector : detectors) {
            if (detector.matches(msg)) {
                String timestamp = getCurrentRoomTimestamp();
                String progress = detector.extractProgress(msg);
                String finalMsg = detector.getFormattedMessage(progress, timestamp);
                McUtils.sendMessageToClient(Text.literal(finalMsg));
                return;
            }
        }
    }

    private static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }


    private static String getCurrentRoomTimestamp() {
        if (Models.Raid.getCurrentRaid() == null || Models.Raid.getCurrentRaid().getCurrentRoom() == null) {
            return "??:??.???";
        }

        long time = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
        return formatTime(time);
    }

    private static interface RaidMessageDetector {
        boolean matches(String msg);
        String extractProgress(String msg);
        String getFormattedMessage(String progress, String timestamp);
    }

    private static class SlimeGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Goo to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);

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
            return "§2[WynnExtras] §bAdded Slime " + progress + " §c@ " + timestamp;
        }
    }

    private static class BindingSealDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Binding Seal! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);

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
            return "§2[WynnExtras] §bCompleted Seal " + progress + " §c@ " + timestamp;
        }
    }

    private static class LightGatheringDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("Light Crystals to the tower! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);

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
            return "§2[WynnExtras] §bAdded light " + progress + " §c@ " + timestamp;
        }
    }

    private static class ShadowlingDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("has been killed! \\[(\\d+/\\d+)]", Pattern.CASE_INSENSITIVE);

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
            return "§2[WynnExtras] §bKilled Shadowling " + progress + " §c@ " + timestamp;
        }
    }

    private static class StaticMessageDetector implements RaidMessageDetector {
        private final Pattern pattern;
        private final String formattedMessage;

        public StaticMessageDetector(String regex, String formattedMessage) {
             this.pattern = Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE);
             this.formattedMessage = formattedMessage;
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
            return formattedMessage + timestamp;
        }
    }

    private static String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    private static long lastWatchPhaseTime = -1;

    private static class WatchPhaseDetector implements RaidMessageDetector {
        private static final Pattern PATTERN = Pattern.compile("The Obelisks have appeared; they must be", Pattern.CASE_INSENSITIVE);

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
              return "§2[WynnExtras] §bStarted Watchphase (no raid data)";
            }

            long currentTime = Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime();
            String message;

            if (lastWatchPhaseTime == -1) {
              message = "§2[WynnExtras] §bFirst Watchphase started §c@ " + timestamp;
            } else {

              long duration = currentTime - lastWatchPhaseTime;
              message = "§2[WynnExtras] §bWatchphase took §c" + formatTime(duration) + " §7(" + timestamp + ")";
            }

            lastWatchPhaseTime = currentTime;
            return message;
        }
    }


    private static final List<RaidMessageDetector> detectors = Arrays.asList(
        new SlimeGatheringDetector(),
        new BindingSealDetector(),
        new LightGatheringDetector(),
        new WatchPhaseDetector(),
        new ShadowlingDetector(),

        new StaticMessageDetector(
            "is preparing to descend! [2/2]",
            "§2[WynnExtras] §bDescend §c@ "
        ),
        new StaticMessageDetector(
            "Upper Level must kill the Slime Chomper",
            "§2[WynnExtras] §bSlime Chomper Spawned §c@ "
        ),
        new StaticMessageDetector(
            "A new platform has appeared on the Lower Area!",
            "§2[WynnExtras] §bLower Mini spawned §c@ "
        ),
        new StaticMessageDetector(
                "players on the Upper Level must kill the Carnivorous",
            "§2[WynnExtras] §bCarnivor spawned §c@ "
        ),
        new StaticMessageDetector(
            "players on the Upper Level must kill the Invasive",
            "§2[WynnExtras] §bTarantula spawned §c@ "
        ),
        new StaticMessageDetector(
            "players on the Upper Level must kill the Unfurling",
            "§2[WynnExtras] §bHorsefly spawned §c@ "
        ),
        new StaticMessageDetector(
                "3/3 Clouds Purified",
                "§2[WynnExtras] §bPurified 3/3 clouds in §c@ "
        ),
        new StaticMessageDetector(
                "The Team has reached the Checkpoint! ",
                "§2[WynnExtras] §bReached Checkpoint §c@ "
        ),
        new StaticMessageDetector(
              "[+1 Slimey Goo]",
              "§2[WynnExtras] §bGot 1 Slimey Goo §c@ "
        ),
        new StaticMessageDetector(
               "[+2 Slimey Goo]",
               "§2[WynnExtras] §bGot 2 Slimey Goo §c@ "
        ),
        new StaticMessageDetector(
               "Keep breaking the rocks to get to the end",
               "§2[WynnExtras] §bRock destroyed §c@ "
        ),
        new StaticMessageDetector(
              "100% Rock Destroyed",
              "§2[WynnExtras] §bRock destroyed §c@ "
        ),
        new StaticMessageDetector(
              "+1 [Isoptera Heart]",
              "§2[WynnExtras] §bGot heart §c@ "
        ),
        new StaticMessageDetector(
              "has entered the tree",
              "§2[WynnExtras] §bEntered the Tree §c@ "
        ),
        new StaticMessageDetector(
               "The Void Holes have begun to destabi",
               "§2[WynnExtras] §b[4/5] Void Matters §c@ "
        )
    );
}