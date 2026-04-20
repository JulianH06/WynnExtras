package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.statuseffects.type.StatusEffect;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class RadiantHud {

    public record CachedEntry(String display, int color) {}
    private static final List<CachedEntry> cachedEntries = new ArrayList<>();

    public static List<CachedEntry> getCachedEntries() { return cachedEntries; }
    private static int tickCount = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.radiantHudEnabled) return;
            if (client.player == null) return;
            if (++tickCount % 10 != 0) return;

            cachedEntries.clear();
            List<StatusEffect> effects;
            try { effects = Models.StatusEffect.getStatusEffects(); } catch (Exception e) { return; }

            for (StatusEffect effect : effects) {
                String name = effect.getName().getStringWithoutFormatting();
                if (!name.contains("Radiance") && !name.contains("Radiant")) continue;

                String display = effect.asString().getStringWithoutFormatting();
                int duration = effect.getDuration();

                int color;
                if (duration < 0) color = 0xFFFFFF00;
                else if (duration >= 10) color = 0xFF44FF44;
                else if (duration >= 5) color = 0xFFFFFF00;
                else color = 0xFFFF4444;

                cachedEntries.add(new CachedEntry(display, color));
            }
        });
        HudRenderCallback.EVENT.register(RadiantHud::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.radiantHudEnabled) return;
        if (cachedEntries.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        float scale = WynnExtrasConfig.INSTANCE.radiantHudScale;
        int baseX = WynnExtrasConfig.INSTANCE.radiantHudX;
        int baseY = WynnExtrasConfig.INSTANCE.radiantHudY;
        int lineH = 10;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY);
        ctx.getMatrices().scale(scale, scale);

        for (int i = 0; i < cachedEntries.size(); i++) {
            CachedEntry e = cachedEntries.get(i);
            ctx.drawText(mc.textRenderer, e.display, 0, i * lineH, e.color, true);
        }

        ctx.getMatrices().popMatrix();
    }
}
