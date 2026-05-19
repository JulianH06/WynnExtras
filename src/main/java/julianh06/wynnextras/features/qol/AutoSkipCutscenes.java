package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.BossBarHudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.UUID;

/**
 * Auto-skips Wynncraft cutscenes that prompt "Swap Hands to skip".
 *
 * Watches the boss-bar HUD for the trigger text and sends a SWAP_ITEM_WITH_OFFHAND
 * action every 4 ticks while it's visible — server interprets it as the player
 * pressing the offhand-swap key, so the cutscene advances without input.
 */
public class AutoSkipCutscenes {
    private static final String TRIGGER = "swap hands to";
    private static final int INTERVAL_TICKS = 4;

    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.autoSkipCutscenesEnabled) { tickCounter = 0; return; }
            if (client == null || client.player == null || client.getNetworkHandler() == null) { tickCounter = 0; return; }
            if (client.inGameHud == null) { tickCounter = 0; return; }

            BossBarHud hud = client.inGameHud.getBossBarHud();
            if (hud == null) { tickCounter = 0; return; }
            Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) hud).getBossBars();
            if (bars == null || bars.isEmpty()) { tickCounter = 0; return; }

            boolean trigger = false;
            for (ClientBossBar bar : bars.values()) {
                Text n = bar.getName();
                if (n == null) continue;
                String s = n.getString();
                if (s != null && s.toLowerCase().contains(TRIGGER)) { trigger = true; break; }
            }
            if (!trigger) { tickCounter = 0; return; }

            if (tickCounter++ % INTERVAL_TICKS == 0) {
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                ));
            }
        });
    }
}
