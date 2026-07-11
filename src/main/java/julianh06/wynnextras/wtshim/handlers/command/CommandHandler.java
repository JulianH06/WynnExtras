// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — CommandHandler. Queues chat commands to the server.
 */
package julianh06.wynnextras.wtshim.handlers.command;

import julianh06.wynnextras.wtshim.core.components.Handler;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import net.minecraft.client.network.ClientPlayerEntity;

public class CommandHandler extends Handler {
    public void queueCommand(String cmd) {
        if (cmd == null || cmd.isEmpty()) return;
        ClientPlayerEntity p = McUtils.player();
        if (p == null) return;
        String trimmed = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        p.networkHandler.sendChatCommand(trimmed);
    }
}
