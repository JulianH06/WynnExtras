package julianh06.wynnextras.compat.wynntils;

import julianh06.wynnextras.core.WynnExtras;

import java.util.Optional;

/**
 * A fail-closed entry point into an optional Wynntils integration.
 *
 * <p>Capability implementations must not expose Wynntils objects to callers. Once resolving or
 * invoking a capability fails, it remains disabled for the rest of the client session.</p>
 */
public final class WynntilsCapability<T> {
    public enum State {
        UNRESOLVED,
        AVAILABLE,
        UNAVAILABLE,
        BROKEN
    }

    @FunctionalInterface
    public interface Resolver<T> {
        T resolve() throws Throwable;
    }

    @FunctionalInterface
    public interface Invocation<T, R> {
        R invoke(T integration) throws Throwable;
    }

    private final String name;
    private final Resolver<T> resolver;
    private volatile State state = State.UNRESOLVED;
    private T integration;

    public WynntilsCapability(String name, Resolver<T> resolver) {
        this.name = name;
        this.resolver = resolver;
    }

    public State state() {
        resolveIfNeeded();
        return state;
    }

    public boolean isAvailable() {
        return state() == State.AVAILABLE;
    }

    public <R> Optional<R> invoke(Invocation<T, R> invocation) {
        resolveIfNeeded();
        if (state != State.AVAILABLE) return Optional.empty();

        try {
            return Optional.ofNullable(invocation.invoke(integration));
        } catch (Throwable throwable) {
            disable(State.BROKEN, "invocation failed", throwable);
            return Optional.empty();
        }
    }

    public void run(Invocation<T, ?> invocation) {
        invoke(invocation);
    }

    private synchronized void resolveIfNeeded() {
        if (state != State.UNRESOLVED) return;
        if (!WynntilsCompat.isLoaded()) {
            state = State.UNAVAILABLE;
            return;
        }

        try {
            integration = resolver.resolve();
            state = integration == null ? State.UNAVAILABLE : State.AVAILABLE;
        } catch (Throwable throwable) {
            disable(State.BROKEN, "resolution failed", throwable);
        }
    }

    private synchronized void disable(State newState, String reason, Throwable throwable) {
        if (state == State.BROKEN) return;
        integration = null;
        state = newState;
        WynnExtras.LOGGER.warn(
                "Disabling optional Wynntils capability '{}' for version {}: {} ({})",
                name,
                WynntilsCompat.version(),
                reason,
                throwable.getClass().getSimpleName()
        );
        WynnExtras.LOGGER.debug("Wynntils capability '{}' failure", name, throwable);
    }
}
