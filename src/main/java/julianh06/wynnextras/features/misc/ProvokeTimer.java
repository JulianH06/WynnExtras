package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class ProvokeTimer {
    private static int storedTicks = -1;
    private static int clientTicks = 0;
    private static int timeToRender = 0;
    private static int calculatedSeconds = 0;

    private static boolean zeroMessageSent = false;
    private static int lastSeconds = -1;

    public static boolean isActive() {
        return storedTicks != -1 && calculatedSeconds > 0;
    }

    public static int getSeconds() {
        return calculatedSeconds;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ProvokeTimer::provokeTimer);
        HudRenderCallback.EVENT.register(ProvokeTimer::renderHud);
    }

    public static void provokeTimer(MinecraftClient client) {
        if (client.world == null || client.player == null || !WynnExtrasConfig.INSTANCE.provokeTimerToggle) return;
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
                    lastSeconds = calculatedSeconds;
                } else if (calculatedSeconds == 0 && !zeroMessageSent) {
                    McUtils.sendMessageToClient(
                        WynnExtras.addWynnExtrasPrefix("§6Provoke effect ended.")
                    );
                    zeroMessageSent = true;
                }
            }
        }
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.provokeTimerToggle) return;
        if (!isActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int secs = calculatedSeconds;
        int color;
        if (secs >= 5) color = 0xFF44FF44;
        else if (secs >= 3) color = 0xFFFFFF00;
        else color = 0xFFFF4444;

        String text = "Provoke: " + secs + "s";
        float scale = WynnExtrasConfig.INSTANCE.provokeTimerScale;
        int x = WynnExtrasConfig.INSTANCE.provokeTimerX;
        int y = WynnExtrasConfig.INSTANCE.provokeTimerY;

        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;

        int boxW = (int)((textWidth + 6) * scale);
        int boxH = (int)(14 * scale);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + boxW / 2f, y + boxH / 2f);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, text, -textWidth / 2, -textHeight / 2, color, true);
        ctx.getMatrices().popMatrix();
    }
}
