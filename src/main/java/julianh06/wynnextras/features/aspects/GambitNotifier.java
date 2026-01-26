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
 * Gambits reset daily at 00:00 UTC
 */
public class GambitNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(GambitNotifier.class);
    private static boolean hasSent5MinuteWarning = false;
    private static boolean hasSentResetNotification = false;
    private static String lastCheckDate = "";

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
        ZonedDateTime nextReset = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // If it's past midnight today, next reset is tomorrow
        if (now.getHour() > 0 || now.getMinute() > 0) {
            nextReset = nextReset.plusDays(1);
        }

        // Reset notifications when new day starts
        String currentDate = now.toLocalDate().toString();
        if (!currentDate.equals(lastCheckDate)) {
            hasSent5MinuteWarning = false;
            hasSentResetNotification = false;
            lastCheckDate = currentDate;
        }

        // Calculate time until reset
        Duration timeUntilReset = Duration.between(now, nextReset);
        long minutesUntilReset = timeUntilReset.toMinutes();
        long secondsUntilReset = timeUntilReset.getSeconds();

        // Send 5 minute warning
        if (minutesUntilReset == 5 && secondsUntilReset >= 295 && secondsUntilReset <= 300 && !hasSent5MinuteWarning) {
            hasSent5MinuteWarning = true;
            sendGambitWarning();
        }

        // Send reset notification (within 10 seconds of reset)
        if (minutesUntilReset == 0 && secondsUntilReset <= 10 && !hasSentResetNotification) {
            hasSentResetNotification = true;
            sendGambitResetNotification();
        }
    }

    private static void sendGambitWarning() {
        Text message = WynnExtras.addWynnExtrasPrefix(Text.literal("§eDaily gambits reset in §65 minutes§e! Open §6/pf§e to scan new gambits."));
        McUtils.sendMessageToClient(message);
        LOGGER.info("Gambit 5-minute warning sent");
    }

    private static void sendGambitResetNotification() {
        Text message = WynnExtras.addWynnExtrasPrefix(Text.literal("§aDaily gambits have reset! §7Open §6/pf§7 to scan today's gambits."));
        McUtils.sendMessageToClient(message);
        LOGGER.info("Gambit reset notification sent");
    }
}
