package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Locale;

public class ServerTpsHud {
    private static final double NANOS_PER_TWENTY_TICKS = 20_000_000_000d;
    private static volatile long previousTimeUpdate;
    private static volatile float serverTps = 20f;

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudRenderCallback.EVENT.register(ServerTpsHud::render);
    }

    public static void onWorldTimeUpdate() {
        long now = System.nanoTime();
        long previous = previousTimeUpdate;
        previousTimeUpdate = now;
        if (previous == 0L) return;

        long elapsed = now - previous;
        if (elapsed > 0L) {
            serverTps = (float) Math.clamp(NANOS_PER_TWENTY_TICKS / elapsed, 0d, 20d);
        }
    }

    private static void reset() {
        previousTimeUpdate = 0L;
        serverTps = 20f;
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.serverTpsEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        String text = String.format(Locale.ROOT, "TPS: %.1f", serverTps);
        Integer override = config.hudColorOverrides.get("serverTps");
        int color = override != null ? override | 0xFF000000 : 0xFFFFFFFF;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(config.serverTpsX, config.serverTpsY);
        ctx.getMatrices().scale(config.serverTpsScale, config.serverTpsScale);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        ctx.getMatrices().popMatrix();
    }
}
