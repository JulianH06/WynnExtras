// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.core.mod.event;

import net.neoforged.bus.api.Event;

public abstract class WynncraftConnectionEvent extends Event {
    private final String host;

    protected WynncraftConnectionEvent(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public static final class Connected extends WynncraftConnectionEvent {
        public Connected(String host) {
            super(host);
        }
    }

    public static final class Disconnected extends WynncraftConnectionEvent {
        public Disconnected(String host) {
            super(host);
        }
    }

    public static final class Connecting extends WynncraftConnectionEvent {
        public Connecting(String host) {
            super(host);
        }
    }

    public static final class ConnectingAborted extends WynncraftConnectionEvent {
        public ConnectingAborted(String host) {
            super(host);
        }
    }
}
