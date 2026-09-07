package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class FpsHud {
    public static void register() {
        HudRenderCallback.EVENT.register(FpsHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.fpsHudEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        String text = "FPS: " + client.getCurrentFps();
        Integer override = config.hudColorOverrides.get("fps");
        int color = override != null ? override | 0xFF000000 : 0xFFFFFFFF;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(config.fpsHudX, config.fpsHudY);
        ctx.getMatrices().scale(config.fpsHudScale, config.fpsHudScale);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        ctx.getMatrices().popMatrix();
    }
}
