package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.BossBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

@WEModule
public final class WarState {
    private static boolean active;
    private static String territory;
    private static long updatedAt;

    public static boolean isActive() { return active; }
    public static Optional<String> territory() { return Optional.ofNullable(territory); }
    public static long updatedAt() { return updatedAt; }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 2 != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        boolean found = false;
        String foundTerritory = null;
        for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
            String text = bar.getName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (!text.contains("Tower")) continue;
            found = true;
            int tower = text.indexOf("Tower");
            int dash = text.indexOf(" - ", tower);
            if (tower >= 0 && dash > tower) foundTerritory = text.substring(tower + "Tower".length(), dash).trim();
            break;
        }
        active = found;
        territory = foundTerritory;
        updatedAt = System.currentTimeMillis();
    }
}
