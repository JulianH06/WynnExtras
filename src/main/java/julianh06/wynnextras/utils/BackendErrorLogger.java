package julianh06.wynnextras.utils;

import julianh06.wynnextras.core.WynnExtras;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Limits repeated backend errors while keeping periodic failures visible in the log. */
public final class BackendErrorLogger {
    public static final long COOLDOWN_MS = 15 * 60 * 1000L;
    private static final Map<String, ErrorState> errorStates = new ConcurrentHashMap<>();

    private BackendErrorLogger() {}

    public static boolean error(String key, String message) {
        ErrorState state = errorStates.computeIfAbsent(key, ignored -> new ErrorState());
        long now = System.currentTimeMillis();

        synchronized (state) {
            if (now - state.lastLogMillis < COOLDOWN_MS) {
                state.suppressedErrors++;
                return false;
            }

            String suppressedSuffix = state.suppressedErrors == 0
                    ? ""
                    : " (" + state.suppressedErrors + " repeated errors suppressed)";
            state.lastLogMillis = now;
            state.suppressedErrors = 0;
            WynnExtras.LOGGER.error("[WynnExtras] " + message + suppressedSuffix);
            return true;
        }
    }

    private static final class ErrorState {
        private long lastLogMillis;
        private int suppressedErrors;
    }
}