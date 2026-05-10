package julianh06.wynnextras.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.BossBarHudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

/**
 * Watches the boss-bar HUD for "X/6 Chains" texts and emits a PB-style chat
 * notification the first time each count (0..6) shows up in a raid.
 *
 * Reset via {@link RaidChatNotifier#resetCounters()} on raid start.
 */
public class ChainsAttachedTracker {
    private static final boolean[] fired = new boolean[7];

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ChainsAttachedTracker::tick);
    }

    public static void resetForNewRaid() {
        for (int i = 0; i < fired.length; i++) fired[i] = false;
    }

    private static void tick(MinecraftClient client) {
        if (!WynnExtrasConfig.INSTANCE.toggleRaidTimestamps) return;
        if (client == null || client.player == null || client.inGameHud == null) return;

        BossBarHud hud = client.inGameHud.getBossBarHud();
        if (hud == null) return;
        Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) hud).getBossBars();
        if (bars == null || bars.isEmpty()) return;

        for (ClientBossBar bar : bars.values()) {
            Text n = bar.getName();
            if (n == null) continue;
            String s = n.getString();
            if (s == null) continue;
            String lower = s.toLowerCase();
            if (!lower.contains("chain")) continue;
            for (int i = 0; i <= 6; i++) {
                if (fired[i]) continue;
                if (s.contains(i + "/6")) {
                    fired[i] = true;
                    emit(i);
                    break;
                }
            }
        }
    }

    private static void emit(int count) {
        long currentTime = (Models.Raid.getCurrentRaid() != null && Models.Raid.getCurrentRaid().getCurrentRoom() != null)
                ? Models.Raid.getCurrentRaid().getCurrentRoom().getRoomTotalTime()
                : 0;
        String timestamp = RaidChatNotifier.formatTime(currentTime);
        String pbKey = "chains_" + count + "_of_6";
        String label = (count == 0) ? "Chainphase started" : count + "/6 Chains attached";
        String message = "§b" + label + " §c@ " + timestamp;

        if (currentTime > 0 && Models.Raid.getCurrentRaid() != null) {
            Long pb = RaidChatNotifier.getPB(pbKey);
            if (pb == null || currentTime < pb) {
                RaidChatNotifier.savePB(pbKey, currentTime);
                message += (pb == null ? " §e[First PB]" : " §e[New PB! Old: " + RaidChatNotifier.formatTime(pb) + "]");
            } else {
                message += " §7[PB: " + RaidChatNotifier.formatTime(pb) + "]";
            }
        }

        final String finalMessage = message;
        MinecraftClient.getInstance().execute(() ->
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(finalMessage))));
    }
}
