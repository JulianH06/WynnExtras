// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class BossHealthUpdateEvent extends Event implements ICancellableEvent {
    private final BossBarS2CPacket packet;
    private final Map<UUID, ClientBossBar> bossEvents;

    public BossHealthUpdateEvent(BossBarS2CPacket packet, Map<UUID, ClientBossBar> bossEvents) {
        this.packet = packet;
        this.bossEvents = bossEvents;
    }

    public BossBarS2CPacket getPacket() {
        return packet;
    }

    public Map<UUID, ClientBossBar> getBossEvents() {
        return bossEvents;
    }
}
