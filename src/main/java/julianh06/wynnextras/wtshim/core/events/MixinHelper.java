// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.core.events;

import java.util.Locale;
import java.util.regex.Pattern;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import net.minecraft.client.network.ServerInfo;
import net.neoforged.bus.api.Event;

public final class MixinHelper {
    // Simplified vs Wynntils: no ConnectionManager state machine — we check the
    // current server address directly. Same accepted hosts as Wynntils.
    private static final Pattern WYNNCRAFT_SERVER_PATTERN =
            Pattern.compile("^(?:(.*)\\.)?wynncraft\\.(?:com|net|org)(?::\\d+)?$");

    private MixinHelper() {}

    public static boolean onWynncraft() {
        ServerInfo server = McUtils.mc().getCurrentServerEntry();
        if (server == null || server.address == null) return false;
        return WYNNCRAFT_SERVER_PATTERN
                .matcher(server.address.toLowerCase(Locale.ROOT))
                .matches();
    }

    public static void post(Event event) {
        if (!onWynncraft()) return;
        if (McUtils.player() == null) return;

        WynntilsMod.postEvent(event);
    }

    /** Post event without checking if we are connected to a Wynncraft server. */
    public static void postAlways(Event event) {
        WynntilsMod.postEvent(event);
    }
}
