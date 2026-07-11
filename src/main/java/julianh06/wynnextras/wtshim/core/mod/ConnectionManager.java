// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * DEVIATION vs Wynntils' ConnectionManager: the shim has no raw
 * ConnectionEvent.ConnectingEvent feed (only ConnectedEvent from onGameJoin and a
 * DisconnectedEvent synthesized from Fabric's ClientPlayConnectionEvents.DISCONNECT).
 * We therefore derive the host from the current server entry on ConnectedEvent and drive
 * doConnecting()+doConnected() together, and cannot inspect a disconnect "reason" (the
 * transfer-suppression check is best-effort). This keeps WorldStateModel's
 * WynncraftConnectionEvent handlers byte-identical to upstream.
 */
package julianh06.wynnextras.wtshim.core.mod;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Manager;
import julianh06.wynnextras.wtshim.core.mod.event.WynncraftConnectionEvent;
import julianh06.wynnextras.wtshim.mc.event.ConnectionEvent;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.network.ServerInfo;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ConnectionManager extends Manager {
    private static final String TRANSFER_REASON = "disconnect.transfer";
    private static final Pattern WYNNCRAFT_SERVER_PATTERN =
            Pattern.compile("^(?:(.*)\\.)?wynncraft\\.(?:com|net|org)(?::\\d+)?$");

    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private String connectedHost = null;

    public boolean onServer() {
        return connectionState == ConnectionState.CONNECTED;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onConnected(ConnectionEvent.ConnectedEvent e) {
        ServerInfo server = McUtils.mc().getCurrentServerEntry();
        if (server == null || server.address == null) return;

        String host = server.address.toLowerCase(Locale.ROOT);
        Matcher matcher = WYNNCRAFT_SERVER_PATTERN.matcher(host);
        if (!matcher.matches()) return;

        if (connectionState != ConnectionState.DISCONNECTED) {
            WynntilsMod.error("Got connected event while already connected to server: " + host);
            doDisconnect();
        }

        String rawHostName = matcher.group(1);
        String hostName = (rawHostName == null) ? "play" : rawHostName.toLowerCase(Locale.ROOT);

        doConnecting(hostName);
        doConnected();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDisconnected(ConnectionEvent.DisconnectedEvent e) {
        // If we are transferred to another server, it acts as a disconnect but
        // we should ignore it. (Reason is best-effort in the shim; see class header.)
        if (TRANSFER_REASON.equals(e.getReason())) return;

        doDisconnect();
    }

    private void doConnecting(String hostName) {
        connectionState = ConnectionState.CONNECTING;
        connectedHost = hostName;
        WynntilsMod.postEvent(new WynncraftConnectionEvent.Connecting(connectedHost));
    }

    private void doConnected() {
        connectionState = ConnectionState.CONNECTED;
        WynntilsMod.postEvent(new WynncraftConnectionEvent.Connected(connectedHost));
    }

    private void doDisconnect() {
        ConnectionState oldState = connectionState;
        String oldHostName = connectedHost;
        connectionState = ConnectionState.DISCONNECTED;
        connectedHost = null;
        if (oldState == ConnectionState.CONNECTED) {
            WynntilsMod.postEvent(new WynncraftConnectionEvent.Disconnected(oldHostName));
        } else if (oldState == ConnectionState.CONNECTING) {
            WynntilsMod.postEvent(new WynncraftConnectionEvent.ConnectingAborted(oldHostName));
        }
    }

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED;
    }
}
