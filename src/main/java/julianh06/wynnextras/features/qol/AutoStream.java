package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.BossBarHudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.hud.ClientBossBar;

public class AutoStream {
    private static long lastObservedStreamerMode = 0;
    private static long lastStreamSent = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.autoStreamEnabled) return;
            if (client.player == null || client.getNetworkHandler() == null) return;

            boolean seenStreamerBar = false;
            for (ClientBossBar bar : ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).getBossBars().values()) {
                String text = bar.getName().getString();
                if (text != null && text.contains("Streamer mode enabled")) {
                    seenStreamerBar = true;
                    lastObservedStreamerMode = System.currentTimeMillis();
                    break;
                }
            }

            if (!seenStreamerBar
                    && System.currentTimeMillis() - lastObservedStreamerMode > 1500
                    && System.currentTimeMillis() - lastStreamSent > 1000) {
                client.getNetworkHandler().sendChatCommand("stream");
                lastStreamSent = System.currentTimeMillis();
            }
        });
    }
}
