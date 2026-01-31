package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;

/**
 * Notifies players about gambit resets
 * Gambits reset daily at 05:00 UTC (midnight EST/America/New_York)
 * Wynncraft uses US Eastern time for daily resets
 */
public class GambitNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(GambitNotifier.class);
    private static boolean hasSent5MinuteWarning = false;
    private static boolean hasSentResetNotification = false;
    private static LocalDate lastResetDate = null;
    private static int debugTickCounter = 0;

    // Wynncraft reset time - 05:00 UTC = midnight EST
    private static final int RESET_HOUR_UTC = 5;
    private static final int RESET_MINUTE_UTC = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Check every 20 ticks (1 second)
            if (client.player.age % 20 == 0) {
                checkGambitReset();
            }
        });
    }

    private static void checkGambitReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        // Calculate next reset time (05:00 UTC = midnight EST)
        ZonedDateTime nextReset = now.withHour(RESET_HOUR_UTC).withMinute(RESET_MINUTE_UTC).withSecond(0).withNano(0);

        // If we're past reset time today, next reset is tomorrow
        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        // Calculate which "gambit day" we're in (resets at 05:00 UTC)
        // If before 05:00 UTC, we're still in "yesterday's" gambit day
        LocalDate currentGambitDay;
        if (now.getHour() < RESET_HOUR_UTC) {
            currentGambitDay = now.toLocalDate().minusDays(1);
        } else {
            currentGambitDay = now.toLocalDate();
        }

        // Reset notifications when new gambit day starts
        if (lastResetDate == null || !currentGambitDay.equals(lastResetDate)) {
            LOGGER.info("New gambit day detected: {} (was {})", currentGambitDay, lastResetDate);
            hasSent5MinuteWarning = false;
            hasSentResetNotification = false;
            lastResetDate = currentGambitDay;
        }

        // Calculate time until reset
        Duration timeUntilReset = Duration.between(now, nextReset);
        long minutesUntilReset = timeUntilReset.toMinutes();
        long secondsUntilReset = timeUntilReset.getSeconds();

        // Debug logging every 60 seconds
        debugTickCounter++;
        if (debugTickCounter >= 60) {
            debugTickCounter = 0;
            LOGGER.debug("Gambit check - UTC time: {}:{}, minutes until reset: {}",
                    now.getHour(), now.getMinute(), minutesUntilReset);
        }

        // Send 5 minute warning (between 4:55 and 5:00 remaining)
        if (minutesUntilReset >= 4 && minutesUntilReset <= 5 && !hasSent5MinuteWarning) {
            hasSent5MinuteWarning = true;
            sendGambitWarning(minutesUntilReset);
        }

        // Send reset notification (within 30 seconds of reset)
        if (minutesUntilReset == 0 && secondsUntilReset <= 30 && !hasSentResetNotification) {
            hasSentResetNotification = true;
            sendGambitResetNotification();
        }
    }

    private static void sendGambitWarning(long minutes) {
        Text message = WynnExtras.addWynnExtrasPrefix(Text.literal("§eDaily gambits reset in §6~" + minutes + " minutes§e! Open §6/pf§e to scan new gambits."));
        McUtils.sendMessageToClient(message);
        LOGGER.info("Gambit warning sent - {} minutes until reset", minutes);
    }

    private static void sendGambitResetNotification() {
        Text message = WynnExtras.addWynnExtrasPrefix(Text.literal("§aDaily gambits have reset! §7Open §6/pf§7 to scan today's gambits."));
        McUtils.sendMessageToClient(message);
        LOGGER.info("Gambit reset notification sent");
    }
}
