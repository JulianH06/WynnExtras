package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.statuseffects.type.StatusEffect;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class RadiantHud {

    public static void init() {
        HudRenderCallback.EVENT.register(RadiantHud::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.radiantHudEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<StatusEffect> effects = Models.StatusEffect.getStatusEffects();
        if (effects.isEmpty()) return;

        float scale = WynnExtrasConfig.INSTANCE.radiantHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.radiantHudX;
        int baseY = WynnExtrasConfig.INSTANCE.radiantHudY;
        int lineH = 10;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY);
        ctx.getMatrices().scale(scale, scale);

        int i = 0;
        for (StatusEffect effect : effects) {
            String name = effect.getName().getStringWithoutFormatting();
            if (!name.contains("Radiance") && !name.contains("Radiant")) continue;

            String display = effect.asString().getStringWithoutFormatting();
            int duration = effect.getDuration();

            int color;
            if (duration < 0) {
                color = 0xFFFFFF00;
            } else if (duration >= 10) {
                color = 0xFF44FF44;
            } else if (duration >= 5) {
                color = 0xFFFFFF00;
            } else {
                color = 0xFFFF4444;
            }

            ctx.drawText(mc.textRenderer, display, 0, i * lineH, color, true);
            i++;
        }

        ctx.getMatrices().popMatrix();
    }
}
