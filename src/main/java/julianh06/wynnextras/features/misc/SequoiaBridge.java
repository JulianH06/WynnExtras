package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.lang.reflect.Method;

/**
 * Soft integration with the Sequoia mod.
 * When Sequoia is installed, reads its guild storage tracker each tick and fires
 * WynnExtras notifications when aspects/emeralds cross a "full" threshold.
 * If Sequoia is not installed, this feature is a no-op.
 */
public class SequoiaBridge {
    private static Boolean available = null;
    private static Method getInstance;
    private static Method currentSnapshot;
    private static Method emeralds;
    private static Method aspects;
    private static Method current;
    private static Method max;

    private static boolean wasAspectsFull = false;
    private static boolean wasEmeraldsFull = false;
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.sequoiaBridgeEnabled) return;
            if (client.player == null) return;
            if (++tickCounter % 200 != 0) return;
            if (!isAvailable()) return;
            checkStorage();
        });
    }

    private static boolean isAvailable() {
        if (available != null) return available;
        if (!FabricLoader.getInstance().isModLoaded("seq")) {
            available = false;
            return false;
        }
        try {
            Class<?> tracker = Class.forName("org.sequoia.seq.managers.GuildStorageTracker");
            getInstance = tracker.getMethod("getInstance");

            Class<?> snapshotClass = Class.forName("org.sequoia.seq.managers.GuildStorageTracker$StorageSnapshot");
            currentSnapshot = tracker.getDeclaredMethod("currentSnapshot");
            currentSnapshot.setAccessible(true);

            emeralds = snapshotClass.getDeclaredMethod("emeralds");
            aspects = snapshotClass.getDeclaredMethod("aspects");

            Class<?> resourceClass = Class.forName("org.sequoia.seq.managers.GuildStorageTracker$ResourceSnapshot");
            current = resourceClass.getDeclaredMethod("current");
            max = resourceClass.getDeclaredMethod("max");
            available = true;
        } catch (Exception e) {
            WynnExtras.LOGGER.info("[SequoiaBridge] Sequoia present but API mismatch: {}", e.getMessage());
            available = false;
        }
        return available;
    }

    private static void checkStorage() {
        try {
            Object instance = getInstance.invoke(null);
            Object snapshot = currentSnapshot.invoke(instance);
            if (snapshot == null) return;

            Object emRes = emeralds.invoke(snapshot);
            Object asRes = aspects.invoke(snapshot);
            long emCurrent = (long) current.invoke(emRes);
            long emMax = (long) max.invoke(emRes);
            long asCurrent = (long) current.invoke(asRes);
            long asMax = (long) max.invoke(asRes);

            int threshold = WynnExtrasConfig.INSTANCE.sequoiaFullThresholdPercent;
            boolean emFull = emMax > 0 && (emCurrent * 100L) / emMax >= threshold;
            boolean asFull = asMax > 0 && (asCurrent * 100L) / asMax >= threshold;

            if (asFull && !wasAspectsFull) {
                fire("Aspect storage " + threshold + "% (" + asCurrent + "/" + asMax + ")");
            }
            if (emFull && !wasEmeraldsFull) {
                fire("Emerald storage " + threshold + "% (" + emCurrent + "/" + emMax + ")");
            }
            wasAspectsFull = asFull;
            wasEmeraldsFull = emFull;
        } catch (Exception ignored) {}
    }

    private static void fire(String message) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.literal("§e" + message)));
    }

    public static void invalidate() {
        // Reset when disconnecting/reconnecting so stale "full" state doesn't block new notifications
        wasAspectsFull = false;
        wasEmeraldsFull = false;
    }
}
