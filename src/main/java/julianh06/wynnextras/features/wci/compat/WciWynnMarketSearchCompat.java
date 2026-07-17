package julianh06.wynnextras.features.wci.compat;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WciWynnMarketSearchCompat {
    private static final String MOD_ID = "wms";
    private static final Object LOCK = new Object();
    private static final SuppressionHandle NO_OP_HANDLE = new NoOpSuppressionHandle();
    private static final ModPresence FABRIC_MOD_PRESENCE =
            () -> FabricLoader.getInstance().isModLoaded(MOD_ID);
    private static final MarketSearchAccessor REFLECTIVE_ACCESSOR = new ReflectiveMarketSearchAccessor();

    private static int activeSuppressions = 0;
    private static Boolean previousMarketSearchValue = null;
    private static boolean reflectionFailureLogged = false;

    private WciWynnMarketSearchCompat() {}

    public static SuppressionHandle suppressDuringWciSearch() {
        return suppressDuringWciSearch(
                WynnExtrasConfig.INSTANCE.wciWynnMarketSearchCompatibility,
                FABRIC_MOD_PRESENCE,
                REFLECTIVE_ACCESSOR);
    }

    static SuppressionHandle suppressDuringWciSearch(boolean compatibilityEnabled,
                                                     ModPresence modPresence,
                                                     MarketSearchAccessor accessor) {
        if (!compatibilityEnabled
                || modPresence == null
                || accessor == null
                || !modPresence.isWynnMarketSearchLoaded()) {
            return NO_OP_HANDLE;
        }

        synchronized (LOCK) {
            try {
                if (activeSuppressions == 0) {
                    previousMarketSearchValue = accessor.marketSearch();
                    accessor.setMarketSearch(false);
                }
                activeSuppressions++;
                return new ActiveSuppressionHandle(accessor);
            } catch (Throwable throwable) {
                activeSuppressions = 0;
                previousMarketSearchValue = null;
                logReflectionFailureOnce(throwable);
                return NO_OP_HANDLE;
            }
        }
    }

    static void resetForTests() {
        synchronized (LOCK) {
            activeSuppressions = 0;
            previousMarketSearchValue = null;
            reflectionFailureLogged = false;
        }
    }

    private static void release(MarketSearchAccessor accessor) {
        synchronized (LOCK) {
            if (activeSuppressions <= 0) {
                activeSuppressions = 0;
                previousMarketSearchValue = null;
                return;
            }

            activeSuppressions--;
            if (activeSuppressions > 0) {
                return;
            }

            Boolean restoreValue = previousMarketSearchValue;
            previousMarketSearchValue = null;
            if (restoreValue == null) {
                return;
            }

            try {
                accessor.setMarketSearch(restoreValue);
            } catch (Throwable throwable) {
                logReflectionFailureOnce(throwable);
            }
        }
    }

    private static void logReflectionFailureOnce(Throwable throwable) {
        if (reflectionFailureLogged) {
            return;
        }
        reflectionFailureLogged = true;
        WynnExtras.LOGGER.warn(
                "WCI: WynnMarketSearch compatibility unavailable; continuing without suppression.",
                throwable);
    }

    public interface SuppressionHandle extends AutoCloseable {
        boolean active();

        @Override
        void close();
    }

    interface ModPresence {
        boolean isWynnMarketSearchLoaded();
    }

    interface MarketSearchAccessor {
        boolean marketSearch() throws ReflectiveOperationException;

        void setMarketSearch(boolean enabled) throws ReflectiveOperationException;
    }

    private static final class NoOpSuppressionHandle implements SuppressionHandle {
        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    private static final class ActiveSuppressionHandle implements SuppressionHandle {
        private final MarketSearchAccessor accessor;
        private boolean closed = false;

        private ActiveSuppressionHandle(MarketSearchAccessor accessor) {
            this.accessor = accessor;
        }

        @Override
        public boolean active() {
            return !closed;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            release(accessor);
        }
    }

    private static final class ReflectiveMarketSearchAccessor implements MarketSearchAccessor {
        private static final String CONFIG_CLASS = "me.a0g.config.ModConfig";
        private static final String GET_METHOD = "get";
        private static final String MARKET_SEARCH_FIELD = "marketSearch";

        @Override
        public boolean marketSearch() throws ReflectiveOperationException {
            return field().getBoolean(config());
        }

        @Override
        public void setMarketSearch(boolean enabled) throws ReflectiveOperationException {
            field().setBoolean(config(), enabled);
        }

        private static Object config() throws ReflectiveOperationException {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Method getMethod = configClass.getMethod(GET_METHOD);
            return getMethod.invoke(null);
        }

        private static Field field() throws ReflectiveOperationException {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getDeclaredField(MARKET_SEARCH_FIELD);
            field.setAccessible(true);
            return field;
        }
    }
}
