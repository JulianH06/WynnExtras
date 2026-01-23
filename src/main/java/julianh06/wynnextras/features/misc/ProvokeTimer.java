package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.mc.event.LocalSoundEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.event.api.RenderEvents;
import julianh06.wynnextras.utils.ChatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;

public class ProvokeTimer {
    private static WynnExtrasConfig config;

    private static int storedTicks = -1;
    private static int clientTicks = 0;
    private static int timeToRender = 0;
    private static int calculatedSeconds = 0;
    private static String shownText = null;

    private static boolean zeroMessageSent = false;
    private static int lastSeconds = -1;

    public static void init() {
        config = WynnExtrasConfig.INSTANCE;
        ClientTickEvents.END_CLIENT_TICK.register(ProvokeTimer::provokeTimer);
    }

    public static void provokeTimer(MinecraftClient client) {
        if (client.world == null || client.player == null || !config.provokeTimerToggle) return;
        clientTicks++;

        boolean provokeActive = Models.StatusEffect.getStatusEffects().stream()
                .anyMatch(effect -> effect.getName().getStringWithoutFormatting().equals("Provoke"));

        if (provokeActive && storedTicks == -1) {
            storedTicks = clientTicks;
            zeroMessageSent = false;
            lastSeconds = -1;
        }

        if (!provokeActive && storedTicks != -1) {
            storedTicks = -1;
            calculatedSeconds = 0;
            zeroMessageSent = false;
            lastSeconds = -1;
        }

        if (storedTicks != -1) {
            timeToRender = storedTicks + 160 - clientTicks;

            if (timeToRender >= 0) {
                calculatedSeconds = timeToRender / 20;

                if (calculatedSeconds > 0 && calculatedSeconds != lastSeconds) {

                    ChatUtils.displayTitle("PROVOKE TIME REMAINING: " + calculatedSeconds, "" ,20, 0, 0, config.provokeTimerColor.getFormatting());
//                    McUtils.sendMessageToClient(
//                            Text.literal("Provoke active for: " + calculatedSeconds + " seconds")
//                    );
                    lastSeconds = calculatedSeconds;
                } else if (calculatedSeconds == 0 && !zeroMessageSent) {
                    McUtils.sendMessageToClient(
                            Text.literal("Provoke effect ended.")
                    );
                    ChatUtils.displayTitle("PROVOKE ENDED", "" ,40, 0, 0);
                    zeroMessageSent = true;
                }
            }
        }
    }
}
