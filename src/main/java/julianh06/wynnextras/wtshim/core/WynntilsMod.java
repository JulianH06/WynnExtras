// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — WynntilsMod main-class stand-in.
 * Minimal logging/identity helpers; the big init surface isn't needed.
 */
package julianh06.wynnextras.wtshim.core;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.wtshim.core.components.Managers;
import julianh06.wynnextras.wtshim.core.events.EventBusWrapper;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WynntilsMod {
    public static final String MOD_ID = "wynnextras";
    /** Plain Gson for the net stack (urls.json). Wynntils' GSON has custom adapters we don't need here. */
    public static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final IEventBus eventBus = EventBusWrapper.createEventBus();

    private WynntilsMod() {}

    /**
     * Reads a bundled mod resource from the wtshim resource root.
     * DEVIATION: Wynntils resolves against {@code /assets/wynntils/}; the shim uses
     * {@code /assets/wynnextras/wtshim/} so the bundled files don't collide with WynnExtras' own assets.
     */
    public static InputStream getModResourceAsStream(String name) {
        return WynntilsMod.class.getClassLoader().getResourceAsStream("assets/wynnextras/wtshim/" + name);
    }

    /**
     * Returns (creating if needed) a persistent storage directory for the net stack.
     * Layout: {@code {gameDir}/wynnextras/<dirName>} (mirrors Wynntils' {gameDir}/wynntils/<dirName>).
     */
    public static File getModStorageDir(String dirName) {
        File dir = FabricLoader.getInstance()
                .getGameDir()
                .resolve(MOD_ID)
                .resolve(dirName)
                .toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.warn("Failed to create storage dir {}", dir);
        }
        return dir;
    }

    /** Best-effort mod version string for the HTTP User-Agent. */
    public static String getVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    /** Minecraft version, read via the loader (avoids Yarn SharedConstants API-name uncertainty). */
    public static String getMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static Logger getLogger() { return LOGGER; }

    public static void registerEventListener(Object object) {
        eventBus.register(object);
    }

    public static void unregisterEventListener(Object object) {
        eventBus.unregister(object);
    }

    public static <T extends Event> void registerListener(Consumer<T> eventConsumer) {
        eventBus.addListener(eventConsumer);
    }

    /** Posts the event on the shim bus; returns true if the event was canceled. */
    public static <T extends Event> boolean postEvent(T event) {
        recordEventForDebug(event);
        try {
            eventBus.post(event);
            return event instanceof ICancellableEvent cancellableEvent && cancellableEvent.isCanceled();
        } catch (Throwable t) {
            LOGGER.error("Exception in event listener for " + event.getClass().getName(), t);
            return false;
        }
    }

    // --- /we compatdebug support: ring buffer of recently posted events ---
    private static final int DEBUG_RING_SIZE = 60;
    private static final java.util.ArrayDeque<String> recentEvents = new java.util.ArrayDeque<>(DEBUG_RING_SIZE);

    private static void recordEventForDebug(Event event) {
        String cls = event.getClass().getName();
        // Tick events fire 20/s and would flush everything else out of the ring
        if (cls.endsWith("TickEvent") || cls.endsWith("TickAlwaysEvent")) return;
        String name = cls.substring(cls.lastIndexOf('.') + 1).replace('$', '.');
        synchronized (recentEvents) {
            if (recentEvents.size() >= DEBUG_RING_SIZE) recentEvents.removeFirst();
            recentEvents.addLast(java.time.LocalTime.now().toString().substring(0, 8) + " " + name);
        }
    }

    public static java.util.List<String> getRecentEventsForDebug() {
        synchronized (recentEvents) {
            return new java.util.ArrayList<>(recentEvents);
        }
    }

    public static void postEventOnMainThread(Event event) {
        Managers.TickScheduler.scheduleNextTick(() -> postEvent(event));
    }

    public static void info(String msg) { LOGGER.info(msg); }
    public static void info(String msg, Object... args) { LOGGER.info(msg, args); }
    public static void warn(String msg) { LOGGER.warn(msg); }
    public static void warn(String msg, Object... args) { LOGGER.warn(msg, args); }
    public static void error(String msg) { LOGGER.error(msg); }
    public static void error(String msg, Throwable t) { LOGGER.error(msg, t); }
    public static void error(String msg, Object... args) { LOGGER.error(msg, args); }

    public static boolean isDevelopmentEnvironment() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
