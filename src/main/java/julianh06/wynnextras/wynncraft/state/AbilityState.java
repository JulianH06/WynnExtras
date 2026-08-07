package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.BossBarUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class AbilityState {
    private static final Pattern BLOOD_POOL = Pattern.compile("(?i)blood\\s+pool\\D*(\\d+)(?:\\s*/\\s*\\d+)?");
    private static int bloodPool = -1;

    public static OptionalInt bloodPoolValue() {
        return bloodPool < 0 ? OptionalInt.empty() : OptionalInt.of(bloodPool);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 2 != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int parsed = -1;
        for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
            Matcher matcher = BLOOD_POOL.matcher(bar.getName().getString().replaceAll("§[0-9a-fk-or]", ""));
            if (matcher.find()) {
                parsed = Integer.parseInt(matcher.group(1));
                break;
            }
        }
        bloodPool = parsed;
    }
}
