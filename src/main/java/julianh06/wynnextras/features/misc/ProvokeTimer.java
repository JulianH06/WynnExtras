package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.wynncraft.state.StatusEffectState;
import julianh06.wynnextras.utils.MinecraftUtils;
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

        boolean provokeActive = StatusEffectState.hasEffect("Provoke");

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
                    MinecraftUtils.sendMessageToClient(
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
        if (mc.options.hudHidden) return;

        int secs = calculatedSeconds;
        int color;
        if (secs >= 5) color = 0xFF44FF44;
        else if (secs >= 3) color = 0xFFFFFF00;
        else color = 0xFFFF4444;

        String text = "Provoke: " + secs + "s";
        float scale = WynnExtrasConfig.INSTANCE.provokeTimerScale;
        int x = WynnExtrasConfig.INSTANCE.provokeTimerX == -1 ? mc.getWindow().getScaledWidth() / 2 : WynnExtrasConfig.INSTANCE.provokeTimerX;
        int y = WynnExtrasConfig.INSTANCE.provokeTimerY;

        int tw = mc.textRenderer.getWidth(text);
        int th = mc.textRenderer.fontHeight;

        WynnExtrasConfig.Align align = WynnExtrasConfig.INSTANCE.provokeTimerAlignment;

        int previewTw = mc.textRenderer.getWidth("Provoke: 7s");

        int textOffsetX;
        if (align == WynnExtrasConfig.Align.LEFT) {
            textOffsetX = -previewTw / 2;
        } else if (align == WynnExtrasConfig.Align.RIGHT) {
            textOffsetX = previewTw / 2 - tw;
        } else {
            textOffsetX = -tw / 2;
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, text, textOffsetX, -th / 2, color, true);
        ctx.getMatrices().popMatrix();
    }
}
